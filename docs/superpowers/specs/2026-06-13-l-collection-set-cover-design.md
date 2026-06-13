# «Сделать обложкой» в меню элемента коллекции L — дизайн

Дата: 2026-06-13
Статус: одобрен

## Проблема

Бэкенд ручной обложки коллекции есть (`SavedL_Collection.setManualCover`,
пишет `coverFolderName` в `collection.json`), но в UI её задать нельзя.

## Решение

Пункт «Сделать обложкой» в выпадающем меню элемента внутри коллекции.
Меню элемента коллекции — `SavedLikesItemExpandMenu` (рендерится через
`ExpandMenuType.LIKES` с `isCollection=true` из `ScreenCollectionName`).

## Компонент

`DropdownMenuItem_SetCover` (новый, `l/ui/element/expandMenu/element/`) —
по образцу `DropdownMenuItem_RemoveFromCollection`:

- сигнатура `(item: PicsDetails? = null, savedL: SavedL? = null, onDismiss: () -> Unit)`;
- `leadingIcon` — иконка обложки, `text` «Сделать обложкой», `style`/`tintColor`
  из `Theme.L.ExpandMenu`;
- `onClick`: `item == null || savedL == null` → `onDismiss` и выход;
  `savedL.collection.currentCollectionName == null` → `onDismiss` и выход;
  иначе `savedL.collection.setManualCover(item)` → `onDismiss`.

`setManualCover(item)` использует `currentCollectionName` по умолчанию, находит
папку item через `lFindCollectionItemFolder`, пишет `collection.json`, шлёт
SnackBar «Обложка коллекции обновлена» и зовёт `refreshCollectionList()`.
Если папка не найдена — SnackBar.error внутри `setManualCover`. Доп. проводки
на стороне меню нет.

## Проводка

В `SavedLikesItemExpandMenu`, внутри ветки `if (isCollection)`, под
`DropdownMenuItem_RemoveFromCollection`:

```
DropdownMenuItem_SetCover(item, savedL) { expanded = false }
```

`savedL` уже передаётся в меню из `ExpandMenuViewModel.ExpandMenuLikes`.

## Решения

- Пункт только в коллекции (`isCollection=true`). Вне коллекции ручная
  обложка смысла не имеет.
- `AlbumItemExpandMenu` не трогаем — экран коллекции использует LIKES-меню.
- Пункт показывается всегда, в т.ч. для текущей обложки (повторная установка
  идемпотентна — YAGNI, не прячем).

## Тесты

Compose-меню юнит-тестами в проекте не покрыты; `setManualCover` — существующий
протестированный путь FS. Проверка: компиляция + ручной смоук (меню элемента →
«Сделать обложкой» → SnackBar → грид коллекций показывает выбранную обложку).

## Вне объёма

- Пометка/чек у текущей обложки в меню.
- Кнопка сброса ручной обложки (возврат к авто).
- Установка обложки из меню альбома.
