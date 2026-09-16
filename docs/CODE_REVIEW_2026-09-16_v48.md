# Код-ревью xvideos — проход 69

> **Срез:** `a05becc` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `a05becc`. Изменено 10 исходных файлов в модулях `core` и `app`.

Линзы:
- `C` / Надёжность и защита от гонок файловой системы в [CollectionDB.kt](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt) (идемпотентность создания каталога при конкуренции `!created && !dir.exists()`, изоляция проверки существования под блокировкой `lock` при `deleteCollection`), устранение `var`-мутабельности списков в [LinkCollectionStore.kt](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/collectionDB/model/LinkCollectionStore.kt) и модульные тесты в [CollectionDBTest.kt](file:///g:/xvideos2/core/src/test/java/com/client/xvideos/common/collectionDB/CollectionDBTest.kt).
- `UI` / Фиксация стабильности Compose-контрактов (`@Immutable`) для моделей коллекций, снэкбаров, бэкапа и настроек в [CollectionGridItem.kt](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/collectionDB/model/CollectionGridItem.kt), [CollectionsGridStyle.kt](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/collectionDB/model/CollectionsGridStyle.kt), [UiMessage.kt](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/snackbar/UiMessage.kt), [XlrBackupModels.kt](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/backup/XlrBackupModels.kt), [SettingsDataHolders.kt](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/screenSettings/SettingsDataHolders.kt), [StorageStatistics.kt](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/screenSettings/components/StorageStatistics.kt) и [AppSettingsScreen.kt](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### C55 — Ложный сбой mkdirs в CollectionDB.create и гонка проверки dir.exists в deleteCollection. Средняя.

[core/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt:58-85](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt#L58)

Метод `CollectionDB.create(collectionName)` проверял `if (!created) return Result.failure(...)`. В JVM метод `File.mkdirs()` возвращает `false`, если директория уже была создана параллельным потоком или процессом. В отличие от метода `insert()`, где корректно использовалась проверка `!created && !dir.exists()`, в `create()` это приводило к ложному `IOException`.
Кроме того, в `deleteCollection` проверка существования `if (!dir.exists())` выполнялась до входа в блок `synchronized(lock)`, что допускало состояние гонки с параллельным переименованием или созданием.

**Исправление:**
- В `CollectionDB.create` проверка обновлена до `if (!created && !dir.exists())`.
- В `CollectionDB.deleteCollection` проверка существования `dir.exists()` и рекурсивное удаление объединены внутри единого блока `synchronized(lock)`.
- В `CollectionDBTest` добавлены тесты на идемпотентность создания каталога и корректность возврата статуса удаления.

---

### T36 — Нестабильная var-ссылка на SnapshotStateList в LinkCollectionStore. Низкая.

[core/src/main/java/com/client/xvideos/common/collectionDB/model/LinkCollectionStore.kt:29](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/collectionDB/model/LinkCollectionStore.kt#L29)

Поле `collectionList` было объявлено как `var collectionList = mutableStateListOf<CollectionEntity<T>>()`. Переприсваивание инстанса ломает отслеживание снапшотов Compose UI.

**Исправление:**
- Поле переведено в неизменяемое `val collectionList`.

---

### UI83 — Отсутствие @Immutable на моделях Core и App. Средняя.

[core/src/main/java/com/client/xvideos/common/collectionDB/model/CollectionGridItem.kt:10](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/collectionDB/model/CollectionGridItem.kt#L10)
[core/src/main/java/com/client/xvideos/common/collectionDB/model/CollectionsGridStyle.kt:10](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/collectionDB/model/CollectionsGridStyle.kt#L10)
[core/src/main/java/com/client/xvideos/common/snackbar/UiMessage.kt:7](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/snackbar/UiMessage.kt#L7)
[core/src/main/java/com/client/xvideos/common/backup/XlrBackupModels.kt:11-33](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/backup/XlrBackupModels.kt#L11)
[app/src/main/java/com/client/xvideos/screenSettings/SettingsDataHolders.kt:22](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/screenSettings/SettingsDataHolders.kt#L22)
[app/src/main/java/com/client/xvideos/screenSettings/components/StorageStatistics.kt:29-37](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/screenSettings/components/StorageStatistics.kt#L29)
[app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt:335](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt#L335)

Ряд ключевых моделей отображения UI не были помечены аннотацией `@Immutable`:
- `CollectionGridItem` и `CollectionsGridStyle` в компоненте `CollectionsGrid`;
- `UiMessage` и `UiSnackbarVisuals` во всей системе уведомлений;
- `XlrBackupReport`, `XlrBackupItem`, `XlrBackupOptions` в экране бэкапа;
- `SettingsDataHolders`, `StorageStat`, `FolderSnapshot`, `SettingsDetailParams` в экране настроек.
Из-за этого Compose компилятор не мог применять smart skipping при рекомпозиции этих экранов.

**Исправление:**
- Добавлен `@Immutable` ко всем перечисленным классам и интерфейсам.

---

## Итог верификации

- **Unit-тесты:** `./gradlew :core:testDebugUnitTest --tests "com.client.xvideos.common.collectionDB.CollectionDBTest" :app:testDebugUnitTest` — успешно (все 13 тестов CollectionDBTest и все тесты модуля app пройдены).
- **Архитектурные ограничения:** Замороженный P2P (`core/.../p2p/`) не изменялся.
