# Figma Design Copy (as-is) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **Figma-specific REQUIRED SUB-SKILLS (load at execution time):**
> - `/figma-use` — MANDATORY before any `use_figma` call.
> - `/figma-generate-design` — when translating an app screen/layout into Figma.
> - `/figma-generate-library` — when building the design-system / component library.

**Goal:** Build a one-file Figma "designer copy (as-is)" of the app — color system as Figma Variables + catalog, a component library, all X/L/R/common screens, every dialog and expand-menu, with per-screen color annotations.

**Architecture:** Approach C. First create Figma Variables (1:1 with code tokens) + text styles + catalog, then a component library with all fills bound to those variables, then assemble screens from components page-by-page, then dialogs/menus, then the index. All authoring via the Figma MCP. Source of truth for layout = Compose source files (reconstruction, placeholder content). Source of truth for color = `common/theme/Theme.kt` + `ui/theme/Color.kt`.

**Tech Stack:** Figma MCP (`use_figma`, `create_new_file`, `get_design_context`, `get_screenshot`, `get_variable_defs`, `whoami`); Figma Variables + Text Styles; spec `docs/superpowers/specs/2026-06-13-figma-design-copy-design.md`.

---

## Conventions for this plan

**No git commits per task.** Figma content lives in the Figma cloud file, not in this repo. "Done" for a task = the node(s) exist in Figma with fills bound to Variables and an annotation present, confirmed by readback. Only this plan's checkboxes track progress.

**Verify-by-readback (replaces the test/commit cycle).** After building a node, confirm with:
- `get_screenshot` of the node → visual sanity vs the Compose source / real screen.
- `get_design_context` or `get_variable_defs` on the node → confirm fills reference a Variable (named token), not a raw hex literal.

**Starter-plan constraint (discovered at execution).** The Figma account is on the
Starter plan, and the Figma MCP cannot persist additional pages on it (only the
file's single default page survives a commit). So the 8 logical pages `00–07`
become **8 Figma Sections on one page** `Design Copy (as-is)` (file key
`7NP57du3gbSpG3RtjQMTG1`). File-level Variables are unaffected (still one shared
source of truth). Section ids: `00`=`8:2`, `01`=`8:3`, `02`=`8:4`, `03`=`8:5`,
`04`=`8:6`, `05`=`8:7`, `06`=`8:8`, `07`=`8:9`. Everywhere the plan says "page NN",
read "section NN". Also: `use_figma` returns **no values** — read state only via
`get_metadata` / `get_screenshot` / `get_variable_defs`.

**Frame size:** phone `360×800` dp. One frame per screen; add a second frame for a state (loading/empty/content) **only if colors differ**.

**Annotation format:** numbered callouts on the frame → a side legend mapping `element → token (hex)`. Every frame and component fill must reference a Variable (exception: deliberate `Unsorted/*` entries).

---

## Screen Build Procedure (shared — referenced by every screen task)

Each screen task supplies its own **inputs** (source file, components used, token bindings, annotations). Run these generic steps with those inputs:

- **P1:** Read the screen's Compose source file(s) listed in the task. Identify layout regions, bars, lists, cards, text styles, and the color token each region uses.
- **P2:** Load `/figma-generate-design`. Create the `360×800` frame on the section's page, named exactly as in the task.
- **P3:** Assemble the frame from existing library components (Phase 2). Place placeholder content for media/text.
- **P4:** Bind every fill to the Variable named in the task's binding table. No raw hex except `Unsorted/*`.
- **P5:** Add numbered callouts + side legend (`element → token (hex)`) per the task's annotation list.
- **P6 (verify):** `get_screenshot` of the frame (visual vs source) **and** `get_variable_defs`/`get_design_context` on the frame → confirm all fills resolve to named Variables. Fix any raw-hex leak (add to `Unsorted/*` only if intentional).

---

## Phase 0 — Prerequisites & file scaffold

### Task 0.1: Verify Figma access and fonts

**Files:** none (environment check).

- [ ] **Step 1:** Call Figma MCP `whoami`. Expected: authenticated user returned. If not authenticated, stop and have the user authenticate the Figma MCP, then retry.
- [ ] **Step 2:** Confirm fonts **Poppins** (Regular/Medium/SemiBold/Bold/ExtraBold) and **DM Sans** are available in Figma. If missing, record in the plan as a known deviation (text will fall back to default) and proceed.

### Task 0.2: Create the Figma file and pages

**Files:** Figma file `App — Design Copy (as-is)`.

- [ ] **Step 1:** Load `/figma-use`.
- [ ] **Step 2:** `create_new_file` named `App — Design Copy (as-is)`.
- [ ] **Step 3:** Create 8 pages exactly: `00 · Index / Cover`, `01 · Color System`, `02 · Components`, `03 · X`, `04 · L`, `05 · R`, `06 · Common`, `07 · Dialogs & Menus`.
- [ ] **Step 4 (verify):** `get_metadata` on the file → confirm all 8 pages exist with exact names.

---

## Phase 1 — Color system (page `01 · Color System`)

### Task 1.1: Create Variable collections + variables

**Source of truth:** `app/src/main/java/com/client/xvideos/common/theme/Theme.kt`, `app/src/main/java/com/client/xvideos/ui/theme/Color.kt`.

- [ ] **Step 1:** Load `/figma-use`.
- [ ] **Step 2:** Create collection **`Theme/Shared`** (1 mode: `dark`) with variables (exact hex):
  `background #262626`, `backgroundAppRoot #262626`, `tabLevel0 #212121`, `tabLevel1 #282828`, `tabLevel2 #333333`, `tabLevel3 #444444`, `tabLevel4 #555555`, `tabLevel5 #666666`, `tabLevel6 #777777`.
- [ ] **Step 3:** Create collection **`Theme/R`**:
  `colorCommonBackground #303030`, `colorBottomBarDivider #323153`, `colorYellow #EBFA63`, `colorBlue #61B2EB`, `colorRed #EA616F`, `colorTextGray #8B8B8B`, `colorBorderSelect #444444`, `colorBorderGray #3F3F3F`.
- [ ] **Step 4:** Create collection **`Theme/L`**:
  `grey0 #dedede`, `grey1 #bababa`, `grey2 #9c9c9c`, `grey3 #3b3b3b`, `grey4 #333333`, `grey5 #292929`, `grey6 #262626`, `grey7 #1c1c1c`, `g0 #4CAF50`, `r0 #F44336`, `b0 #2196F3`, `lavender #a3aff5`, `primaryColor #ff96a3`, `red #C9554C`, `secondaryColor #3b3b3b`, `textColor #bababa` (alias of grey1), `ExpandMenu_tint #1F1F1F`, `ExpandMenu_bg #FFFAF5`.
- [ ] **Step 5:** Create collection **`Theme/X`**: `expandMenuBackground #F2EDF7`.
- [ ] **Step 6:** Create collection **`Legacy/PornHub`**:
  `Orange #EF9E00`, `Red #E01E5A`, `Green #1ED760`, `Blue #0095F6`, `Purple #9C27B0`, `Yellow #FFD600`, `Pink #F06292`, `Brown #A1887F`, `Grey #9E9E9E`, `separator #9E9E9E`, `gray0E #0E0E0E`, `gray10 #101010`, `gray15 #151515`, `gray1F #1F1F1F`, `gray21 #212121`, `gray25 #252525`, `gray3F #3F3F3F`, `gray96 #969696`, `grayC6 #C6C6C6`, `Black #000000`, `White #FFFFFF`.
- [ ] **Step 7:** Create collection **`Material/XvideosTheme`**:
  `Purple80 #D0BCFF`, `PurpleGrey80 #CCC2DC`, `Pink80 #EFB8C8`, `Purple40 #6650A4`, `PurpleGrey40 #625B71`, `Pink40 #7D5260`.
- [ ] **Step 8 (verify):** `get_variable_defs` on the file → confirm every variable above exists with the exact hex. Fix mismatches.

### Task 1.2: Create text styles

**Source of truth:** `Theme.L.Type{}` in `common/theme/Theme.kt`; fonts Poppins + DM Sans.

- [ ] **Step 1:** Create Figma text styles (font = DM Sans unless noted), `size/lineHeight`:
  `screenTitle 24/30 Medium`, `heroTitle 28/34 SemiBold`, `sectionTitle 13/18 Medium`, `rowTitle 16/22 Medium`, `rowValue 16/22 Normal`, `rowSubtitle 13/18 Normal`, `body 16/22 Normal`, `bodyLarge 18/24 Normal`, `button 16/20 Medium`, `caption 12/16 Normal`, `mediaIndex 12/16 Medium`, `menuItem 18/24 Normal`, `dialogTitle 18/24 Medium`, `dialogBody 14/20 Normal`.
- [ ] **Step 2:** Default text-style color = `Theme/L.textColor` (grey2 `#9c9c9c` for `sectionTitle`/`rowSubtitle`/`caption`, per source).
- [ ] **Step 3 (verify):** `get_design_context` on the styles → confirm names, sizes, weights match the table.

### Task 1.3: Build the catalog layout

**Page:** `01 · Color System`.

- [ ] **Step 1:** Load `/figma-use`. For each collection from Task 1.1, lay out a swatch grid; each swatch = color chip + token name + hex + one-line "used in" note.
- [ ] **Step 2:** Add a **contrast block**: dark section backgrounds (`tabLevel*`) next to the light expand-menus (`#FFFAF5`, `#F2EDF7`) to highlight the deliberate contrast.
- [ ] **Step 3:** Add a typography specimen block rendering each text style.
- [ ] **Step 4:** Add a note on `Material/XvideosTheme`: "dynamic color (Material You, Android 12+); barely visible on custom-themed screens."
- [ ] **Step 5 (verify):** `get_screenshot` of page `01` → every collection + typography + contrast block present; swatch fills bound to Variables.

### Task 1.4: Audit hardcoded colors → `Unsorted/*`

**Goal:** catch `Color(0x…)` literals in Compose not covered by tokens.

- [ ] **Step 1:** Search the codebase for raw color literals: pattern `Color\(0x[0-9A-Fa-f]{8}\)` across `app/src/main/java/**/*.kt`. Use `ctx_execute` (shell + ripgrep) so the full match list stays in the sandbox; print only `hex → file:line` deduped.
- [ ] **Step 2:** Drop any hex already present in Task 1.1 collections. For each remaining unique hex, create a variable in a new collection **`Unsorted/Misc`** named `unsorted_<hex>` with a "used in `file`" note (expected examples: niche `0x0F0F0F`, fullscreen-player backgrounds).
- [ ] **Step 3:** Add the `Unsorted/Misc` grid to the catalog page.
- [ ] **Step 4 (verify):** `get_variable_defs` → `Unsorted/Misc` exists; re-run the Step 1 search and confirm no remaining literal is missing from Variables (covered or in `Unsorted/Misc`).

---

## Phase 2 — Component library (page `02 · Components`)

Build with `/figma-generate-library`. Every component uses auto-layout, variants where listed, all fills bound to Variables, and a token-name callout.

### Task 2.1: Bars

- [ ] **Step 1:** Load `/figma-generate-library`.
- [ ] **Step 2:** `TopBar` — bg `Theme/Shared.tabLevel0`, title `screenTitle`.
- [ ] **Step 3:** `BottomNavBar` with variants `R` / `L` / `X` — bg per section (R divider `Theme/R.colorBottomBarDivider`; bars use `tabLevel*`).
- [ ] **Step 4:** `TabRow` + `SubTabRow` — selected indicator vs unselected (`Theme/R.colorBorderSelect` where applicable).
- [ ] **Step 5 (verify):** `get_screenshot` + `get_variable_defs` → variants exist, fills bound.

### Task 2.2: Media

- [ ] **Step 1:** `VideoCard`, `GifCard`, `AlbumCover`, `ImageThumb` — placeholder image fill, rounded corners per source.
- [ ] **Step 2:** `DurationBadge` and `IndexBadge` (text style `mediaIndex`).
- [ ] **Step 3 (verify):** `get_screenshot` + binding check.

### Task 2.3: Lists

- [ ] **Step 1:** `SettingsRow` (`rowTitle` + `rowValue`), `CollectionItem`, `CreatorRow`, `TagChip`.
- [ ] **Step 2 (verify):** `get_screenshot` + binding check.

### Task 2.4: Controls

- [ ] **Step 1:** `Button` variants `primary` / `secondary` / `text` (`button` text style); `CheckboxWithLabel`; `TextField`; `Seekbar` + player-control icons.
- [ ] **Step 2 (verify):** `get_screenshot` + binding check.

### Task 2.5: Overlays

- [ ] **Step 1:** `DialogShell` — bg, `dialogTitle`, `dialogBody`, action buttons.
- [ ] **Step 2:** `ExpandMenuShell` variants: L/default bg `Theme/L.ExpandMenu_bg #FFFAF5` tint `#1F1F1F`; X bg `Theme/X.expandMenuBackground #F2EDF7`.
- [ ] **Step 3:** `DropdownMenuItem` — leading icon + `menuItem` text.
- [ ] **Step 4 (verify):** `get_screenshot` + binding check; confirm light expand-menu fills bound to the correct token.

---

## Phase 3 — X screens (page `03 · X`)

Run the **Screen Build Procedure** for each task. Section bars use `BottomNavBar` variant `X`; backgrounds `Theme/Shared.background`.

### Task 3.1: Dashboards
**Inputs:** source `x/screens/dashboards/ScreenXDashBoards.kt` (+ `DashboardsPaginatedListScreen.kt`); components TopBar, grid of `VideoCard`+`DurationBadge`, BottomNavBar(X); bindings bg→`background`, bar→`tabLevel0`; annotate grid bg, card, badge, bar.

### Task 3.2: Tags
**Inputs:** `x/screens/tags/ScreenTags.kt` (+ `atom/TagsPaginatedListScreen.kt`); components TopBar, `TagChip` list/grid; bindings bg→`background`; annotate chip + bg.

### Task 3.3: ScreenK
**Inputs:** `x/screens/k/ScreenK.kt`; build per source; annotate dominant tokens.

### Task 3.4: Profile
**Inputs:** `x/screens/profile/ScreenProfile.kt`; components header + media grid; annotate.

### Task 3.5: Favorites
**Inputs:** `x/screens/favorites/ScreenFavorites.kt`; components grid; annotate (delete action surfaces `FavoritesDeleteDialog` → built in Phase 7).

### Task 3.6: SavedX
**Inputs:** `x/screens/saved/ScreenSavedX.kt`; annotate.

### Task 3.7: VideoPlayer
**Inputs:** `x/screens/videoplayer/ScreenX_VideoPlayer.kt`; components `Seekbar` + player controls over a video placeholder; annotate control colors + scrim.

### Task 3.8: LocalVideoPlayer
**Inputs:** `x/screens/videoplayer/ScreenX_LocalVideoPlayer.kt`; same control set; annotate differences from 3.7.

### Task 3.9: VideoPlayerFullScreen
**Inputs:** `x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreen.kt`; landscape/fullscreen frame; annotate scrim + controls.

### Task 3.10: Bottom nav buttons
**Inputs:** `x/screens/common/bottomKeyboard/ScreenDashBoardsBottomNavigationButtons.kt`; document the X BottomNavBar states; annotate selected/unselected.

---

## Phase 4 — L screens (page `04 · L`)

BottomNavBar variant `L`; backgrounds `Theme/Shared.background`; accents `Theme/L.primaryColor #ff96a3`, text `Theme/L.textColor`.

### Task 4.1: Login
**Inputs:** `l/ui/screens/L_ScreenLogin.kt`; components `TextField`, `Button(primary)`; annotate input/button/bg.

### Task 4.2: Explorer shell + tab bar
**Inputs:** `l/ui/screens/explorer/L_ScreenExplorer.kt`; components BottomNavBar(L), TabRow; annotate tab bar tokens.

### Task 4.3: AlbumList tab
**Inputs:** `l/ui/screens/screenAlbumList/ScreenAlbumList.kt`; grid of `AlbumCover`; annotate.

### Task 4.4: Saved tab — Likes subtab
**Inputs:** `l/ui/screens/explorer/tab/saved/ScreenSaved.kt` + `likes/L_ScreenSavedLikesTab.kt`; SubTabRow + `ImageThumb` grid; annotate subtab + grid.

### Task 4.5: Saved tab — Collection subtab
**Inputs:** `l/ui/screens/explorer/tab/saved/collection/L_Screen_CollectionTab.kt` (+ `ScreenCollectionName.kt`); `CollectionItem` list; annotate.

### Task 4.6: Saved tab — Albums subtab
**Inputs:** `l/ui/screens/explorer/tab/saved/albums/L_ScreenSavedAlbumsTab.kt`; `AlbumCover` grid; annotate.

### Task 4.7: TopHits tab
**Inputs:** `l/ui/screens/explorer/tab/albumTopHits/L_ScreenAlbumTopHits.kt`; annotate.

### Task 4.8: Search tab
**Inputs:** `l/ui/screens/explorer/tab/albumSearch/L_ScreenAlbumSearch.kt`; `TextField` + results grid; annotate.

### Task 4.9: Album
**Inputs:** `l/ui/screens/screenAlbum/ScreenAlbum.kt`; header + image grid; annotate (item long-press → `AlbumItemExpandMenu`, Phase 7).

### Task 4.10: AlbumList (filter view)
**Inputs:** `l/ui/screens/screenAlbumList/ScreenAlbumList.kt` + filter molecules `screenAlbumList/molecule/filter/...`; show filter controls; annotate (selectors → Phase 7).

### Task 4.11: AlbumLandingTag
**Inputs:** `l/ui/screens/albumLandingTag/ScreenLAlbumLandingTag.kt`; annotate.

### Task 4.12: FullScreenImage (overlay)
**Inputs:** `l/ui/screens/screenFullScreen/L_FullScreenImage.kt`; full-bleed image + overlay controls; annotate scrim/controls. Note: if bg is a non-token literal, route to `Unsorted/Misc`.

---

## Phase 5 — R screens (page `05 · R`)

BottomNavBar variant `R` (divider `Theme/R.colorBottomBarDivider`); accents `Theme/R.colorYellow/colorBlue/colorRed`.

### Task 5.1: Root + bottom navigation
**Inputs:** `r/ui/root/R_Screen_Root.kt`; BottomNavBar(R); annotate bar + divider tokens.

### Task 5.2: Explorer shell + tabs
**Inputs:** `r/ui/explorer/ScreenExplorer.kt`; TabRow; annotate.

### Task 5.3: Gifs tab
**Inputs:** `r/ui/explorer/tab/gifs/R_ScreenGifsTab.kt`; `GifCard` grid; annotate.

### Task 5.4: Niches tab
**Inputs:** `r/ui/explorer/tab/niches/R_ScreenNichesTab.kt`; niche tiles; annotate.

### Task 5.5: Saved — Collection subtab
**Inputs:** `r/ui/explorer/tab/saved/ScreenSaved.kt` + `tab/R_Screen_CollectionTab.kt`; SubTabRow + `CollectionItem`; annotate.

### Task 5.6: Saved — Creators subtab
**Inputs:** `tab/R_Screen_CreatorsTab.kt`; `CreatorRow` list; annotate.

### Task 5.7: Saved — Likes subtab
**Inputs:** `tab/R_Screen_Saved_LikesTab.kt`; grid; annotate.

### Task 5.8: Saved — Download subtab
**Inputs:** `tab/R_Screen_Saved_DownloadTab.kt`; download rows + progress; annotate progress colors.

### Task 5.9: Saved — Subscriptions subtab
**Inputs:** `tab/R_Screen_Saved_SubscriptionsTab.kt`; list; annotate.

### Task 5.10: Niche
**Inputs:** `r/ui/niche/ScreenNiche.kt`; annotate.

### Task 5.11: Profile
**Inputs:** `r/ui/profile/ScreenRedProfile.kt`; annotate.

### Task 5.12: TopThisWeek
**Inputs:** `r/ui/top_this_week/ScreenRedTopThisWeek.kt`; annotate.

### Task 5.13: ManageBlock
**Inputs:** `r/ui/manager_block/ScreenRedManageBlock.kt`; annotate (block action → `DialogBlock`, Phase 7).

### Task 5.14: FullScreen
**Inputs:** `r/ui/fullscreen/ScreenRedFullScreen.kt`; full-bleed + controls; annotate.

---

## Phase 6 — Common screens (page `06 · Common`)

### Task 6.1: Main Menu
**Inputs:** `screenRoot/ScreenRoot.kt` (MenuScreen / app root); App-root bg `Theme/Shared.backgroundAppRoot`; entries to X/L/R/Settings/P2P; annotate.

### Task 6.2: Settings
**Inputs:** `AppSettingsScreen.kt` + `common/settings/ui/components/SettingsListItems.kt`; `SettingsRow` list; settings bg/topbar use `Theme/L.grey6` / `grey5`; annotate.

### Task 6.3: P2P Send
**Inputs:** `common/p2p/ui/ScreenP2pSend.kt`; annotate (chooser → `P2pSendChooserDialog`, Phase 7).

### Task 6.4: P2P Receive
**Inputs:** `common/p2p/ui/ScreenP2pReceive.kt`; annotate.

### Task 6.5: AppLock (code entry)
**Inputs:** `common/applock/*` + `AccessCodeVisualTransformation.kt`; keypad + dots; annotate.

### Task 6.6: Permission
**Inputs:** `PermissionScreenActivity.kt`; annotate.

---

## Phase 7 — Dialogs & expand-menus (page `07 · Dialogs & Menus`)

Build each on `DialogShell` / `ExpandMenuShell` / `DropdownMenuItem` from Phase 2. Lay out as a matrix: row = menu/dialog, opened state + token callouts. Run the relevant parts of the Screen Build Procedure (P1, P4, P5, P6) per item.

### Task 7.1: Dialogs
**Inputs (source files):**
- L: `l/ui/screens/screenAlbum/atom/AlbumDialogDeleteAlbum.kt`, `l/ui/screens/explorer/L_DialogCollection.kt`, `common/collectionDB/ui/DialogCollection.kt`, `common/collectionDB/ui/DaialogNewCollection.kt`
- R: `r/common/block/ui/DialogBlock.kt`, `r/ui/explorer/tab/saved/tab/DialogSubscriptionDelete.kt`, `r/ui/explorer/tab/saved/tab/savedNiche/DialogNicheDelete.kt`
- X: `x/screens/favorites/FavoritesDeleteDialog.kt`
- Common: `common/p2p/ui/P2pSendChooserDialog.kt`, `common/settings/ui/DialogButton.kt`, `common/settings/ui/ConfigTextAndButtonWithDialog.kt`
- [ ] Build each on `DialogShell`; bind bg/title/body/buttons; annotate; verify per item.

### Task 7.2: L expand-menus
**Inputs:** `l/ui/element/expandMenu/AlbumItemExpandMenu.kt`, `SavedLikesItemExpandMenu.kt`, and items in `l/ui/element/expandMenu/element/`: `DropdownMenuItem_SetCover.kt`, `_SaveToGallery.kt`, `_Share.kt`, `_Delete.kt`, `_Download.kt`, `_AddCollection.kt`, `_RemoveFromCollection.kt`.
- [ ] Build on `ExpandMenuShell` (L, light `#FFFAF5`) with `DropdownMenuItem`s; annotate light-bg + tint tokens; verify.

### Task 7.3: R expand-menus
**Inputs:** `r/common/expand_menu_video/ExpandMenuVideo.kt`, `ExpandMenuVideoTags.kt`, `r/common/search/ExpandMenuHistoryContent.kt`, `ExpandMenuHelperContent.kt`, and items in `r/common/expand_menu_video/`: `DropdownMenuItem_Like.kt`, `_Follow.kt`, `_Block.kt`, `_Subscription.kt`, `_Download.kt`, `_Share.kt`, `_AddCollection.kt`, `_RemoveFromCollection.kt`, `_SaveToGallery.kt`.
- [ ] Build the R video/tags/history/helper menus + items; annotate; verify.

### Task 7.4: X expand-menus
**Inputs:** `x/screens/ui/expandMenu/X_DashboardExpandMenu.kt`, `x/screens/common/bottomKeyboard/MenuDot.kt`.
- [ ] Build on `ExpandMenuShell` (X, light `#F2EDF7`); annotate; verify.

### Task 7.5: Filter selectors
**Inputs:** `l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterDisplay.kt`, `AlbumListFilterSize.kt`, `l/ui/screens/screenAlbumList/atom/AlbumListPageSelector.kt`, `r/ui/ui/sortByOrder/SortByOrder.kt`, `common/settings/ui/components/ThumbnailSizeSelector.kt`.
- [ ] Build each selector's opened state; annotate; verify.

---

## Phase 8 — Index / Cover + final audit (page `00 · Index / Cover`)

### Task 8.1: Index / Cover
- [ ] **Step 1:** Build a cover with file title, a map of pages `00–07`, the annotation legend convention, and a link to the nav-board `figma.com/board/QVsa8oJOkWrFfdCP7FGNi1`.
- [ ] **Step 2 (verify):** `get_screenshot` of page `00`.

### Task 8.2: Final binding + coverage audit (Definition of Done)
- [ ] **Step 1:** `get_variable_defs` across the file → confirm catalog covers all tokens from `Theme/Shared`, `Theme/R`, `Theme/L`, `Theme/X`, `Legacy/PornHub`, `Material/XvideosTheme`, `Unsorted/Misc`.
- [ ] **Step 2:** Walk pages `03–07` → confirm all ~40 screens + all dialogs/menus from the spec are present as frames.
- [ ] **Step 3:** Spot-check binding: sample frames per section with `get_design_context` → fills resolve to named Variables; no stray hex outside `Unsorted/*`.
- [ ] **Step 4:** Section visual spot-check (X / L / R / Common) vs Compose source / real screen.
- [ ] **Step 5:** Confirm fonts match (Poppins/DM Sans) or the Task 0.1 deviation note is recorded.

---

## Self-review notes (coverage vs spec)

- **Color system** → Phase 1 (1.1 collections, 1.2 type, 1.3 catalog, 1.4 Unsorted).
- **Component library** → Phase 2 (bars/media/lists/controls/overlays).
- **All screens X/L/R/common** → Phases 3–6 (matches spec inventory).
- **Dialogs + expand-menus + selectors** → Phase 7 (all source files from the spec).
- **Per-screen color annotations** → Screen Build Procedure step P5, applied in every screen task.
- **Index + nav-board link** → Phase 8.1.
- **Done criteria** → Phase 8.2 mirrors the spec's "Критерии готовности".
- **Prerequisites (auth/fonts/batching)** → Phase 0 + Conventions.
