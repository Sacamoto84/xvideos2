# Код-ревью xvideos — проход 64

> **Срез:** `ecae41e` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `ecae41e`. Изменено 6 исходных файлов в модуле `feature-l`.

Линзы:
- `T` / Устранение гонок и защита фоновых файловых мутаций в коллекциях (in-flight mutation job cancellation) в [SavedL_Collection.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt).
- `UI` / Фиксация стабильности Compose-контрактов (`@Stable`) для ScreenModel-моделей экранов в [ScreenSavedCollectionSM.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/ScreenSavedCollectionSM.kt), [ScreenCollectionName.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/ScreenCollectionName.kt), [L_ScreenSavedAlbumsTab.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/albums/L_ScreenSavedAlbumsTab.kt), [L_ScreenSavedLikesTab.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/likes/L_ScreenSavedLikesTab.kt) и [ScreenSaved.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/ScreenSaved.kt).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### T33 — Гонки параллельных файловых мутаций в SavedL_Collection. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt:290-440](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt#L290)

В `SavedL_Collection` операции `addAll`, `removeAll`, `setManualCover` и `removeDuplicateItems` запускали длительные асинхронные обходы диска (`scope.launch(Dispatchers.IO)`) без отслеживания `Job`. При быстром повторном вызове (например, многократное нажатие на добавление/удаление или чистку дублей) параллельные задачи выполнялись одновременно, что могло приводить к повреждению файловой структуры коллекции и наложению результатов `refreshCollectionList()` / `refresh()`.

**Исправление:**
- Добавлено поле `private var mutationJob: Job? = null`.
- Перед запуском операций мутации выполняется `mutationJob?.cancel()`, исключая наложение незавершённых фоновых записей.

---

### UI78 — Отсутствие @Stable на классах Voyager ScreenModel в feature-l. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/ScreenSavedCollectionSM.kt:17](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/ScreenSavedCollectionSM.kt#L17)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/ScreenCollectionName.kt:294](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/ScreenCollectionName.kt#L294)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/albums/L_ScreenSavedAlbumsTab.kt:117](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/albums/L_ScreenSavedAlbumsTab.kt#L117)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/likes/L_ScreenSavedLikesTab.kt:147](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/likes/L_ScreenSavedLikesTab.kt#L147)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/ScreenSaved.kt:109](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/ScreenSaved.kt#L109)

В экранах сохранённых элементов и коллекций модуля `feature-l` классы `ScreenModel` не имели явной аннотации `@Stable`. Compose-компилятор расценивал их как нестабильные типы, отключая умный пропуск рекомпозиций.

**Исправление:**
- Добавлена аннотация `@Stable` ко всем 5 моделям экранов (`ScreenSavedCollectionSM`, `ScreenLCollectionNameSM`, `ScreenLSavedAlbumsSM`, `ScreenSavedLLikesSM`, `L_SavedTabSM`).

---

## Итог верификации

- **Unit-тесты:** `./gradlew :feature-l:testDebugUnitTest` — успешно.
- **Detekt:** `./gradlew :feature-l:detekt` — 0 ошибок и предупреждений.
- **Архитектурные ограничения:** Замороженный P2P (`core/.../p2p/`) не изменялся.
