# Код-ревью xvideos — проход 104

> **Срез:** `063f8cf` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `063f8cf`. Затронуты модули `:feature-l`, `:feature-r` и конфигурация статического анализатора Detekt. Полностью устранены категории нарушений `MayBeConst` и `EmptyDefaultConstructor` из кодовой базы и `config/detekt/baseline.xml`: статические строковые литералы фильтров альбомов (`byDate`, `byTopRated`, `byFirstLetter`) в `Display.kt` и запрос `mediaCategoriesBootstrap` в `MediaCategoriesBootstrap.kt` переведены в статус истинных констант времени компиляции (`const val`), а из `ItemEmptyPagingSource` удалены лишние пустые круглые скобки конструктора по умолчанию. Структура и целостность ключей `albumFilterDisplay` покрыты модульными тестами в `AlbumFilterDisplayStructureTest`.

Линзы:
- `Q` / Полная ликвидация категорий `MayBeConst` и `EmptyDefaultConstructor` из Detekt baseline ([Display.kt](../feature-l/src/main/java/com/client/xvideos/l/model/Display.kt), [MediaCategoriesBootstrap.kt](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/MediaCategoriesBootstrap.kt), [ItemEmptyPagingSource.kt](../feature-r/src/main/java/com/client/xvideos/r/common/pagin/ItemEmptyPagingSource.kt), [baseline.xml](../config/detekt/baseline.xml)).
- `T` / Добавление тестов валидации целостности фильтров и уникальности сетевых запросов ([AlbumFilterDisplayStructureTest.kt](../feature-l/src/test/java/com/client/xvideos/l/model/AlbumFilterDisplayStructureTest.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### Q16 — Устаревшие подавления MayBeConst и EmptyDefaultConstructor в Detekt baseline. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/model/Display.kt:8](../feature-l/src/main/java/com/client/xvideos/l/model/Display.kt#L8)
[feature-l/src/main/java/com/client/xvideos/l/net/graphQl/MediaCategoriesBootstrap.kt:14](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/MediaCategoriesBootstrap.kt#L14)
[feature-r/src/main/java/com/client/xvideos/r/common/pagin/ItemEmptyPagingSource.kt:7](../feature-r/src/main/java/com/client/xvideos/r/common/pagin/ItemEmptyPagingSource.kt#L7)
[config/detekt/baseline.xml](../config/detekt/baseline.xml)

В файлах `Display.kt` и `MediaCategoriesBootstrap.kt` строковые константы были объявлены как `val`, из-за чего в `baseline.xml` годами хранились правила `MayBeConst`. Класс `ItemEmptyPagingSource` объявлял явный пустой конструктор `()`, а для уже исправленных классов `ScreenFavorites`, `ScreenRedManageBlock` и `ProfileInfo1` подавления `EmptyDefaultConstructor` и `EmptyIfBlock` оставались в `baseline.xml` балластом.

**Исправление:**
- `byDate`, `byTopRated` и `byFirstLetter` в `Display.kt` объявлены как `const val`.
- `mediaCategoriesBootstrap` в `MediaCategoriesBootstrap.kt` объявлен как `private const val`.
- В `ItemEmptyPagingSource` удалены пустые скобки `()`.
- Из `config/detekt/baseline.xml` удалены все 8 устаревших записей (категории `MayBeConst`, `EmptyDefaultConstructor` и `EmptyIfBlock` полностью исключены из baseline).

---

### T47 — Модульное тестирование уникальности ключей запросов и категорий фильтров альбомов. Средняя.

[feature-l/src/test/java/com/client/xvideos/l/model/AlbumFilterDisplayStructureTest.kt](../feature-l/src/test/java/com/client/xvideos/l/model/AlbumFilterDisplayStructureTest.kt)

Список параметров фильтрации альбомов `albumFilterDisplay` (содержащий десятки диапазонов дат, рейтингов и буквенных фильтров) не проверялся тестами на наличие дубликатов ключей запроса `request`, пропусков полей или расхождений с константами категорий. Опечатка в ключе `request` приводила бы к искажению выборки альбомов на сервере.

**Исправление:**
- Добавлен тестовый класс `AlbumFilterDisplayStructureTest` (5 тестов), проверяющий непустоту всех полей элементов фильтра, 100% уникальность ключей `request`, строгую принадлежность категориям `byDate`, `byTopRated`, `byFirstLetter`, полноту алфавитного диапазона A–Z и присутствие всех временных интервалов рейтинга.

---

## Сводка верификации

- `detekt`: 0 предупреждений и ошибок по всему проекту (`detekt --offline`).
- `testDebugUnitTest`: 100% тестов всех модулей зелёные (`testDebugUnitTest --offline`).
- `compileReleaseKotlin`: успешная компиляция release-версии всех модулей проекта.
