# Код-ревью xvideos — проход 50

> **Срез:** `efd4240` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `efd4240`. Изменено 10 файлов в модулях `app`, `core`, `feature-l`, `feature-r`, `feature-x`.
Линзы:
- `C` / жизненный цикл кэша плеера X: сохранение в RAM-кэш только страниц с валидным видеопотоком, гарантированная очистка битых записей и параметр `forceReload` при повторных попытках в `ScreenX_VideoPlayerSM` и `ScreenX_VideoPlayerFullScreenSM`.
- `UI` / UX фонового восстановления: подавление `SnackBar.error` при ошибках скачивания видео в `Downloader.downloadMissingFiles`, если `showSnackBarErrors = false`.
- `C` / синхронизация состояния после бэкапа: вызов `data.downloadRed?.refreshDownloadList()` в `BackupSettingsSection` для немедленного обновления списков загрузок R без перезапуска приложения.
- `S` / надежность дескрипторов: безопасное закрытие `rawOutput` при сбое инициализации шифрованного потока в `XlrBackupManager.createBackup`.
- `C` / целостность файловой системы: атомарная запись `writeTextAtomically` метаданных альбома в `LAlbumExporter` для защиты от 0-байтовых файлов при экспорте в outbox.
- `UI` / устойчивость URL: санитизация спецсимволов путей и запросов (`#`, `?`, `&`, `/`, `\`) в `ScreenTagsViewModel`.
Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

## Находки

### C118 — Застревание плеера X в состоянии ошибки из-за преждевременного RAM-кэширования и отсутствие обхода кэша на повторе. Высокая.

[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt:100-145](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt#L100)
[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt:70-112](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt#L70)

В `ScreenX_VideoPlayerSM` и `ScreenX_VideoPlayerFullScreenSM` ответ `readHtmlFromURLDirect(url)` немедленно записывался в `db.cacheUrlStringRam.put(url, content)` ещё до разбора DOM и проверки наличия видеопотока (`hls.isNotBlank()`). Если сервер отдавал страницу проверки/ошибки, токен истёк или структура HTML изменилась, в кэш попадала страница без рабочего потока. Метод выставлял `isError = true`, пользователь нажимал «Повторить», однако метод читал `db.cacheUrlStringRam.get(url)`, возвращавший ту же самую невалидную страницу без выполнения сетевого запроса. Пользователь оказывался навсегда заблокирован в состоянии ошибки до перезапуска процесса.
**Исправление:** В RAM-кэш сохраняются только страницы с непустым потоком (`hls.isNotBlank()`). При отсутствии потока или ошибке запроса/парсинга ключ принудительно удаляется из кэша (`db.cacheUrlStringRam.delete(url)`). Метод `loadVideo(forceReload = true)` поддержал принудительную очистку кэша перед повторным сетевым запросом при нажатии кнопки «Повторить» в обычном и полноэкранном плеерах.

### UI57 — Спам всплывающими ошибками при фоновом восстановлении загрузок в `Downloader.downloadMissingFiles`. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt:198-204](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt#L198)

Метод `downloadMissingFiles` принимает параметр `showSnackBarErrors: Boolean = true`, который при фоновом восстановлении (`downloadMissingFilesForRecovery`) равен `false`, чтобы пакетная докачка файлов не мешала пользователю. При ошибке загрузки превью проверка `if (showSnackBarErrors)` выполнялась, однако в колбэке `onError` загрузки видео вызов `SnackBar.error("Ошибка закачки: $it")` вызывался безусловно. Из-за этого при сбоях сети во время фонового восстановления бэкапа экран засорялся ошибками.
**Исправление:** Вызов `SnackBar.error` в блоке `onError` обёрнут в проверку `if (showSnackBarErrors)`.

### C119 — Рассинхронизация списков загрузок R после восстановления бэкапа в `BackupSettingsSection`. Средняя.

[app/src/main/java/com/client/xvideos/screenSettings/backup/BackupSettingsSection.kt:379-383](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/screenSettings/backup/BackupSettingsSection.kt#L379)

После завершения `XlrBackupManager.restoreBackup` вызывались `data.savedRed?.refreshAll()` и `data.blockRed?.refresh()`, но вызов обновления загрузок `data.downloadRed?.refreshDownloadList()` отсутствовал. Если бэкап восстанавливал сохранённые ролики R (`R/Download`), но автоматическое восстановление отсутствующих файлов не требовалось, синглтон `DownloadRed` сохранял устаревшие in-memory потоки `_downloadList` и `_downloadedVideoKeys`, и раздел оставался пустым до перезапуска приложения.
**Исправление:** В блок синхронизации данных после успешного восстановления добавлен вызов `data.downloadRed?.refreshDownloadList()`.

### S5 — Утечка системного дескриптора при сбое инициализации шифрования в `XlrBackupManager.createBackup`. Средняя.

[core/src/main/java/com/client/xvideos/common/backup/XlrBackupManager.kt:156-168](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/backup/XlrBackupManager.kt#L156)

В методе создания бэкапа `rawOutput = context.contentResolver.openOutputStream(uri, "wt")` открывал дескриптор, после чего выполнялась инициализация потоков `BufferedOutputStream` и `XlrEncryptedOutputStream` (выполняющего деривацию PBKDF2). В случае ошибки при деривации ключа или сбое закрытия `ZipOutputStream` (например, ошибка в `finish()` при нехватке места) системный дескриптор мог остаться открытым.
**Исправление:** Инициализация потоков обёрнута в верхнеуровневый блок `rawOutput.use`, гарантированно закрывающий `rawOutput` при любом исключении, а также устранено избыточное двойное буферирование.

### C120 — Неатомарная запись метаданных альбома L в `LAlbumExporter` с риском 0-байтовых файлов. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/featured/saved/LAlbumExporter.kt:33](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/LAlbumExporter.kt#L33)

При экспорте альбома L в папку outbox выполнялась запись через `outFile.writeText(AppJson.encodeToString(album), Charsets.UTF_8)`. При сбое питания или завершении процесса ОС посреди записи на диске мог остаться повреждённый или 0-байтовый `.album`-файл, который при последующих вызовах воспринимался как готовый (`savedFile.exists() && savedFile.length() > 0L`), ломая передачу альбома.
**Исправление:** Запись переведена на стандарт проекта `outFile.writeTextAtomically(...)`.

### UI58 — Искажение URL страниц тегов при наличии спецсимволов запросов в `ScreenTagsViewModel`. Низкая.

[feature-x/src/main/java/com/client/xvideos/x/screens/tags/ScreenTagsViewModel.kt:70-77](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/tags/ScreenTagsViewModel.kt#L70)

В `ScreenTagsViewModel.loadPage` выполнялась только замена пробелов на дефисы: `tag.trim().replace(Regex("\\s+"), "-")`. Если тег содержал спецсимволы путей и URL (`#`, `?`, `&`, `/`, `\`), они не экранировались: символ `#` отсекал фрагмент пути с номером страницы `$index`, превращая запрос в вечный цикл нулевой страницы, а `?` искажал URL query-параметрами. Кроме того, теги, состоящие исключительно из спецсимволов, порождали пустой путь `.../tags//0`.
**Исправление:** Добавлена санитизация недопустимых символов `Regex("[#?&/\\\\]+")` с заменой на дефис и очисткой концевых дефисов, а также ранний guard `if (formattedTag.isEmpty()) throw IOException(...)`. Поведение покрыто тестами в `X_TagUrlSanitizationTest`.

### C121 — Зависание записей IS_PENDING в MediaStore при ошибке финализации в `GallerySaver`. Низкая.

[core/src/main/java/com/client/xvideos/common/gallery/GallerySaver.kt:173-180](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/gallery/GallerySaver.kt#L173)

В `GallerySaver.publish` вызов `resolver.update(uri, ... IS_PENDING = 0)` находился вне блока `try-catch`. При ошибке обновления в системном `MediaProvider` блок `catch` с `resolver.delete(uri, ...)` не срабатывал, оставляя висящую скрытую запись в MediaStore.
**Исправление:** Вызов `resolver.update` внесён внутрь блока `try` с гарантированным удалением при любом сбое.

### UI59 — Частые рекомпозиции панели управления полноэкранного плеера R и Compose Stability. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreenSM.kt:28-95](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreenSM.kt#L28)
[feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/bottom_bar/FeedControls_Container_Line0.kt:40-160](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/bottom_bar/FeedControls_Container_Line0.kt#L40)

`FeedControls_Container_Line0` принимал `ScreenRedFullScreenSM` и читал/мутировал свойства плеера напрямую. При обновлении `currentPlayerTime` (20 раз в секунду) рекомпоновалась вся нижняя панель кнопок.
**Исправление:** Мутации инкапсулированы в методы `ScreenRedFullScreenSM` (`setTimeA`, `setTimeB`, `toggleAB`, `togglePlay`, `rewind`, `forward`, `toggleMute`), на класс добавлена аннотация `@Stable`, а `FeedControls_Container_Line0` разделена на stateless-компонент с примитивными параметрами (`timeA: Float`, `timeB: Float`...) и тонкую обёртку, что обеспечило smart skipping кнопок при воспроизведении.

## Статус

| Находка | Класс | Статус | Описание / Исправление |
| --- | --- | --- | --- |
| C118 | Feature-X / Player | Исправлен | Инвалидация RAM-кэша при ошибках плеера и forceReload при повторных попытках |
| UI57 | Feature-R / Downloader | Исправлен | Учёт `showSnackBarErrors` в `onError` загрузки видео при фоновом восстановлении |
| C119 | App / Backup | Исправлен | Синхронизация `data.downloadRed?.refreshDownloadList()` после восстановления бэкапа |
| S5 | Core / Backup | Исправлен | Безопасное закрытие `rawOutput` через `rawOutput.use` при создании бэкапа |
| C120 | Feature-L / P2P | Исправлен | Атомарная запись метаданных `.album` через `writeTextAtomically` в `LAlbumExporter` |
| UI58 | Feature-X / Tags | Исправлен | Санитизация спецсимволов URL (`#`, `?`, `&`, `/`, `\`) и guard в `ScreenTagsViewModel` |
| C121 | Core / Gallery | Исправлен | Защита от зависания `IS_PENDING` при сбое обновления MediaStore в `GallerySaver` |
| UI59 | Feature-R / Fullscreen | Исправлен | Compose Stability, инкапсуляция UDF и изоляция рекомпозиций `FeedControls_Container_Line0` |

## Проверка

- `./gradlew testDebugUnitTest`: **все 140+ тестов успешно выполнены** во всех 5 модулях (`:app`, `:core`, `:feature-l`, `:feature-r`, `:feature-x`).
- `./gradlew detekt`: **зелёный**, нарушений правил detekt нет.
- Ручная верификация:
  1. Плеер X: повторная попытка при ошибке гарантированно очищает RAM-кэш и выполняет свежий сетевой запрос.
  2. Восстановление R: фоновая докачка недостающих файлов не выводит всплывающие ошибки пользователю; список загрузок R обновляется сразу после отката/восстановления.
  3. Бэкап: системный дескриптор `openOutputStream` безопасно закрывается через `use`.
  4. Теги X: теги со спецсимволами (`#`, `?`, `&`, `/`, `\`) формируют валидный URL без обрезания пейджинга.
  5. Галерея: сбой финализации MediaStore гарантированно подчищает запись.
  6. Полноэкранный плеер R: контролы `FeedControls_Container_Line0` пропускают рекомпозицию при обновлении времени таймлайна.

## Что проверено и оказалось в порядке

- `CalculatorState`: сохранение отрицательных чисел, ввод после `-0`, форматирование дробей и повторяющиеся операции по `=`.
- `AppLockScreen`: обработка блокировки по таймауту, корректность возвращения фокуса на `ON_RESUME` и отсутствие гонок при двойном сабмите.
- `GallerySaver`: нормализация имён файлов через `cleanFileName`, валидация схем URL, маршрутизация MediaStore (Movies/Pictures) и защита от 0-байтовых файлов.
- `MediaDownloadWorker`: отмена задач WorkManager, очистка временных `.part`-файлов и обработка HTTP 416.
