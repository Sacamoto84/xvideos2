# Превью сохранённого ролика X в карточке — план реализации

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** карточка сохранённого ролика X везде играет по тапу превью из
скачанного файла (5 отрезков по 1 с, по кругу), а по удержанию и двойному
тапу открывает скачанный файл.

**Architecture:** общий путь плеера (`MediaPlayerHost` → `CMPPlayer2` →
`rememberExoPlayerWithLifecycle`) учится играть отрезки одного файла по
кругу (`ClippingMediaSource`), так что превью наследует все защиты (пауза в
фоне и под замком, освобождение). `SavedX_Downloads.localVideo(id)` даёт
сведения о скачанном файле. Общая карточка `UrlVideoImageAndLongClickX`
получает их и выбирает превью из файла; куда вести удержание, решает
`playerScreenFor(item, local)`. Экраны собирают `downloadedVideoIds` и
передают карточкам поиск сведений.

**Tech Stack:** Kotlin, Jetpack Compose, Media3 1.11.1 (`ClippingMediaSource`), Voyager, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-22-x-saved-card-local-preview-design.md`

## Global Constraints

- Отрезки превью: начала в 10, 30, 50, 70, 90% длительности файла, каждый
  1000 мс (конец не дальше длительности); файл короче 5000 мс или с
  неизвестной длительностью — весь файл по кругу. Без звука, `FILL`.
- Тап по сохранённой карточке — превью из файла; повторный тап — постер.
  Удержание и двойной тап — `ScreenX_LocalVideoPlayer(localUrl, item)`.
  Несохранённая карточка — без изменений. Несколько превью одновременно —
  можно.
- Постер сохранённой карточки — локальный `<id>.jpg`, иначе
  `item.previewImage`.
- «История» в режиме выбора: тап и удержание сохранённой карточки
  переключают выбор, как сейчас.
- Меню карточек, значки и длительность не меняются; P2P не трогать.
- Один Composable — один файл; атомы и молекулы с `@Preview`; молекулы без
  ScreenModel; производное от изменяемого списка — не через `remember` с
  ключом-списком; названия сайтов и пути файлов — не в журнал.
- Новые тесты в `feature-x` — без `runTest`.
- Перед каждым коммитом — `gradlew.bat testDebugUnitTest detekt :app:lintRelease`;
  коммиты — в `master`, по явным путям; незакоммиченные файлы владельца
  (L, R, версии, `.idea`) не трогать.

## Review Focus

- Карточка с играющим превью ушла с экрана при прокрутке или уходе с
  экрана — плеер освобождён, ничего не играет в фоне. Проверка — Task 5.
- Уход приложения в фон и экран замка — превью из файла на паузе, как
  остальные плееры (общий путь `CMPPlayer2`). Проверка — Task 5.
- Ролик докачался, пока экран открыт, — карточка без переоткрытия экрана
  начинает играть превью из файла и открывать скачанный файл (поиск
  сведений пересчитывается по собранному `downloadedVideoIds`). Проверка —
  Task 5.
- Короткий файл и файл без длительности — весь файл по кругу. Тест — Task 2.
- Ролик удалён из «Сохранённого», пока его превью играет, — карточка
  уходит из списка, плеер освобождается. Проверка — Task 5.

---

### Task 1: Отрезки файла в общем плеере (`core`)

**Files:**
- Create: `core/src/main/java/com/client/xvideos/common/videoplayer/model/MediaClip.kt`
- Modify: `core/src/main/java/com/client/xvideos/common/videoplayer/host/MediaPlayerHost.kt` (параметр конструктора)
- Modify: `core/src/main/java/com/client/xvideos/common/videoplayer/model/PlayerPlaybackConfig.kt` (`clips`, `toPlaybackConfig`)
- Modify: `core/src/main/java/com/client/xvideos/common/videoplayer/util/CMPlayer2.kt`, `core/src/main/java/com/client/xvideos/common/videoplayer/rememberExoPlayerWithLifecycle.kt`
- Test: `core/src/test/java/com/client/xvideos/common/videoplayer/model/MediaClipTest.kt`, `core/src/test/java/com/client/xvideos/common/videoplayer/host/MediaPlayerHostTest.kt`

**Interfaces:**
- Produces:
  - `data class MediaClip(val startMs: Long, val endMs: Long)` (`@Immutable`);
  - `fun playerRepeatMode(isLooping: Boolean, hasClips: Boolean): Int` — `REPEAT_MODE_OFF` / `ONE` / `ALL`;
  - `MediaPlayerHost(…, clips: List<MediaClip> = emptyList())` → `val clips`; `PlayerPlaybackConfig.clips`; `toPlaybackConfig` переносит;
  - `rememberExoPlayerWithLifecycle(…, clips: List<MediaClip> = emptyList())`: при непустом — `setMediaSources(clips.map { ClippingMediaSource.Builder(источник файла).setStartPositionMs(it.startMs).setEndPositionMs(it.endMs).build() })` вместо одного источника, начальная перемотка не делается; режим повтора — `playerRepeatMode(isLooping, clips.isNotEmpty())`; `clips` — в ключах обоих эффектов.

- [ ] **Step 1: Write the failing tests**

```kotlin
// MediaClipTest
@Test fun `режим повтора — весь список для отрезков, один ролик без них`() {
    assertEquals(Player.REPEAT_MODE_OFF, playerRepeatMode(isLooping = false, hasClips = false))
    assertEquals(Player.REPEAT_MODE_ONE, playerRepeatMode(isLooping = true, hasClips = false))
    assertEquals(Player.REPEAT_MODE_ALL, playerRepeatMode(isLooping = true, hasClips = true))
    assertEquals(Player.REPEAT_MODE_OFF, playerRepeatMode(isLooping = false, hasClips = true))
}
// MediaPlayerHostTest
@Test fun `отрезки хоста доходят до плеера`() {
    val clips = listOf(MediaClip(1000, 2000), MediaClip(5000, 6000))
    assertEquals(clips, MediaPlayerHost(mediaUrl = "file:///v.mp4", clips = clips).toPlaybackConfig().clips)
}
```

- [ ] **Step 2: Run, expect FAIL** — `:core:testDebugUnitTest --tests *MediaClipTest* --tests *MediaPlayerHostTest*` → `Unresolved reference 'MediaClip'`.
- [ ] **Step 3: Implement** по Interfaces. `Player.REPEAT_MODE_*` — константы `media3-common` (в `core` есть).
- [ ] **Step 4: Run, expect PASS**; затем полный гейт.
- [ ] **Step 5: Commit** — `feat(core): плеер играет отрезки одного файла по кругу`.

### Task 2: Сведения о скачанном файле и отрезки превью (`feature-x`, домен)

**Files:**
- Create: `feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedLocalVideo.kt`
- Create: `feature-x/src/main/java/com/client/xvideos/x/feature/saved/LocalPreviewClips.kt`
- Modify: `feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt`
- Test: `feature-x/src/test/java/com/client/xvideos/x/feature/saved/LocalPreviewClipsTest.kt`, `…/SavedX_DownloadsStorageTest.kt`

**Interfaces:**
- Consumes: `MediaClip` (Task 1).
- Produces:
  - `@Immutable data class SavedLocalVideo(val fileUrl: String, val posterPath: String?)`;
  - `fun SavedX_Downloads.localVideo(id: Long): SavedLocalVideo?` (метод) — `contains(id)` → `SavedLocalVideo(localUrl(id), localPosterPath(id))`, иначе `null`;
  - `fun localPreviewClips(durationMs: Long?): List<MediaClip>`;
  - `fun videoDurationMs(fileUrl: String): Long?` — `MediaMetadataRetriever` по пути из `file://`-URI, сбой — `null` (Android, без юнит-теста).

- [ ] **Step 1: Write the failing tests**

```kotlin
// LocalPreviewClipsTest
@Test fun `пять отрезков по секунде в 10–90 процентах`() {
    assertEquals(
        listOf(MediaClip(10_000, 11_000), MediaClip(30_000, 31_000), MediaClip(50_000, 51_000),
            MediaClip(70_000, 71_000), MediaClip(90_000, 91_000)),
        localPreviewClips(100_000L)
    )
}
@Test fun `конец отрезка не дальше длительности`() {
    assertEquals(MediaClip(4_500, 5_000), localPreviewClips(5_000L).last())
}
@Test fun `короткий файл и неизвестная длительность — весь файл`() {
    assertEquals(emptyList<MediaClip>(), localPreviewClips(4_999L))
    assertEquals(emptyList<MediaClip>(), localPreviewClips(null))
    assertEquals(emptyList<MediaClip>(), localPreviewClips(0L))
}
// SavedX_DownloadsStorageTest (тот же каталог и ожидание awaitSaved)
@Test fun `сведения о скачанном файле — только для сохранённого ролика`() {
    writeVideo(5L); writeInfo(5L); File(dir, "5.jpg").writeBytes(ByteArray(4) { 1 })
    writeVideo(6L); writeInfo(6L)
    val downloads = SavedX_Downloads(scope, fakeRenderer); downloads.awaitSaved(5L); downloads.awaitSaved(6L)
    assertEquals(downloads.localUrl(5L), downloads.localVideo(5L)?.fileUrl)
    assertEquals(File(dir, "5.jpg").absolutePath, downloads.localVideo(5L)?.posterPath)
    assertNull(downloads.localVideo(6L)?.posterPath)
    assertNull(downloads.localVideo(7L))
}
```

- [ ] **Step 2: Run, expect FAIL** — `:feature-x:testDebugUnitTest --tests *LocalPreviewClipsTest* --tests *SavedX_DownloadsStorageTest*` → unresolved references.
- [ ] **Step 3: Implement** по Interfaces (`localPreviewClips`: `< 5000` или `null`/`≤ 0` → пусто; иначе `start = durationMs * p / 100` для `p` 10, 30, 50, 70, 90, `end = min(start + 1000, durationMs)`).
- [ ] **Step 4: Run, expect PASS**; полный гейт.
- [ ] **Step 5: Commit** — `feat(x): сведения о скачанном файле и отрезки превью из него`.

### Task 3: Превью из файла в карточке, переход в плеер, лента, поиск, теги, канал

**Files:**
- Create: `feature-x/src/main/java/com/client/xvideos/x/screens/common/LocalVideoPreview.kt`
- Create: `feature-x/src/main/java/com/client/xvideos/x/screens/common/playerScreenFor.kt`
- Modify: `feature-x/src/main/java/com/client/xvideos/x/screens/common/UrlVideoImageAndLongClickX.kt`
- Modify: `dashboards/molecule/DashboardsPaginatedListContent.kt`, `dashboards/molecule/DashboardGridCell.kt`, `dashboards/vm/ScreenXDashBoardsSM.kt` и экран ленты; `search/molecule/SearchResultsView.kt`, `search/ScreenXSearchTab.kt`; `tags/model/TagCellActions.kt`, `tags/molecule/TagGridCell.kt`, `tags/ScreenTags.kt`; `channel/molecule/ChannelVideoItem.kt`, `channel/molecule/ChannelVideosPager.kt`, `channel/ScreenX_Channel.kt` (все — в `feature-x/.../x/screens/`)
- Test: `feature-x/src/test/java/com/client/xvideos/x/screens/common/PlayerScreenForTest.kt`

**Interfaces:**
- Consumes: `SavedLocalVideo`, `SavedX_Downloads.localVideo`, `localPreviewClips`, `videoDurationMs` (Task 2); `MediaPlayerHost(clips = …)` (Task 1).
- Produces:
  - `fun playerScreenFor(item: ItemsX, local: SavedLocalVideo?): Screen` — `ScreenX_LocalVideoPlayer(local.fileUrl, item)` при `local != null`, иначе `ScreenX_VideoPlayer(normalizeXUrl(item.href), item)`;
  - `@Composable fun LocalVideoPreview(video: SavedLocalVideo, posterUrl: String, onClick: () -> Unit, modifier: Modifier = Modifier)` — длительность `videoDurationMs` на `Dispatchers.IO` (`produceState`), затем `MediaPlayerHost(mediaUrl = video.fileUrl, isMuted = true, clips = localPreviewClips(…))`, `FILL`, `VideoPlayerWithMenuContent(…, seekDragEnabled = false, onClick)`, постер поверх, пока `playerHost.poster`, — как `UrlVideoLite`; с `@Preview`;
  - `UrlVideoImageAndLongClickX(…, localVideo: SavedLocalVideo? = null)` — при `localVideo`: постер `localVideo.posterPath ?: item.previewImage`, превью — `LocalVideoPreview`; удержание и двойной тап — по-прежнему колбэки вызывающего;
  - `DashboardsPaginatedListContent(…, localVideoOf: (ItemsX) -> SavedLocalVideo? = { null })`, `DashboardGridCell(…, localVideo: SavedLocalVideo? = null)`, `TagCellActions.localVideoOf` (в `None` — `{ null }`), `ChannelVideoItem(…, localVideo: SavedLocalVideo?)` вместо `isDownloaded` (значок — при `localVideo != null`), `ChannelVideosPager(…, localVideoOf)` вместо `isDownloaded`.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun `сохранённый ролик открывается скачанным файлом, остальные — из сети`() {
    val item = ItemsX(id = 5L, href = "/video5/x")
    val local = SavedLocalVideo(fileUrl = "file:///d/5.mp4", posterPath = null)
    assertEquals("file:///d/5.mp4", (playerScreenFor(item, local) as ScreenX_LocalVideoPlayer).fileUrl)
    assertTrue(playerScreenFor(item, null) is ScreenX_VideoPlayer)
}
```

- [ ] **Step 2: Run, expect FAIL** — `--tests *PlayerScreenForTest*` → `Unresolved reference 'playerScreenFor'`.
- [ ] **Step 3: Implement** `playerScreenFor`, `LocalVideoPreview`, параметр карточки.
- [ ] **Step 4: Экраны ленты, поиска, тегов и канала** — каждый собирает `saved.downloads.downloadedVideoIds` (`collectAsStateWithLifecycle`), строит `localVideoOf = remember(ids) { { item -> if (item.id in ids) saved.downloads.localVideo(item.id) else null } }` и передаёт вниз; переход в плеер вместо `ScreenX_VideoPlayer(...)` — `playerScreenFor(item, saved.downloads.localVideo(item.id))` (в `ScreenXDashBoardsSM.openVideo` — так же).
- [ ] **Step 5: Run** — `PlayerScreenForTest` зелёный; полный гейт.
- [ ] **Step 6: Commit** — `feat(x): превью сохранённого ролика из файла в ленте, поиске, тегах и канале`.

### Task 4: «Избранное», «История», «Сохранённое»

**Files:**
- Modify: `favorites/molecule/FavoriteRow.kt`, `favorites/molecule/FavoritesContent.kt`, `favorites/ScreenFavorites.kt`
- Modify: `history/molecule/HistoryCardMedia.kt`, `history/molecule/HistoryRow.kt`, `history/molecule/HistoryGrid.kt`, `history/model/HistoryRowActions.kt`, `history/ScreenXHistory.kt`
- Modify: `saved/molecule/SavedRow.kt`, `saved/molecule/X_SavedList.kt`, `saved/X_SavedContent.kt`

**Interfaces:**
- Consumes: Task 2 и Task 3 (`localVideoOf`, `playerScreenFor`, `UrlVideoImageAndLongClickX(localVideo)`).
- Produces:
  - `FavoriteRow(item, localVideo: SavedLocalVideo?, onDelete, onDownload, onOpenVideo, …)` — без `localUrl`, `posterUrl`, `onPlayLocal`; всегда `UrlVideoImageAndLongClickX(item, localVideo = localVideo, onLongClick/onDoubleClick = onOpenVideo)`, значок `IconSave18` при `localVideo != null`; `FavoritesContent` — `localVideoOf` вместо `localUrlOf`;
  - `HistoryCardMedia(item, localVideo: SavedLocalVideo?, posterUrl, isSelectionMode, onToggleSelect, onOpenVideo, …)` — без `localUrl`/`onPlayLocal`; в режиме выбора у сохранённой — прежняя ветка «постер + `combinedClickable` переключает выбор»; вне режима — `UrlVideoImageAndLongClickX(localVideo = …)`; `posterUrlOf` остаётся для диалогов;
  - `SavedRow(item, localVideo: SavedLocalVideo, onOpen: (ItemsX) -> Unit, onDelete, onShareP2p, …)` — вместо `clickable` + `UrlImage` — `UrlVideoImageAndLongClickX(item, localVideo = localVideo, onLongClick/onDoubleClick = onOpen)`; меню и длительность прежние.
  - Переход в плеер во всех трёх экранах — `playerScreenFor(item, saved.downloads.localVideo(item.id))`.

- [ ] **Step 1: Переписать молекулы и экраны** по Interfaces; превью-функции — с `localVideo = null` / `SavedLocalVideo("file:///v.mp4", null)`.
- [ ] **Step 2: Verify** — полный гейт (тесты «Истории» и «Избранного», если есть, остаются зелёными; detekt `LongParameterList` — параметры только убираются).
- [ ] **Step 3: Commit** — `feat(x): превью сохранённого ролика в «Избранном», «Истории» и «Сохранённом»`.

### Task 5: Проверка на устройствах и документы

- [ ] **Step 1: Телефон владельца** (скачанные ролики есть; код доступа — только если владелец даст его в чате этой сессии, нигде не сохранять; `monkey` не запускать без записанного `accelerometer_rotation`). Только дампы интерфейса, время и числа, кадры не смотреть:
  1. «Сохранённое»: тап по карточке — превью из файла (в дампе нет плеера на весь экран; постер сменяется видео — по снимку численно), повторный тап — постер; удержание — плеер скачанного ролика с позицией из истории.
  2. «Избранное», «История» (и режим выбора), лента, теги, поиск, канал — то же для сохранённого; несохранённый — превью с сайта и плеер из сети.
  3. Без сети (`svc wifi disable; svc data disable` — только с согласия владельца; иначе пропустить): превью из файла играет.
  4. Два-три превью сразу; прокрутка играющей карточки за экран; Home и возврат; экран замка (если владелец включит автоблокировку) — `dumpsys audio` / `dumpsys media.player` или журнал плеера: нет играющих плееров в фоне.
- [ ] **Step 2: Складной эмулятор** (тестовый ролик — запись экрана эмулятора в загрузках X): карточка в «Сохранённом» на внутреннем и внешнем экране, тап — превью, удержание — плеер.
- [ ] **Step 3: README** — число тестов и наборов. **Спека** — статус «реализован в `<sha>`», раздел «Как реализовано».
- [ ] **Step 4: Commit** — `docs(readme): …`, `docs(spec): …` отдельными коммитами.
