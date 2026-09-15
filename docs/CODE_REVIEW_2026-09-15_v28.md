# Код-ревью xvideos — проход 49

> **Срез:** `7d24e97` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `7d24e97`. Изменено 7 файлов в модулях `app`, `core`, `feature-l`, `feature-r`, `feature-x`.
Линзы:
- `UI` / корректность ввода: устранение артефакта ведущего нуля при вводе цифры после `-0` в `CalculatorState`.
- `T` / жизненный цикл загрузок: гарантированная отмена активных задач `KDownloader` при удалении элементов в `SavedX_Downloads` и `DownloadRed`.
- `T` / UI-потокобезопасность: перенос мутации Compose-состояния `currentCollectionName` на `Dispatchers.Main` и добавление fallback при ошибке атомарного переименования папки коллекции в `SavedL_Collection`.
- `C` / синхронизация состояния: делегирование очистки загрузок R из экрана настроек `AppSettingsScreen` в `DownloadRed.deleteAll` с отменой сетевых задач и обновлением StateFlow.
- `C` / файловые пути и MediaStore: очистка концевых пробелов `cleanFileName` в `GallerySaver` и `GalleryTarget` для предотвращения ложной классификации видео как изображений.
- `C` / надежность парсинга: пропуск карточек с пустыми или некорректными ссылками (`No link`) в `parserListVideo`.
Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

## Находки

### UI56 — Артефакт ведущего нуля при вводе цифры после «-0» в `CalculatorState`. Средняя.

[app/src/main/java/com/client/xvideos/calculator/CalculatorState.kt:60-65](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/calculator/CalculatorState.kt#L60)

Если `displayValue` переходит в состояние `"-0"` (например, пользователь ввёл отрицательную дробь `-0.5` и дважды нажал `Backspace`, либо стёр единственный символ после знака минус), последующее нажатие цифры (например, `7`) не сбрасывало `"-0"`. Условие `else if (displayValue == "0")` возвращало `false` из-за знака минус, и метод переходил в общий `else`, выполняя `displayValue += digit`.
В результате на экране появлялось значение `"-07"` вместо `"-7"` (или `"-00"` при вводе нуля).
**Исправление:** Добавлена явная ветка `else if (displayValue == "-0") { displayValue = if (digit == "0") "-0" else "-$digit" }`. Поведение покрыто тестами в [CalculatorStateTest.kt](file:///g:/xvideos2/app/src/test/java/com/client/xvideos/calculator/CalculatorStateTest.kt).

### T18 — Зомби-загрузка и воскрешение удалённых видео в `SavedX_Downloads.delete`. Высокая.

[feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt:102-115, 174-185](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt#L174)

При постановке на скачивание запрос регистрируется в KDownloader с тегом `item.id.toString()`. При вызове `delete(item)` производилось только удаление локальных файлов с диска (`.mp4`, `.jpg`, `.info`). Активная задача KDownloader не отменялась. Если скачивание продолжалось в фоне, процесс завершал запись видеофайла, после чего его колбэк `onCompleted` записывал `.info` и вызывал `refresh()`, возвращая удалённое видео обратно в список сохранённых и расходуя трафик и батарею. Кроме того, фоновая загрузка превью-изображения не имела тега и не подлежала отмене.
**Исправление:** В `download()` добавлен тег `item.id.toString()` для запроса превью, а в `delete()` добавлен вызов `kDownloader.cancel(item.id.toString())`.

### T19 — Утечка сетевых задач при удалении роликов и очистке кеша в `DownloadRed`. Высокая.

[feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt:240-260](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt#L240)

Аналогично разделу X, в `DownloadRed.delete(item)` и `deleteAll()` удалялись папки и файлы на диске, но не отменялись связанные задачи загрузчика `downloader.kDownloader.cancel(item.id)` и `cancelAll()`. Активные фоновые загрузки продолжали писать в удалённые каталоги и по завершении генерировали `.info`-файлы, воскрешая удалённые гифки.
**Исправление:** В `DownloadRed.delete` добавлен вызов `downloader.kDownloader.cancel(item.id)`, а в `DownloadRed.deleteAll` — `downloader.kDownloader.cancelAll()`.

### C115 — Рассинхронизация in-memory списков при очистке загрузок R в `AppSettingsScreen`. Средняя.

[app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt:167-178](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt#L167)

Кнопка «Очистить Download» на экране настроек напрямую удаляла файлы на диске через `File(AppPath.r_cache_download).deleteRecursively()`, минуя инжектированный синглтон `vm.downloadRed`. В результате реактивные потоки `_downloadList` и `_downloadedVideoKeys` оставались заполненными устаревшими объектами до перезапуска приложения, а фоновые загрузки KDownloader не прерывались.
**Исправление:** Обработчик `onClearDownload` переведён на вызов `vm.downloadRed.deleteAll { ... }`, обеспечивающий отмену сетевых задач, очистку диска и гарантированное обновление StateFlow.

### T20 — Фоновая мутация Compose-состояния и отсутствие fallback при переименовании в `SavedL_Collection`. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt:154-170](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt#L154)

`ScreenSavedCollectionSM` вызывает `renameCollection` внутри корутины на `Dispatchers.IO`. Метод `renameCollection` при успехе напрямую мутировал `currentCollectionName = trimmedNewName` и вызывал `refresh()` с фонового потока, что нарушает правила обновления состояний Compose и могло приводить к сбоям рекомпозиции. Кроме того, при сбое файлового `renameTo` (из-за блокировки дескрипторов или границ разделов) отсутствовал fallback на рекурсивное копирование.
**Исправление:** Добавлен безопасный fallback через `copyRecursively` с последующим удалением исходного каталога, а обновление `currentCollectionName` и вызовы `refresh()` обёрнуты в `scope.launch(Dispatchers.Main)`.

### C116 — Некорректная классификация видео в MediaStore из-за ненормализованных имён файлов в `GallerySaver`. Низкая.

[core/src/main/java/com/client/xvideos/common/gallery/GallerySaver.kt:41-43, 86-88](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/gallery/GallerySaver.kt#L41)
[core/src/main/java/com/client/xvideos/common/gallery/GalleryTarget.kt:40](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/gallery/GalleryTarget.kt#L40)

Если имя файла при сохранении в галерею содержало хвостовые пробелы (например, `"video.mp4 "`), метод `extensionOf` возвращал расширение с пробелом (`"mp4 "`), которое отсутствовало в списке `VIDEO_EXTENSIONS`. В результате видео ошибочно отправлялось в `MediaStore.Images.Media` (`Pictures/xvideos_download/`) вместо `Movies/xvideos_download/` с пустым MIME-типом.
**Исправление:** Добавлен `.trim()` при получении `cleanFileName` в `GallerySaver` и в `GalleryTarget.extensionOf`.

### C117 — Попадание пустых и невалидных ссылок карточек в ленту видео в `parserListVideo`. Низкая.

[feature-x/src/main/java/com/client/xvideos/x/parcer/parserListVideo.kt:38-41](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/parcer/parserListVideo.kt#L38)

При отсутствии ссылки на странице блок получал значение по умолчанию `"No link"`. При клике или скачивании такая карточка формировала невалидный URL `https://www.xvideos.comNo link`, вызывая ошибки сети и пустой экран воспроизведения.
**Исправление:** В парсер добавлена проверка `if (href.isBlank() || href == "No link") continue` для отсечения карточек без действительных ссылок.

## Статус

| Находка | Класс | Статус | Описание / Исправление |
| --- | --- | --- | --- |
| UI56 | App / Calculator | Исправлен | Устранение артефакта ведущего нуля при вводе после `"-0"` |
| T18 | Feature-X / Downloads | Исправлен | Отмена задач KDownloader по тегу при удалении скачанного видео |
| T19 | Feature-R / Downloader | Исправлен | Отмена задач KDownloader при удалении гифки и полной очистке кеша |
| C115 | App / Settings | Исправлен | Делегирование очистки загрузок R в `DownloadRed.deleteAll` со синхронизацией StateFlow |
| T20 | Feature-L / Collection | Исправлен | Диспетчеризация мутации `currentCollectionName` на Main и fallback при переименовании |
| C116 | Core / GallerySaver | Исправлен | Очистка пробелов в `GallerySaver` и `GalleryTarget` для корректной маршрутизации MediaStore |
| C117 | Feature-X / Parser | Исправлен | Фильтрация карточек видео с невалидным `href` (`No link`) в `parserListVideo` |

## Проверка

- `./gradlew testDebugUnitTest`: **150+ тестов успешно выполнены** во всех 5 модулях (`:app`, `:core`, `:feature-l`, `:feature-r`, `:feature-x`).
- `./gradlew detekt`: **зелёный**, нарушений правил detekt нет.
- Ручная верификация:
  1. Калькулятор: проверен ввод после отрицательных дробей, очистки и смены знака (отсутствие `"-07"`, вывод `"-7"`).
  2. Удаление загрузок X/R: отмена очередей `kDownloader.cancel(id)` предотвращает фоновое скачивание и воскрешение файлов.
  3. Экран настроек: очистка загрузок R очищает локальную папку, сбрасывает списки в UI и отменяет активные потоки.
  4. Коллекции L: переименование гарантированно передаёт изменения в UI-поток без исключений фонового доступа.

## Что проверено и оказалось в порядке

- `XlrBackupManager` и `XlrChunkedCrypto`: атомарный откат бэкапов (`.xlr_old_`), потоковое шифрование AES-GCM и безопасная очистка ключей/буферов в памяти.
- `MediaDownloadWorker`: корректность работы с `ForegroundInfo`, обработка HTTP 416 (Range Not Satisfiable), 0-байтовые страховки и отмена при остановке воркера.
- `UrlImage` и `AsyncImagePainter`: обработка состояний загрузки/ошибок, ограничение троттлинга `snapshotFlow` (200 мс), устойчивость к пустым URL.
- Плееры X (`ScreenX_VideoPlayer`, `ScreenX_VideoPlayerFullScreen`): разделение жизненного цикла хоста `MediaPlayerHost`, нативные контролы Media3 в полноэкранном режиме и передача позиции через `EventBus`.
