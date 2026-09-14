# Отчёт о код-ревью: 14 сентября 2026 (проход 31, v10)

Срез: `master` (`a8a85c4`)
Фокус прохода: мультилинзовое ревью по направлениям **Correctness**, **Concurrency / Lifecycle**, **UI/UX**, **Architecture / Performance**.

---

## Сводка находок

| ID | Линза | Серьёзность | Статус | Суть |
| --- | --- | --- | --- | --- |
| **C67** | Correctness / Storage | Высокая | **закрыт** | `DownloadTask.createAndInsertNewModel()` не сохранял `dirPath` и `fileName`, ломая фоновую очистку `.temp`-файлов в `DownloadDispatchers.cleanup()`. |
| **T36** | Concurrency / Lifecycle | Высокая | **закрыт** | `DownloadDispatchers.cancelAll()` синхронно сбрасывал дочерние задачи `scope`, отменяя запланированные коллбэки слушателей `onError("Cancelled")` на `Dispatchers.Main`. |
| **T37** | Concurrency / Network I/O | Средняя | **закрыт** | Блокирующее чтение сокета в `DownloadTask` не прерывалось при отмене задачи, задерживая освобождение семафора загрузок до истечения таймаута сокета. |
| **UI30** | UI / UX | Средняя | **закрыт** | Плашки каналов (`mainUploader`) и моделей (`pornstars`) в оверлее плеера `ComposeTags` не реагировали на нажатия. |
| **UI31** | UI / UX | Низкая | **закрыт** | Кнопка «Создать» в `DaialogNewCollection` была активна при пустом вводе и бесшумно закрывала диалог без создания коллекции. |
| **A13** | Architecture / Performance | Низкая | **закрыт** | Лишний `withContext(Dispatchers.Main)` и избыточное многострочное логирование всех тегов в `AlbumList.getAlbumListAggregationsImpl`. |

---

## Подробное описание и решения

### C67 — Утечка временных файлов на диске из-за потери `dirPath` и `fileName` в `KDownloader`
- **Файл:** [DownloadTask.kt](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt#L69-L84), [KDownloaderQueueTest.kt](../core/src/test/java/com/client/xvideos/common/kdownloader/KDownloaderQueueTest.kt)
- **Проблема:** Метод `createAndInsertNewModel()` в `DownloadTask` создавал `DownloadModel`, не указывая `dirPath` и `fileName`. Поля сохранялись в SQLite как пустые строки. При последующей фоновой очистке `DownloadDispatchers.cleanup(days)` вызывался `getTempPath("", "")` -> `"/.temp"`, из-за чего реальные временные файлы незавершённых загрузок на диске никогда не удалялись, расходуя дисковое пространство.
- **Решение:** В `createAndInsertNewModel()` теперь явно передаются `dirPath = req.dirPath`, `fileName = req.fileName`, `downloadedBytes = req.downloadedBytes`, `lastModifiedAt = System.currentTimeMillis()`. Добавлен юнит-тест корректности формирования пути очистки по модели.

### T36 — Потеря нотификаций слушателей при `DownloadDispatchers.cancelAll()`
- **Файл:** [DownloadDispatchers.kt](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadDispatchers.kt#L23-L27), [DownloadDispatchers.kt](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadDispatchers.kt#L60-L70)
- **Проблема:** `executeOnMainThread` запускал корутины обратного вызова слушателей через `scope.launch(Dispatchers.Main)`. При вызове `cancelAll()` сразу после прохода по очереди выполнялось `scope.coroutineContext.cancelChildren()`, отменявшее ещё не успевшие выполниться на главном потоке коллбэки `onError("Cancelled")`.
- **Решение:** Введён независимый `callbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)` для доставки событий слушателям. Вызов `scope.coroutineContext.cancelChildren()` отменяет только воркеры сетевых загрузок, не сбивая нотификации UI.

### T37 — Непрерываемое блокирующее чтение сети в `DownloadTask` при отмене
- **Файл:** [DownloadTask.kt](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt#L127-L135), [DownloadTask.kt](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt#L310-L330)
- **Проблема:** При отмене задачи корутина могла оставаться заблокированной на синхронном чтении сокета `inputStream.read()` вплоть до истечения таймаута (до 20 сек), удерживая слот параллельных загрузок в `downloadSemaphore`. Кроме того, принудительный разрыв соединения генерировал сетевой `IOException`, который ошибочно переводил статус задачи в `Status.FAILED` вместо `Status.CANCELLED`.
- **Решение:** Зарегистрирован `cancelHandler = req.job?.invokeOnCompletion { runCatching { httpClient?.close() } }`, гарантирующий немедленный разрыв сокета при отмене. Обработчики ошибок в `DownloadTask` проверяют флаг отмены задачи и сохраняют статус `Status.CANCELLED`.

### UI30 — Интерактивность чипов каналов и актрис в оверлее плеера
- **Файл:** [ScreenItemTagsModelPornostars.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/atom/ScreenItemTagsModelPornostars.kt#L24-L38), [ComposeTags.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/atom/ComposeTags.kt#L33-L44)
- **Проблема:** Плашки каналов (`mainUploader`) и моделей (`pornstars`) в плеере `ScreenX_VideoPlayer` отображались с бейджами и счетчиками как интерактивные теги, но не имели обработчика нажатия.
- **Решение:** В `ScreenItemTagsModelPornostars` добавлен опциональный параметр `onClick: (() -> Unit)? = null` с модификатором `.clickable`. В `ComposeTags` подключен вызов `onClick = { onClick(it.name) }`, позволяющий переходить к выдаче видео по каналу/актрисе.

### UI31 — Защита от создания пустых коллекций в `DaialogNewCollection`
- **Файл:** [DaialogNewCollection.kt](../core/src/main/java/com/client/xvideos/common/collectionDB/ui/DaialogNewCollection.kt#L70-L80)
- **Проблема:** В диалоге создания коллекции кнопка «Создать» была включена даже при пустом поле ввода. При нажатии диалог закрывался без создания коллекции и без объяснения пользователю.
- **Решение:** Передан `confirmEnabled = text.isNotBlank()` в `LavenderDialog`, кнопка блокируется до ввода осмысленного названия, а введенный текст очищается от краевых пробелов (`text.trim()`).

### A13 — Удаление лишнего переключения на Main поток в `AlbumList`
- **Файл:** [AlbumList.kt](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumList.kt#L126-L135)
- **Проблема:** В `getAlbumListAggregationsImpl` парсинг тегов содержал `withContext(Dispatchers.Main)` для локального списка и тяжелый лог всех тегов через перенос строки `\n`.
- **Решение:** Удален `withContext(Dispatchers.Main)`, список наполняется в фоновом потоке аналогично другим полям агрегаций, удалены неиспользуемые импорты корутин.

---

## Верификация
1. `./gradlew detekt` — 0 ошибок во всех модулях проекта (`app`, `core`, `feature-l`, `feature-r`, `feature-x`).
2. `./gradlew testDebugUnitTest` — 100% тестов прошли успешно (140 задач, включая новый тест в `KDownloaderQueueTest`).
3. Границы `core/.../p2p/` не затронуты.
