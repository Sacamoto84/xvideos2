# Отчёт о код-ревью: 14 сентября 2026 (проход 32, v11)

Срез: `master` (`35324da`)
Фокус прохода: мультилинзовое ревью по направлениям **Correctness**, **Concurrency / Lifecycle**, **UI/UX**, **Architecture / Performance**.

---

## Сводка находок

| ID | Линза | Серьёзность | Статус | Суть |
| --- | --- | --- | --- | --- |
| **C69** | Correctness / Concurrency | Высокая | **закрыт** | `DownloadTask` игнорировал `Status.PAUSED` при старте задачи после ожидания пермита семафора, переводя задачу в `RUNNING` и начиная скачивание по сети. |
| **C70** | Correctness / State Machine | Высокая | **закрыт** | Невалидированный вызов `resume(id)` в `DownloadRequestQueue` запускал дублирующуюся параллельную корутину загрузки в тот же файл; `pause(id)` не фильтровал терминальные состояния. |
| **C71** | Correctness / Null-Safety | Средняя | **закрыт** | Чтение nullable-колонок SQLite cursor в `AppDbHelper` без `.orEmpty()` приводило к риску `NullPointerException` в Kotlin non-null свойствах `DownloadModel`. |
| **UI32** | UI / UX | Средняя | **закрыт** | Экран `ScreenTags` не имел кнопки «Назад» (`navigator.pop()`), отступов и тёмного стиля в `topBar`, запирая пользователя на экране тега. |
| **UI33** | UI / UX | Средняя | **закрыт** | В `RSearchField` коллбэк `onUndoClick` обновлял только поле ввода `searchText`, не синхронизируя `searchTextDone`, что рассинхронизировало UI и результаты выдачи. |
| **UI34** | UI / UX | Низкая | **закрыт** | Экран ошибки загрузки видео в `ScreenX_VideoPlayer` содержал только кнопку «Повторить» без кнопки «Назад», блокируя выход с экрана при сбоях сети. |
| **A14** | Architecture / Defensive Input | Низкая | **закрыт** | Переход в `ScreenX_VideoPlayerSM.openTag()` не валидировал пустые и состоящие из пробелов теги. |

---

## Подробное описание и решения

### C69 — Игнорирование статуса `PAUSED` при старте задачи в `DownloadTask`
- **Файл:** [DownloadTask.kt](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt#L133-L160), [DownloadTask.kt](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt#L335-L350)
- **Проблема:** Если запрос был поставлен пользователем на паузу во время ожидания в очереди семафора параллельных загрузок (`downloadSemaphore(4)`), после получения пермита `DownloadTask.run()` проверял только `Status.CANCELLED`. Статус `Status.PAUSED` игнорировался, перезаписывался на `Status.RUNNING`, вызывался `listener.onStart()` и инициировалось сетевое скачивание. Кроме того, при возникновении `IOException` во время паузы блок `catch (e: Exception)` безусловно удалял временный файл `.temp` и очищал модель из БД.
- **Решение:** 
  1. Добавлена проверка `if (req.status == Status.PAUSED) { listener.onPause(); return@withContext }` перед стартом скачивания и после подтверждения соединения.
  2. В блоке `catch (e: Exception)` проверяется `val wasPaused = req.status == Status.PAUSED`: временный файл и запись в БД сохраняются, а вызывается `listener.onPause()`.

### C70 — Защита от дублирующихся параллельных загрузок в `DownloadRequestQueue`
- **Файл:** [DownloadRequestQueue.kt](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadRequestQueue.kt#L104-L122), [KDownloaderQueueTest.kt](../core/src/test/java/com/client/xvideos/common/kdownloader/KDownloaderQueueTest.kt)
- **Проблема:** Метод `resume(id)` безусловно устанавливал `req.status = Status.QUEUED` и вызывал `downloader.enqueue(req)`. При повторном нажатии пользователем кнопки возобновления на уже выполняющейся (`RUNNING`) или стоящей в очереди (`QUEUED`) задаче запускалась вторая параллельная корутина, читавшая сеть и писавшая в тот же самый временный файл, что приводило к повреждению скачиваемого медиафайла. Также метод `pause(id)` не имел фильтрации терминальных состояний (`CANCELLED`, `COMPLETED`).
- **Решение:** 
  1. В `resume(id)` введён строгий гард `if (req.status != Status.PAUSED) return`.
  2. В `pause(id)` разрешена пауза только активных задач: `if (req.status != Status.RUNNING && req.status != Status.QUEUED) return`.
  3. Добавлены unit-тесты переходов состояний очереди в `KDownloaderQueueTest`.

### C71 — Null-safety при чтении строк из SQLite Cursor в `AppDbHelper`
- **Файл:** [AppDbHelper.kt](../core/src/main/java/com/client/xvideos/common/kdownloader/database/AppDbHelper.kt#L33-L44), [AppDbHelper.kt](../core/src/main/java/com/client/xvideos/common/kdownloader/database/AppDbHelper.kt#L128-L140)
- **Проблема:** Метод `cursor.getString(...)` возвращает `@Nullable String?`, однако в `find()` и `getUnwantedModels()` возвращаемые значения напрямую присваивались в non-null Kotlin-поля `DownloadModel` (`url`, `eTag`, `dirPath`, `fileName`). При наличии NULL в базе это приводило либо к немедленному `NullPointerException`, либо к скрытому хранению `null` в non-null полях с падением в нижележащей логике.
- **Решение:** К результатам вызовов `cursor.getString(...)` добавлен `.orEmpty()`.

### UI32 — Стилизация `topBar` и кнопка «Назад» в `ScreenTags`
- **Файл:** [ScreenTags.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/tags/ScreenTags.kt#L45-L85)
- **Проблема:** Экран выдачи по тегу `ScreenTags` имел сырой `topBar`: два нестилизованных текстовых элемента без отступов, без фона и без кнопки навигации «Назад» (`navigator.pop()`). Пользователь, перешедший из плеера по тегу, не имел возможности вернуться обратно внутри приложения.
- **Решение:** Панель оформлена в едином тёмном стиле приложения (`Theme.L.grey6`), добавлен `IconButton` с `Icons.AutoMirrored.Filled.ArrowBack` для возврата по стеку навигации, заданы отступы, жирный шрифт заголовка с ограничением в 1 строку и правильные цвета подзаголовков. Для Scaffold задан `containerColor = Theme.L.grey6`.

### UI33 — Синхронизация фильтрации при Undo в `RSearchField`
- **Файл:** [RSearchField.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/search/RSearchField.kt#L66-L73)
- **Проблема:** При нажатии кнопки отката поискового запроса (`onUndoClick`) восстанавливалось значение поля ввода `search.searchText.value`, но не обновлялось состояние применённого фильтра `search.searchTextDone.value`. В результате поле отображало восстановленный запрос, а результаты выдачи продолжали соответствовать предыдущему фильтру.
- **Решение:** В обработчик `onUndoClick` добавлено обновление `search.searchTextDone.value = last`.

### UI34 — Кнопка «Назад» на экране ошибки плеера `ScreenX_VideoPlayer`
- **Файл:** [ScreenX_VideoPlayer.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayer.kt#L52-L65)
- **Проблема:** При сетевой ошибке загрузки видео `ScreenX_VideoPlayer` предлагал только кнопку «Повторить». В отличие от полноэкранного плеера `ScreenX_VideoPlayerFullScreen`, где предусмотрена кнопка возврата, на обычном экране плеера пользователь оказывался заблокирован при неработающей сети, если системный жест «Назад» недоступен.
- **Решение:** На экран ошибки добавлена кнопка «Назад» (`Button(onClick = { navigator.pop() }) { Text("Назад") }`) рядом с кнопкой «Повторить».

### A14 — Защита от пустых тегов в `ScreenX_VideoPlayerSM.openTag`
- **Файл:** [ScreenX_VideoPlayerSM.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt#L144-L151)
- **Проблема:** Метод `openTag(tag, navigator)` открывал `ScreenTags(tag)` без предварительной валидации строки тега, допуская сетевой запрос к URL вида `.../tags//0`.
- **Решение:** Добавлен гард `if (tag.isNotBlank()) navigator.push(ScreenTags(tag.trim()))`.

---

## Верификация
1. `./gradlew detekt` — 0 ошибок и предупреждений во всех модулях проекта (`app`, `core`, `feature-l`, `feature-r`, `feature-x`).
2. `./gradlew testDebugUnitTest` — 100% тестов пройдены успешно (140 тестов, включая новые тесты в `KDownloaderQueueTest`).
3. Код и логика P2P (`core/.../p2p/`) не затрагивались.
