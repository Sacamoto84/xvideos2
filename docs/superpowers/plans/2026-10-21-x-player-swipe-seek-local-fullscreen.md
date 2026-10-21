# Свайп ±5 с в плеерах X и полный экран в плеере скачанного ролика — план реализации

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** свайп по видео влево-вправо перематывает на ±5 с в обоих плеерах
X; плеер скачанного ролика получает полный экран, как у ролика из сети.

**Architecture:** свайп распознаёт общий `ComposeVideoPlayer` (`core`) —
он один знает масштаб картинки; при `swipeSeekSeconds != null` он
перематывает хост через новый `MediaPlayerHost.seekBy` и показывает значок
«+5 с». Оба плеера X передают `swipeSeekSeconds = 5`. Полный экран
скачанного ролика повторяет механизм ролика из сети: `isFullScreen` в
ScreenModel, `OrientationAndSystemBarsEffect` в экране, автоскрытие панели и
верхняя панель «назад + название» в `LocalVideoPlayerContent`.

**Tech Stack:** Kotlin, Jetpack Compose (pointer input), `net.engawapg.lib:zoomable` 2.16.0, Media3 через `CMPPlayer2`, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-21-x-player-swipe-seek-local-fullscreen-design.md`

## Global Constraints

- Свайп: один свайп — ровно 5 с; вправо — вперёд, влево — назад; свайпы
  подряд складываются; границы 0…длительность; на паузе остаётся пауза.
- Свайп засчитывается: один палец, горизонтальное смещение от касания до
  отпускания ≥ 48dp и по модулю больше удвоенного вертикального; при
  увеличенной картинке (`isZoomActive(zoomState.scale)`) свайпа нет.
- Значок: «+5 с» или «−5 с» (знак минус U+2212) в центре видео, ~600 мс,
  каждый свайп — свой шаг, у границы — тот же значок.
- Полный экран скачанного ролика: кнопка в нижней панели; альбомная
  ориентация и скрытые системные панели (`OrientationAndSystemBarsEffect`);
  автоскрытие через `shouldAutoHideControls` (3,5 с); `FitModeToggle`;
  `FILL` на весь экран, `FIT` в портрете; «Назад»: зум → полный экран →
  выход; сверху «назад» и название, без меню.
- Один Composable — один файл, имя файла = имя функции; атомы и молекулы с
  `@Preview` без обёрток; молекулы без ScreenModel; названия сайтов не
  пишутся.
- Новые тесты в `feature-x` — без `runTest` (свой планировщик).
- Перед каждым коммитом — `gradlew.bat testDebugUnitTest detekt :app:lintRelease`;
  коммиты — в `master`, по явным путям; незакоммиченные файлы владельца
  (L, R, версии, `.idea`) не трогать.

## Review Focus

- Тап по видео после изменений: в портрете — пауза, на весь экран —
  показать/спрятать панель; свайп не должен срабатывать на тап, а тап — на
  свайп. Проверка — Task 4 на устройстве.
- Щипок двумя пальцами и перетаскивание увеличенной картинки — не
  перематывают. Проверка — Task 2 (условие в коде) и Task 4.
- Свайп до того, как известна длительность (ролик ещё грузится) — ничего не
  делает. Тест — Task 1.
- Быстрые свайпы подряд, пока плеер не выполнил прошлую перемотку, — цель
  считается от уже сдвинутого `currentTime`. Тест — Task 1.
- Полный экран скачанного ролика и уход с экрана — ориентация и системные
  панели возвращаются (как у ролика из сети, `DisposableEffect` эффекта).
  Проверка — Task 4.

---

### Task 1: `seekBy` и распознавание свайпа

**Files:**
- Modify: `core/src/main/java/com/client/xvideos/common/videoplayer/host/MediaPlayerHost.kt` (после `seekTo`)
- Create: `core/src/main/java/com/client/xvideos/common/videoplayer/model/SwipeSeek.kt`
- Test: `core/src/test/java/com/client/xvideos/common/videoplayer/host/MediaPlayerHostTest.kt`
- Test: `core/src/test/java/com/client/xvideos/common/videoplayer/model/SwipeSeekTest.kt`

**Interfaces:**
- Produces:
  - `fun MediaPlayerHost.seekBy(deltaSeconds: Float)` (метод класса) — `seekTo((currentTime + delta).coerceIn(0f, totalTime.toFloat()))`; при `totalTime <= 0` — ничего.
  - `fun swipeSeekDirection(dx: Float, dy: Float, minDistancePx: Float): Int` — `+1` вперёд, `−1` назад, `0` не свайп.

- [ ] **Step 1: Write the failing tests**

```kotlin
// MediaPlayerHostTest
@Test fun `seekBy сдвигает от текущего времени и складывается до выполнения перемотки`() {
    val host = createHost(); host.updateTotalTime(100); host.updateCurrentTime(10f)
    host.seekBy(5f); host.seekBy(5f)
    assertEquals(20f, host.currentTime, 0.001f)
    assertEquals(20f, host.seekRequest?.seconds)
}
@Test fun `seekBy не выходит за начало и конец`() {
    val host = createHost(); host.updateTotalTime(100)
    host.updateCurrentTime(3f); host.seekBy(-5f); assertEquals(0f, host.currentTime, 0.001f)
    host.updateCurrentTime(98f); host.seekBy(5f); assertEquals(100f, host.currentTime, 0.001f)
}
@Test fun `seekBy без длительности ничего не делает`() {
    val host = createHost(); host.updateCurrentTime(10f)
    host.seekBy(5f)
    assertEquals(10f, host.currentTime, 0.001f); assertNull(host.seekRequest)
}
@Test fun `seekBy не трогает паузу`() {
    val host = createHost(isPaused = true); host.updateTotalTime(100)
    host.seekBy(5f)
    assertTrue(host.isPaused)
}

// SwipeSeekTest
@Test fun `свайп — длинное горизонтальное движение`() {
    assertEquals(1, swipeSeekDirection(dx = 100f, dy = 10f, minDistancePx = 48f))
    assertEquals(-1, swipeSeekDirection(dx = -100f, dy = -10f, minDistancePx = 48f))
}
@Test fun `короткое, диагональное и вертикальное движение — не свайп`() {
    assertEquals(0, swipeSeekDirection(dx = 30f, dy = 0f, minDistancePx = 48f))
    assertEquals(0, swipeSeekDirection(dx = 100f, dy = 60f, minDistancePx = 48f))
    assertEquals(0, swipeSeekDirection(dx = 0f, dy = 200f, minDistancePx = 48f))
}
```

- [ ] **Step 2: Run, expect FAIL** — `:core:testDebugUnitTest --tests *MediaPlayerHostTest* --tests *SwipeSeekTest*` → `Unresolved reference 'seekBy'` / `'swipeSeekDirection'`.
- [ ] **Step 3: Implement** `seekBy` (через `seekTo`, поэтому `isSliding`, `seekRequest` и время ведут себя как у перемотки ползунком) и `swipeSeekDirection` (`|dx| ≥ minDistancePx && |dx| > 2·|dy|`).
- [ ] **Step 4: Run, expect PASS** — та же команда; весь `MediaPlayerHostTest` зелёный.
- [ ] **Step 5: Commit** — `feat(core): перемотка на шаг и распознавание свайпа для плеера` (после гейта).

### Task 2: Свайп в `ComposeVideoPlayer` и в обоих плеерах X

**Files:**
- Modify: `core/src/main/java/com/client/xvideos/common/videoplayer/ui/ComposeVideoPlayer.kt`
- Create: `core/src/main/java/com/client/xvideos/common/videoplayer/ui/component/SwipeSeekBadge.kt`
- Modify: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/molecule/VideoPlayerContentView.kt` (вызов `ComposeVideoPlayer`)
- Modify: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/molecule/LocalVideoPlayerContent.kt` (вызов `ComposeVideoPlayer`)

**Interfaces:**
- Consumes: `MediaPlayerHost.seekBy`, `swipeSeekDirection` (Task 1); `isZoomActive(scale)` уже есть в `core`.
- Produces:
  - `ComposeVideoPlayer(…, swipeSeekSeconds: Int? = null, …)` — `null` — свайпа нет (по умолчанию, поведение прежнее);
  - `@Composable fun SwipeSeekBadge(text: String, modifier: Modifier = Modifier)` — значок в центре видео.

- [ ] **Step 1: Детектор свайпа** — `pointerInput(swipeSeekSeconds)` на той же `Box`, что и `zoomable`, **перед** `.zoomable(...)` в цепочке (внешний узел видит касания после `zoomable`). При масштабе 1 `zoomable` перетаскивание не забирает (`canConsumeGesture`: `zoomState.scale == 1f -> false`), тапы и двойной тап остаются его. Касания не забирать. Схема:

```kotlin
awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    var last = down.position
    var multiTouch = false
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Final)
        if (event.changes.count { it.pressed } > 1) multiTouch = true
        val change = event.changes.firstOrNull { it.id == down.id } ?: break
        last = change.position
        if (!change.pressed) break
    }
    if (multiTouch || isZoomActive(zoomState.scale)) return@awaitEachGesture
    val direction = swipeSeekDirection(last.x - down.position.x, last.y - down.position.y, 48.dp.toPx())
    if (direction != 0) { playerHost.seekBy(direction * seconds.toFloat()); /* значок */ }
}
```

- [ ] **Step 2: Значок** — состояние значка в `ComposeVideoPlayer` (`remember { mutableStateOf<…?>(null) }` с номером свайпа, чтобы повторный свайп перезапускал таймер); `LaunchedEffect` по нему — `delay(600)` и снять. Текст: `"+$seconds с"` / `"−$seconds с"`. `SwipeSeekBadge` — полупрозрачная тёмная плашка с белым текстом, по центру; файл с `@Preview`.
- [ ] **Step 3: Подключить** — `swipeSeekSeconds = 5` в обоих вызовах `ComposeVideoPlayer` (X).
- [ ] **Step 4: Verify** — полный гейт: 0 падений, detekt и lint без ошибок (жест проверяется на устройстве в Task 4).
- [ ] **Step 5: Commit** — `feat(x): свайп по видео перематывает на ±5 с в обоих плеерах`.

### Task 3: Полный экран в плеере скачанного ролика

**Files:**
- Modify: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_LocalVideoPlayerSM.kt`
- Modify: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_LocalVideoPlayer.kt`
- Modify: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/molecule/LocalVideoPlayerContent.kt`
- Create: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/molecule/LocalPlayerFullScreenTopBar.kt`

**Interfaces:**
- Consumes: `OrientationAndSystemBarsEffect(isFullScreen)`, `shouldAutoHideControls`, `X_PlayerBottomBar(host, isFullScreen, onFullScreen, onScrub)`, `FitModeToggle` (внутри панели).
- Produces:
  - `ScreenX_LocalVideoPlayerSM.isFullScreen: Boolean` (`mutableStateOf`, `private set`), `toggleFullScreen()`, `exitFullScreen()` — как у `ScreenX_VideoPlayerSM`;
  - `LocalVideoPlayerContent(…, isFullScreen: Boolean = false, title: String = "", onToggleFullScreen: () -> Unit = {}, onExitFullScreen: () -> Unit = {})`;
  - `@Composable fun LocalPlayerFullScreenTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier)` — стрелка «назад» и название в одну строку с многоточием, тот же фон и отступы (вырез камеры), что у `PlayerFullScreenTopBar`; с `@Preview`.

- [ ] **Step 1: ScreenModel** — `isFullScreen`, `toggleFullScreen`, `exitFullScreen`.
- [ ] **Step 2: Экран** — в `ScreenX_LocalVideoPlayer.Content()` `OrientationAndSystemBarsEffect(sm.isFullScreen)`; `ScreenX_LocalVideoPlayerContent` передаёт `isFullScreen`, `title = resolvedItem.title`, `onToggleFullScreen`, `onExitFullScreen` в `LocalVideoPlayerContent`.
- [ ] **Step 3: `LocalVideoPlayerContent`** — по образцу `VideoPlayerContentView`: `areControlsVisible`; `LaunchedEffect(isFullScreen) { areControlsVisible = true }`; `LaunchedEffect(host, isFullScreen)` — `FILL`/`FIT`; автоскрытие `shouldAutoHideControls(isFullScreen, areControlsVisible, host.isPaused, false, isScrubbing)` с `delay(3500)`; `onScrub` при захвате ползунка возвращает панель; тап — на весь экран показать/спрятать панель, в портрете — пауза; `BackHandler`: зум → `onExitFullScreen` → `onPopBack`; на весь экран сверху `AnimatedVisibility(areControlsVisible)` с `LocalPlayerFullScreenTopBar(title, onBack = onExitFullScreen)`; нижняя панель в `AnimatedVisibility(!isFullScreen || areControlsVisible)` с `isFullScreen` и `onFullScreen = onToggleFullScreen`; плашка возобновления видна при `!isFullScreen || areControlsVisible`. `@Suppress("LongMethod")`, если detekt попросит, — как у `VideoPlayerContentView`.
- [ ] **Step 4: Verify** — полный гейт.
- [ ] **Step 5: Commit** — `feat(x): полный экран в плеере скачанного ролика`.

### Task 4: Проверка на устройствах и документы

- [ ] **Step 1: Телефон владельца** (скачанные ролики есть; приложение под кодом доступа — код вводится, только если владелец дал его в чате этой сессии; нигде не сохраняется). Сборка из рабочего дерева, `adb install -r -t`. Только дамп интерфейса и числа, кадры не смотреть:
  1. Скачанный ролик, портрет: время до свайпа; свайп вправо (`input swipe` по центру видео, ~400 px за 200 мс) — время +5; три свайпа подряд — +15; свайп влево у 0:03 — 0:00; вертикальный свайп — время не меняется; тап — пауза ставится и снимается.
  2. Кнопка полного экрана — альбомная, панели скрыты (`dumpsys window` — insets), через 3,5 с панель управления пропала, тап вернул; Fit/Fill переключается; «Назад» — портрет, ещё «Назад» — выход; свайп на весь экран работает; превью перемотки на весь экран.
  3. Ролик из сети (если сайт доступен): свайп вправо и влево; двойной тап — зум, после зума свайп не перематывает.
- [ ] **Step 2: Складной эмулятор** (холодная загрузка; тестовый ролик — запись экрана эмулятора в загрузках X, см. проход превью): внутренний экран в портрете и альбомной, Medium (`wm density 475`, потом `reset`), внешний экран; полный экран на раскрытом — без поворота, без системных панелей; раскрытие и сложение посреди воспроизведения — плеер, позиция и режим сохраняются. Тестовые файлы — удалить.
- [ ] **Step 3: README** — число тестов и наборов из JUnit XML.
- [ ] **Step 4: Спека** — статус «реализован в `<sha>`» и раздел «Как реализовано» (пороги, что проверено, что нет).
- [ ] **Step 5: Commit** — `docs(readme): …` и `docs(spec): …` отдельными коммитами.
