# Dialogs Catalog (as-is) → Figma — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild `docs/design-copy/dialogs.html` into a pixel-faithful, style-grouped catalog of every popup in the app (modals + menus + bottom sheets), ready for one-shot import to Figma via the `html.to.design` plugin.

**Architecture:** Static HTML page. Colors come only from `docs/design-copy/tokens.css` CSS variables (1:1 with code). Each popup = one card (faithful dialog frame on a dark scrim + caption + token chips + optional ⚠ badge), grouped into `<section>` blocks per style family. Layout uses flex/grid (imports cleanly into Figma auto-layout); text is real; icons/thumbnails are plain placeholder rectangles (no external images).

**Tech Stack:** HTML + CSS (DM Sans web font), existing `tokens.css` + `styles.css`, preview server for verification (`design-copy`, port 4599), `html.to.design` Figma plugin for the manual import step.

---

## Reference: color → token map (use `var(--…)`, never hardcode)

| Hex | Meaning | Token |
|-----|---------|-------|
| #6552A5 | L purple accent | `--misc-6552a5` |
| #EBE6EE | lavender dialog bg | `--misc-ebe6ee` |
| #474747 | body text gray | `--misc-474747` |
| #EBFA63 | R yellow accent | `--r-colorYellow` |
| #ff96a3 | L pink accent | `--l-primaryColor` |
| #090909 | dark dialog bg | `--misc-090909` |
| #3F3F3F | dark border | `--r-colorBorderGray` |
| #363636 | dark separator | `--misc-363636` |
| #232323 | dark footer | `--misc-232323` |
| #FFFFFF | white dialog bg | `#fff` (or `--sh-background` if white) |
| #E4E4E4 | white-dialog border | `--misc-e4e4e4` |
| #CCCCCC | light separator | `--misc-cccccc` |
| #2196F3 | blue accent | `--l-b0` |
| #F44336 | red accent | `--l-r0` |
| #2E2E2E | X dark dialog bg | `--misc-2e2e2e` |
| #FF6B6B | X coral accent | `--misc-ff6b6b` |

Type tokens (from `Theme.L.Type`, DM Sans): dialogTitle 18sp/Medium-500, dialogBody 14sp/Normal-400, button 16sp/Medium-500. Some M3 dialogs override title to 20sp/Bold-700.

Verification note: this catalog is content, not testable logic. "Tests" = preview-server checks: clean console (`preview_console_logs`), structural snapshot (`preview_snapshot`), and a screenshot (`preview_screenshot`) compared against the source Kotlin and the agreed render.

---

## Task 1: Enumerate menus & bottom sheets

The 11 modal dialogs are already inventoried in the spec. The menu/bottom-sheet lists must be discovered before they can be built.

**Files:**
- Modify: `docs/superpowers/plans/2026-06-14-dialogs-catalog.md` (append the discovered list under this task as a checklist)

- [ ] **Step 1: Sweep for dropdown/context menus**

Run:
```bash
cd "G:/Android_xvideos-24dc005fedb7ca163f5c0c1c997aceb406315c16/app/src/main/java/com/client/xvideos"
grep -rnE 'DropdownMenu\(|DropdownMenuItem|ExpandMenu|ContextMenu|PopupMenu' . | grep -v Preview
```
Expected: hits in `r/common/expand_menu_video/*`, `x/screens/common/bottomKeyboard/MenuDot.kt`, `l/ui/element/expandMenu/*`, `r/common/expand_menu_video/DropdownMenuItem_*`.

- [ ] **Step 2: Sweep for bottom sheets**

Run:
```bash
cd "G:/Android_xvideos-24dc005fedb7ca163f5c0c1c997aceb406315c16/app/src/main/java/com/client/xvideos"
grep -rnE 'ModalBottomSheet|BottomSheet|rememberModalBottomSheetState|Scaffold.*sheet' . | grep -v Preview
```
Expected: candidates in `common/p2p/ui/ScreenP2pSend.kt` and any filter/sort selectors.

- [ ] **Step 3: Record the enumerated list**

For each found composable, record one row: `name | file | trigger surface | bg color (token) | text/accent colors | corner/padding`. Append as a markdown checklist under this task. Each row becomes one card in Task 7 (menus) or Task 8 (sheets). Mark any that are NOT popups (e.g. inline rows) as "skip — not a popup".

**Enumeration result (done):**

Menus (5) → Task 7:
- [ ] **ThumbnailSizeSelector** — `common/settings/ui/components/ThumbnailSizeSelector.kt` — trigger Button — bg M3 default surface — items = `ThumbnailsSize.displayNames` (размеры миниатюр).
- [ ] **AlbumItemExpandMenu (L)** — `l/ui/element/expandMenu/AlbumItemExpandMenu.kt` — trigger MoreVert — bg `--l-expandMenu-bg` (#FFFAF5) — item tint `--l-expandMenu-tint` (#1F1F1F) — items EN: Download / Share / SaveToGallery / Add to Collection / Remove from Collection.
- [ ] **ExpandMenuVideo (R)** — `r/common/expand_menu_video/ExpandMenuVideo.kt` — trigger MoreVert — bg hardcoded `#F1EDF4` (add token `--misc-f1edf4` if absent) — items EN: Download / Share / SaveToGallery / Block / Like / Follow / Add to Collection / Remove from Collection / Subscribtion.
- [ ] **X_DashboardExpandMenu** — `x/screens/ui/expandMenu/X_DashboardExpandMenu.kt` — trigger MoreVert — bg `--x-expandMenuBackground`.
- [ ] **FavoriteActionsExpandMenu** — `x/screens/favorites/ScreenFavorites.kt:247` — trigger MoreVert — bg `--l-expandMenu-bg` (#FFFAF5, reuses L) — item tint `--l-expandMenu-tint` — items RU: Скачать / В галерею / Удалить.

Legacy dialog (1) → Task 7 (with ⚠):
- [ ] **MenuDotConfig** — `x/screens/common/bottomKeyboard/MenuDot.kt:123` — `Dialog` white Surface corner 16dp, title "Set value" 24sp bold + Cancel icon, separator #9E9E9E, full-width pill "Done" (corner 50dp). ⚠ legacy: sibling `MenuDot` полностью закомментирован, кнопка ничего не делает.

Bottom sheets → Task 8: **none found** (grep пуст). Task 8 = заметка «нет».

Discovered inconsistencies (flag with ⚠ in Task 7):
- 4 разных фона меню: M3 default / #FFFAF5 (L, X-favorites) / #F1EDF4 (R) / `--x-expandMenuBackground` (X-dashboard).
- Язык пунктов: L/R меню — **English** (Download/Share/Block…), X-favorites — **русский** (Скачать/В галерею/Удалить). i18n-расхождение.

- [ ] **Step 4: Commit**

```bash
git add docs/superpowers/plans/2026-06-14-dialogs-catalog.md
git commit -m "docs: enumerate menus and bottom sheets for dialogs catalog"
```

---

## Task 2: Scaffold `dialogs.html` shell + scoped CSS

**Files:**
- Modify (full rebuild): `docs/design-copy/dialogs.html`

- [ ] **Step 1: Replace the file with the shell**

Write `docs/design-copy/dialogs.html`:

```html
<!DOCTYPE html>
<html lang="ru">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Каталог диалогов (as-is)</title>
<link rel="stylesheet" href="tokens.css">
<link rel="stylesheet" href="styles.css">
<style>
@import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;700&display=swap');
body{font-family:'DM Sans',sans-serif;margin:0;padding:24px;background:#1b1b1f;color:#eee}
h1{font-size:22px;font-weight:500;margin:0 0 4px}
.lead{color:#aaa;font-size:14px;margin:0 0 24px;max-width:720px}
section{margin:0 0 32px}
section>h2{font-size:18px;font-weight:500;margin:0 0 2px}
section>.fdesc{color:#aaa;font-size:13px;margin:0 0 14px;max-width:760px}
.dlg-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:16px;align-items:start}
.dlg-cell{display:flex;flex-direction:column;gap:8px}
.dlg-scrim{background:#000;display:flex;align-items:center;justify-content:center;padding:28px 18px;border-radius:8px}
.dlg-cap{font-size:12px;color:#bbb}
.dlg-cap b{color:#fff;font-weight:500}
.dlg-chips{display:flex;flex-wrap:wrap;gap:6px}
.dlg-chip{font-size:11px;color:#ddd;border:1px solid #444;border-radius:4px;padding:2px 6px}
.dlg-chip .sw{display:inline-block;width:9px;height:9px;border-radius:2px;margin-right:5px;vertical-align:-1px}
.dlg-warn{font-size:11px;color:#FFD08A;border:1px solid #6b5520;background:#2a2410;border-radius:4px;padding:3px 7px}
.ph{display:flex;align-items:center;justify-content:center;background:#3a3a3a;color:#888;font-size:24px}
.m3{width:280px;border-radius:28px;padding:24px;text-align:center}
.m3 .ttl{font-size:18px;font-weight:700;margin-bottom:8px}
.m3 .body{font-size:14px;line-height:1.4}
.m3 .acts{display:flex;justify-content:flex-end;gap:8px;align-items:center;margin-top:22px}
.btn-text{font-size:14px;font-weight:500;padding:8px 12px}
.btn-fill{font-size:14px;font-weight:500;padding:10px 22px;border-radius:20px}
.card-dark{width:280px;background:var(--misc-090909);border:1px solid var(--r-colorBorderGray);border-radius:12px;overflow:hidden}
.card-white{width:280px;background:#fff;border:1px solid var(--misc-e4e4e4);border-radius:12px;overflow:hidden}
.foot-dark{display:flex;justify-content:flex-end;align-items:center;gap:8px;height:50px;background:var(--misc-232323);padding:0 8px}
</style>
</head>
<body>
<h1>Каталог диалогов приложения (as-is)</h1>
<p class="lead">Все всплывающие окна, точно как в приложении, сгруппированы по визуальным стилям. Цвета из tokens.css (1:1 с кодом). Расхождения и баги помечены.</p>

<section id="famA"><h2>A — лавандовый Material3 (раздел L)</h2><p class="fdesc">Фон #EBE6EE, иконка 96dp по центру, акцент фиолетовый #6552A5. Внутри расхождение: filled-кнопка против только-текста.</p><div class="dlg-grid"></div></section>
<section id="famA2"><h2>A2 — дефолтный Material3</h2><p class="fdesc">containerColor не задан → дефолтная светлая M3-поверхность.</p><div class="dlg-grid"></div></section>
<section id="famB"><h2>B — тёмный Material3 (раздел X)</h2><p class="fdesc">Фон #2E2E2E, иконка = превью 16:9, акцент коралл #FF6B6B.</p><div class="dlg-grid"></div></section>
<section id="famC"><h2>C — тёмный кастом «список коллекций»</h2><p class="fdesc">Фон #090909, рамка #3F3F3F 12dp, футер 56dp #232323. Один макет — жёлтый (R) против розового (L).</p><div class="dlg-grid"></div></section>
<section id="famD"><h2>D — тёмный кастом «ввод»</h2><p class="fdesc">Тот же корпус с OutlinedTextField. Кнопка без явного цвета → дефолтный M3.</p><div class="dlg-grid"></div></section>
<section id="famE"><h2>E — белый кастом (настройки/блок)</h2><p class="fdesc">Фон белый, рамка #E4E4E4 12dp, кнопка радиус 4dp. Синий против красного.</p><div class="dlg-grid"></div></section>
<section id="menus"><h2>Меню (dropdown / контекст)</h2><p class="fdesc">Заполняется из Task 1.</p><div class="dlg-grid"></div></section>
<section id="sheets"><h2>Bottom sheets</h2><p class="fdesc">Заполняется из Task 1.</p><div class="dlg-grid"></div></section>
</body>
</html>
```

- [ ] **Step 2: Start preview server and load the page**

Use `preview_start` to serve `docs/design-copy` (or the existing `design-copy` launch config, port 4599). Navigate to `dialogs.html`.

- [ ] **Step 3: Verify shell loads clean**

Run `preview_console_logs`. Expected: no errors (font + tokens.css + styles.css load). Run `preview_snapshot`. Expected: 8 empty section headings A, A2, B, C, D, E, Меню, Bottom sheets.

- [ ] **Step 4: Commit**

```bash
git add docs/design-copy/dialogs.html
git commit -m "feat(design-copy): rebuild dialogs.html shell with style-family sections"
```

---

## Task 3: Family A — 4 lavender Material3 dialogs

**Files:**
- Modify: `docs/design-copy/dialogs.html` (fill `#famA .dlg-grid`)

- [ ] **Step 1: Insert the 4 cards into `#famA .dlg-grid`**

```html
<div class="dlg-cell"><div class="dlg-scrim"><div class="m3" style="background:var(--misc-ebe6ee);color:#000">
  <div class="ph" style="width:72px;height:72px;border-radius:6px;margin:0 auto 16px">▦</div>
  <div class="ttl">Заголовок</div>
  <div class="body" style="color:var(--misc-474747)">Текст диалога</div>
  <div class="acts"><span class="btn-text" style="color:var(--misc-6552a5)">Отмена</span>
  <span class="btn-fill" style="background:var(--misc-6552a5);color:#fff">Удалить</span></div>
</div></div>
<div class="dlg-cap"><b>DialogTemplate</b><br>common/theme/DialogTemplate.kt</div>
<div class="dlg-chips"><span class="dlg-chip"><span class="sw" style="background:var(--misc-ebe6ee)"></span>bg --misc-ebe6ee</span><span class="dlg-chip"><span class="sw" style="background:var(--misc-6552a5)"></span>filled --misc-6552a5</span></div></div>

<div class="dlg-cell"><div class="dlg-scrim"><div class="m3" style="background:var(--misc-ebe6ee);color:#000">
  <div class="ph" style="width:72px;height:72px;border-radius:6px;margin:0 auto 16px">▦</div>
  <div class="ttl">Удалить Альбом?</div>
  <div class="body" style="color:var(--misc-474747)">Удалить «<b>Summer Vacation</b>» из сохранённых?</div>
  <div class="acts"><span class="btn-text" style="color:var(--misc-6552a5)">Отмена</span>
  <span class="btn-fill" style="background:var(--misc-6552a5);color:#fff">Удалить</span></div>
</div></div>
<div class="dlg-cap"><b>AlbumDialogDeleteAlbum</b><br>l/.../screenAlbum/dialog/AlbumDialogDeleteAlbum.kt</div>
<div class="dlg-chips"><span class="dlg-chip"><span class="sw" style="background:var(--misc-ebe6ee)"></span>bg --misc-ebe6ee</span><span class="dlg-chip"><span class="sw" style="background:var(--misc-6552a5)"></span>filled --misc-6552a5</span></div></div>

<div class="dlg-cell"><div class="dlg-scrim"><div class="m3" style="background:var(--misc-ebe6ee);color:#000">
  <div class="ph" style="width:72px;height:72px;border-radius:8px;margin:0 auto 16px;background:#444;color:#fff">☻</div>
  <div class="ttl" style="font-size:20px">Удалить подписку?</div>
  <div class="body" style="font-size:16px;color:#000">Удалить автора «<b>SampleUser</b>» из подписок?</div>
  <div class="acts"><span class="btn-text" style="font-size:16px;color:var(--misc-6552a5)">Отмена</span>
  <span class="btn-text" style="font-size:16px;color:var(--misc-6552a5)">Удалить</span></div>
</div></div>
<div class="dlg-cap"><b>DialogSubscriptionDelete</b><br>r/.../saved/tab/DialogSubscriptionDelete.kt</div>
<div class="dlg-chips"><span class="dlg-chip"><span class="sw" style="background:var(--misc-ebe6ee)"></span>bg --misc-ebe6ee</span><span class="dlg-chip"><span class="sw" style="background:var(--misc-6552a5)"></span>text --misc-6552a5</span></div>
<div class="dlg-warn">⚠ обе кнопки текстовые (в A1/A2 — filled): расхождение</div></div>

<div class="dlg-cell"><div class="dlg-scrim"><div class="m3" style="background:var(--misc-ebe6ee);color:#000">
  <div class="ph" style="width:72px;height:72px;border-radius:8px;margin:0 auto 16px">▦</div>
  <div class="ttl" style="font-size:20px">Удалить группу?</div>
  <div class="body" style="font-size:16px;color:#000">Удалить «<b>Sample Niche</b>» из сохранённых?</div>
  <div class="acts"><span class="btn-text" style="font-size:16px;color:var(--misc-6552a5)">Отмена</span>
  <span class="btn-text" style="font-size:16px;color:var(--misc-6552a5)">Удалить</span></div>
</div></div>
<div class="dlg-cap"><b>DialogNicheDelete</b><br>r/.../savedNiche/DialogNicheDelete.kt</div>
<div class="dlg-chips"><span class="dlg-chip"><span class="sw" style="background:var(--misc-ebe6ee)"></span>bg --misc-ebe6ee</span><span class="dlg-chip"><span class="sw" style="background:var(--misc-6552a5)"></span>text --misc-6552a5</span></div></div>
```

- [ ] **Step 2: Reload and verify**

Reload preview. Run `preview_console_logs` (expect clean). Run `preview_snapshot` (expect 4 cards under A with the Russian titles). Run `preview_screenshot`; visually confirm lavender bg, purple accents, 1 filled + 1 text variant each match the agreed render.

- [ ] **Step 3: Commit**

```bash
git add docs/design-copy/dialogs.html
git commit -m "feat(design-copy): add family A lavender M3 dialogs"
```

---

## Task 4: Family A2 (P2P default M3) + Family B (X dark M3)

**Files:**
- Modify: `docs/design-copy/dialogs.html` (fill `#famA2 .dlg-grid` and `#famB .dlg-grid`)

- [ ] **Step 1: Insert A2 card into `#famA2 .dlg-grid`**

```html
<div class="dlg-cell"><div class="dlg-scrim"><div class="m3" style="background:#FEF7FF;color:#1d1b20;text-align:left">
  <div style="font-size:20px;font-weight:500;margin-bottom:14px">Поделиться</div>
  <div style="display:flex;flex-direction:column;gap:4px">
    <span style="color:#65558F;font-size:14px;font-weight:500;padding:10px 12px;text-align:center">Системное (через приложения)</span>
    <span style="color:#65558F;font-size:14px;font-weight:500;padding:10px 12px;text-align:center">P2P рядом (Nearby)</span></div>
  <div style="display:flex;justify-content:flex-end;margin-top:16px"><span style="color:#65558F;font-size:14px;font-weight:500;padding:8px 12px">Отмена</span></div>
</div></div>
<div class="dlg-cap"><b>P2pSendChooserDialog</b><br>common/p2p/ui/P2pSendChooserDialog.kt</div>
<div class="dlg-chips"><span class="dlg-chip">bg M3 default (#FEF7FF)</span><span class="dlg-chip">контент = текст-кнопки</span></div></div>
```

- [ ] **Step 2: Insert B card into `#famB .dlg-grid`**

```html
<div class="dlg-cell"><div class="dlg-scrim"><div class="m3" style="background:var(--misc-2e2e2e);color:#fff">
  <div class="ph" style="width:170px;height:96px;border-radius:8px;margin:0 auto 16px">▶</div>
  <div class="ttl" style="font-size:18px;font-weight:500;margin-bottom:4px">Удалить из избранного?</div>
  <div class="acts"><span class="btn-text" style="color:var(--misc-cccccc)">Отмена</span>
  <span class="btn-text" style="color:var(--misc-ff6b6b)">Удалить</span></div>
</div></div>
<div class="dlg-cap"><b>ConfirmDeleteFavoriteDialog</b><br>x/screens/favorites/FavoritesDeleteDialog.kt</div>
<div class="dlg-chips"><span class="dlg-chip"><span class="sw" style="background:var(--misc-2e2e2e)"></span>bg --misc-2e2e2e</span><span class="dlg-chip"><span class="sw" style="background:var(--misc-ff6b6b)"></span>text --misc-ff6b6b</span></div></div>
```

- [ ] **Step 3: Reload and verify**

Reload. `preview_console_logs` clean. `preview_snapshot`: A2 has 1 card (Поделиться), B has 1 card (Удалить из избранного?). `preview_screenshot`: confirm default-M3 surface vs dark #2E2E2E surface.

- [ ] **Step 4: Commit**

```bash
git add docs/design-copy/dialogs.html
git commit -m "feat(design-copy): add family A2 (P2P) and B (X favorite) dialogs"
```

---

## Task 5: Family C — dark collection-list dialogs (R yellow vs L pink)

**Files:**
- Modify: `docs/design-copy/dialogs.html` (fill `#famC .dlg-grid`)

- [ ] **Step 1: Insert the 2 cards into `#famC .dlg-grid`**

```html
<div class="dlg-cell"><div class="dlg-scrim"><div class="card-dark">
  <div style="font-size:16px;font-weight:500;color:#0d0d0d;padding:10px 10px 6px">Добавить в коллекцию</div>
  <div style="display:flex;align-items:center;gap:8px;padding:4px 8px"><div class="ph" style="width:56px;height:56px;border-radius:14px">▦</div><span style="color:#fff;font-size:14px">Favorites</span></div>
  <div style="display:flex;align-items:center;gap:8px;padding:4px 8px 10px"><div style="width:56px;height:56px;border-radius:14px;background:#808080"></div><span style="color:#fff;font-size:14px">Funny</span></div>
  <div style="height:1px;background:var(--misc-363636)"></div>
  <div class="foot-dark"><span style="color:#bbb;font-size:14px;font-weight:500">Отмена</span><span style="background:var(--r-colorYellow);color:#000;font-size:14px;font-weight:500;padding:8px 12px;border-radius:8px">Создать</span></div>
</div></div>
<div class="dlg-cap"><b>DialogCollection</b> (R)<br>common/collectionDB/ui/DialogCollection.kt</div>
<div class="dlg-chips"><span class="dlg-chip"><span class="sw" style="background:var(--misc-090909);border:1px solid #555"></span>bg --misc-090909</span><span class="dlg-chip"><span class="sw" style="background:var(--r-colorYellow)"></span>filled --r-colorYellow</span></div>
<div class="dlg-warn">⚠ цвет заголовка не задан → почти невидим на #090909</div></div>

<div class="dlg-cell"><div class="dlg-scrim"><div class="card-dark">
  <div style="font-size:16px;font-weight:500;color:#fff;padding:10px 10px 6px">Добавить в коллекцию</div>
  <div style="display:flex;align-items:center;gap:8px;padding:4px 8px"><div class="ph" style="width:56px;height:56px;border-radius:14px">▦</div><span style="color:#fff;font-size:14px">Favorites</span></div>
  <div style="display:flex;align-items:center;gap:8px;padding:4px 8px 10px"><div style="width:56px;height:56px;border-radius:14px;background:#808080"></div><span style="color:#fff;font-size:14px">Funny</span></div>
  <div style="height:1px;background:var(--misc-363636)"></div>
  <div class="foot-dark"><span style="color:#fff;font-size:14px;font-weight:500">Отмена</span><span style="background:var(--l-primaryColor);color:#000;font-size:14px;font-weight:500;padding:8px 12px;border-radius:8px">Создать</span></div>
</div></div>
<div class="dlg-cap"><b>L_DialogCollection</b><br>l/ui/screens/explorer/L_DialogCollection.kt</div>
<div class="dlg-chips"><span class="dlg-chip"><span class="sw" style="background:var(--misc-090909);border:1px solid #555"></span>bg --misc-090909</span><span class="dlg-chip"><span class="sw" style="background:var(--l-primaryColor)"></span>filled --l-primaryColor</span></div>
<div class="dlg-warn">⚠ тот же макет, что R, но акцент розовый вместо жёлтого</div></div>
```

- [ ] **Step 2: Reload and verify**

Reload. `preview_console_logs` clean. `preview_snapshot`: C has 2 cards. `preview_screenshot`: confirm identical layout, only accent differs (yellow vs pink), R title near-invisible.

- [ ] **Step 3: Commit**

```bash
git add docs/design-copy/dialogs.html
git commit -m "feat(design-copy): add family C dark collection dialogs (R/L)"
```

---

## Task 6: Family D (new-collection input) + Family E (white blue/red)

**Files:**
- Modify: `docs/design-copy/dialogs.html` (fill `#famD .dlg-grid` and `#famE .dlg-grid`)

- [ ] **Step 1: Insert D card into `#famD .dlg-grid`**

```html
<div class="dlg-cell"><div class="dlg-scrim"><div class="card-dark">
  <div style="font-size:18px;color:#fff;padding:8px 8px 4px">Создать коллекцию</div>
  <div style="margin:8px;border:1px solid #79747E;border-radius:4px;padding:14px 12px;position:relative">
    <span style="position:absolute;top:-8px;left:8px;background:var(--misc-090909);padding:0 4px;font-size:12px;color:#b0a8c0">Название коллекции</span></div>
  <div style="height:1px;background:var(--misc-363636)"></div>
  <div class="foot-dark"><span style="color:#bbb;font-size:14px;font-weight:500">Отмена</span><span style="background:#65558F;color:#000;font-size:14px;font-weight:500;padding:8px 12px;border-radius:8px">Создать</span></div>
</div></div>
<div class="dlg-cap"><b>DaialogNewCollection</b><br>common/collectionDB/ui/DaialogNewCollection.kt</div>
<div class="dlg-chips"><span class="dlg-chip"><span class="sw" style="background:var(--misc-090909);border:1px solid #555"></span>bg --misc-090909</span><span class="dlg-chip">filled M3 default</span></div>
<div class="dlg-warn">⚠ кнопка «Создать» без явного цвета → дефолтный M3-фиолетовый (а не --r-colorYellow)</div></div>
```

- [ ] **Step 2: Insert the 2 E cards into `#famE .dlg-grid`**

```html
<div class="dlg-cell"><div class="dlg-scrim"><div class="card-white">
  <div style="padding:16px 24px 0"><div style="font-size:18px;font-weight:500;color:#000;margin-bottom:8px">Dialog Title</div>
  <div style="font-size:14px;color:var(--misc-474747);line-height:1.4">This is the body of the dialog.</div></div>
  <div style="height:16px"></div><div style="height:1px;background:var(--misc-cccccc)"></div>
  <div style="display:flex;justify-content:flex-end;align-items:center;gap:8px;padding:8px 12px"><span style="color:#6750A4;font-size:14px;font-weight:500;padding:8px 12px">Отмена</span><span style="background:var(--l-b0);color:#fff;font-size:14px;font-weight:500;padding:8px 12px;border-radius:4px">Confirm</span></div>
</div></div>
<div class="dlg-cap"><b>DialogButton</b><br>common/settings/ui/DialogButton.kt</div>
<div class="dlg-chips"><span class="dlg-chip"><span class="sw" style="background:#fff;border:1px solid #555"></span>bg #fff</span><span class="dlg-chip"><span class="sw" style="background:var(--l-b0)"></span>filled --l-b0</span></div></div>

<div class="dlg-cell"><div class="dlg-scrim"><div class="card-white">
  <div style="padding:16px 24px 0"><div style="font-size:18px;font-weight:500;color:#000;margin-bottom:8px">Подтвердите блокировку</div>
  <div style="font-size:14px;color:var(--misc-474747);line-height:1.4">Вы уверены, что хотите заблокировать этот GIFs?</div></div>
  <div style="height:16px"></div><div style="height:1px;background:var(--misc-cccccc)"></div>
  <div style="display:flex;justify-content:flex-end;align-items:center;gap:8px;padding:8px 12px"><span style="color:#6750A4;font-size:14px;font-weight:500;padding:8px 12px">Отмена</span><span style="background:var(--l-r0);color:#fff;font-size:14px;font-weight:500;padding:8px 12px;border-radius:4px">Блокировать</span></div>
</div></div>
<div class="dlg-cap"><b>DialogBlock</b><br>r/common/block/ui/DialogBlock.kt</div>
<div class="dlg-chips"><span class="dlg-chip"><span class="sw" style="background:#fff;border:1px solid #555"></span>bg #fff</span><span class="dlg-chip"><span class="sw" style="background:var(--l-r0)"></span>filled --l-r0</span></div>
<div class="dlg-warn">⚠ тот же макет, что DialogButton, но акцент красный вместо синего</div></div>
```

- [ ] **Step 3: Reload and verify**

Reload. `preview_console_logs` clean. `preview_snapshot`: D has 1 card, E has 2 cards. `preview_screenshot`: confirm white bg, 4dp button corners, blue vs red accent.

- [ ] **Step 4: Commit**

```bash
git add docs/design-copy/dialogs.html
git commit -m "feat(design-copy): add family D (new collection) and E (white) dialogs"
```

---

## Task 7: Menus section (from Task 1)

**Files:**
- Modify: `docs/design-copy/dialogs.html` (fill `#menus .dlg-grid`)

- [ ] **Step 1: Read each enumerated menu's source**

For every menu row recorded in Task 1, open its file and extract: background color, item text color, divider color, corner radius, item paddings, the actual item labels. Map each color to its `tokens.css` variable using the reference table (add the var with `grep` in `tokens.css` if a new hex appears; if truly missing, add it to the Unsorted section of `tokens.css` named `--misc-<hex>` and commit that change in this task).

- [ ] **Step 2: Build one card per menu**

Use this template per menu, substituting the extracted values (bg, item color, divider, labels):

```html
<div class="dlg-cell"><div class="dlg-scrim"><div style="width:240px;background:var(--TOKEN_BG);border-radius:8px;padding:6px 0;border:1px solid #2a2a2a">
  <div style="padding:10px 16px;color:var(--TOKEN_ITEM);font-size:16px">Пункт меню 1</div>
  <div style="padding:10px 16px;color:var(--TOKEN_ITEM);font-size:16px">Пункт меню 2</div>
</div></div>
<div class="dlg-cap"><b>NAME</b><br>FILE</div>
<div class="dlg-chips"><span class="dlg-chip"><span class="sw" style="background:var(--TOKEN_BG)"></span>bg --TOKEN_BG</span></div></div>
```

Replace `Пункт меню N` with the real labels from source. One card per menu.

- [ ] **Step 3: Reload and verify**

Reload. `preview_console_logs` clean. `preview_snapshot`: menus section has one card per enumerated menu with real labels. `preview_screenshot`: confirm each menu's bg/text matches its source file.

- [ ] **Step 4: Commit**

```bash
git add docs/design-copy/dialogs.html docs/design-copy/tokens.css
git commit -m "feat(design-copy): add menus section to dialogs catalog"
```

---

## Task 8: Bottom sheets section (from Task 1)

**Files:**
- Modify: `docs/design-copy/dialogs.html` (fill `#sheets .dlg-grid`)

- [ ] **Step 1: Build one card per bottom sheet**

For each sheet enumerated in Task 1, read its source and build a card. Bottom sheets dock to the screen bottom with a top-rounded container and a drag handle; render faithfully:

```html
<div class="dlg-cell"><div class="dlg-scrim" style="align-items:flex-end"><div style="width:300px;background:var(--TOKEN_BG);border-radius:16px 16px 0 0;padding:8px 0 16px">
  <div style="width:32px;height:4px;border-radius:2px;background:#666;margin:8px auto 12px"></div>
  <div style="padding:12px 16px;color:var(--TOKEN_ITEM);font-size:16px">Действие 1</div>
  <div style="padding:12px 16px;color:var(--TOKEN_ITEM);font-size:16px">Действие 2</div>
</div></div>
<div class="dlg-cap"><b>NAME</b><br>FILE</div>
<div class="dlg-chips"><span class="dlg-chip"><span class="sw" style="background:var(--TOKEN_BG)"></span>bg --TOKEN_BG</span></div></div>
```

Substitute real bg token and real action labels. If Task 1 found no real bottom sheets, replace this section's `.fdesc` with "В приложении модальных bottom sheet'ов как диалогов не найдено" and leave the grid empty.

- [ ] **Step 2: Reload and verify**

Reload. `preview_console_logs` clean. `preview_snapshot` + `preview_screenshot`: confirm sheets render docked to bottom with handle, or the "none found" note is present.

- [ ] **Step 3: Commit**

```bash
git add docs/design-copy/dialogs.html
git commit -m "feat(design-copy): add bottom sheets section to dialogs catalog"
```

---

## Task 9: Full-page verification pass

**Files:**
- Modify: `docs/design-copy/dialogs.html` (fixes only if issues found)

- [ ] **Step 1: Full structural snapshot**

Reload. Run `preview_snapshot` on the whole page. Confirm every popup from the spec inventory + Task 1 list is present exactly once, in the correct family section.

- [ ] **Step 2: Console + full screenshot**

Run `preview_console_logs` (expect zero errors/warnings). Run `preview_screenshot` of the full page. Compare each card against its source Kotlin: bg, accent, corner radius, button style, title/body text. Fix any mismatch inline and re-run this step until clean.

- [ ] **Step 3: Token-only check**

Run:
```bash
grep -nE 'background:#|color:#[0-9A-Fa-f]{6}' docs/design-copy/dialogs.html | grep -viE '#fff|#000|FEF7FF|65558F|6750A4|79747E|808080|3a3a3a|#444|#666|#555|#bbb|#aaa|#ddd|#eee|#2a2a2a|1b1b1f|#0d0d0d'
```
Expected: no hardcoded app-palette hexes remain (allow-list = neutral scaffolding + M3-default surfaces that have no token). Any remaining app color must be swapped to its `var(--…)` token.

- [ ] **Step 4: Commit**

```bash
git add docs/design-copy/dialogs.html
git commit -m "fix(design-copy): full-page verification pass for dialogs catalog"
```

---

## Task 10: Figma import instructions

**Files:**
- Modify: `docs/design-copy/dialogs.html` (append a non-printing instructions comment) OR create `docs/design-copy/README-figma-import.md`

- [ ] **Step 1: Write the import recipe**

Create `docs/design-copy/README-figma-import.md`:

```markdown
# Импорт каталога диалогов в Figma

1. Запусти preview-сервер: `python -m http.server -d docs/design-copy` (или launch.json `design-copy`, порт 4599). Открой `http://localhost:4599/dialogs.html`.
2. В Figma установи и запусти плагин **html.to.design**.
3. Способ A (URL): вставь `http://localhost:4599/dialogs.html` — плагин импортирует страницу в слои.
   Способ B (без сервера): сохрани страницу как .html и импортируй файлом.
4. Целевой файл: `7NP57du3gbSpG3RtjQMTG1` (или новый), отдельная страница/секция «Dialogs».
5. После импорта проверь: секции по семействам, реальный текст редактируется, плейсхолдеры пришли как rect.

Источник истины — этот HTML на `tokens.css`. При изменении диалогов в коде обнови HTML и переимпортируй.
```

- [ ] **Step 2: Commit**

```bash
git add docs/design-copy/README-figma-import.md
git commit -m "docs(design-copy): add Figma import recipe for dialogs catalog"
```

- [ ] **Step 3: Manual handoff (user action, not agent)**

The `html.to.design` import is performed by the user in the Figma desktop app (the Figma MCP cannot run plugins and would hit the Starter tool-call limit). Agent stops here and reports the catalog is ready for import.

---

## Self-Review

**Spec coverage:**
- All-popups scope → Tasks 1 (enumerate menus/sheets), 3–6 (11 modals), 7 (menus), 8 (sheets). ✓
- Style-family grouping → section shell Task 2; families A/A2/B/C/D/E. ✓
- Faithful render + tokens-only → per-card markup uses `var(--…)`; Task 9 Step 3 enforces. ✓
- Bugs reproduced + flagged → `.dlg-warn` badges in Tasks 3 (subscription text-button), 5 (invisible R title), 6 (default-purple button). ✓
- Token chips / mini-spec → `.dlg-chips` on every card. ✓
- Import-friendliness (flex/grid, real text, rect placeholders) → CSS in Task 2, placeholders as `.ph` rects. ✓
- Delivery via html.to.design → Task 10. ✓
- Browser verification → every build task has snapshot + console + screenshot steps. ✓

**Placeholder scan:** Tasks 7/8 use `NAME`/`FILE`/`--TOKEN_*` as substitution markers, not plan placeholders — they are filled from the Task 1 enumeration (the real list cannot be known until the grep runs). All known-dialog tasks (3–6) contain complete literal markup. ✓

**Type/name consistency:** CSS class names (`.dlg-scrim`, `.m3`, `.card-dark`, `.card-white`, `.foot-dark`, `.dlg-chip`, `.dlg-warn`, `.ph`) defined in Task 2 and used identically in Tasks 3–8. Token names match the reference table and the verified `tokens.css`. ✓
