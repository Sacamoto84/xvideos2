# Превью кадра при перемотке в X — план реализации

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** в полноэкранном плеере ролика X из сети при перетаскивании ползунка
показывать кадр раскадровки в виде A (за ползунком) или B (на весь плеер);
видео перематывается только при отпускании.

**Architecture:** раскадровка `thumbSlideBig` (сетка 10×10) уже приходит в
`ScreenX_VideoPlayerSM.playerConfig`. Экран грузит её одной картинкой через
общий Coil, нижняя панель X сообщает позицию ползунка и границы полосы
(`SeekScrub`), оверлей поверх плеера вырезает кадр и рисует A или B. Вид
хранится в `Settings`, его меняют строка в «Настройках» и временная кнопка
на экране. В `core` надёжная перемотка: запрос перемотки с номером, который
опрос позиции не может сбросить.

**Tech Stack:** Kotlin, Jetpack Compose, Media3 ExoPlayer (через
`CMPPlayer2`), Coil 3, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-21-x-seek-preview-design.md`

## Global Constraints

- Только полноэкранный режим плеера ролика X из сети; плеер скачанного
  ролика (`LocalVideoPlayerContent`) перематывает на ходу, как сейчас.
- Во время перетаскивания в полноэкранном режиме видео не перематывается;
  одна перемотка при отпускании.
- Раскадровка: `thumbSlideBig`, сетка 10 столбцов × 10 строк, по строкам
  слева направо; кадр `i = clamp(floor(позиция / длительность × 100), 0, 99)`,
  строка `i / 10`, столбец `i % 10`; размер кадра — `ширина / 10`,
  `высота / 10` картинки.
- Картинку раскадровки не уменьшать при загрузке (`Size.ORIGINAL`), грузить
  через `CoilImageLoaderFactory.getImageLoader`.
- Настройка: `Settings.x_seek_preview_mode`, `SettingElementInt`, ключ
  `"x_seek_preview_mode"`, 0 — A, 1 — B, по умолчанию 0.
- Строка в «Настройках» — `ChoiceRow` со стилем `ChoiceRowStyle.MATERIAL`;
  на экране — одна кнопка-значок, помеченная как временная.
- Один Composable — один файл, имя файла = имя функции; Composable-блоки —
  в `atom/` или `molecule/` экрана плеера X, в каждом таком файле
  `@Preview` без обёрток; атомы и молекулы не принимают ScreenModel.
- Без `screenWidthDp` и ориентации; размер кадра A — от ширины области
  плеера.
- Названия сайтов не пишутся нигде (код, комментарии, журнал, коммиты) —
  только X / R / L; адреса раскадровки в журнал не писать.
- Новые тесты в `feature-x` — без `runTest` и `Dispatchers.setMain`.
- Гейт перед каждым коммитом: `gradlew.bat testDebugUnitTest detekt
  :app:lintRelease` (из PowerShell: `cmd /c ".\gradlew.bat …"`), результат —
  фактический, из JUnit XML.
- Коммиты — в `master`, только после «коммить» владельца, явными путями
  (`git add -- <пути>`); незакоммиченные файлы владельца
  (`GifsTabBottomBar.kt`, `libs.versions.toml`,
  `L_LazyRowPictureDetails.kt`, `.idea/…`) не трогать. Сообщения коммитов —
  по-русски, Conventional Commits.

## Review Focus

- **Отпустил ползунок, пока ролик играет** → ролик перематывается в эту
  точку каждый раз; бегунок не прыгает назад. Тесты — Task 1.
- **У ролика нет раскадровки или картинка не загрузилась** → показывается
  только время, без рамки и без падения. Тест на вырожденную картинку —
  Task 2; путь `null` в оверлее — обязательные `@Preview` с
  `storyboard = null` у `SeekPreviewFollow` и `SeekPreviewFullscreen`
  (Task 4).
- **Длительность ещё неизвестна** (`totalTime == 0` в начале HLS) → кадр 0,
  якорь в начале полосы, без деления на ноль. Тесты — Task 2.
- **Палец увели за конец полосы или позиция NaN** → крайний кадр, кадр A
  упирается в край. Тесты — Task 2.
- **Тянут дольше 3,5 с** → панель управления не прячется, пока палец на
  полосе. Тест — Task 5.

---

### Task 1: Перемотка при отпускании доходит до плеера

Сейчас `StaticPlayer` на каждом опросе позиции (раз в 50 мс, когда палец не
на полосе) делает `playerHost.seekToTime = null`. Если опрос успевает между
`host.seekTo()` и перекомпоновкой, `LaunchedEffect(config.seekToTime)` в
`CMPPlayer2` новой цели не видит — перемотка теряется, а время откатывается
к старой позиции. Перемотка на ходу это прятала; без неё (Task 4) это станет
заметно. Правка общая для всех плееров на `MediaPlayerHost` и только делает
перемотку надёжнее.

**Files:**
- Create: `core/src/main/java/com/client/xvideos/common/videoplayer/model/SeekRequest.kt`
- Modify: `core/src/main/java/com/client/xvideos/common/videoplayer/host/MediaPlayerHost.kt`
- Modify: `core/src/main/java/com/client/xvideos/common/videoplayer/model/PlayerPlaybackConfig.kt`
- Modify: `core/src/main/java/com/client/xvideos/common/videoplayer/ui/StaticPlayer.kt`
- Modify: `core/src/main/java/com/client/xvideos/common/videoplayer/util/CMPlayer2.kt`
- Test: `core/src/test/java/com/client/xvideos/common/videoplayer/host/MediaPlayerHostTest.kt`

**Interfaces:**
- Produces: `data class SeekRequest(val seconds: Float, val id: Long)`;
  в `MediaPlayerHost`: `var seekRequest: SeekRequest?` (state, `private set`),
  `fun onPlayerPosition(seconds: Float)`, `fun onSeekApplied(requestId: Long)`;
  `seekTo(seconds: Float?)` — прежняя сигнатура.
- `PlayerPlaybackConfig.seekRequest: SeekRequest? = null`;
  `PlayerPlaybackCallbacks.seekApplied: (Long) -> Unit = {}`.

- [ ] **Step 1: Тесты в `MediaPlayerHostTest`**

```kotlin
@Test
fun `позиция от плеера до выполнения перемотки не сбрасывает её и не откатывает время`() {
    val host = MediaPlayerHost()
    host.seekTo(30f)
    host.onPlayerPosition(10f)
    assertEquals(30f, host.currentTime)
    assertEquals(30f, host.seekToTime)
    assertEquals(30f, host.seekRequest?.seconds)
}

@Test
fun `после выполнения перемотки позиция от плеера снова обновляет время`() {
    val host = MediaPlayerHost()
    host.seekTo(30f)
    host.onSeekApplied(host.seekRequest!!.id)
    host.onPlayerPosition(31f)
    assertEquals(31f, host.currentTime)
    assertNull(host.seekToTime)
    assertNull(host.seekRequest)
}

@Test
fun `повторная перемотка в ту же точку — новый запрос`() {
    val host = MediaPlayerHost()
    host.seekTo(30f)
    val first = host.seekRequest!!.id
    host.onSeekApplied(first)
    host.seekTo(30f)
    assertTrue(host.seekRequest!!.id != first)
}

@Test
fun `подтверждение старого запроса не снимает новый`() {
    val host = MediaPlayerHost()
    host.seekTo(10f)
    val old = host.seekRequest!!.id
    host.seekTo(20f)
    host.onSeekApplied(old)
    assertEquals(20f, host.seekRequest?.seconds)
}

@Test
fun `пока ползунок держат, позиция от плеера не меняет время`() {
    val host = MediaPlayerHost()
    host.updateCurrentTime(50f)
    host.isSliding = true
    host.onPlayerPosition(5f)
    assertEquals(50f, host.currentTime)
}

@Test
fun `невалидная перемотка запроса не создаёт`() {
    val host = MediaPlayerHost()
    host.seekTo(Float.NaN)
    host.seekTo(-1f)
    assertNull(host.seekRequest)
}
```

- [ ] **Step 2: Запустить, убедиться, что падают**

Run: `cmd /c ".\gradlew.bat :core:testDebugUnitTest --tests *MediaPlayerHostTest*"`
Expected: FAIL — `onPlayerPosition`, `onSeekApplied`, `seekRequest` не
существуют (ошибка компиляции).

- [ ] **Step 3: Реализация**

- `SeekRequest` — в своём файле, KDoc: зачем номер (две перемотки в одну
  точку — два запроса).
- `MediaPlayerHost.seekTo`: как сейчас, плюс для валидной позиции
  `seekRequest = SeekRequest(seconds, ++счётчик)`.
- `onPlayerPosition(seconds)`: если `isSliding` или `seekRequest != null` —
  ничего; иначе `updateCurrentTime(seconds)` и `seekToTime = null` (то, что
  сейчас делает лямбда в `StaticPlayer`).
- `onSeekApplied(id)`: снять `seekRequest`, только если `id` совпал.
- `toPlaybackConfig` передаёт `seekRequest`; `StaticPlayer`:
  `currentTime = { playerHost.onPlayerPosition(it) }`,
  `seekApplied = { playerHost.onSeekApplied(it) }`.
- `CMPPlayer2`: `LaunchedEffect(exoPlayer, config.seekRequest)` —
  `exoPlayer.seekTo(ms)`, затем `currentCallbacks.seekApplied(id)`.
  Существующий эффект на `config.seekToTime` (перемотка на ходу) оставить.

- [ ] **Step 4: Тесты проходят**

Run: `cmd /c ".\gradlew.bat :core:testDebugUnitTest --tests *MediaPlayerHostTest*"`
Expected: PASS, включая прежние тесты `seekTo`.

- [ ] **Step 5: Коммит** (после «коммить» владельца) —
  `fix(core): перемотка при отпускании ползунка доходит до плеера`.

### Task 2: Геометрия раскадровки и вид превью — чистые функции

**Files:**
- Create: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/model/StoryboardGrid.kt`
- Create: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/model/XSeekPreviewMode.kt`
- Create: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/model/SeekScrub.kt`
- Test: `feature-x/src/test/java/com/client/xvideos/x/screens/videoplayer/model/StoryboardGridTest.kt`
- Test: `feature-x/src/test/java/com/client/xvideos/x/screens/videoplayer/model/XSeekPreviewModeTest.kt`

**Interfaces:**
- Produces:
  - `fun storyboardFrameIndex(positionSec: Float, durationSec: Int): Int`
  - `fun storyboardFrameRect(index: Int, imageWidth: Int, imageHeight: Int): IntRect?`
    (`androidx.compose.ui.unit.IntRect`; `null`, если кадр получается 0 px)
  - `fun seekPreviewAnchorX(positionSec: Float, durationSec: Int, trackStartPx: Float, trackWidthPx: Float, overlayStartPx: Float): Float`
    — x бегунка в координатах оверлея; та же доля, что в
    `calculateCustomSeekBarProgressX` (`ratio × ширина`, ratio в 0…1).
  - `fun seekPreviewFollowLeft(anchorX: Float, frameWidth: Float, containerWidth: Float, edgeMargin: Float): Float`
    — левый край кадра A с упором в края; если кадр с отступами не влезает —
    по центру `(containerWidth − frameWidth) / 2`.
  - `enum class XSeekPreviewMode(val settingValue: Int, val label: String) { FOLLOW(0, "У ползунка"), FULLSCREEN(1, "На весь экран") }`
    с `fun toggled(): XSeekPreviewMode` и
    `companion fun fromSetting(value: Int): XSeekPreviewMode` (неизвестное → `FOLLOW`).
  - `data class SeekScrub(val positionSec: Float, val trackStartPx: Float, val trackWidthPx: Float)`
    — позиция ползунка и границы полосы в координатах корня.

- [ ] **Step 1: Тесты**

```kotlin
// StoryboardGridTest
@Test fun `номер кадра — процент длины`() {
    assertEquals(0, storyboardFrameIndex(0f, 100))
    assertEquals(0, storyboardFrameIndex(0.99f, 100))
    assertEquals(1, storyboardFrameIndex(1f, 100))
    assertEquals(50, storyboardFrameIndex(50.5f, 100))
    assertEquals(99, storyboardFrameIndex(99.99f, 100))
}
@Test fun `номер кадра за краями и без длительности`() {
    assertEquals(99, storyboardFrameIndex(100f, 100))
    assertEquals(99, storyboardFrameIndex(150f, 100))
    assertEquals(0, storyboardFrameIndex(-5f, 100))
    assertEquals(0, storyboardFrameIndex(Float.NaN, 100))
    assertEquals(0, storyboardFrameIndex(30f, 0))
}
@Test fun `прямоугольник кадра в сетке 10x10`() {
    assertEquals(IntRect(0, 0, 192, 108), storyboardFrameRect(0, 1920, 1080))
    assertEquals(IntRect(1728, 0, 1920, 108), storyboardFrameRect(9, 1920, 1080))
    assertEquals(IntRect(0, 108, 192, 216), storyboardFrameRect(10, 1920, 1080))
    assertEquals(IntRect(1728, 972, 1920, 1080), storyboardFrameRect(99, 1920, 1080))
    assertEquals(IntRect(1728, 972, 1920, 1080), storyboardFrameRect(150, 1920, 1080))
}
@Test fun `вырожденная картинка кадра не даёт`() {
    assertNull(storyboardFrameRect(5, 9, 9))
    assertNull(storyboardFrameRect(0, 0, 0))
}
@Test fun `якорь — доля длины на полосе в координатах оверлея`() {
    assertEquals(150f, seekPreviewAnchorX(50f, 100, 100f, 200f, 50f))
    assertEquals(50f, seekPreviewAnchorX(30f, 0, 100f, 200f, 50f))
    assertEquals(250f, seekPreviewAnchorX(500f, 100, 100f, 200f, 50f))
}
@Test fun `кадр A упирается в края`() {
    assertEquals(420f, seekPreviewFollowLeft(500f, 160f, 1000f, 8f))
    assertEquals(8f, seekPreviewFollowLeft(20f, 160f, 1000f, 8f))
    assertEquals(832f, seekPreviewFollowLeft(990f, 160f, 1000f, 8f))
    assertEquals(-5f, seekPreviewFollowLeft(100f, 160f, 150f, 8f))
}

// XSeekPreviewModeTest
@Test fun `значение настройки туда и обратно`() {
    XSeekPreviewMode.entries.forEach { assertEquals(it, XSeekPreviewMode.fromSetting(it.settingValue)) }
    assertEquals(XSeekPreviewMode.FOLLOW, XSeekPreviewMode.fromSetting(0))
    assertEquals(XSeekPreviewMode.FULLSCREEN, XSeekPreviewMode.fromSetting(1))
    assertEquals(XSeekPreviewMode.FOLLOW, XSeekPreviewMode.fromSetting(7))
}
@Test fun `переключение A и B`() {
    assertEquals(XSeekPreviewMode.FULLSCREEN, XSeekPreviewMode.FOLLOW.toggled())
    assertEquals(XSeekPreviewMode.FOLLOW, XSeekPreviewMode.FULLSCREEN.toggled())
}
```

- [ ] **Step 2: Запустить, убедиться, что падают**

Run: `cmd /c ".\gradlew.bat :feature-x:testDebugUnitTest --tests *StoryboardGridTest* --tests *XSeekPreviewModeTest*"`
Expected: FAIL — функции и enum не существуют.

- [ ] **Step 3: Реализовать функции, enum и `SeekScrub`** по сигнатурам выше;
  константы сетки (`10`) — одна пара `private const val` в `StoryboardGrid.kt`.
  Номер кадра считать как `floor(positionSec * 100f / durationSec)`, именно в
  этом порядке: `positionSec / durationSec * 100f` на границе процента даёт
  `0.99999994` вместо `1.0`, и тест `1f → 1` падает.

- [ ] **Step 4: Тесты проходят** — та же команда, PASS.

- [ ] **Step 5: Коммит** вместе с Task 3–5 (см. Task 5).

### Task 3: Настройка вида превью

**Files:**
- Modify: `core/src/main/java/com/client/xvideos/common/settings/Settings.kt` (блок `//-- xvideos ---`)
- Modify: `app/src/main/java/com/client/xvideos/screenSettings/section/XSettingsSection.kt`

**Interfaces:**
- Consumes: `XSeekPreviewMode` (Task 2).
- Produces: `Settings.x_seek_preview_mode: SettingElementInt` (ключ
  `"x_seek_preview_mode"`, по умолчанию 0).

- [ ] **Step 1: Добавить настройку** —
  `val x_seek_preview_mode by lazy { SettingElementInt(pref, "x_seek_preview_mode", 0) }`
  с комментарием: номер в `XSeekPreviewMode` (0 — у ползунка, 1 — на весь экран).

- [ ] **Step 2: Строка в `XSettingsSection`** после «Shemale» через
  `SettingsDivider`: подпись «Превью при перемотке» и
  `ChoiceRow(options = XSeekPreviewMode.entries.map { it.label }.toPersistentList(), selectedIndex = XSeekPreviewMode.fromSetting(value).ordinal, onSelectIndex = { Settings.x_seek_preview_mode.setValue(XSeekPreviewMode.entries[it].settingValue) }, style = ChoiceRowStyle.MATERIAL, accent = SettingsAccentColor)`.
  Значение — через `collectAsStateWithLifecycle`, как у соседних строк.

- [ ] **Step 3: Сборка** —
  `cmd /c ".\gradlew.bat :app:compileDebugKotlin"` → BUILD SUCCESSFUL;
  превью `XSettingsSectionPreview` открывается.

### Task 4: Нижняя панель X, загрузка раскадровки и оверлей превью

**Files:**
- Modify: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/molecule/X_PlayerBottomBar.kt`
- Create: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/model/rememberStoryboardImage.kt`
- Create: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/atom/StoryboardFrame.kt`
- Create: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/atom/SeekPreviewModeToggle.kt`
- Create: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/molecule/SeekPreviewFollow.kt`
- Create: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/molecule/SeekPreviewFullscreen.kt`
- Create: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/molecule/SeekPreviewOverlay.kt`

**Interfaces:**
- Consumes: Task 2 целиком; `formatTime(Int)` из `X_PlayerBottomBar.kt`.
- Produces:
  - `X_PlayerBottomBar(host, isFullScreen = false, onFullScreen = null, seekWhileDragging: Boolean = true, onScrub: (SeekScrub?) -> Unit = {}, seekPreviewMode: XSeekPreviewMode? = null, onToggleSeekPreviewMode: () -> Unit = {}, modifier)`.
    Перетаскивание: `host.isSliding = true`; `host.seekToTime = v` — только
    при `seekWhileDragging`; `onScrub(SeekScrub(v, левый край полосы в корне,
    ширина полосы))`. Отпускание: `host.seekTo(v)`, `onScrub(null)`. Границы
    полосы — `onGloballyPositioned` на модификаторе `CustomSeekBar`
    (`positionInRoot().x`, `size.width`). Кнопка `SeekPreviewModeToggle` —
    рядом с `FitModeToggle`, только при `isFullScreen && seekPreviewMode != null`.
  - `@Composable fun rememberStoryboardImage(url: String?): ImageBitmap?` —
    `produceState(null, url)`: пустой адрес → `null`; иначе `ImageRequest`
    с `size(Size.ORIGINAL)` через `CoilImageLoaderFactory.getImageLoader`,
    успех → `toBitmap().asImageBitmap()`, иначе `null`; при ошибке в журнал
    только класс ошибки.
  - `StoryboardFrame(storyboard: ImageBitmap, frameIndex: Int, contentScale: ContentScale, modifier)`
    — `Canvas`: `storyboardFrameRect` → `drawImage(src → dst)` с масштабом по
    `contentScale` по центру, с обрезкой; `null` из `storyboardFrameRect` —
    ничего не рисует.
  - `SeekPreviewModeToggle(mode: XSeekPreviewMode, onToggle: () -> Unit, modifier)`
    — значок 28dp, белый (`PhotoSizeSelectSmall` для A, `PhotoSizeSelectLarge`
    для B); KDoc и комментарий: «ВРЕМЕННО: для теста, удалить после выбора
    вида (спека 2026-10-21)».
  - `SeekPreviewFollow(storyboard: ImageBitmap?, frameIndex: Int, timeText: String, anchorX: Float, modifier)`
    — `BoxWithConstraints`: ширина кадра `(maxWidth × 0.22).coerceIn(120.dp, 240.dp)`,
    16:9, рамка 2dp белая, скругление 8dp, левый край
    `seekPreviewFollowLeft(anchorX, ширина, maxWidth, 8.dp)`, низ кадра в
    56dp от низа; время под кадром, 15sp, жирный, белый с тенью. Без
    картинки — только время в той же точке.
  - `SeekPreviewFullscreen(storyboard: ImageBitmap?, frameIndex: Int, timeText: String, contentScale: ContentScale, modifier)`
    — с картинкой: чёрный фон на всю область + `StoryboardFrame`; время
    по центру, 28sp, жирный, с тенью. Без картинки — только время, видео не
    перекрывается.
  - `SeekPreviewOverlay(mode: XSeekPreviewMode, storyboard: ImageBitmap?, scrub: SeekScrub?, durationSec: Int, videoFitMode: ScreenResize, modifier)`
    — `scrub == null` → ничего; мерит свой `positionInRoot().x`; кадр —
    `storyboardFrameIndex`, время — `formatTime(positionSec.toInt())`, якорь —
    `seekPreviewAnchorX`; B: `ScreenResize.FILL` → `ContentScale.Crop`,
    иначе `ContentScale.Fit`.

- [ ] **Step 1: Реализовать файлы** по интерфейсам выше; у каждого
  Composable-файла `@Preview` без обёрток (картинка — `ImageBitmap(1920, 1080)`).
  У `SeekPreviewFollow` и `SeekPreviewFullscreen` — второе превью с
  `storyboard = null`: только время, без рамки.

- [ ] **Step 2: Сборка и прежние тесты X** —
  `cmd /c ".\gradlew.bat :feature-x:testDebugUnitTest :feature-x:detekt"` →
  BUILD SUCCESSFUL, падений нет.

### Task 5: Связка на экране, автоскрытие панели, проверка

**Files:**
- Modify: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/molecule/VideoPlayerContentView.kt`
- Modify: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayer.kt`
- Create: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/model/ControlsAutoHide.kt`
- Test: `feature-x/src/test/java/com/client/xvideos/x/screens/videoplayer/model/ControlsAutoHideTest.kt`

**Interfaces:**
- Consumes: Task 3 (`Settings.x_seek_preview_mode`), Task 4 целиком.
- Produces: `fun shouldAutoHideControls(isFullScreen: Boolean, areControlsVisible: Boolean, isPaused: Boolean, isMenuExpanded: Boolean, isScrubbing: Boolean): Boolean`;
  `VideoPlayerContentView(…, storyboardUrl: String? = null, seekPreviewMode: XSeekPreviewMode = XSeekPreviewMode.FOLLOW, onToggleSeekPreviewMode: () -> Unit = {})`;
  те же три параметра у `ScreenX_VideoPlayerContent`.

- [ ] **Step 1: Тест автоскрытия**

```kotlin
@Test fun `панель прячется только в полном экране при игре без меню и без перетаскивания`() {
    assertTrue(shouldAutoHideControls(true, true, false, false, false))
    assertFalse(shouldAutoHideControls(true, true, false, false, true))
    assertFalse(shouldAutoHideControls(true, true, true, false, false))
    assertFalse(shouldAutoHideControls(true, true, false, true, false))
    assertFalse(shouldAutoHideControls(false, true, false, false, false))
    assertFalse(shouldAutoHideControls(true, false, false, false, false))
}
```

- [ ] **Step 2: Запустить, убедиться, что падает**

Run: `cmd /c ".\gradlew.bat :feature-x:testDebugUnitTest --tests *ControlsAutoHideTest*"`
Expected: FAIL — функции нет.

- [ ] **Step 3: Реализация**
  - `shouldAutoHideControls` — в `ControlsAutoHide.kt`; эффект автоскрытия
    в `VideoPlayerContentView` берёт её, ключи эффекта дополняются
    `scrub != null`.
  - `VideoPlayerContentView`: `var scrub by remember { mutableStateOf<SeekScrub?>(null) }`,
    `val storyboard = rememberStoryboardImage(storyboardUrl)` (грузится и в
    портрете — к полному экрану картинка уже есть); в `overlay` при
    `isFullScreen` — `SeekPreviewOverlay` после верхней панели и до нижней
    (нижняя рисуется поверх кадра B); `X_PlayerBottomBar(…,
    seekWhileDragging = !isFullScreen, onScrub = { scrub = it },
    seekPreviewMode = seekPreviewMode.takeIf { isFullScreen },
    onToggleSeekPreviewMode = onToggleSeekPreviewMode)`.
  - `ScreenX_VideoPlayer.Content`: значение настройки через
    `collectAsStateWithLifecycle` → `XSeekPreviewMode.fromSetting`;
    переключение — `Settings.x_seek_preview_mode.setValue(mode.toggled().settingValue)`;
    `storyboardUrl = vm.playerConfig?.thumbSlideBig?.takeIf { it.isNotBlank() }`.
  - `LocalVideoPlayerContent` не меняется.

- [ ] **Step 4: Тесты проходят** — та же команда, PASS.

- [ ] **Step 5: Полный гейт**

Run: `cmd /c ".\gradlew.bat testDebugUnitTest detekt :app:lintRelease --continue"`
Expected: BUILD SUCCESSFUL; в JUnit XML всех пяти модулей 0 падений;
`app/build/reports/lint-results-release.xml` — 0 Error/Fatal. Нестабильные
тесты из памяти (`KDownloaderQueueTest`, `ScreenX_ChannelSMPageTest`,
`ItemCollectionPagingSourceTest`) — перезапуск, в отчёт — какой прогон зелёный.

- [ ] **Step 6: Проверка на эмуляторе** (`Pixel_10_Pro_Fold` = `emulator-5554`):
  - внешний экран и внутренний в альбомной: открыть ролик X, полный экран,
    тянуть ползунок — A едет за бегунком и упирается в края, время верное;
  - кнопка-переключатель → B: кадр на весь плеер, время по центру, нижняя
    панель поверх; в «Настройках» → X строка показывает тот же вид, и
    наоборот;
  - во время перетаскивания видео не перематывается (индикатор буферизации
    не появляется), после отпускания — одна перемотка в точку, бегунок
    не прыгает назад; держать палец > 4 с — панель не прячется;
  - тап по полосе без движения — перемотка сразу, без превью;
  - портрет: превью нет, перемотка на ходу как раньше; плеер скачанного
    ролика — перемотка на ходу, превью нет;
  - Medium (`adb shell wm density 475`, потом `wm density reset`).
  Скриншоты — `screencap -d`; ролики X на эмуляторе грузятся (в отличие от R).

- [ ] **Step 7: Коммит** (после «коммить» владельца) — серией:
  `feat(x): превью кадра при перемотке в полноэкранном плеере`
  (Task 2–5 вместе с тестами), затем `docs(readme)` с числом тестов, если
  его меняет гейт.
