# Код-ревью xvideos — проход 111

> **Срез:** `d9ba203` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `d9ba203`. Затронут модуль `:feature-l`: сетевой уровень GraphQL-запросов лендингов альбомов (`com.client.xvideos.l.net.graphQl`). Реализована ранняя валидация входящих параметров в `LandingPageAlbumSearch` и `LandingPageAlbumTag`: при пустых или состоящих из одних пробелов строках функции немедленно возвращают `Result.failure(IllegalArgumentException)` без отправки холостых сетевых запросов. В генераторе запроса `getLandingPageAlbumSearch` введено принудительное ограничение `limit.coerceAtLeast(1)`. Поведение покрыто тестами.

Линзы:
- `L` / Защита от невалидных сетевых запросов и холостой нагрузки ([LandingPageAlbumSearch.kt](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/LandingPageAlbumSearch.kt), [LandingPageAlbumTag.kt](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/LandingPageAlbumTag.kt), [getLandingPageAlbumSearch.kt](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/getLandingPageAlbumSearch.kt)).
- `T` / Тестирование параметров пагинации и переменных GraphQL ([LandingPageGraphQlTest.kt](../feature-l/src/test/java/com/client/xvideos/l/net/graphQl/LandingPageGraphQlTest.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### L38 — Отсутствие валидации поисковых строк и тегов перед отправкой GraphQL-запросов. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/net/graphQl/LandingPageAlbumSearch.kt:16](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/LandingPageAlbumSearch.kt#L16)
[feature-l/src/main/java/com/client/xvideos/l/net/graphQl/LandingPageAlbumTag.kt:15](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/LandingPageAlbumTag.kt#L15)
[feature-l/src/main/java/com/client/xvideos/l/net/graphQl/getLandingPageAlbumSearch.kt:97](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/getLandingPageAlbumSearch.kt#L97)

Функции `LandingPageAlbumSearch` и `LandingPageAlbumTag` формировали и отправляли тяжеловесные сетевые запросы даже при передаче пустых строк `""` или `"   "`, что приводило к бессмысленным обращениям к API и получению сетевых ошибок от сервера Luscious. Кроме того, значение `limit` в `getLandingPageAlbumSearch` не ограничивалось снизу, допуская неположительные значения (0 или отрицательные).

**Исправление:**
- В `LandingPageAlbumSearch` добавлена ранняя проверка `search.trim().isBlank()` с немедленным возвратом ошибки.
- В `LandingPageAlbumTag` добавлена ранняя проверка `tag.trim().isBlank()` с немедленным возвратом ошибки.
- В `getLandingPageAlbumSearch` аргумент `limit` защищён вызовом `limit.coerceAtLeast(1)`.

---

### T54 — Модульное тестирование ограничений лимита и параметров GraphQL LandingPage. Низкая.

[feature-l/src/test/java/com/client/xvideos/l/net/graphQl/LandingPageGraphQlTest.kt:66](../feature-l/src/test/java/com/client/xvideos/l/net/graphQl/LandingPageGraphQlTest.kt#L66)

Ранее проверка граничных значений `limit` в `LandingPageGraphQlTest` отсутствовала.

**Исправление:**
- В `LandingPageGraphQlTest` добавлен модульный тест `landing page search clamps limit to at least one`, проверяющий приведение неположительных значений лимита (0 и отрицательных чисел) к безопасному минимуму `1`.

---

## Сводка верификации

- `detekt`: 0 предупреждений и ошибок по всему проекту (`detekt --offline`).
- `testDebugUnitTest`: 100% тестов всех модулей зелёные (`testDebugUnitTest --offline`).
- `compileReleaseKotlin`: успешная компиляция release-версии всех модулей проекта.
