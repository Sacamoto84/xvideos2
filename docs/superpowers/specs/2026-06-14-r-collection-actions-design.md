# R-коллекции: меню действий (как в L) + обложка в action-диалогах R/L

Дата: 2026-06-14
Статус: дизайн утверждён, готов к плану реализации

## Цель

1. По долгому нажатию на R-коллекцию показывать **меню действий** (как в L):
   Переименовать · Поделиться (P2P) · Удалить — вместо текущего прямого удаления.
2. Реализовать **renameCollection для R** (сейчас есть только у L) — общим методом в базе.
3. Добавить **обложку коллекции** (96dp) в action-диалоги **и R, и L** (сейчас только текст).

Правка кода Kotlin. Все диалоги уже на `LavenderDialog` (стиль A, лавандовый).

## Решения (через brainstorming)

- **P2P для R** реально не реализован (R = ссылки `GifsInfo`, не скачанные файлы как у L;
  обработчик `ShareCollection` жёстко завязан на `AppPath.l_collection`/`LCollectionExporter`).
  Сейчас: пункт «Поделиться (P2P)» в R-меню **показан, но disabled с подписью «скоро»**.
  Полноценный P2P-экспорт R-коллекций — **отдельный спек** (не входит в эту задачу).
- **renameCollection** — реализуем **общим в базе** (`CollectionDB` + `LinkCollectionStore`),
  L переводим на него, R получает наследованием. DRY.
- Заголовок action-диалога — «Действие с коллекцией»; имя коллекции — строкой под обложкой.

## Не-цели (YAGNI)

- Не реализуем P2P-экспорт/приём R-коллекций (отдельная задача).
- Не трогаем L-поведение P2P (остаётся рабочим).
- Не трогаем не связанные диалоги/экраны.

## Контекст (как есть)

- `LinkCollectionStore<T>` (база) — абстрактные `addCollection/deleteItemFromCollection/
  deleteCollection/createCollection/refreshCollectionList`. **Нет rename.**
- `CollectionDB<T>` — `create/deleteCollection/deleteItem/insert/readAllCollections`. **Нет rename.**
- `renameCollection(old,new): Boolean` есть только в `SavedL_Collection.kt:109` (L).
- `R_Saved_Collection : LinkCollectionStore<GifsInfo>` — есть delete/create/add, нет rename.
- R long-press: `R_Screen_CollectionTab.kt:97` `onCollectionLongClick = { itemPendingDelete = it }` → сразу удаление.
- L action-меню: `L_Screen_CollectionTab` — `itemPendingAction` → `LavenderDialog`, content =
  имя (Theme.L.b0) + 3 `DropdownMenuItem` (Переименовать/Поделиться P2P/Удалить). **Обложки нет.**
- Обложка по имени: R — `collectionList.first { it.collection == name }.items.lastOrNull()?.urls?.thumbnail`;
  L — `CollectionGridItem.previewUrl` (поле уже есть).

## Раздел 1 — Общий `renameCollection` в базе

- `CollectionDB.renameCollection(oldName, newName): Result<Boolean>` — физическое переименование
  файла/папки коллекции (по образцу существующей L-логики из `SavedL_Collection.renameCollection`).
- `LinkCollectionStore`: добавить `abstract fun renameCollection(oldName, newName)` (или дефолт,
  делегирующий в `collectionDb.renameCollection` + `refreshCollectionList`).
- `R_Saved_Collection`: реализовать через базу.
- `SavedL_Collection.renameCollection`: переключить на базовый метод (тонкая обёртка), L-поведение
  не меняется. Если базовый формат каталога L и R различается — base-метод параметризуется
  корнем; иначе единый.

## Раздел 2 — R action-меню (паритет с L)

`R_Screen_CollectionTab`:
- Состояния: `itemPendingAction: String?`, `itemPendingRename: String?`, `renameValue: String`
  (существующий `itemPendingDelete` остаётся).
- `onCollectionLongClick = { itemPendingAction = it }` (было `itemPendingDelete`).
- **Action-диалог** (`LavenderDialog`, `content` = список), как L:
  - Переименовать → `renameValue = pending; itemPendingRename = pending; itemPendingAction = null`.
  - Поделиться (P2P) → **disabled** (`enabled = false`) + текст «Поделиться (P2P) — скоро».
  - Удалить коллекцию → `itemPendingDelete = pending; itemPendingAction = null`.
- **Rename-диалог** (`itemPendingRename`): `LavenderDialog` + `OutlinedTextField`
  (цвета `Theme.L.DialogLavande.buttonBackground`, как в `DaialogNewCollection`),
  confirm «Сохранить» → `savedRed.collections.renameCollection(pending, renameValue)` → закрыть.
- **Delete-диалог** — существующий деструктив, без изменений.

## Раздел 3 — Обложка 96dp в action-диалогах R и L

- В action-`LavenderDialog` (R и L) добавить `icon = { … }`:
  обложка через `UrlImage(coverUrl, Modifier.clip(RoundedCornerShape(8.dp)).size(Theme.L.DialogLavande.iconSize))`;
  если `coverUrl == null` → серый `Box` того же размера.
- Резолв обложки по имени коллекции:
  - **R**: из `savedRed.collections.collectionList` найти по `collection == name`, взять
    `items.lastOrNull()?.urls?.thumbnail`.
  - **L**: из `savedL.collection.collectionList` (CollectionGridItem) найти по `name`, взять `previewUrl`.
- Имя коллекции остаётся строкой в `content` (как сейчас в L), заголовок «Действие с коллекцией».

## Верификация

- `gradlew :app:compileDebugKotlin` → `BUILD SUCCESSFUL` (гейт; Compose-вёрстка юнит-тестами не покрыта).
- `@Preview` action/rename диалогов R и L обновлены; визуальная сверка в Android Studio.
- Ручная проверка: long-press R-коллекции → меню; rename работает; P2P-пункт серый/неактивный;
  обложка видна в R и L; пустая коллекция → плейсхолдер.

## Критерии готовности

- R long-press → меню (Переименовать/Поделиться-disabled/Удалить), не прямое удаление.
- `renameCollection` работает для R (и L по-прежнему), реализован общим в базе.
- Обложка 96dp видна в action-диалогах R и L; плейсхолдер при отсутствии.
- P2P-пункт R disabled «скоро»; L P2P не сломан.
- Проект компилируется.
