# Код-ревью xvideos — проход 37

> **Срез:** `a47bbf1` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `a47bbf12e2e6739b9afba200becf86ed227e1b96`. Изменено 6 файлов в модулях `feature-r` и `feature-x`.
Линзы: корректность данных и защита от повреждения файлов (`C`), устойчивость сетевого и медийного трактов (`C`), отказоустойчивость WebView (`T`/`C`). Граница P2P (`core/.../p2p/`) заморожена по решению владельца и не затрагивалась.

## Находки

### C79 — `Downloader.findVideoInDownload` и `downloadMissingFiles` блокируют повторную загрузку и восстановление при наличии 0-байтовых файлов. Высокая.

[feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt:255](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt#L255), [Downloader.kt:159, 180](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt#L159)

1. Метод `findVideoInDownload` проверял исключительно `file.exists()`. Если закачка ролика была аварийно прервана (сбой процесса, отключение батареи), на диске оставался пустой файл `$id.mp4` размером 0 байт. `findVideoInDownload` возвращал `true`, из-за чего `downloadRedName` пропускал загрузку, а `ensureDownloaded` отдавал коллбэк готовности на воспроизведение пустого файла. Пользователь навсегда терял возможность скачать ролик.
2. В методе `downloadMissingFiles` проверка файлов шла как `!previewFile.exists()` и `!videoFile.exists()`. Битая 0-байтовая запись на диске считалась «присутствующей», и механизм восстановления повреждённых загрузок её игнорировал.
**Исправление:**
- В `findVideoInDownload` добавлено требование `file.exists() && file.length() > 0L`.
- В `downloadMissingFiles` неполными считаются файлы `!file.exists() || file.length() == 0L`.

### C80 — Публикация 0-байтовых файлов в системную галерею и ложные ключи в `DownloadRed`. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt:127](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt#L127), [DownloadRed.kt:196, 308](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt#L196)

1. В `saveToGallery` проверка `if (local.exists())` без проверки `local.length() > 0L` отправляла в системный `MediaStore` повреждённые нулевые файлы, захламляя галерею устройства.
2. В `scanIncompleteDownloadsInternal` проверка на отсутствие видео `!File(parent, "$id.mp4").exists()` пропускала 0-байтовые файлы, не ставя их в очередь восстановления.
3. В `refreshDownloadList` во flow `_downloadedVideoKeys` попадали ключи файлов с нулевой длиной.
**Исправление:**
- Проверка `saveToGallery` дополнена `local.length() > 0L`.
- `scanIncompleteDownloadsInternal` считает ролик отсутствующим, если `!it.exists() || it.length() == 0L`.
- В `_downloadedVideoKeys` фильтруются только валидные видео: `it.extension == "mp4" && it.length() > 0L`.

### C81 — Перманентная блокировка скачивания «Уже сохранено» и экспорт пустышек в `SavedX_Downloads`. Высокая.

[feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt:158](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt#L158), [SavedX_Downloads.kt:194](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt#L194)

1. В `refresh()` идентификаторы скачанных видео `videoIds` и постеров `posterIds` извлекались из `allFiles.filter { it.isFile && it.extension == "mp4" }` без проверки размера.
2. При сбое загрузки пустой `.mp4` попадал в `_downloadedVideoIds`. При повторной попытке метод `download(item)` вызывал `if (contains(item.id))`, выводил «Уже сохранено» и завершал работу, не позволяя перекачать файл.
3. В `saveToGallery` проверялся только `local.exists()`.
**Исправление:**
- `refresh()` фильтрует файлы с обязательным условием `it.length() > 0L`.
- `saveToGallery` копирует файл в галерею только при `local.exists() && local.length() > 0L`.

### C82 — Ложный отказ воспроизведения (`isError = true`) в видеоплеере X при отсутствии HLS-потока. Высокая.

[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt:123](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt#L123), [feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt:90](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt#L90)

В обоих экранах плеера X (`ScreenX_VideoPlayerSM` и `ScreenX_VideoPlayerFullScreenSM`) адрес воспроизведения извлекался строго из `config?.videoHLS`. У ряда старых или низкоразрешённых видео на платформе xvideos отсутствует HLS master-плейлист, однако присутствуют прямые mp4-потоки `videoUrlHigh` и `videoUrlLow`. Из-за отсутствия fallback переменная `passedHLS` оставалась пустой, выставлялся флаг `isError = true` и пользователю демонстрировался экран ошибки при наличии доступного видеопотока.
**Исправление:** Реализован каскадный fallback: `videoHLS -> videoUrlHigh -> videoUrlLow`. Видео корректно воспроизводятся плеером.

### C83 — 45-секундное зависание `readHtmlFromURLWebView` при сетевых ошибках из-за отсутствия `onReceivedError`. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/feature/net/readHtmlFromURLWebView.kt:98](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/net/readHtmlFromURLWebView.kt#L98)

В асинхронном headless-WebView клиент `webViewClient` переопределял только `onPageFinished`. При сетевых сбоях (DNS error, connection reset, SSL handshake failure) `onPageFinished` в Android WebView часто не вызывается. Корутина блокировалась на полный таймаут 45 секунд (`WEB_VIEW_LOAD_TIMEOUT_MS`).
**Исправление:** Переопределены методы `onReceivedError` (для legacy API и API 23+ с проверкой `isForMainFrame`), мгновенно возвращающие `""` и освобождающие WebView без ожидания 45-секундного таймаута.

## Статус

| Находка | Класс | Статус | Описание / Исправление |
| --- | --- | --- | --- |
| C79 | корректность | закрыт | `Downloader`: 0-байтовые файлы не блокируют закачку и не игнорируются при recovery |
| C80 | корректность | закрыт | `DownloadRed`: защита галереи, recovery и ключей от 0-байтовых файлов |
| C81 | корректность | закрыт | `SavedX_Downloads`: исключение 0-байтовых пустышек из `downloadedVideoIds` и галереи |
| C82 | корректность | закрыт | Плееры X: каскадный fallback `videoHLS -> videoUrlHigh -> videoUrlLow` |
| C83 | корректность | закрыт | `readHtmlFromURLWebView`: мгновенный failover по `onReceivedError` вместо зависания на 45 сек |

## Проверка

```
> Task :feature-r:compileDebugKotlin SUCCESS
> Task :feature-x:compileDebugKotlin SUCCESS
> Task :feature-r:detekt SUCCESS
> Task :feature-x:detekt SUCCESS
> Task :feature-r:testDebugUnitTest SUCCESS
> Task :feature-x:testDebugUnitTest SUCCESS
BUILD SUCCESSFUL in 12s
```

## Что осталось открытым

- Все открытые ранее проектные решения владельца (включая заморозку P2P) сохраняются в силе.
- Все находки прохода 37 закрыты полностью.
