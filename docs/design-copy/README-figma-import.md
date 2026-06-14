# Импорт каталога диалогов в Figma

Каталог: `dialogs.html` — 21 всплывающее окно (11 модальных диалогов + 7 меню + 3 селектора),
сгруппированы по визуальным стилям, цвета 1:1 из `tokens.css`.

## Шаги

1. Запусти preview-сервер:
   - `python -m http.server -d docs/design-copy` → открой `http://localhost:4599/dialogs.html`
   - (или launch.json конфиг `design-copy`, порт 4599)
2. В Figma установи и запусти плагин **html.to.design**.
3. Импорт:
   - **Способ A (URL):** вставь `http://localhost:4599/dialogs.html` — плагин импортирует страницу в слои.
   - **Способ B (файл/без сервера):** сохрани страницу как `.html` и импортируй файлом.
4. Целевой файл: `7NP57du3gbSpG3RtjQMTG1`
   (https://www.figma.com/design/7NP57du3gbSpG3RtjQMTG1) или новый — отдельная страница/секция «Dialogs».
5. После импорта проверь: секции по семействам (A, A2, B, C, D, E, Меню, Селекторы), реальный
   текст редактируется, иконки/превью пришли как rect, плашки ⚠ на месте.

## Что внутри (семейства)

- **A** — лавандовый Material3 (L): DialogTemplate, AlbumDialogDeleteAlbum, DialogSubscriptionDelete, DialogNicheDelete.
- **A2** — дефолтный Material3: P2pSendChooserDialog.
- **B** — тёмный Material3 (X): ConfirmDeleteFavoriteDialog.
- **C** — тёмный кастом-список: DialogCollection (R, жёлтый), L_DialogCollection (L, розовый).
- **D** — тёмный кастом-ввод: DaialogNewCollection.
- **E** — белый кастом: DialogButton (синий), DialogBlock (красный).
- **Меню** — AlbumItemExpandMenu (L), ExpandMenuVideo (R), ExpandMenuVideoTags (R), X_DashboardExpandMenu, FavoriteActionsExpandMenu, ThumbnailSizeSelector, MenuDotConfig (legacy).
- **Селекторы** — SortByOrder, AlbumListPageSelector, AlbumFilterDisplay.

## Помеченные расхождения (⚠)

- 5 разных фонов диалогов + 4 разных фона меню.
- Один макет — разный акцент: C (жёлтый/розовый), E (синий/красный).
- A: filled-кнопка против только-текста.
- Меню: язык пунктов смешан (L/R English, X русский).
- Баги: DialogCollection (R) — невидимый заголовок; DaialogNewCollection — дефолтная M3-кнопка.
- MenuDotConfig — legacy (MenuDot закомментирован).

## Источник истины

Этот HTML на `tokens.css` (1:1 с кодом). При изменении диалогов в коде обнови HTML и переимпортируй.
Спека: `docs/superpowers/specs/2026-06-14-dialogs-catalog-design.md`,
план: `docs/superpowers/plans/2026-06-14-dialogs-catalog.md`.
