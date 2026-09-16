# Код-ревью xvideos — проход 59

> **Срез:** `6f7d521` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `6f7d521`. Изменено 3 исходных файла в модуле `feature-l`, расширены unit-тесты в `feature-l` (`LPureFunctionsTest.kt`).

Линзы:
- `IO` / Вынос дисковых операций и экспорта бандла с Main-потока в [ExpandMenuVM.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/element/expandMenu/ExpandMenuVM.kt).
- `T` / Устранение гонок и отмена незавершённых фоновых задач (in-flight job cancellation) в [SavedL_Albums.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Albums.kt).
- `S` / `UI` / Безопасный доступ к элементам страниц в пейджерах полноэкранного просмотра [L_FullScreenImage.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/L_FullScreenImage.kt).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### IO42 — Блокирующий дисковый поиск папки и сборка P2P-бандла на главном потоке в ExpandMenuViewModel. Высокая.

[feature-l/src/main/java/com/client/xvideos/l/ui/element/expandMenu/ExpandMenuVM.kt:190-205](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/element/expandMenu/ExpandMenuVM.kt#L190)

В методе `startP2p(item: PicsDetails)` вызовы `lFindLikeFolder(File(AppPath.l_likes), it)` и `LExporter.export(it)` происходили синхронно на вызывающем потоке. `lFindLikeFolder` сканирует каталог `l_likes` с чтением файлов метаданных каждой папки. Поскольку вызов инициировался по клику в UI диалога (`onP2p = { startP2p(item) }`), весь файловый ввод-вывод выполнялся на UI/Main-потоке, провоцируя дропы кадров и риск ANR.

**Исправление:**
Операции поиска папки лайка и сборки бандла перенесены в фоновую корутину `scope.launch(Dispatchers.IO)` с публикацией `p2pSource` через `withContext(Dispatchers.Main)`.

---

### T31 — Гонки повторного обновления альбомов в SavedL_Albums. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Albums.kt:90-95](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Albums.kt#L90)

В `SavedL_Albums.refresh()` отсутствовало отслеживание активного `Job`. При повторных вызовах `refresh()` запускалось несколько параллельных обходов каталога альбомов, результаты которых могли накладываться друг на друга.

**Исправление:**
Добавлено поле `refreshJob: Job?` с вызовом `cancel()` перед запуском нового обновления.

---

### S39 / UI74 — Риск IndexOutOfBoundsException при перелистывании в пейджерах L_FullScreenImage. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/L_FullScreenImage.kt:222-225, 246-248](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/L_FullScreenImage.kt#L222)

В `VerticalPager` и `HorizontalPager` доступ к текущему элементу страницы производился по прямому индексу `filteredPic[page]`. В случаях динамического обновления размера списка при восстановлении состояния или асинхронном догрузе страниц это приводило к сбою с `IndexOutOfBoundsException`.

**Исправление:**
Использован безопасный доступ с дефолтным fallback `filteredPic.getOrNull(page) ?: item`. Добавлен unit-тест в `LPureFunctionsTest.kt`.

---

## Итог верификации

- **Unit-тесты:** `./gradlew testDebugUnitTest` — успешно (все тесты по всем 5 модулям выполнены без ошибок).
- **Detekt:** `./gradlew detekt --rerun-tasks` — 0 ошибок и предупреждений по всем 5 модулям.
- **Архитектурные ограничения:** Замороженный P2P (`core/.../p2p/`) не изменялся.
