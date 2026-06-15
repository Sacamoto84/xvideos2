# L module — three UI fixes (design)

Date: 2026-06-15
Module: `app/src/main/java/com/client/xvideos/l`

Three independent, small fixes in the Luscious (L) section. Each has a confirmed
root cause and a minimal fix. No shared state between them.

---

## Issue 1 — album-list scroll position resets to 0 after returning from an album

### Symptom
In the L explorer, the album list (`L_ScreenAlbumList`, rendered as
`L_ScreenAlbumList.Content()` inside `L_ScreenExplorer`) loses its scroll
position. Open a concrete album from the list, press back — the list jumps to
position 0. No scroll restoration.

### Root cause
- `ScreenLAlbumListSM` already holds a persistent per-page scroll map intended to
  survive navigation:
  - `ScreenAlbumListSM.kt:89` — `val stateGrid = mutableStateMapOf<Int, LazyGridState>()`
  - `ScreenAlbumList.kt:304` — `vm.stateGrid.clear()` on filter change (proof of intent).
- But the actual grid is backed by a **local** state, not the SM map:
  - `ScreenAlbumList.kt:209` — `val stateGrid = rememberSaveable(page, saver = LazyGridState.Saver) { LazyGridState() }`
- `L_ScreenExplorer` hosts tabs via a `when (vm.screenType) { 0 -> L_ScreenAlbumList.Content() ... }`
  inline call (`L_ScreenExplorer.kt:128-134`). When the user taps an album,
  `navigator.push(ScreenLAlbum(...))` pushes onto the navigator and the whole
  explorer subtree leaves composition. The local `rememberSaveable` is not
  restored across this push, so the grid state is recreated at index 0.
- Sibling tabs (Saved Albums, Top Hits, Search) keep their scroll because they
  store the list state in their ScreenModel — proving ScreenModel state survives
  the push reliably in this app, while the local saveable here does not.

### Fix
Wire the grid to the persistent SM map. Replace `ScreenAlbumList.kt:209`:

```kotlin
// before
val stateGrid = rememberSaveable(page, saver = LazyGridState.Saver) { LazyGridState() }
// after
val stateGrid = vm.stateGrid.getOrPut(page) { LazyGridState() }
```

`vm.stateGrid` is a `mutableStateMapOf` in the ScreenModel, so per-page scroll
state survives the navigation push/pop. Matches the author's original intent
(the map and its `clear()` on filter reset already exist). `bigList` and
`statePager` already persist in the SM, so page data is present on return and the
grid can restore its index immediately.

### Alternatives considered
- Pager-level `SaveableStateHolder` keyed by page — more code, duplicates what the
  SM map already provides. Rejected.

### Files
- `app/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumList.kt` (line 209)

---

## Issue 2 — two "scroll to top" Floating Action Buttons on the album screen

### Symptom
On the album screen, while items are loading and the user scrolls, a FAB ("up")
appears that looks like two stacked buttons (a "shadow" under it). When the album
finishes loading, the shadow disappears.

### Root cause
Two FABs render simultaneously, both anchored bottom-end, both driven by
`host.state`:
1. `ScreenAlbum.kt:170-176` — Scaffold `floatingActionButton` slot renders
   `ScrollToTopButton(vm.host.state)`, visible when `firstVisibleItemIndex > 3 &&
   selectedImage == null`.
2. `L_LazyRowPictureDetails.kt:256-267` — the shared list component renders its
   own `FloatingActionButton`, visible when `firstVisibleItemIndex > 2`.

Because both are bottom-end on the same scroll state, they overlap (offset
slightly: thresholds `>3` vs `>2`, and the Scaffold FAB sits above the bottomBar
progress indicator). That overlap reads as "two buttons / extra shadow". When the
album finishes loading, the `LinearProgressIndicator` bottomBar
(`ScreenAlbum.kt:177-187`) disappears, the Scaffold FAB drops down onto the inner
FAB, and the visual changes.

### Fix
Remove the duplicate Scaffold FAB from the album screen; keep the shared inner FAB
(used by every screen built on `L_LazyRowPictureDetails`, e.g. the Collection tab).

- Delete the `floatingActionButton = { ... ScrollToTopButton(vm.host.state) }`
  block from the Scaffold in `ScreenAlbum.kt:170-176`.
- `ScrollToTopButton.kt` is then used only there (confirmed via grep) — delete the
  now-dead file `screenAlbum/atom/ScrollToTopButton.kt`.
- Remove the now-unused `import ...ScrollToTopButton` from `ScreenAlbum.kt`.

Resulting behavior: a single "up" FAB (the shared one, threshold `index > 2`), no
overlap, no phantom shadow.

### Alternatives considered
- Keep the Scaffold FAB, remove the inner one — would break all other screens that
  rely on the shared component's FAB. Rejected.

### Files
- `app/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenAlbum.kt` (remove FAB block + import)
- `app/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/atom/ScrollToTopButton.kt` (delete)

---

## Issue 3 — Lavender dialog confirm button must be a pill (50%), not ~16dp

### Symptom
On every dialog themed with `Theme.DialogLavande`, the confirm button corner is a
small radius (looks like ~16.dp). It should be fully rounded — a pill, 50%.

### Root cause
- `Theme.kt:57` — `const val buttonBorderRadius = 50f` (a **Float**).
- `LavenderDialog.kt:99` — `shape = RoundedCornerShape(d.buttonBorderRadius)`.
- `RoundedCornerShape(Float)` is the **pixel** overload: `50f` = 50 px corner
  radius (a small fixed radius), not 50 percent.
- All Lavender-themed dialogs route through the single `LavenderDialog` component
  (confirmed: `AlbumDialogDeleteAlbum`, `P2pSendChooserDialog`,
  `DaialogNewCollection`, `L_Screen_CollectionTab`, `R_Screen_CollectionTab` — all
  call `LavenderDialog(...)`; the `AlertDialog`/`Dialog` imports in those files are
  unused leftovers, and other `RoundedCornerShape(8.dp)` calls are for image
  clipping, not the confirm button).

### Fix
Make `buttonBorderRadius` an `Int` so the call site hits the **percent** overload:

```kotlin
// Theme.kt:57
const val buttonBorderRadius = 50   // percent -> RoundedCornerShape(50) = 50% pill
```

`LavenderDialog.kt:99` stays as `RoundedCornerShape(d.buttonBorderRadius)`; with an
`Int` argument it resolves to `RoundedCornerShape(percent: Int)` = 50% = pill. One
edit fixes every Lavender dialog. `buttonBorderRadius` is referenced nowhere else
(only `Theme.kt` def + `LavenderDialog.kt`), so the `Float -> Int` change is safe.

### Alternatives considered
- Hardcode `RoundedCornerShape(percent = 50)` in `LavenderDialog` — drops the theme
  constant's meaning. Rejected.

### Files
- `app/src/main/java/com/client/xvideos/common/theme/Theme.kt` (line 57)

---

## Scope summary

Edits in 3 files, plus 1 file deletion:
- `screenAlbumList/ScreenAlbumList.kt` — line 209 (Issue 1)
- `screenAlbum/ScreenAlbum.kt` — remove FAB block + import (Issue 2)
- `screenAlbum/atom/ScrollToTopButton.kt` — delete (Issue 2)
- `common/theme/Theme.kt` — line 57 (Issue 3)

### Verification
No UI test harness in the project. Verify manually / via Compose `@Preview`:
- Issue 1: scroll the album list deep, open an album, back → position retained.
- Issue 2: scroll the album screen → exactly one "up" FAB, no phantom shadow when
  loading completes.
- Issue 3: open any Lavender dialog (e.g. delete album) → confirm button is a full
  pill (50%).
- Build: `./gradlew assembleDebug` compiles (confirms `Float -> Int` change and
  removed import/file cause no breakage).
