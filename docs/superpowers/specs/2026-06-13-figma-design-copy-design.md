# Figma Design Copy (as-is) — Design Spec

**Date:** 2026-06-13
**Status:** Pending user review of spec

## Problem / Goal

Нужна **дизайнерская копия приложения в Figma** как документация текущего
состояния (source of truth, снимок «как есть»):

- зафиксировать **цветовую систему** приложения (все токены) как Figma Variables
  + styles + страница-каталог;
- воспроизвести **все экраны** трёх секций (X / L / R) + common как фреймы;
- учесть **выпадающие меню (expand-menu / dropdown)** и **диалоги**;
- на каждом экране — **описание используемых цветов** (выноски → токены).

Не редизайн, не редактируемая для переработки система — точный снимок текущего
UI с привязкой цветов к переменным.

## Решения (по итогам брейншторма)

| Вопрос | Решение |
|---|---|
| Цель | Документация дизайна, «как есть», source of truth |
| Охват | Все 3 секции (X / L / R) + common, в одном проходе (батчами) |
| Источник визуала | Реконструкция из Compose-кода + токенов; контент/картинки = плейсхолдеры |
| Глубина цвета | Figma **Variables + Styles**, привязка к элементам + каталог + выноски на экранах |
| Подход | **C** — сначала Variables + библиотека компонентов, затем сборка экранов из компонентов |

## Источники в коде

- Центральная тема: `app/src/main/java/com/client/xvideos/common/theme/Theme.kt`
  (`Theme.*` shared + `Theme.R` / `Theme.L` / `Theme.X`).
- Legacy / Material seeds: `app/src/main/java/com/client/xvideos/ui/theme/Color.kt`,
  `Theme.kt` (Material `XvideosTheme`, dynamic color), `Type.kt`.
- Карта навигации: `docs/architecture/navigation.md`
  (+ FigJam board `figma.com/board/QVsa8oJOkWrFfdCP7FGNi1`).
- Предыстория токенов: `docs/superpowers/specs/2026-06-07-theme-unification-design.md`.

## Архитектура Figma-файла

Один файл **«App — Design Copy (as-is)»**. Страницы:

| Стр. | Содержимое |
|---|---|
| `00 · Index / Cover` | карта файла, легенда выносок, ссылка на nav-board |
| `01 · Color System` | каталог Variables + типографика (источник истины) |
| `02 · Components` | библиотека: бары, карточки, строки, кнопки, dialog-shell, expand-menu |
| `03 · X` | экраны секции X |
| `04 · L` | экраны секции L |
| `05 · R` | экраны секции R |
| `06 · Common` | Menu, Settings, P2P, AppLock, Permission |
| `07 · Dialogs & Menus` | все диалоги и выпадающие меню матрицей |

Фрейм экрана = телефон **360×800 dp** (точка-в-точку с dp Compose).

## Цветовая система (Figma Variables, 1 режим = dark)

Коллекции переменных 1:1 с кодом. Имя переменной = путь токена в коде.

### `Theme/Shared`
| Токен | Hex |
|---|---|
| `background` | `#262626` |
| `backgroundAppRoot` | `#262626` |
| `tabLevel0` | `#212121` |
| `tabLevel1` | `#282828` |
| `tabLevel2` | `#333333` |
| `tabLevel3` | `#444444` |
| `tabLevel4` | `#555555` |
| `tabLevel5` | `#666666` |
| `tabLevel6` | `#777777` |

### `Theme/R`
`colorCommonBackground` `#303030`, `colorBottomBarDivider` `#323153`,
`colorYellow` `#EBFA63`, `colorBlue` `#61B2EB`, `colorRed` `#EA616F`,
`colorTextGray` `#8B8B8B`, `colorBorderSelect` `#444444`, `colorBorderGray` `#3F3F3F`.

### `Theme/L`
`grey0` `#dedede`, `grey1` `#bababa`, `grey2` `#9c9c9c`, `grey3` `#3b3b3b`,
`grey4` `#333333`, `grey5` `#292929`, `grey6` `#262626`, `grey7` `#1c1c1c`,
`g0` `#4CAF50`, `r0` `#F44336`, `b0` `#2196F3`, `lavender` `#a3aff5`,
`primaryColor` `#ff96a3`, `red` `#C9554C`, `secondaryColor` `#3b3b3b`,
`textColor` (= `grey1`), `ExpandMenu.tint` `#1F1F1F`, `ExpandMenu.bg` `#FFFAF5`.

### `Theme/X`
`expandMenuBackground` `#F2EDF7`.

### `Legacy/PornHub`
Orange `#EF9E00`, Red `#E01E5A`, Green `#1ED760`, Blue `#0095F6`,
Purple `#9C27B0`, Yellow `#FFD600`, Pink `#F06292`, Brown `#A1887F`,
Grey `#9E9E9E`, separator `#9E9E9E`; gray-ramp `#0E0E0E` `#101010` `#151515`
`#1F1F1F` `#212121` `#252525` `#3F3F3F` `#969696` `#C6C6C6`; Black `#000000`,
White `#FFFFFF`.

### `Material/XvideosTheme`
Purple80 `#D0BCFF`, PurpleGrey80 `#CCC2DC`, Pink80 `#EFB8C8`,
Purple40 `#6650a4`, PurpleGrey40 `#625b71`, Pink40 `#7D5260`.
> Пометка: `XvideosTheme` использует dynamic color (Material You, Android 12+);
> на кастомно-тематизированных экранах почти не проявляется. Включить в каталог
> как «существует, но вне основного UI».

### `Unsorted/*`
Шаг сборки: пройти хардкод-литералы `Color(0x…)` в Compose, не покрытые
токенами выше (напр. niche `0x0F0F0F`, фоны fullscreen-плееров), и внести сюда
с указанием места.

### Типографика (Figma text styles из `Theme.L.Type{}`)
`screenTitle` 24/30, `heroTitle` 28/34, `sectionTitle` 13/18, `rowTitle` 16/22,
`rowValue` 16/22, `rowSubtitle` 13/18, `body` 16/22, `bodyLarge` 18/24,
`button` 16/20, `caption` 12/16, `mediaIndex` 12/16, `menuItem` 18/24,
`dialogTitle` 18/24, `dialogBody` 14/20.
Шрифты: **Poppins** (Regular/Medium/SemiBold/Bold/ExtraBold) + **DM Sans** (app font; `Karla` — алиас на DM Sans).

### Каталог (стр. `01`)
Свотч на каждую переменную: имя токена + hex + краткое «где используется».
Сетки свотчей сгруппированы по коллекциям. Контраст-блок: тёмные фоны секций
vs **светлые** expand-menu (`#FFFAF5`, `#F2EDF7`).

## Библиотека компонентов (стр. `02`)

Компоненты с вариантами, **все заливки привязаны к Variables** (не сырой hex),
авто-лейаут, у каждого — выноска с именами токенов.

- **Бары:** TopBar; BottomNavBar (варианты по секциям R/L/X — разный фон `tabLevel*`); TabRow + подтабы.
- **Медиа:** VideoCard / GifCard / AlbumCover / ImageThumb (контент = плейсхолдер); badge длительности; badge индекса (`mediaIndex`).
- **Списки:** settings-row (`rowTitle` + `rowValue`); collection-item; creator-row; tag-chip.
- **Контролы:** кнопка (primary / secondary / text); checkbox + label; поле ввода; seekbar + плеер-контролы.
- **Оверлеи:** Dialog-shell (фон, `dialogTitle`, `dialogBody`, кнопки); ExpandMenu-shell (светлый `#FFFAF5` / X `#F2EDF7`); DropdownMenuItem (иконка + `menuItem`).

## Инвентарь экранов

Где у экрана есть состояния (loading / empty / content) — отдельный фрейм
**только если** меняются цвета; иначе один фрейм.

### X (стр. `03`)
Dashboards (грид), Tags, ScreenK, Profile, Favorites, SavedX, VideoPlayer,
LocalVideoPlayer, VideoPlayerFullScreen, нижние нав-кнопки.

### L (стр. `04`)
Login; Explorer + табы (AlbumList, Saved[подтабы: Likes / Collection / Albums],
TopHits, Search); Album; AlbumList (с фильтром); AlbumLandingTag; CollectionName;
FullScreenImage (оверлей).

### R (стр. `05`)
Root + нижняя навигация; Explorer + табы (Gifs, Niches, Saved[подтабы:
Collection / Creators / Likes / Download / Subscriptions]); Niche; Profile;
TopThisWeek; ManageBlock; FullScreen.

### Common (стр. `06`)
Menu (главное меню), Settings, P2P Send, P2P Receive, AppLock (ввод кода),
Permission.

Ориентировочно ~40 фреймов.

## Диалоги и меню (стр. `07`, матрица)

Строка = меню/диалог; рядом — раскрытое состояние + выноски токенов.

### Диалоги
- **L:** `AlbumDialogDeleteAlbum`, `L_DialogCollection`, `DialogCollection`, `DaialogNewCollection`.
- **R:** `DialogBlock`, `DialogSubscriptionDelete`, `DialogNicheDelete`.
- **X:** `FavoritesDeleteDialog`.
- **Common:** `P2pSendChooserDialog`, settings `DialogButton` / `ConfigTextAndButtonWithDialog`.

### Expand-menu / dropdown
- **L:** `AlbumItemExpandMenu`, `SavedLikesItemExpandMenu` + пункты `DropdownMenuItem_*` (SetCover, SaveToGallery, Share, Delete, Download, AddCollection, RemoveFromCollection).
- **R:** `ExpandMenuVideo`, `ExpandMenuVideoTags`, `ExpandMenuHistoryContent`, `ExpandMenuHelperContent` + пункты `DropdownMenuItem_*` (Like, Follow, Block, Subscription, Download, Share, Add/RemoveCollection, SaveToGallery).
- **X:** `X_DashboardExpandMenu`, `MenuDot`.

### Селекторы-фильтры
`AlbumFilterDisplay`, `AlbumListFilterSize`, `SortByOrder`, `ThumbnailSizeSelector`, `AlbumListPageSelector`.

> Подсветить контраст: таб-меню R/L/X — тёмные (`tabLevel*`), а expand-menu
> L/X — **светлые** (`#FFFAF5` / `#F2EDF7`).

## Аннотации (описание цветов по экранам)

На каждом фрейме — нумерованные выноски, ведущие в боковую легенду
«элемент → токен (hex)». Все заливки фреймов и компонентов привязаны к Figma
Variables, поэтому правка одного токена обновляет всё. Это и есть требуемое
«описание используемых цветов» по экранам.

## Порядок сборки (батчами)

1. Variables (все коллекции) + text styles + каталог (стр. `01`).
2. Библиотека компонентов (стр. `02`).
3. Экраны: X → L → R → Common (стр. `03`–`06`), батч на страницу/секцию.
4. Диалоги и меню (стр. `07`).
5. Index / Cover / легенда (стр. `00`).

## Предпосылки

- **Figma MCP авторизован** — проверить `whoami` до начала. Если нет — пользователь авторизуется.
- **Шрифты Poppins + DM Sans доступны в Figma**. Если нет — текст подменится дефолтом; зафиксировать как известное отклонение.
- Генерация ~60 поверхностей через Figma MCP **токеноёмкая** — строго батчами по страницам, не одним вызовом.

## Критерии готовности

Это документация, не код. «Готово» =:

- все ~40 экранов + все перечисленные диалоги/меню присутствуют как фреймы;
- каждая заливка привязана к переменной; нет «висячих» hex, кроме осознанно вынесенных в `Unsorted/*`;
- каталог покрывает все токены из `Theme.*` + `Legacy/PornHub` + `Material`;
- типографика и шрифты соответствуют `Theme.L.Type{}`;
- секционная визуальная сверка фреймов с кодом/реальным экраном (spot-check X / L / R / Common).

## Вне области

- Редизайн или изменение токенов (только фиксация «как есть»).
- Изменение кода приложения.
- Анимации/переходы (фиксируем статичные состояния; навигация уже описана в `navigation.md`).
- Реальный медиа-контент (везде плейсхолдеры).
- Воспроизведение dynamic-color вариаций Material You (фиксируем только базовые seed-цвета).

## Риски

- **Фиделити Figma-генерации из кода** варьируется на сложных Compose-экранах (плеер, гриды) — митигируем секционным spot-check и ручной правкой.
- **Объём** (~40 экранов + 30+ меню/диалогов) — митигируем батч-сборкой и тем, что экраны собираются из готовых компонентов.
- **Хардкод-цвета вне токенов** — отлавливаем на шаге `Unsorted/*`.
- **Доступность шрифтов** в Figma — проверить заранее.
