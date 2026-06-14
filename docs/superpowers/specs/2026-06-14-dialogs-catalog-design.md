# Каталог всех диалогов приложения (as-is) → Figma

Дата: 2026-06-14
Статус: дизайн утверждён, готов к плану реализации

## Цель

Свести **все всплывающие окна** приложения в один точный (pixel-faithful)
каталог, сгруппированный по визуальным стилям, чтобы увидеть зоопарк стилей и
расхождения. Артефакт — `docs/design-copy/dialogs.html`, далее импорт в Figma
плагином `html.to.design`. Каталог **as-is**: воспроизводим как есть, ничего не
унифицируем.

## Не-цели (YAGNI)

- Не предлагаем единый стиль и не унифицируем (отдельная задача потом).
- Не правим сам код приложения (баги только фиксируем визуально, не чиним).
- Не строим нативные Figma-фреймы через MCP (путь заблокирован лимитом Starter,
  см. ниже) — доставка только через HTML→плагин.

## Контекст / предыстория

- Figma на плане **Starter**: `figma.createPage()` не персистится, жёсткий лимит
  вызовов MCP. Нативный билд диалогов (Phase 7 прошлой «designer copy») **упёрся
  в лимит и не сделан**. Файл `7NP57du3gbSpG3RtjQMTG1` содержит только переменные
  (63 токена, 6 коллекций) и каталог цветов/типографики.
- После лимита был пивот на локальный HTML `docs/design-copy/` (`tokens.css` =
  159 CSS-переменных, 1:1 с кодом; `styles.css` = примитивы + текст-стили).
- Текущий `docs/design-copy/dialogs.html` — тонкая заглушка (12 КБ, 3 секции, ни
  одного реального диалога). Перестраивается в полный каталог.

## Охват (что каталогизируем)

Три группы попапов; точный список меню/шторок собирается грепом на этапе сбора.

1. **Модальные диалоги (11, уже разобраны)** — `Dialog` / `AlertDialog`.
2. **Меню** — dropdown / контекстные (`ExpandMenuVideo`, `MenuDot`,
   `DropdownMenuItem_Block`, `DropdownMenuItem_AddCollection`, expand-menu из
   `ExpandMenuVM` / `LazyRowPictureDetailsHost` и др.).
3. **Bottom sheet'ы** — `ModalBottomSheet` / нижние шторки, если применяются как
   диалоги (`ScreenP2pSend`, фильтры/сортировки).

### Инвентарь модальных диалогов и семейства стилей

| # | Composable | Файл | Семейство | Фон | Акцент-кнопка |
|---|-----------|------|-----------|-----|---------------|
| 1 | DialogTemplate | common/theme/DialogTemplate.kt | A — лавандовый M3 | #EBE6EE | filled #6552A5 |
| 2 | AlbumDialogDeleteAlbum | l/.../screenAlbum/dialog/AlbumDialogDeleteAlbum.kt | A | #EBE6EE | filled #6552A5 |
| 3 | DialogSubscriptionDelete | r/.../saved/tab/DialogSubscriptionDelete.kt | A | #EBE6EE | text #6552A5 |
| 4 | DialogNicheDelete | r/.../savedNiche/DialogNicheDelete.kt | A | #EBE6EE | text #6552A5 |
| 5 | P2pSendChooserDialog | common/p2p/ui/P2pSendChooserDialog.kt | A2 — дефолтный M3 | M3 default | text |
| 6 | ConfirmDeleteFavoriteDialog | x/screens/favorites/FavoritesDeleteDialog.kt | B — тёмный M3 | #2E2E2E | text #FF6B6B |
| 7 | DialogCollection | common/collectionDB/ui/DialogCollection.kt | C — тёмный кастом-список | #090909 | filled #EBFA63 (жёлтый, R) |
| 8 | L_DialogCollection | l/ui/screens/explorer/L_DialogCollection.kt | C | #090909 | filled #ff96a3 (розовый, L) |
| 9 | DaialogNewCollection | common/collectionDB/ui/DaialogNewCollection.kt | D — тёмный кастом-ввод | #090909 | filled M3 default |
| 10 | DialogButton | common/settings/ui/DialogButton.kt | E — белый кастом | #FFFFFF | filled #2196F3 (синий) |
| 11 | DialogBlock | r/common/block/ui/DialogBlock.kt | E | #FFFFFF | filled #F44336 (красный) |

### Зафиксированные расхождения / баги (воспроизводим as-is + плашка ⚠)

- **Пять разных фонов** диалогов (#EBE6EE, M3 default, #2E2E2E, #090909, #FFFFFF).
- **Шесть акцент-цветов** кнопок (фиолетовый, жёлтый, розовый, синий, красный, коралл).
- **Один макет — разный акцент:** C (жёлтый R / розовый L), E (синий / красный).
- **Непоследовательность кнопок** внутри A: filled vs только текст.
- **Баг:** `DialogCollection` (R) — цвет заголовка не задан → почти невидим на #090909.
- **Баг:** `DaialogNewCollection` — кнопка «Создать» осталась дефолтно-фиолетовой M3.

## Артефакт и структура

- **Файл:** перестроенный `docs/design-copy/dialogs.html`; подключает
  `tokens.css` + `styles.css`. Открывается preview-сервером `design-copy` (порт 4599).
- **Организация:** секции по семействам стилей (A · A2 · B · C · D · E · Меню ·
  Bottom sheets). Заголовок секции `<h2>` → в Figma Section/Frame.
- **Анатомия карточки (одна на попап):**
  - Frame диалога — точная копия (корпус, контент, кнопки) на тёмном scrim-подложке.
  - Подпись — имя composable + путь к файлу.
  - Мини-спека — чипы использованных токенов (фон / рамка / акцент: имя токена + hex).
  - Плашка ⚠ при баге/непоследовательности.

## Правила точности

- **Цвета только через `tokens.css`** (`var(--…)`), хардкод запрещён → 1:1 с кодом
  и осмысленные имена слоёв в Figma. Если токена нет — добавить в `tokens.css`
  (раздел Unsorted) с именем по источнику.
- **Шрифт DM Sans**; размеры = sp из кода (dialogTitle 18, dialogBody 14, button 16);
  вес Medium=500 / Bold=700 / Normal=400.
- **dp→px 1:1**; радиусы/паддинги/высоты точно из исходника (corner 28/12/8/4,
  футер 56dp, иконка 96dp, превью X 16:9 160dp и т.д.).

## Дружелюбность к импорту (`html.to.design`)

- flex/grid вместо абсолютного позиционирования → импорт в авто-лейаут.
- Реальный текст, не картинки.
- Иконки/превью = простые прямоугольники-плейсхолдеры (без внешних изображений) →
  импортируются как rect.
- Никаких теней/градиентов/блюра.

## Поток доставки

1. Собрать `dialogs.html`; **проверить в браузере** preview-сервером, сверить со
   снапшотом эталонного рендера.
2. В Figma запустить плагин `html.to.design` → вставить URL/HTML → импорт в файл
   `7NP57du3gbSpG3RtjQMTG1` (или новый), отдельная страница/секция «Dialogs».
3. Результат: секции по семействам, карточки с подписями и токен-чипами, баги помечены.

## Критерии готовности

- Все попапы трёх групп присутствуют, каждый = отдельная карточка.
- Цвета/шрифты/размеры визуально совпадают с приложением (сверка по `tokens.css`
  и исходникам).
- Расхождения и баги помечены.
- HTML открывается preview-сервером без ошибок консоли.
- Структура пригодна для чистого импорта `html.to.design` (авто-лейаут, текст, rect).
