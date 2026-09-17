# Код-ревью xvideos — проход 89

> **Срез:** `94e4c62` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `94e4c62`. Затронуты модули `:core`, `:feature-l`, `:feature-r`, `:feature-x`, `:app`. Усилена безопасность работы с файловой системой, предотвращены потенциальные блокировки главного потока (ANR), улучшен UX поиска подсказок и проведена комплексная чистка кода от однобуквенных переменных.

Линзы:
- `C` / Защита имён коллекций от внедрения null-байтов (`\u0000`) в `CollectionName` ([CollectionName.kt](../core/src/main/java/com/client/xvideos/common/collectionDB/CollectionName.kt), [CollectionNameTest.kt](../core/src/test/java/com/client/xvideos/common/collectionDB/CollectionNameTest.kt)).
- `C` / Асинхронное переименование коллекций L на пуле `Dispatchers.IO` ([SavedL_Collection.kt](../feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt), [ScreenSavedCollectionSM.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/ScreenSavedCollectionSM.kt)).
- `UI` / Устранение спама снекбарами ошибок при поиске подсказок ниш в `R_SearchNiches.suggestionsFor` ([R_SearchNiches.kt](../feature-r/src/main/java/com/client/xvideos/r/common/search/R_SearchNiches.kt)).
- `UI` / Устранение неинформативных однобуквенных имён переменных (`l`, `a`), удаление мертвого кода и вынос констант ([DashboardsPaginatedListScreen.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/DashboardsPaginatedListScreen.kt), [ComposeTags.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/atom/ComposeTags.kt), [ProfileInfo1.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/top_this_week/ProfileInfo1.kt), [R_Saved_Collection.kt](../feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Collection.kt), [ItemCollectionPagingSource.kt](../feature-r/src/main/java/com/client/xvideos/r/common/pagin/ItemCollectionPagingSource.kt), [ScreenAlbumListSM.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumListSM.kt), [AlbumListFilterContentType.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumListFilterContentType.kt), [KeyboardNumber.kt](../core/src/main/java/com/client/xvideos/common/ui/keyboard/KeyboardNumber.kt), [TabRow.kt](../core/src/main/java/com/client/xvideos/common/ui/TabRow.kt), [Config_G_0_4.kt](../app/src/main/java/com/client/xvideos/screenSettings/Config_G_0_4.kt)).
- `T` / Добавление тестов коллекций L (`SavedL_CollectionTest`) и валидации null-байтов `CollectionNameTest` ([SavedL_CollectionTest.kt](../feature-l/src/test/java/com/client/xvideos/l/featured/saved/SavedL_CollectionTest.kt), [CollectionNameTest.kt](../core/src/test/java/com/client/xvideos/common/collectionDB/CollectionNameTest.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### C144 — Отсутствие проверки null-байтов (\u0000) при валидации имён коллекций в CollectionName. Средняя.

[core/src/main/java/com/client/xvideos/common/collectionDB/CollectionName.kt:28-34](../core/src/main/java/com/client/xvideos/common/collectionDB/CollectionName.kt#L28)

В `CollectionName.normalizeOrNull` проверялись слэши, двоеточия и скрытый префикс `.`, однако отсутствовала проверка на null-байт (`\u0000`). Поскольку имя коллекции напрямую формирует имя каталога на диске (`File(path, safeName)`), наличие null-байта в низкоуровневых системных вызовах POSIX/Android приводит к обрезанию строки до null-байта (Null Byte Injection) либо к неожиданным ошибкам в файловой подсистеме.

**Исправление:**
- В `CollectionName` добавлен массив запрещённых символов `FORBIDDEN_CHARS = charArrayOf('/', '\\', ':', '\u0000')`.
- Имена, содержащие `\u0000`, теперь гарантированно возвращают `null`.
- В `CollectionNameTest` добавлен юнит-тест `имя с нулевым байтом отвергается`.

---

### C145 — Потенциальная блокировка главного потока (ANR) при переименовании коллекций L. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt:145-203](../feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt#L145)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/ScreenSavedCollectionSM.kt:27-31](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/ScreenSavedCollectionSM.kt#L27)

В `SavedL_Collection` метод `renameCollection` выполнялся синхронно на потоке вызывающего:
при невозможности прямого `oldRoot.renameTo(newRoot)` вызывался резервный метод `oldRoot.copyRecursively(newRoot, overwrite = false)` с последующим `oldRoot.deleteRecursively()`. Если в коллекции находились сотни или тысячи медиафайлов, вызов из UI-потока или некорректно задиспатченной корутины приводил бы к зависанию приложения (ANR). В отличие от `deleteCollection` и `createCollection`, перенос на пул ввода-вывода не был инкапсулирован внутри держателя состояния.

**Исправление:**
- Вся файловая работа (`exists`, `renameTo`, `copyRecursively`, `deleteRecursively`) в `SavedL_Collection.renameCollection` перенесена в `scope.launch(Dispatchers.IO)`.
- Обновление Compose-состояния (`currentCollectionName`, `refresh()`, `refreshCollectionList()`) и показ уведомлений `SnackBar` выполняются строго на `Dispatchers.Main`.
- В `ScreenSavedCollectionSM` удалён избыточный внешний `scope.launch(Dispatchers.IO)`.

---

### UI117 — Спам снекбарами ошибок при вводе в строке поиска ниш R_SearchNiches. Низкая.

[feature-r/src/main/java/com/client/xvideos/r/common/search/R_SearchNiches.kt:73-76](../feature-r/src/main/java/com/client/xvideos/r/common/search/R_SearchNiches.kt#L73)

В `R_SearchNiches.suggestionsFor` блок `catch (e: Exception)` вызывал `SnackBar.error(e.localizedMessage ?: "Unknown error")`. При наборе поискового запроса в условиях нестабильной сети или оффлайн-режима пользователь получал всплывающее окно ошибки на каждое дебаунсированное нажатие клавиши, что разрушало пользовательский опыт. В аналогичном компоненте `R_SearchExplorer` фоновые ошибки подсказок корректно логировались без прерывания ввода.

**Исправление:**
- Вызов `SnackBar.error` заменён на логирование `Timber.e(e, "!!! R_SearchNiches suggestions error: ${e.localizedMessage}")`.
- Удалён неиспользуемый импорт `SnackBar`.

---

### UI118 — Однобуквенные неинформативные имена переменных, мёртвый код и вынос констант. Низкая.

[feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/DashboardsPaginatedListScreen.kt:93](../feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/DashboardsPaginatedListScreen.kt#L93)
[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/atom/ComposeTags.kt:37-54](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/atom/ComposeTags.kt#L37)
[feature-r/src/main/java/com/client/xvideos/r/ui/top_this_week/ProfileInfo1.kt:50-64](../feature-r/src/main/java/com/client/xvideos/r/ui/top_this_week/ProfileInfo1.kt#L50)
[feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Collection.kt:96](../feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Collection.kt#L96)
[feature-r/src/main/java/com/client/xvideos/r/common/pagin/ItemCollectionPagingSource.kt:18](../feature-r/src/main/java/com/client/xvideos/r/common/pagin/ItemCollectionPagingSource.kt#L18)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumListSM.kt:132,191](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumListSM.kt#L132)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumListFilterContentType.kt:61](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumListFilterContentType.kt#L61)
[core/src/main/java/com/client/xvideos/common/ui/keyboard/KeyboardNumber.kt:194](../core/src/main/java/com/client/xvideos/common/ui/keyboard/KeyboardNumber.kt#L194)
[core/src/main/java/com/client/xvideos/common/ui/TabRow.kt:128](../core/src/main/java/com/client/xvideos/common/ui/TabRow.kt#L128)
[app/src/main/java/com/client/xvideos/screenSettings/Config_G_0_4.kt:57](../app/src/main/java/com/client/xvideos/screenSettings/Config_G_0_4.kt#L57)

По кодовой базе обнаружены остаточные однобуквенные имена `val l` и `val a`, ухудшающие читаемость и создающие путаницу:
1. `DashboardsPaginatedListScreen`: `val l` переименована в `videoItems`.
2. `ComposeTags`: цвета каналов, порнозвёзд, подложки и рамки вынесены в именованные константы (`TAG_CHANNEL_COLOR`, `TAG_PORNSTAR_COLOR`, `TAG_BG_COLOR`, `TAG_BORDER_COLOR`).
3. `ProfileInfo1`: удалён пустой блок `if (a == null) {}`, переменная `a` переименована в `matchedUser`, добавлена проверка `avatarUrl != null`.
4. `R_Saved_Collection`: `val a` переименована в `collectionsResult`.
5. `ItemCollectionPagingSource`: `val a` переименована в `items`.
6. `ScreenAlbumListSM`: `val a` в `loadInitialData` и `loadAlbumList` переименована в `albumListResult`.
7. `AlbumListFilterContentType`: `val a` переименована в `selectedContent`.
8. `KeyboardNumber`: `val a` $\rightarrow$ `enteredNumber`, `val i` $\rightarrow$ `clampedValue`.
9. `TabRow`: в превью `val l` $\rightarrow$ `previewIcons`.
10. `Config_G_0_4`: `val a` $\rightarrow$ `updatedList`.

---

### T51 — Добавление юнит-тестов коллекций L и покрытия null-байтов в CollectionName.

1. Создан тестовый класс `SavedL_CollectionTest` ([SavedL_CollectionTest.kt](../feature-l/src/test/java/com/client/xvideos/l/featured/saved/SavedL_CollectionTest.kt)):
   - Проверка сортировки `LCollectionSortOrder.RECENT` по убыванию даты изменения.
   - Проверка сортировки `LCollectionSortOrder.NAME` по алфавиту без учета регистра.
   - Проверка сортировки `LCollectionSortOrder.SIZE` по количеству элементов с разрешением конфликтов по имени.
   - Проверка контракта группировки дубликатов `LCollectionDuplicateGroup`.
   - Проверка сериализации/десериализации конфига коллекции `LCollectionConfig`.
2. Дополнен `CollectionNameTest` ([CollectionNameTest.kt](../core/src/test/java/com/client/xvideos/common/collectionDB/CollectionNameTest.kt)):
   - Тест отклонения null-байтов `\u0000` в `CollectionName.normalizeOrNull` и `CollectionName.isValid`.
