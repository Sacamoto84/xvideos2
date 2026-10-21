# Превью кадра при перемотке в плеере скачанного ролика X — план реализации

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** в плеере скачанного ролика X при перетаскивании ползунка показывать
маленький кадр над бегунком из сетки, собранной из самого файла; ролик
перематывается один раз при отпускании.

**Architecture:** `LocalStoryboards` (в `feature/saved/`) собирает сетку 10×10
кадров ролика в `<id>.storyboard.jpg` рядом с `.mp4`: одна сборка на ролик,
запись через временный файл. Кадры достаёт `RetrieverStoryboardRenderer`
(системный `MediaMetadataRetriever`). `SavedX_Downloads` запускает сборку
после скачивания и удаляет сетку вместе с роликом; экран плеера скачанного
ролика получает файл сетки (готовый или собранный при открытии) и рисует тот
же `SeekPreviewOverlay`, что плеер ролика из сети.

**Tech Stack:** Kotlin, Jetpack Compose, `MediaMetadataRetriever`, Coil 3,
kotlinx.coroutines, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-21-x-local-seek-preview-design.md`

## Global Constraints

- Кадры — только из скачанного файла; сервер не трогаем.
- Сетка: 10 столбцов × 10 строк, кадр `i` (0…99) — позиция `i`% длины
  ролика, строка `i / 10`, столбец `i % 10`; клетка 192×108, картинка
  1920×1080, JPEG; кадр вписан в клетку по центру с чёрными полями.
- Файл сетки — `<id>.storyboard.jpg` в `AppPath.x_cache_download`, временный
  — `getTempPath(dir, "<id>.storyboard.jpg")` (`<…>.storyboard.jpg.temp`).
- Кадры — `MediaMetadataRetriever.getScaledFrameAtTime(…, OPTION_CLOSEST_SYNC, 192, 108)`;
  новых зависимостей нет (`FrameExtractor` из `media3-inspector` — только
  после разговора с владельцем).
- Пока палец ведёт ползунок, ролик стоит; одна перемотка при отпускании
  (`MediaPlayerHost.seekTo`). Перемотки на ходу больше нет ни в одном плеере X.
- Один Composable — один файл, имя файла = имя функции; атомы и молекулы с
  `@Preview`, без ScreenModel в параметрах.
- Названия сайтов не пишутся в коде, комментариях, журнале и коммитах; в
  журнал — без пути и имени файла ролика.
- Новые тесты в `feature-x` — без `runTest`: свой `TestCoroutineScheduler` и
  `StandardTestDispatcher`, продвигать вручную (`advanceUntilIdle()`).
- Перед каждым коммитом — полный гейт
  `gradlew.bat testDebugUnitTest detekt :app:lintRelease`; коммиты — в
  `master`, по явным путям (`git add -- <пути>`), с согласия владельца; в
  рабочем дереве лежат его незакоммиченные файлы (L, R, версии, `.idea`) —
  их не трогать.

## Review Focus

- Сборщик кадров бросил исключение (битый файл, нет видеодорожки, на JVM —
  заглушка Android) — превью показывает только время, временного файла не
  остаётся, приложение не падает. Тест — Task 2.
- Процесс убили посреди сборки — на диске остался `.temp`; следующая сборка
  его перезаписывает, обход каталога его не берёт. Тест — Task 2.
- Плеер закрыли посреди сборки — сборка доходит до конца в области загрузок,
  при следующем открытии сетка уже готова. Тест — Task 2.
- Вертикальный или очень широкий ролик — кадр по центру клетки с полями,
  без выхода за клетку. Тест — Task 1.
- Ролик удалили и скачали заново под тем же id — превью не берёт старую
  картинку из памяти Coil. Проверка — Task 4 (память Coil для локальной
  сетки выключена) и сценарий на устройстве в Task 5.

---

### Task 1: Геометрия сетки

**Files:**
- Create: `feature-x/src/main/java/com/client/xvideos/x/feature/saved/LocalStoryboardSheet.kt`
- Modify: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/model/StoryboardGrid.kt:11-12` (`STORYBOARD_COLUMNS`, `STORYBOARD_ROWS` — `private` → `internal`)
- Test: `feature-x/src/test/java/com/client/xvideos/x/feature/saved/LocalStoryboardSheetTest.kt`

**Interfaces:**
- Produces:
  - `const val LOCAL_STORYBOARD_CELL_WIDTH = 192`, `const val LOCAL_STORYBOARD_CELL_HEIGHT = 108`
  - `fun localStoryboardFileName(id: Long): String` → `"<id>.storyboard.jpg"`
  - `fun storyboardFrameTimeUs(index: Int, durationMs: Long): Long` — начало процента `index`
  - `fun storyboardCellOrigin(index: Int, frameWidth: Int, frameHeight: Int): IntOffset` — левый верхний угол кадра в картинке-сетке
  - Ширина и высота картинки — `LOCAL_STORYBOARD_CELL_* * STORYBOARD_COLUMNS/ROWS` (константы сетки из `StoryboardGrid.kt`, одна правда на чтение и сборку).

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun `имя файла сетки`() {
    assertEquals("42.storyboard.jpg", localStoryboardFileName(42L))
}

@Test fun `время кадра — начало его процента`() {
    assertEquals(0L, storyboardFrameTimeUs(0, 600_000L))
    assertEquals(300_000_000L, storyboardFrameTimeUs(50, 600_000L))
    assertEquals(990_000L, storyboardFrameTimeUs(99, 1_000L))
    assertEquals(35_640_000_000L, storyboardFrameTimeUs(99, 36_000_000L)) // 10 часов — без переполнения
}

@Test fun `кадр по центру своей клетки`() {
    assertEquals(IntOffset(0, 0), storyboardCellOrigin(0, 192, 108))
    assertEquals(IntOffset(641, 108), storyboardCellOrigin(13, 61, 108))   // вертикальный ролик
    assertEquals(IntOffset(1728, 985), storyboardCellOrigin(99, 192, 81)) // широкий ролик
    assertEquals(IntOffset(576, 108), storyboardCellOrigin(13, 200, 120))  // больше клетки — не выходит за её угол
}
```

- [ ] **Step 2: Run, expect FAIL** — `cmd /c ".\gradlew.bat :feature-x:testDebugUnitTest --tests *LocalStoryboardSheetTest*"` → `Unresolved reference 'localStoryboardFileName'`.
- [ ] **Step 3: Implement** the three functions and two constants in `LocalStoryboardSheet.kt`; make the two grid constants `internal`. Origin: `столбец * 192 + max(0, (192 − w) / 2)`, `строка * 108 + max(0, (108 − h) / 2)`.
- [ ] **Step 4: Run, expect PASS** — та же команда, 3 теста, 0 падений.
- [ ] **Step 5: Commit** — `feat(x): геометрия сетки кадров скачанного ролика` (после гейта).

### Task 2: Сборка сетки — одна на ролик, через временный файл

**Files:**
- Create: `feature-x/src/main/java/com/client/xvideos/x/feature/saved/LocalStoryboards.kt`
- Test: `feature-x/src/test/java/com/client/xvideos/x/feature/saved/LocalStoryboardsTest.kt`

**Interfaces:**
- Consumes: `localStoryboardFileName(id)` (Task 1), `getTempPath(dirPath, fileName)` (`core/.../kdownloader/utils/Utils.kt`).
- Produces:
  - `fun interface StoryboardRenderer { suspend fun render(video: File, out: File): Boolean }` — пишет JPEG сетки в `out`; `false` — собрать не удалось.
  - `class LocalStoryboards(dir: String, scope: CoroutineScope, renderer: StoryboardRenderer, io: CoroutineDispatcher = Dispatchers.IO)`:
    - `fun file(id: Long): File` — `<dir>/<id>.storyboard.jpg`;
    - `suspend fun obtain(id: Long): File?` — готовая сетка с диска; иначе ждёт идущую сборку или запускает новую; `null` — нет ролика `<id>.mp4`, `id <= 0`, сбой или сборку отменили. Отмену самого вызывающего пробрасывает;
    - `fun prepare(id: Long)` — `scope.launch { obtain(id) }`, для конца скачивания;
    - `fun cancel(id: Long)` — отменяет идущую сборку ролика.
- Правила сборки (их держат тесты): сборка живёт в `scope` (не в корутине вызывающего) и одна на `id` (`HashMap<Long, Deferred<File?>>` под замком, запись убирается по завершении); рендер — на `io`, в `out` = временный файл; любое исключение рендера (кроме отмены) — `false`, в журнал только класс ошибки; переименование временного в готовый — только если сборка не отменена и `<id>.mp4` ещё на диске, иначе временный удаляется; при `false` временный удаляется.

- [ ] **Step 1: Write the failing tests** (`TemporaryFolder`, `TestCoroutineScheduler`, `StandardTestDispatcher` и как `scope`, и как `io`; фальшивый рендер считает вызовы и пишет байты в `out`, по желанию ждёт `CompletableDeferred`):
  - `готовая сетка на диске — без сборки` — файл сетки есть → `obtain` возвращает его, вызовов рендера 0.
  - `нет ролика — null без сборки` — нет `<id>.mp4` → `null`, вызовов 0; `obtain(0L)` → `null`.
  - `два запроса во время сборки — одна сборка` — рендер ждёт ворота; два `obtain` → вызовов 1; ворота открыты → оба получили один файл, файл есть, `.temp` нет.
  - `сбой сборки — null, следующий запрос собирает заново` — первый рендер `false`, второй `true` → `null`, затем файл; вызовов 2; `.temp` после первого нет.
  - `исключение рендера — null и без временного файла` — рендер пишет `out` и бросает `RuntimeException` → `null`, ни файла, ни `.temp`.
  - `остаток оборванной сборки перезаписывается` — лежит `<id>.storyboard.jpg.temp` с мусором → `obtain` даёт файл с байтами нового рендера, `.temp` нет.
  - `плеер закрыли посреди сборки — сетка всё равно готова` — `obtain` в отдельной `Job`, рендер ждёт ворота; `job.cancel()`; ворота открыты → файл есть.
  - `ролик удалили посреди сборки — ни сетки, ни временного файла` — рендер написал `out` и ждёт ворота; удалить `<id>.mp4`, `cancel(id)`; ворота открыты → `obtain` вернул `null`, файла и `.temp` нет.
- [ ] **Step 2: Run, expect FAIL** — `--tests *LocalStoryboardsTest*` → `Unresolved reference 'LocalStoryboards'`.
- [ ] **Step 3: Implement** `StoryboardRenderer` и `LocalStoryboards` по правилам выше.
- [ ] **Step 4: Run, expect PASS** — 8 тестов, 0 падений; ещё раз весь `:feature-x:testDebugUnitTest` (тесты X не должны оставить висящих корутин).
- [ ] **Step 5: Commit** — `feat(x): сборка сетки кадров скачанного ролика, одна на ролик`.

### Task 3: Кадры из файла и загрузки X

**Files:**
- Create: `feature-x/src/main/java/com/client/xvideos/x/feature/saved/RetrieverStoryboardRenderer.kt`
- Modify: `feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt` (конструктор, `completeDownload`, `delete`, `deleteFiles`)
- Test: `feature-x/src/test/java/com/client/xvideos/x/feature/saved/SavedX_DownloadsStorageTest.kt`

**Interfaces:**
- Consumes: Task 1 (`storyboardFrameTimeUs`, `storyboardCellOrigin`, размеры), Task 2 (`StoryboardRenderer`, `LocalStoryboards`).
- Produces:
  - `object RetrieverStoryboardRenderer : StoryboardRenderer`;
  - `class SavedX_Downloads(scope: CoroutineScope, storyboardRenderer: StoryboardRenderer = RetrieverStoryboardRenderer)` с `val storyboards: LocalStoryboards` (объявить сразу после `dir`: `init` уже запускает обход каталога).

- [ ] **Step 1: Write the failing tests** в `SavedX_DownloadsStorageTest`:
  - `после скачивания собирается сетка` — `SavedX_Downloads(scope, фальшивый рендер)`, `writeVideo(7)`, `markStarted(7)`, `completeDownload(item(7))` → в течение 3 с появляется `7.storyboard.jpg` (ожидание как в `awaitSaved`).
  - `удаление ролика убирает и его сетку` — `7.storyboard.jpg` и `7.storyboard.jpg.temp` рядом с роликом 7 и `8.mp4` → `deleteFiles(7L)` → на диске только `8.mp4`.
  - `сетка не считается обложкой` — `9.mp4`, `9.info`, `9.storyboard.jpg`, без `9.jpg` → после `awaitSaved(9)` `localPosterPath(9L) == null`.
- [ ] **Step 2: Run, expect FAIL** — `--tests *SavedX_DownloadsStorageTest*`: первый — нет параметра конструктора; второй — файлы сетки остались. Третий проходит и до правки: обход уже пропускает `.jpg` с нечисловым именем; это страж выбора имени `<id>.storyboard.jpg`, а не красный тест.
- [ ] **Step 3: Implement**
  - `SavedX_Downloads`: `completeDownload` в ветке успеха после `loadFromDisk()` → `storyboards.prepare(item.id)`; `delete` до `deleteFiles` → `storyboards.cancel(item.id)`; `deleteFiles` удаляет `localStoryboardFileName(id)` и его временный **после** `"$id.mp4"` (порядок важен: сборка проверяет `.mp4` перед переименованием).
  - `RetrieverStoryboardRenderer.render`: `MediaMetadataRetriever().use { … }` — `setDataSource(video.absolutePath)`, длительность из `METADATA_KEY_DURATION` (нет или ≤ 0 → `false`); картинка 1920×1080 `ARGB_8888`, залитая чёрным; для `i = 0…99`: `ensureActive()`, `getScaledFrameAtTime(storyboardFrameTimeUs(i, d), OPTION_CLOSEST_SYNC, 192, 108)`, `null` — клетка остаётся чёрной; кадр рисуется в `storyboardCellOrigin(i, w, h)` и освобождается. Ни одного кадра → `false`. JPEG качества 80 в `out`, картинка освобождается.
- [ ] **Step 4: Run, expect PASS** — `SavedX_DownloadsStorageTest` целиком (включая прежний `успешная загрузка пишет info…`: настоящий рендер на JVM бросает исключение заглушки Android, `LocalStoryboards` его глотает).
- [ ] **Step 5: Commit** — `feat(x): сетка кадров собирается после скачивания и удаляется с роликом`.

### Task 4: Превью в плеере скачанного ролика

**Files:**
- Modify: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/model/rememberStoryboardImage.kt`
- Modify: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/molecule/X_PlayerBottomBar.kt`
- Modify: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/molecule/VideoPlayerContentView.kt:410` (убрать `seekWhileDragging = false`)
- Modify: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/molecule/LocalVideoPlayerContent.kt`
- Modify: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_LocalVideoPlayer.kt` (корневая `ScreenX_LocalVideoPlayerContent`)

**Interfaces:**
- Consumes: `SavedX_Downloads.storyboards.obtain(id)` (Task 3), `SeekPreviewOverlay`, `SeekScrub`.
- Produces:
  - `fun rememberStoryboardImage(data: Any?): ImageBitmap?` — `String` (адрес; пустой → `null`) или `File`; для `File` память Coil выключена (`memoryCachePolicy(CachePolicy.DISABLED)`): ролик могут удалить и скачать заново под тем же id;
  - `X_PlayerBottomBar(host, isFullScreen, onFullScreen, onScrub, modifier)` — без `seekWhileDragging`: при перетаскивании только `isSliding = true` и `onScrub`, перемотка — при отпускании;
  - `LocalVideoPlayerContent(…, modifier, storyboard: File? = null)`.

- [ ] **Step 1: Убрать `seekWhileDragging`** из `X_PlayerBottomBar` (параметр, `rememberUpdatedState`, строка `host.seekToTime = v`, KDoc) и из вызова в `VideoPlayerContentView`. Прогнать `:feature-x:testDebugUnitTest` — зелёный (`SeekDragTest`, `ControlsAutoHideTest` не меняются).
- [ ] **Step 2: Обобщить `rememberStoryboardImage`** до `data: Any?` по Interfaces; вызов в `VideoPlayerContentView` не меняется.
- [ ] **Step 3: `LocalVideoPlayerContent`** — как в `VideoPlayerContentView`: `scrub` (`remember { mutableStateOf<SeekScrub?>(null) }`), `onScrub` через `remember`, `val image = rememberStoryboardImage(storyboard)`; в `overlay` перед панелью — `SeekPreviewOverlay(storyboard = image, scrub = scrub, durationSec = host.totalTime)`; `X_PlayerBottomBar(host = host, onScrub = onScrub)`. Превью-функция компилируется без изменений (новый параметр со значением по умолчанию).
- [ ] **Step 4: `ScreenX_LocalVideoPlayerContent`** — `val storyboard by produceState<File?>(initialValue = null, videoId) { value = sm.saved.downloads.storyboards.obtain(videoId) }` и `storyboard = storyboard` в `LocalVideoPlayerContent`. Сетка на диске, поэтому `produceState` при возврате на экран стоит одного обращения к файлу — в ScreenModel ничего не кладём.
- [ ] **Step 5: Verify** — полный гейт: 0 падений, detekt и lint без ошибок.
- [ ] **Step 6: Commit** — `feat(x): превью кадра при перемотке в плеере скачанного ролика`.

### Task 5: Проверка на устройстве и документы

- [ ] **Step 1: Эмулятор** `Pixel_10_Pro_Fold`, холодная загрузка (`-no-snapshot-load -no-snapshot-save`; со снимком часы отстают и ролики X не качаются), сборка из рабочего дерева, `adb install -r -t`. Кадры ролика не снимать и не смотреть: только дамп интерфейса, список файлов через `run-as` и численная проверка пикселей.
  1. Скачать ролик X из ленты. Через `run-as com.client.xvideos ls -l files/…/X/Download` — появился `<id>.storyboard.jpg` (около 0,1–0,4 МБ), `.temp` нет. Записать, сколько секунд от «Скачано» до появления файла.
  2. «Сохранённое» → «Загрузки» → открыть ролик. Тянуть ползунок медленным `input swipe`: время на полосе стоит, в дампе есть подпись времени превью; после отпускания время встаёт в новую точку и идёт дальше. Численно: в прямоугольнике кадра превью (над бегунком) во время перетаскивания есть не только чёрные пиксели.
  3. Удалить сетку через `run-as rm`, открыть ролик снова: сначала превью только со временем, через несколько секунд — с кадром; файл сетки снова на диске.
  4. Удалить ролик в приложении — ни `.mp4`, ни `.storyboard.jpg`, ни `.temp`. Скачать его заново и открыть — превью с кадром (не из памяти Coil).
  5. Складной: то же превью на внешнем экране — кадр не выходит за края.
- [ ] **Step 2: README** — число тестов и наборов (`<module>/build/test-results/testDebugUnitTest/*.xml`) в строке «юнит-тестов … в … файлах».
- [ ] **Step 3: Спека** — статус «реализован в `<sha>`», а в «Что остаётся сборщику» — что выбрано (время кадра — начало процента, JPEG 80).
- [ ] **Step 4: Commit** — `docs(readme): …` и `docs(spec): …` отдельными коммитами.
- [ ] **Step 5: Владельцу** — сценарий для телефона: скачать длинный ролик, засечь время сборки сетки, перемотать скачанный ролик; сказать, что перемотка на ходу в X убрана целиком, а канал `seekToTime` в `core` остался без писателей (отдельная работа).
