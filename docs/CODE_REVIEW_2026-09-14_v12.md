# Отчёт о код-ревью: 14 сентября 2026 (проход 33, v12)

Срез: `master` (`6c5174f`)
Фокус прохода: мультилинзовое ревью по направлениям **Correctness**, **Concurrency / Lifecycle**, **UI/UX**, **Architecture / Performance**.

---

## Сводка находок

| ID | Линза | Серьёзность | Статус | Суть |
| --- | --- | --- | --- | --- |
| **T38** | Concurrency / Network Cancellation | Высокая | **закрыт** | В `DefaultHttpClient` создаваемый OkHttp `Call` не сохранялся в экземпляре, из-за чего `close()` не мог отменить активный сетевой вызов при прерывании задачи. |
| **C72** | Correctness / Network Redirection | Высокая | **закрыт** | Пустой заголовок `Location` создавал бесконечный цикл редиректов; `DownloadTask` не обновлял ссылку `httpClient` при редиректах, ломая отмену задачи. |
| **C73** | Correctness / Data Parsing | Средняя | **закрыт** | В `parserItemVideoTags` теги не дедуплицировались и не очищались от пробелов, а разбор имён каналов и актрис не учитывал вложенные теги. |
| **A15** | Performance / Redundant Parsing | Средняя | **закрыт** | Двойной последовательный разбор HTML страницы дашборда X через `Jsoup.parse()` заменён на однократный разбор DOM дерева в пуле `Dispatchers.Default`. |
| **UI35** | UI / UX Navigation | Средняя | **закрыт** | На экране ошибки загрузки альбома в `ScreenAlbum` отсутствовала кнопка «Назад», блокируя пользователя внутри экрана ошибки. |
| **T39** | Lifecycle / Tag Tracking | Низкая | **закрыт** | В `Downloader` запрос на скачивание превью `requestImage` не имел тега `item.id`, из-за чего не отменялся при вызове `kDownloader.cancel(tag)`. |

---

## Подробное описание и решения

### T38 — Прерывание OkHttp Call при отмене задачи в `DefaultHttpClient`
- **Файл:** [DefaultHttpClient.kt](../core/src/main/java/com/client/xvideos/common/kdownloader/httpclient/DefaultHttpClient.kt#L14-L75)
- **Проблема:** Метод `connect(req)` создавал и сразу блокирующе выполнял `client.newCall(builder.build()).execute()`. Ссылка на экземпляр `Call` не сохранялась. При отмене корутины вызов `close()` пытался закрыть `bodyStream` и `response`, которые во время DNS-запроса, TCP-соединения или SSL-рукопожатия ещё были `null`. В результате поток оставался заблокированным вплоть до таймаута сокета (до 20 сек).
- **Решение:** Добавлено поле `private var call: Call? = null`. Перед вызовом `execute()` сохраняется ссылка на `newCall`, а в методе `close()` вызывается `runCatching { call?.cancel() }`, мгновенно прерывая сетевой вызов.

### C72 — Защита от зацикливания редиректов и сохранение управляемости отмены в `Utils.kt` и `DownloadTask`
- **Файл:** [Utils.kt](../core/src/main/java/com/client/xvideos/common/kdownloader/utils/Utils.kt#L55-L83), [DownloadTask.kt](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt#L145-L155)
- **Проблема:** 
  1. В `getRedirectedConnectionIfAny` заголовок `Location` проверялся только на `null`. Если сервер отдавал пустую строку `Location: ""`, вызов `URI(req.url).resolve("").toString()` возвращал исходный URL и цикл редиректа повторялся до достижения `MAX_REDIRECTION`.
  2. При создании новых экземпляров `DefaultHttpClient` внутри цикла редиректов ссылка `DownloadTask.httpClient` оставалась указывающей на старый закрытый клиент. При отмене задачи `cancelHandler` вызывал `close()` на старом клиенте, не затрагивая активное редирект-соединение.
- **Решение:** 
  1. Добавлена проверка `if (location.isNullOrBlank()) throw IOException(...)`.
  2. В `getRedirectedConnectionIfAny` добавлен коллбэк `onNewClient: (HttpClient) -> Unit = {}`, передаваемый из `DownloadTask`, что гарантирует актуальность `this@DownloadTask.httpClient` на каждом шаге редиректа.

### C73 — Дедупликация и очистка тегов и имён в `parserItemVideoTags`
- **Файл:** [parserItemVideoTags.kt](../feature-x/src/main/java/com/client/xvideos/x/parcer/parserItemVideoTags.kt#L10-L35)
- **Проблема:** `tags` извлекались через `.map { it.text() }` без удаления краевых пробелов, пустых значений и дубликатов, что приводило к дублированию чипов в оверлее плеера. При разборе имён каналов и актрис `ownText()` давал пустоту, если текст был обёрнут во вложенные теги разметки.
- **Решение:** Добавлена нормализация тегов `.map { it.text().trim() }.filter { it.isNotEmpty() }.distinct()`. Для каналов и моделей реализован fallback `ownText().trim().takeIf { it.isNotEmpty() } ?: text().trim()`, а карточки с пустыми именами исключены из результата.

### A15 — Однократный разбор DOM дерева в дашбордах X
- **Файл:** [parserListVideo.kt](../feature-x/src/main/java/com/client/xvideos/x/parcer/parserListVideo.kt#L13-L30), [DashboardsPaginatedListScreen.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/DashboardsPaginatedListScreen.kt#L60-L75)
- **Проблема:** В `openNew` HTML разметка загружалась и затем парсилась через `Jsoup.parse()` дважды подряд: сначала в `parseSiteCountryFlag(html)`, затем в `parserListVideo(html)`. Это создавало двойную нагрузку на CPU и лишние аллокации памяти при загрузке каждой страницы дашборда.
- **Решение:** Добавлены перегрузки с готовым объектом `Document`: `parseSiteCountryFlag(document)` и `parserListVideo(document)`. Вся обработка переведена на однократный вызов `Jsoup.parse(html)` в пуле `Dispatchers.Default`.

### UI35 — Кнопка «Назад» на экране ошибки загрузки альбома Luscious
- **Файл:** [ScreenAlbum.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenAlbum.kt#L265-L280)
- **Проблема:** При сетевой ошибке загрузки альбома в `ScreenAlbum` отображалась только кнопка «Повторить». Пользователь без системного жеста «Назад» оказывался заперт на экране с ошибкой.
- **Решение:** Рядом с кнопкой «Повторить» добавлена экранная кнопка «Назад» (`navigator.pop()`).

### T39 — Добавление тега в запросы загрузки preview изображений
- **Файл:** [Downloader.kt](../feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt#L84-L90), [Downloader.kt](../feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt#L160-L168)
- **Проблема:** В `downloadRedName` и `downloadMissingFiles` запрос к видео помечался `.tag(item.id)`, а сопутствующий запрос на скачивание превью `requestImage` формировался без тега. При отмене загрузки `kDownloader.cancel(item.id)` скачивание изображения оставалось активным.
- **Решение:** К запросам превью добавлен `.tag(item.id)`.

---

## Верификация
1. `./gradlew detekt` — 0 ошибок и предупреждений во всех модулях проекта (`app`, `core`, `feature-l`, `feature-r`, `feature-x`).
2. `./gradlew testDebugUnitTest` — 100% тестов пройдены успешно (140 задач, включая новые тесты в `KDownloaderQueueTest` и `XParsersTest`).
3. Код и логика P2P (`core/.../p2p/`) не затрагивались.
