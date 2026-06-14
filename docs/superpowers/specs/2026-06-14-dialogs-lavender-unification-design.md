# Унификация всех диалогов под лавандовый Material3 (стиль A)

Дата: 2026-06-14
Статус: дизайн утверждён, готов к плану реализации

## Цель

Привести **все диалоги приложения** к единому лавандовому Material3-стилю (семейство A):
фон `#EBE6EE`, акцент `#6552A5`, иконка 96dp по центру, типографика из `Theme.L.Type`.
Все настройки стиля живут в `Theme.L.DialogLavande`. Один общий composable
`LavenderDialog`, через который проходят все диалоги.

Это правка **кода Kotlin** (не HTML-каталога). Каталог `docs/design-copy/dialogs.html`
из прошлой задачи — справочник «как было».

## Решения (через brainstorming)

- **Кнопки:** filled главная (`#6552A5`) + text отмена (`#6552A5`).
- **Деструктив** (Удалить, Блокировать): красная заливка (`#F44336`); обычные действия — фиолетовые.
- **Архитектура:** один общий composable `LavenderDialog`, все диалоги через него (DRY).

## Не-цели (YAGNI)

- Не трогаем меню (expand-menus), `ThumbnailSizeSelector`, `SortByOrder`, `AlbumFilterDisplay`.
- Не трогаем `AlbumListPageSelector` и страничные селекторы с клавиатурой ввода.
- Не трогаем `MenuDotConfig` (legacy, не вызывается нигде).
- Не вводим тёмную/светлую вариативность — только лавандовый.

## Канонический composable `LavenderDialog`

Создаём **новый** `common/theme/LavenderDialog.kt` (прежний `DialogTemplate.kt` удалён
вне этой задачи и нигде не вызывался — не воссоздаём). Построен на M3
`BasicAlertDialog` + `Surface(color = Theme.L.DialogLavande.content, shape =
RoundedCornerShape(DialogLavande.cornerRadius))`, внутри `Column`:

- опциональная **иконка** (slot, размер `DialogLavande.iconSize` = 96dp, по центру);
- **заголовок** — `Theme.L.Type.dialogTitle` (центр при наличии иконки, иначе start);
- опциональное **тело** — `Theme.L.Type.dialogBody`, цвет `DialogLavande.bodyColor`;
- опциональный **слот контента** `@Composable (ColumnScope.() -> Unit)?` — список коллекций,
  поле ввода, строки-выбора; при больших списках сохраняем `heightIn(min=280,max=560)` и скролл;
- **Row действий** (`Arrangement.End`): dismiss = `TextButton` (цвет `dismissTextColor`),
  confirm = filled `Button` (заливка `buttonBackground` или `buttonBackgroundDestructive`).

Сигнатура:

```kotlin
@Composable
fun LavenderDialog(
    icon: @Composable (() -> Unit)? = null,
    title: String,
    body: AnnotatedString? = null,
    content: @Composable (ColumnScope.() -> Unit)? = null,
    confirmText: String? = null,
    onConfirm: () -> Unit = {},
    destructive: Boolean = false,
    dismissText: String = "Отмена",
    onDismiss: () -> Unit,
)
```

Поведение: `confirmText == null` → кнопка подтверждения не рисуется (для чистого chooser).
`destructive == true` → заливка `buttonBackgroundDestructive`.

## Дополнения в `Theme.L.DialogLavande`

Сейчас объект содержит: `content` `#EBE6EE`, `buttonBackground` `#6552A5`,
`buttonTextColor` White, `buttonBorderColor` `#6552A5`, `buttonBorderWidth` 1dp,
`buttonBorderRadius` 16dp, `button` TextStyle (16sp Medium). Добавить:

- `bodyColor = Color(0xFF474747)`
- `dismissTextColor = Color(0xFF6552A5)`
- `buttonBackgroundDestructive = Color(0xFFF44336)`
- `cornerRadius = 28.dp`
- `iconSize = 96.dp`

Заголовок/тело — из `Theme.L.Type.dialogTitle` (18sp Medium) и `dialogBody` (14sp Normal).

## Карта конверсии (11 диалогов)

| Диалог | Файл | Иконка | Контент | Главная кнопка |
|---|---|---|---|---|
| LavenderDialog (новый, канон) | common/theme/LavenderDialog.kt | опц. | опц. слот | по параметрам |
| AlbumDialogDeleteAlbum | l/.../screenAlbum/dialog/ | обложка 96 | — | «Удалить» красная |
| DialogSubscriptionDelete | r/.../saved/tab/ | аватар 96 | — | «Удалить» красная |
| DialogNicheDelete | r/.../savedNiche/ | превью 96 | — | «Удалить» красная |
| P2pSendChooserDialog | common/p2p/ui/ | — | 2 строки выбора | нет (только Отмена) |
| ConfirmDeleteFavoriteDialog | x/screens/favorites/ | превью 16:9 | — | «Удалить» красная |
| DialogCollection (R) | common/collectionDB/ui/ | — | список коллекций | «Создать» фиолетовая |
| L_DialogCollection | l/ui/screens/explorer/ | — | список коллекций | «Создать» фиолетовая |
| DaialogNewCollection | common/collectionDB/ui/ | — | OutlinedTextField | «Создать» фиолетовая |
| DialogButton | common/settings/ui/ | — | опц. composable-слот | filled фиолетовая (`destructive` опц.) |
| DialogBlock | r/common/block/ui/ | — | — | «Блокировать» красная |

Изменения акцентов: Album/Subscription/Niche/Favorite «Удалить» → красные; списки коллекций
и NewCollection «Создать» → фиолетовые; все фоны → `#EBE6EE`.

Исправляемые баги (попутно, как следствие унификации):
- `DialogCollection` (R): заголовок получает явный цвет (был не задан → невидим на тёмном).
- `DaialogNewCollection`: кнопка «Создать» перестаёт быть дефолт-M3 (станет фиолетовой).

## Сохранение интерфейсов

- `DialogButton(visible, title, body, buttonText, onDismiss, onBlockConfirmed, composable)` —
  сигнатуру сохраняем (становится тонкой обёрткой над `LavenderDialog`), чтобы
  `ConfigTextAndButtonWithDialog` и прочие вызовы не править.
- Остальные диалоги — внутренняя перерисовка, публичные сигнатуры по возможности не меняем.

## Граничные случаи

- Иконки-картинки (обложка/аватар/превью) на лаванде — скругление 8dp как было; нет картинки → заглушка.
- Списки коллекций: превью 72dp скруглённые, имя коллекции тёмным цветом; футер-кнопки уходят
  в стандартный Row действий `LavenderDialog`; внутренний разделитель не нужен.
- Иконки-less диалоги (DialogButton, DialogBlock, P2P, NewCollection) — заголовок по start (дефолт M3).

## Верификация

- Сборка компилируется (нет битых ссылок после переноса на `LavenderDialog`; старый
  `Theme.L.Dialog` уже переименован в `DialogLavande`, внешних ссылок не было).
- `@Preview` на каждый диалог обновлён; визуальная сверка в Android Studio.
- Чек-лист на каждый диалог: фон `#EBE6EE`; заголовок/тело из `Type`; кнопки по правилу
  (filled главная / text отмена / красный деструктив).
- Compose-вёрстка юнит-тестами не покрывается — гейт = успешная компиляция + previews.

## Критерии готовности

- Все 11 диалогов рисуются через `LavenderDialog`, лавандовые, единые по кнопкам.
- `Theme.L.DialogLavande` — единственный источник цветов/размеров диалогов.
- Объекты вне охвата не тронуты.
- Проект компилируется; previews отражают лавандовый стиль.
