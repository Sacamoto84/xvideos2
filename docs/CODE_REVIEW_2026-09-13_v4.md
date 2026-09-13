# Код-ревью xvideos — проход 19

> **Срез:** `master` · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода — `master` (после прохода 18).
Линзы:
- **Целостность данных при сетевых сбоях и защита от сохранения недокачанных файлов** (`DownloadTask`, `MediaDownloadWorker`, `LMediaPersist`);
- **Конкурентность, дедупликация и предотвращение утечек слушателей очереди фоновой загрузки** (`DownloadRequestQueue`, `DownloadDispatchers`);
- **Потокобезопасность, предотвращение блокировок Main UI Thread и корректность Compose State** (`R_Saved_Collection`, `LinkCollectionStore`, `SavedRed`);
- **Корректность идентификации и удаления медиаресурсов без исходных URL** (`ExpandMenuVM`, `L_ScreenSavedLikesTab`);
- **Защита файловой системы от Path Traversal при операциях с коллекциями и восстановлением медиа** (`SavedL_Collection`, `LDownloadRecovery`);
- **Пользовательский интерфейс и отзывчивость поиска** (`SearchTab`).

Ключевой итог: **все 6 находок 19-го прохода устранены**. Репозиторий полностью собирается, detekt чист (0 замечаний), все unit-тесты успешно проходят.

---

## Находки

### C36 — Повреждение данных: преждевременное завершение сетевого потока и сохранение недокачанных файлов как завершённых. Высокая.

[DownloadTask.kt:257-268](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt#L257-L268), [MediaDownloadWorker.kt:321-326](../core/src/main/java/com/client/xvideos/common/download/work/MediaDownloadWorker.kt#L321-L326), [LMediaPersist.kt:193-200](../feature-l/src/main/java/com/client/xvideos/l/featured/saved/LMediaPersist.kt#L193-L200)

В трёх различных механизмах загрузки медиафайлов присутствовал критический дефект обработки сетевых обрывов:
1. В `DownloadTask.kt`: если сервер преждевременно разрывал соединение и `inputStream.read(...)` возвращал `-1`, цикл завершался штатным `break`. Без проверки `req.downloadedBytes < totalBytes` недокачанный `.temp` файл переименовывался в целевой `.mp4`, вызывался `listener.onCompleted()` и статус запроса переводился в `Status.COMPLETED`. Пользователь получал битый видеофайл, который больше не докачивался.
2. В `MediaDownloadWorker.kt`: метод `copyStreamWithProgress` при выходе из цикла чтения не проверял соответствие фактически прочитанных байтов (`downloadedSoFar`) ожидаемому размеру контента (`totalBytes`). В результате `doWork` переходил к `finalizeDownloadedFile`, переименовывал временный файл и рапортовал об успешном завершении `Result.success(...)`.
3. В `LMediaPersist.kt`: функция `lDownloadToFile` после чтения потока безусловно переименовывала `.part` во временный файл. В сочетании с проверкой `if (file.exists() && file.length() > 0L) return` в начале метода, недокачанный файл навсегда оставался в хранилище как валидный.

*Исправление:*
- Во всех трёх загрузчиках добавлена строгая проверка: если `totalBytes > 0` и `downloadedBytes < totalBytes`, генерируется `IOException("Download incomplete...")`, файл не переименовывается в целевой, а операция переходит в обработку ошибок (с возможностью последующей докачки, если поддерживается `Range`).

---

### T24 — Состояние гонки и потеря слушателей при отмене и параллельном скачивании в KDownloader. Средняя.

[DownloadRequestQueue.kt:58-65](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadRequestQueue.kt#L58-L65), [DownloadDispatchers.kt:67-85](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadDispatchers.kt#L67-L85)

1. **Гонка параллельных задач:** в `DownloadRequestQueue.enqueue(request)` при повторном добавлении запроса с тем же идентификатором (вычисленным из URL и пути назначения) запись в `idRequestMap` перезаписывалась и запускалась вторая параллельная корутина. Оба потока начинали одновременно писать в один и тот же файл `$fileName.temp`, разрушая структуру байтов.
2. **Утечка слушателей:** при вызове `cancel(req)` для запроса в статусе `QUEUED` флаг `wasPaused` был ложным, из-за чего блок `req.listener?.onError("Cancelled")` не вызывался. Поскольку корутина задачи отменялась до вызова `DownloadTask.run`, слушатели (включая ожидающие продолжения в `KDownloader`) навсегда зависали.

*Исправление:*
- В `DownloadRequestQueue.enqueue` добавлена дедупликация: если запрос с таким `downloadId` уже находится в очереди (`QUEUED`) или выполняется (`RUNNING`), возвращается существующий `downloadId` без порождения дублирующих конкурирующих задач.
- В `DownloadDispatchers.cancel` для запросов в очереди (`QUEUED` или ещё не запущенных) обеспечен вызов `onError("Cancelled")` на главном потоке и очистка ссылки на слушатель.
- Добавлен юнит-тест в `KDownloaderQueueTest`.

---

### T25 — Блокировка Main UI Thread и фоновая мутация Compose State в коллекциях R. Средняя.

[R_Saved_Collection.kt:32-95](../feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Collection.kt#L32-L95), [LinkCollectionStore.kt:47-65](../core/src/main/java/com/client/xvideos/common/collectionDB/model/LinkCollectionStore.kt#L47-L65), [SavedRed.kt:22](../feature-r/src/main/java/com/client/xvideos/r/common/saved/SavedRed.kt#L22)

1. В `R_Saved_Collection.kt` операции работы с файловой БД (`insert`, `deleteItem`, `deleteCollection`, `create`, `readAllCollections`) выполнялись синхронно на потоке вызова. При кликах в UI (`R_DialogCollection`, экраны коллекций) полный обход директорий и парсинг файлов JSON блокировал главный поток интерфейса (ANR).
2. В `SavedRed.init` вызов `collections.refreshCollectionList()` запускался на `Dispatchers.IO`, откуда метод `publish` вызывал `collectionList.replaceWith(items)`. Мутация `SnapshotStateList` из фонового пула нарушает правила потокобезопасности снапшотов Compose.

*Исправление:*
- Все операции дискового ввода-вывода в `R_Saved_Collection` перенесены на пул `Dispatchers.IO`.
- Публикация результатов в Compose-список `collectionList` переведена строго на `Dispatchers.Main` (с защитой через `Looper.getMainLooper()` в `LinkCollectionStore.publish`).
- В `SavedRed` в конструктор `R_Saved_Collection` передан жизненный цикл `ApplicationScope`.

---

### C37 — Невозможность удаления медиафайлов Luscious из сохранённых лайков при отсутствии `url_to_original`. Средняя.

[ExpandMenuVM.kt:100-104](../feature-l/src/main/java/com/client/xvideos/l/ui/element/expandMenu/ExpandMenuVM.kt#L100-L104), [L_ScreenSavedLikesTab.kt:186-193](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/likes/L_ScreenSavedLikesTab.kt#L186-L193)

Для видеофайлов и анимаций Luscious поле `url_to_original` часто равно `null`, а медиаресурс сохраняется по ссылке `url_to_video` или `lDownloadUrl()`.
В методах удаления:
- В `ExpandMenuVM.kt`: `item.url_to_original?.let { url -> saved.likes.remove(url) }` молча ничего не делал;
- В `L_ScreenSavedLikesTab.kt`: `val url = item.url_to_original ?: return` мгновенно прерывал удаление.
В итоге элементы оставались в избранных лайках без возможности их удаления пользователем.

*Исправление:*
- Использована унифицированная цепочка поиска URL: `item.url_to_original ?: item.url_to_video ?: item.lDownloadUrl()`.
- Добавлен юнит-тест в `LPureFunctionsTest`.

---

### S10 — Уязвимость Path Traversal в операциях с коллекциями L и восстановлении медиа. Средняя.

[SavedL_Collection.kt:178-360](../feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt#L178-L360), [LDownloadRecovery.kt:105-155](../feature-l/src/main/java/com/client/xvideos/l/featured/saved/LDownloadRecovery.kt#L105-L155)

1. В то время как методы создания/удаления коллекций валидировали имя через `CollectionName.normalizeOrNull`, методы `refresh`, `refreshDuplicates`, `addAll`, `removeAll`, `setManualCover`, `removeDuplicateItems` напрямую конструировали путь `File(AppPath.l_collection, collectionName)`.
2. В `LDownloadRecovery.kt` пути для восстановления медиа и превью создавались на основе имён файлов из метаданных без проверки выхода за пределы папки (`metadata.mediaFileName`, `preview.fileName`), что открывало возможность перезаписи произвольных файлов на устройстве при повреждённых или недоверенных метаданных.

*Исправление:*
- Во все методы `SavedL_Collection` встроена обязательная нормализация имени коллекции через `CollectionName.normalizeOrNull`.
- В `LDownloadRecovery` добавлена проверка выхода за пределы папки (`lIsInside`), а также отклонение имён файлов с символами обхода каталогов (`..`, `/`, `\`).

---

### UI12 / C38 — Спам уведомлениями SnackBar, гонка поисковых запросов и риск падения LazyColumn в поиске R. Средняя.

[SearchTab.kt:97-103, 173-195](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/search/SearchTab.kt#L97-L103)

1. В `SearchTab.kt` на каждое изменение поисковой строки вызывался `SnackBar.info(text)`, заваливая пользователя бесконечными всплывающими плашками при вводе текста.
2. Поток `searchText` опрашивался через обычный `collect` без задержки (`debounce`). Быстрый ввод приводил к вееру одновременных сетевых запросов, и более старый ответ мог перезаписать более актуальный.
3. В `LazyColumn` ключом элемента выступал `it.text`, что приводило к фатальному `IllegalArgumentException` при получении авторов с одинаковым или пустым текстом.

*Исправление:*
- Удалён отладочный вызов `SnackBar.info`.
- Добавлен оператор `.debounce(300)` с переходом на `.collectLatest`.
- Список переведён на `itemsIndexed` с составным уникальным ключом `"${item.text}_${item.name}_$index"`.

---

## Статус

| Находка | Класс | Статус | Коммит |
| --- | --- | --- | --- |
| C36 | корректность | закрыт | рабочее дерево (`master`) |
| T24 | конкурентность | закрыт | рабочее дерево (`master`) |
| T25 | конкурентность | закрыт | рабочее дерево (`master`) |
| C37 | корректность | закрыт | рабочее дерево (`master`) |
| S10 | безопасность | закрыт | рабочее дерево (`master`) |
| UI12 / C38 | интерфейс / корректность | закрыт | рабочее дерево (`master`) |

---

## Проверка

1. **Компиляция и Unit-тесты:**
```
./gradlew testDebugUnitTest --no-daemon
BUILD SUCCESSFUL
```
Все тесты (150+) проходят без единой ошибки.

2. **Статический анализ Detekt:**
```
./gradlew detekt --no-daemon
BUILD SUCCESSFUL (0 issues found)
```
Правила code health соблюдены в полном объёме.

---

## Что осталось открытым

Все выявленные дефекты 19-го прохода устранены.
Транспортный слой P2P (`core/.../p2p/`) сохранён замороженным в строгом соответствии с решением владельца от 11.09.2026.
