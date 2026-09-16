# Код-ревью xvideos — проход 63

> **Срез:** `0dc2864` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `0dc2864`. Изменено 7 исходных файлов в модуле `feature-r`, добавлен новый файл unit-тестов `SearchHistoryStackTest.kt`.

Линзы:
- `C` / Корректность логики отмены поиска и защита от неограниченного роста стека в [ISearchTemplate.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/search/ISearchTemplate.kt) и [RSearchField.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/search/RSearchField.kt).
- `UI` / Фиксация стабильности Compose-контрактов (`@Stable`) для ScreenModel-моделей экранов в [ScreenRedTopThisWeekSM.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/top_this_week/ScreenRedTopThisWeekSM.kt), [ScreenNicheSM.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/niche/ScreenNicheSM.kt), [ScreenRedExplorerNichesSM.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/niches/ScreenRedExplorerNichesSM.kt), [SearchTab.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/search/SearchTab.kt) и [R_Screen_CollectionTab.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_CollectionTab.kt).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### C52 — Баг логики отмены запроса, отсутствие лимита стека и перезаписываемые StateFlow в ISearchTemplate. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/common/search/ISearchTemplate.kt:37-65](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/search/ISearchTemplate.kt#L37)
[feature-r/src/main/java/com/client/xvideos/r/ui/search/RSearchField.kt:47-90](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/search/RSearchField.kt#L47)

1. В `ISearchTemplate` поле `stack` представляло собой несинхронизированный `ArrayDeque<String>` без ограничения размера. При активном использовании поиска коллекция могла расти бесконечно.
2. В `RSearchField.kt` обработчик `onUndoClick` делал `search.stack.removeLast()` и устанавливал полученный элемент в `searchText.value`. Так как в стек предварительно помещался текущий активный запрос, первое нажатие «Отмена» просто возвращало тот же самый запрос (холостой клик), либо при одном введённом слове отмена не возвращала пустое поле.
3. Поля `searchText`, `searchTextDone`, `searchTextSuggestions` были объявлены как `var`, что допускало случайную замену ссылки на `MutableStateFlow` с потерей подписчиков.
4. Вызовы Room DAO `search.add` и `search.delete` внутри `RSearchField` запускались на `Dispatchers.Main`.

**Исправление:**
- Поля StateFlow в `ISearchTemplate` переведены в `val`.
- Реализованы потокобезопасные методы управления историей `pushHistory(query: String)` и `popHistory(currentQuery: String): String?`. Размер стека ограничен константой `MAX_STACK_SIZE = 50`, последовательные дубликаты и пустые строки отсекаются. Метод `popHistory` корректно снимает текущий активный запрос и возвращает предшествующее состояние (или `""`, если стек опустел).
- Вызовы `search.add` и `search.delete` в `RSearchField.kt` переведены на `Dispatchers.IO`.
- Создан тестовый класс [SearchHistoryStackTest.kt](file:///g:/xvideos2/feature-r/src/test/java/com/client/xvideos/r/common/search/SearchHistoryStackTest.kt), полностью покрывающий сценарии работы стека.

---

### UI77 — Отсутствие @Stable на классах ScreenModel в feature-r. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/ui/top_this_week/ScreenRedTopThisWeekSM.kt:28](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/top_this_week/ScreenRedTopThisWeekSM.kt#L28)
[feature-r/src/main/java/com/client/xvideos/r/ui/niche/ScreenNicheSM.kt:33](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/niche/ScreenNicheSM.kt#L33)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/niches/ScreenRedExplorerNichesSM.kt:24](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/niches/ScreenRedExplorerNichesSM.kt#L24)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/search/SearchTab.kt:164](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/search/SearchTab.kt#L164)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_CollectionTab.kt:263](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_CollectionTab.kt#L263)

Ряд ключевых моделей экранов в модуле `feature-r` не имели явной аннотации `@Stable`, что препятствовало Compose-компилятору применять скиппинг рекомпозиции (skippable recomposition).

**Исправление:**
- Добавлена аннотация `@Stable` к `ScreenRedTopThisWeekSM`, `ScreenNicheSM`, `ScreenRedExplorerNichesSM`, `ScreenRedExplorerSearchSM` и `ScreenSavedCollectionSM`.

---

## Итог верификации

- **Unit-тесты:** `./gradlew :feature-r:testDebugUnitTest` — успешно (все 83 теста пройдены).
- **Detekt:** `./gradlew :feature-r:detekt` — 0 ошибок и предупреждений.
- **Архитектурные ограничения:** Замороженный P2P (`core/.../p2p/`) не изменялся.
