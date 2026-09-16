# Код-ревью xvideos — проход 51

> **Срез:** `80cd61c` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `80cd61c`. Изменено 10 файлов в модулях `core`, `feature-r`, `feature-x`, добавлен 1 тестовый файл в `core`.
Линзы:
- `BUILD` / `UI` / соответствие Detekt: рефакторинг `FeedControls_Container_Line0` с разбиением на `@Immutable data class FeedPlaybackState` и `FeedPlaybackActions`, вынос компактных приватных подкомпонентов кнопок управления, устранение нарушений `LongParameterList` и `LongMethod`.
- `UI` / производительность Compose / Smart Skipping: изоляция создания `FeedPlaybackActions` через `remember(vm)` в `FeedControls_Container_Line0`, устраняющая 20-герцовую рекомпозицию всех кнопок нижней панели при тиках `vm.currentPlayerTime`.
- `C` / состояние ленты: сброс A-B петли (`vm.enableAB = false`) при смене активного ролика в вертикальном пейджере `ScreenRedFullScreen`, исключающий перенос границ отрезка с предыдущего видео.
- `UI` / консистентность управления: унификация клика по полотну плеера с `vm.togglePlay()` в `ScreenRedFullScreen`.
- `UI` / верстка и Edge-to-Edge: защита `AppLockScreen` от наложения системных инсетов и вырезов камеры (`displayCutoutPadding()`, `statusBarsPadding()`, `Alignment.TopCenter`).
- `C` / обработка ошибок плеера X: гарантированное всплытие ошибки и очистка RAM-кэша через `onPlaybackError()` в `ScreenX_VideoPlayerSM` при получении события `onError` из `MediaPlayerHost`.
- `C` / обработка ошибок полноэкранного плеера X: реализация `onPlayerError` в `Player.Listener` и проброс `error` в `rememberExoPlayerWithLifecycle` в `ScreenX_VideoPlayerFullScreen` с очисткой RAM-кэша в `ScreenX_VideoPlayerFullScreenSM`.
- `T` / инфраструктура видеоплеера: регистрация слушателя `onPlayerError` на экземпляре `ExoPlayer` внутри `rememberExoPlayerWithLifecycle` для своевременного асинхронного уведомления потребителей об ошибках потока.
- `S` / `C` / надежность временных файлов: гарантированное удаление временного `.tmp` файла через `try-catch` при исключении во время записи в `AtomicWrite.kt`.
- `Q` / гигиена строк: устранение плейсхолдера `contentDescription = "TODO()"` в `KeyboardNumber`.
Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

## Находки

### BUILD1 — Падение статического анализатора `detekt` из-за `LongParameterList` и `LongMethod` в `FeedControls_Container_Line0`. Высокая.

[feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/bottom_bar/FeedControls_Container_Line0.kt:46-60](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/bottom_bar/FeedControls_Container_Line0.kt#L46)

В проходе 50 в рамках оптимизации Compose-стабильности параметры управления полноэкранным плеером R были распакованы в сигнатуру функции `FeedControls_Container_Line0`. Это привело к 13 аргументам (при лимите правила `LongParameterList` в 12) и размеру метода в 125 строк (при лимите `LongMethod` в 120 строк). В результате сборка `:feature-r:detekt` завершалась с ошибкой.
**Исправление:** Введены `@Immutable` структуры данных `FeedPlaybackState` и `FeedPlaybackActions`. UI-разметка кнопок декомпозирована на легковесные приватные подкомпоненты (`TimeMarkerButton`, `AbToggleButton`, `PlayPauseButton`, `SeekButton`, `MuteButton`). Корневой контейнер теперь принимает 3 аргумента (`state`, `actions`, `modifier`) и занимает 45 строк, а перегрузка `FeedControls_Container_Line0(vm = vm)` инкапсулирует маппинг стейта и действий из ScreenModel. Проверка detekt полностью зелёная.

### UI60 — Срыв Smart Skipping и холостые рекомпозиции кнопок плеера R из-за пересоздания `FeedPlaybackActions`. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/bottom_bar/FeedControls_Container_Line0.kt:240-262](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/bottom_bar/FeedControls_Container_Line0.kt#L240)

В `FeedControls_Container_Line0(vm = vm)` объект действий `FeedPlaybackActions` инстанцировался напрямую в теле функции без `remember(vm)`. Поскольку родительский `RedFullScreenScaffold` читает `vm.currentPlayerTime`, обновляемый 20 раз в секунду таймлайном, он рекомпонуется 20 раз/с, вызывая пересоздание `FeedPlaybackActions`. Из-за ссылочного сравнения лямбд и функциональных ссылок (`vm::setTimeA`, `{ vm.rewind() }`) Compose считал параметр `actions` изменившимся на каждом кадре и не мог пропустить рекомпозицию stateless-контейнера и всех пяти кнопок управления.
**Исправление:** Инстанс `FeedPlaybackActions` обёрнут в `val actions = remember(vm) { FeedPlaybackActions(...) }`. Так как `vm` стабилен, инстанс действий сохраняется неизменным, а неизменяемое состояние `state` позволяет Compose полностью пропускать рекомпозицию панели при тиках времени.

### C125 — Утечка активной петли A-B на следующий ролик ленты в `ScreenRedFullScreen`. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt:166-184](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt#L166)

При скролле ленты в `RedFullScreenFeed` коллектор `snapshotFlow { pagerState.currentPage }` сбрасывал `currentPlayerTime = 0f` и `currentPlayerDuration = 0`, однако флаг `enableAB` и границы `timeA`/`timeB` сохранялись от предыдущего ролика. В результате следующий ролик стартовал с активной A-B петлёй от чужого видео, сразу перематывался на `timeA` и зацикливался по чужим временным меткам.
**Исправление:** В коллектор смены страницы добавлено обнуление флага `vm.enableAB = false`.

### UI61 — Рассинхронизация паузы/воспроизведения при клике по экрану в `ScreenRedFullScreen`. Низкая.

[feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt:368-372](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt#L368)

В `RedFullScreenPage` тап по экрану выполнял непосредственную мутацию `vm.play = !vm.play`, минуя инкапсулированный метод `vm.togglePlay()`, вызывающий `currentPlayerControls?.play()` / `pause()`.
**Исправление:** Обработчик переведён на `onClick = { if (isCurrentPage) vm.togglePlay() }`.

### UI62 — Наложение клавиатуры и вырезов экрана на форму блокировки в `AppLockScreen`. Низкая.

[core/src/main/java/com/client/xvideos/common/applock/AppLockScreen.kt:270-288](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/applock/AppLockScreen.kt#L270)

При открытии экранной клавиатуры на устройствах с вырезами камеры и жестовой навигацией контент формы пин-кода с вертикальным выравниванием по центру (`Alignment.Center`) смещался под статус-бар и вырез экрана, затрудняя ввод.
**Исправление:** Добавлены модификаторы `.displayCutoutPadding()` и `.statusBarsPadding()`, выравнивание контейнера переведено на `Alignment.TopCenter`, а внутренняя колонка получила адаптивные верхние отступы для естественного скролла над клавиатурой.

### C122 — Зависание полноэкранного плеера X в чёрном экране при ошибке воспроизведения и отсутствие сброса RAM-кэша. Высокая.

[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreen.kt:144-172](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreen.kt#L144)
[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt:118-125](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt#L118)

В `ScreenX_VideoPlayerFullScreen.kt` плеер инициализировался с пустым обработчиком `error = {}`, а зарегистрированный `Player.Listener` обрабатывал только смену состояний `STATE_READY`. При сбоях декодера, обрыве сетевого соединения или истечении HLS-токена Media3 вызывал `onPlayerError`, который никем не перехватывался. Экран оставался навсегда чёрным/зависшим без отображения состояния ошибки и кнопки «Повторить», а невалидный URL сохранялся в RAM-кэше.
**Исправление:** В `ScreenX_VideoPlayerFullScreenSM` добавлен метод `onPlaybackError()`, выставляющий `isError = true` и асинхронно очищающий ключ из `db.cacheUrlStringRam.delete(url)`. В `ScreenX_VideoPlayerFullScreen` подключен колбэк `error = { vm.onPlaybackError() }` и переопределен метод `onPlayerError(error: PlaybackException)` в `Player.Listener`.

### C123 — Поглощение ошибок воспроизведения в стандартном плеере X `ScreenX_VideoPlayer`. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayer.kt:80-92](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayer.kt#L80)
[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt:150-160](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt#L150)

При создании `MediaPlayerHost` в стандартном плеере `ScreenX_VideoPlayer` свойство `host.onError` оставалось `null`. Хотя компонент `StaticPlayer` честно транслирует сбои воспроизведения через `playerHost.triggerError(it)`, отсутствие подписчика приводило к тому, что при сетевой ошибке во время проигрывания видео плеер застревал в индикаторе бесконечной буферизации (`CircularProgressIndicator`), не переключаясь на экран ошибки с кнопкой «Повторить».
**Исправление:** В `ScreenX_VideoPlayerSM` реализован метод `onPlaybackError()`, переводящий экран в состояние ошибки и сбрасывающий кэш. В `ScreenX_VideoPlayer.kt` хост инициализируется с установкой `onError = { vm.onPlaybackError() }`.

### T50 — Отсутствие слушателя асинхронных ошибок воспроизведения Media3 в `rememberExoPlayerWithLifecycle`. Средняя.

[core/src/main/java/com/client/xvideos/common/videoplayer/rememberExoPlayerWithLifecycle.kt:77-90](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/videoplayer/rememberExoPlayerWithLifecycle.kt#L77)

В инфраструктурной Compose-функции `rememberExoPlayerWithLifecycle` параметр `error: (MediaPlayerError) -> Unit` вызывался исключительно синхронно при исключении в блоке подготовки источника данных `LaunchedEffect(url)`. Асинхронные ошибки самого ExoPlayer в процессе воспроизведения оставались неуслышанными на уровне базового плеера `:core-player`.
**Исправление:** В блок `DisposableEffect(exoPlayer)` добавлен слушатель `Player.Listener` с переопределением `onPlayerError`, который транслирует ошибку в `currentError(MediaPlayerError.PlaybackError(...))` и корректно отписывается при смене экземпляра плеера или выходе из композиции.

### S6 / C124 — Утечка временных файлов `.tmp` при сбое записи в `AtomicWrite.kt`. Низкая.

[core/src/main/java/com/client/xvideos/common/io/AtomicWrite.kt:27-39](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/io/AtomicWrite.kt#L27)

Функция `File.writeTextAtomically` создаёт временный файл `File.createTempFile("atomic-", ".tmp", parentFile)`. Если запись `temp.writeText` падала с исключением (например, исчерпание свободного места на накопителе, сбой потока I/O или отказ прав доступа), выполнение прерывалось до попытки переименования, и временный `.tmp`-файл оставался висеть на диске без удаления.
**Исправление:** Запись и перемещение обёрнуты в `try-catch`, гарантирующий немедленный вызов `temp.delete()` при любом выброшенном `Throwable`. Поведение покрыто модульным тестом в `AtomicWriteTest`.

### Q1 — Литерал `TODO()` в атрибуте `contentDescription` кнопки в `KeyboardNumber.kt`. Низкая.

[core/src/main/java/com/client/xvideos/common/ui/keyboard/KeyboardNumber.kt:209](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/ui/keyboard/KeyboardNumber.kt#L209)

В компоненте цифровой клавиатуры `KeyboardNumber` иконка клавиши подтверждения ввода содержала плейсхолдер `contentDescription = "TODO()"`, нарушающий требования доступности (Accessibility / TalkBack).
**Исправление:** Плейсхолдер заменён на понятное описание `contentDescription = "Ввод"`.

## Статус

| Находка | Класс | Статус | Описание / Исправление |
| --- | --- | --- | --- |
| BUILD1 | Feature-R / Detekt | Исправлен | Декомпозиция `FeedControls_Container_Line0` на subcomponents и структуры `FeedPlaybackState`/`FeedPlaybackActions` |
| UI60 | Feature-R / Compose | Исправлен | `remember(vm)` для `FeedPlaybackActions` для устранения рекомпозиций кнопок плеера при тиках 20 Гц |
| C125 | Feature-R / Feed | Исправлен | Сброс A-B петли (`enableAB = false`) при смене активного видео в пейджере `ScreenRedFullScreen` |
| UI61 | Feature-R / Player | Исправлен | Унификация тапа по видео с `vm.togglePlay()` в `ScreenRedFullScreen` |
| UI62 | Core / Layout | Исправлен | Защита `AppLockScreen` от перекрытия статус-баром и вырезами дисплея при открытии клавиатуры |
| C122 | Feature-X / Fullscreen | Исправлен | Обработка `onPlayerError` и `error` в `ScreenX_VideoPlayerFullScreen` с вызовом `onPlaybackError()` и очисткой кэша |
| C123 | Feature-X / Player | Исправлен | Подключение `host.onError` к `vm.onPlaybackError()` в стандартном плеере `ScreenX_VideoPlayer` |
| T50 | Core / Video Player | Исправлен | Регистрация слушателя `onPlayerError` на `exoPlayer` в `rememberExoPlayerWithLifecycle` |
| S6 / C124 | Core / I/O | Исправлен | Защита от утечки временных `.tmp` файлов через `try-catch` с `temp.delete()` в `AtomicWrite.kt` |
| Q1 | Core / A11y | Исправлен | Замена `contentDescription = "TODO()"` на `"Ввод"` в `KeyboardNumber.kt` |

## Проверка

- `./gradlew testDebugUnitTest`: **все тесты успешно выполнены** во всех 5 модулях (`:app`, `:core`, `:feature-l`, `:feature-r`, `:feature-x`), включая `AtomicWriteTest`.
- `./gradlew detekt`: **зелёный во всех модулях** (`:app`, `:core`, `:feature-l`, `:feature-r`, `:feature-x`), нарушений правил нет.
- Ручная верификация:
  1. `FeedControls_Container_Line0`: компактные кнопки A/B, повтора A-B, паузы/плей, перемотки и звука изолированы, `actions` кэшируются через `remember(vm)`, рекомпозиция кнопок при 20 Гц тиках таймлайна не происходит, detekt полностью доволен.
  2. Лента R: при скролле между роликами A-B петля сбрасывается в `false`, новое видео не зацикливается по чужим точкам.
  3. Плееры X: сетевой сбой или истечение токена HLS во время воспроизведения корректно вызывают экран ошибки с кнопкой «Повторить» и удаляют невалидный URL из `db.cacheUrlStringRam`.
  4. `AtomicWrite`: при отказе записи или невозможности атомарного замещения временный `.tmp` файл гарантированно удаляется из файловой системы.
  5. `AppLockScreen`: форма ввода пин-кода остаётся видимой и доступной для ввода над клавиатурой без заезда под вырез камеры.

## Что проверено и оказалось в порядке

- `LAlbumExporter`: атомарная запись метаданных `.album` в outbox и корректность валидации идентификатора альбома.
- `ScreenTagsViewModel`: санитизация спецсимволов путей (`#`, `?`, `&`, `/`, `\`) и обработка граничных пустых тегов.
- `GallerySaver`: обработка `IS_PENDING` и корректность очистки при сбоях публикаций в MediaStore.
- `BackupSettingsSection`: синхронизация списков `downloadRed` после восстановления и очистка пароля в памяти.
