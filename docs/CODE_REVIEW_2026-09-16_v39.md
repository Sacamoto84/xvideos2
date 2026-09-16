# Код-ревью xvideos — проход 60

> **Срез:** `9497c4a` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `9497c4a`. Изменено 3 исходных файла в модуле `feature-x`, расширены unit-тесты в `feature-x` (`SavedX_FavoritesTest.kt`).

Линзы:
- `IO` / Вынос дисковой сборки P2P-бандла с UI-потока в [ScreenSavedX.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/saved/ScreenSavedX.kt).
- `T` / Устранение гонок и отмена незавершённых фоновых сканирований диска (in-flight job cancellation) в [SavedX_Downloads.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt) и [SavedX_Favorites.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Favorites.kt).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### IO43 — Синхронный экспорт P2P-бандла XExporter на главном потоке. Высокая.

[feature-x/src/main/java/com/client/xvideos/x/screens/saved/ScreenSavedX.kt:162-175](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/saved/ScreenSavedX.kt#L162)

В строке списка сохранённых видео X кнопка шаринга P2P вызывала `XExporter.export(File(AppPath.x_cache_download), item.id)` синхронно в теле обработчика `onClick`. Метод проверяет наличие файлов на диске и читает метаданные, что на главном потоке блокировало UI и создавало риск лагов интерфейса.

**Исправление:**
Вызов `XExporter.export` перенесён в `withContext(Dispatchers.IO)` внутри `coroutineScope.launch`.

---

### T32 — Гонки дискового сканирования и обновления состояний в SavedX. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt:197-205](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt#L197)
[feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Favorites.kt:75-85](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Favorites.kt#L75)

1. В `SavedX_Downloads.refresh()` чтение каталога и декодирование всех `.info`-файлов через `loadFromDisk()` не отменяло предыдущие запущенные задачи. При серии вызовов `refresh()` несколько обходов диска выполнялись параллельно, и устаревший скан мог перезаписать `_list.value` более старыми данными.
2. В `SavedX_Favorites.refresh()` отсутствовало отслеживание `Job`, и очистка `favoriteIds.clear()` / `addAll()` могла пересекаться во времени.

**Исправление:**
- Добавлено `refreshJob?.cancel()` перед запуском новых операций в обоих классах.
- В `SavedX_Favorites` чтение элементов снимка `list.map { it.id }` изолировано на главном потоке.
- В `SavedX_FavoritesTest.kt` добавлен модульный тест сбора набора идентификаторов.

---

## Итог верификации

- **Unit-тесты:** `./gradlew testDebugUnitTest` — успешно (все тесты по всем 5 модулям выполнены без ошибок).
- **Detekt:** `./gradlew detekt --rerun-tasks` — 0 ошибок и предупреждений по всем 5 модулям.
- **Архитектурные ограничения:** Замороженный P2P (`core/.../p2p/`) не изменялся.
