# Код-ревью xvideos — проход 113

> **Срез:** `d9ba203` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `d9ba203`. Затронут модуль `:feature-r`: компоненты поиска и подсказок тегов (`com.client.xvideos.r.common.search`). В цепочке `R_SearchExplorer` добавлена нормализация текста запроса `map { it.text.trim() }` перед `distinctUntilChanged()`, что исключает повторную отправку сетевых запросов подсказок при случайном нажатии пробелов в конце строки. В базовом классе `ISearchTemplate` операции добавления в стек истории (`pushHistory`, `popHistory`) и сохранения в базу данных (`add`, `delete`) нормализованы через `trim()`, исключая сохранение невидимых дубликатов с пробелами. Поведение покрыто тестами.

Линзы:
- `S` / Предотвращение лишних сетевых запросов подсказок тегов ([R_SearchExplorer.kt](../feature-r/src/main/java/com/client/xvideos/r/common/search/R_SearchExplorer.kt)).
- `C` / Нормализация строк и устранение ложных дубликатов в истории поиска ([ISearchTemplate.kt](../feature-r/src/main/java/com/client/xvideos/r/common/search/ISearchTemplate.kt)).
- `T` / Тестирование нормализации строк и дедупликации в стеке поиска ([SearchHistoryStackTest.kt](../feature-r/src/test/java/com/client/xvideos/r/common/search/SearchHistoryStackTest.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### S52 — Избыточные запросы подсказок при вводе завершающих пробелов и пробельные дубликаты стека. Низкая.

[feature-r/src/main/java/com/client/xvideos/r/common/search/R_SearchExplorer.kt:46](../feature-r/src/main/java/com/client/xvideos/r/common/search/R_SearchExplorer.kt#L46)
[feature-r/src/main/java/com/client/xvideos/r/common/search/ISearchTemplate.kt:57](../feature-r/src/main/java/com/client/xvideos/r/common/search/ISearchTemplate.kt#L57)

1. В реактивном пайплайне подсказок `R_SearchExplorer` оператор `distinctUntilChanged()` вызывался до нормализации строки. Ввод `"cat "` после `"cat"` расценивался как изменение данных и запускал повторный опрос сетевого API подсказок.
2. В методе `pushHistory(query)` запросы `"query"` и `"query "` расценивались как разные элементы, из-за чего в стек истории попадали одинаковые запросы с пробелами.
3. Методы `add` и `delete` не обрезали пробелы перед записью в DAO.

**Исправление:**
- В реактивную цепочку `R_SearchExplorer` добавлен `.map { it.text.trim() }` перед `distinctUntilChanged()`.
- В `ISearchTemplate` методы `pushHistory`, `popHistory`, `add`, `delete` нормализуют строку через `trim()`, исключая появление дубликатов в стеке и пустых строк в DAO.

---

### T56 — Модульное тестирование дедупликации с учётом пробелов в SearchHistoryStackTest. Низкая.

[feature-r/src/test/java/com/client/xvideos/r/common/search/SearchHistoryStackTest.kt:32](../feature-r/src/test/java/com/client/xvideos/r/common/search/SearchHistoryStackTest.kt#L32)

Ранее проверка дедупликации в `SearchHistoryStackTest` тестировала только посимвольно одинаковые строки.

**Исправление:**
- В `SearchHistoryStackTest` добавлены проверки на то, что запросы с пробелами (`"apple "`) распознаются как дубликаты уже существующего `"apple"`, а извлечение из стека через `popHistory("second ")` корректно сопоставляется с вершиной стека.

---

## Сводка верификации

- `detekt`: 0 предупреждений и ошибок по всему проекту (`detekt --offline`).
- `testDebugUnitTest`: 100% тестов всех модулей зелёные (`testDebugUnitTest --offline`).
- `compileReleaseKotlin`: успешная компиляция release-версии всех модулей проекта.
