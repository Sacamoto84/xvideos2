# Код-ревью xvideos — проход 98

> **Срез:** `063f8cf` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `063f8cf`. Затронут модуль `:feature-l`. Выполнено устранение уязвимости к синтаксическим ошибкам и инъекциям JSON в GraphQL-запросах лендингов тегов и поиска Luscious (`getLandingPageAlbumSearch`, `getLandingPageAlbumTag`) путём перехода от небезопасной конкатенации строк к канонической структурированной сборке `buildJsonObject`, удалён закомментированный отладочный код, снижен уровень рутинных логов вызовов и обращений к кэшу с информационного `Timber.i` до отладочного `Timber.d` с удалением отладочных префиксов `!!!`, добавлено предупреждающее логирование при сбоях декодирования `MediaCategoriesBootstrapResponse`, а также создан набор модульных тестов `LandingPageGraphQlTest`.

Линзы:
- `C` / Безопасное формирование JSON-переменных GraphQL через `buildJsonObject` во избежание сбоев парсинга при наличии кавычек, спецсимволов и переводов строк ([getLandingPageAlbumSearch.kt](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/getLandingPageAlbumSearch.kt), [getLandingPageAlbumTag.kt](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/getLandingPageAlbumTag.kt)).
- `L` / Перевод сетевых событий и операций с кэшем на `Timber.d`, удаление отладочных маркеров `!!!`, сохранение контекста ошибок декодирования категорий ([MediaCategoriesBootstrap.kt](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/MediaCategoriesBootstrap.kt), [LandingPageAlbumSearch.kt](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/LandingPageAlbumSearch.kt), [LandingPageAlbumTag.kt](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/LandingPageAlbumTag.kt), [AlbumTopHits.kt](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumTopHits.kt), [Repository.kt](../feature-l/src/main/java/com/client/xvideos/l/repository/Repository.kt)).
- `D` / Очистка закомментированных отладочных строк в `Repository.kt` и ручных вызовов `.replace()` в генераторах запросов ([Repository.kt](../feature-l/src/main/java/com/client/xvideos/l/repository/Repository.kt), [getLandingPageAlbumTag.kt](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/getLandingPageAlbumTag.kt)).
- `T` / Добавление тестов безопасной сборки и экранирования переменных GraphQL `LandingPageGraphQlTest` ([LandingPageGraphQlTest.kt](../feature-l/src/test/java/com/client/xvideos/l/net/graphQl/LandingPageGraphQlTest.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### C18 — Уязвимость к инъекциям JSON и синтаксическим сбоям при интерполяции переменных GraphQL в LandingPage. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/net/graphQl/getLandingPageAlbumSearch.kt:9](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/getLandingPageAlbumSearch.kt#L9)
[feature-l/src/main/java/com/client/xvideos/l/net/graphQl/getLandingPageAlbumTag.kt:9](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/getLandingPageAlbumTag.kt#L9)

В функциях построения GraphQL-запросов `getLandingPageAlbumSearch` и `getLandingPageAlbumTag` переменные запроса подставлялись через строковую интерполяцию `"$search"` и `"$tag"`. При наличии в поисковом запросе кавычек (`"`), обратных слэшей (`\`) или управляющих символов формировался невалидный JSON, приводивший к ошибкам на стороне сервера и невозможности загрузки лендинга.

**Исправление:**
- Формирование JSON-пейлоада переведено на `buildJsonObject` с типобезопасными `put("id", ...)` и `put("limit", ...)`.
- Гарантировано корректное экранирование всех спецсимволов и переводов строк.

---

### L32 — Засорение логов маркерами !!! и избыточный уровень в Repository и GraphQL Luscious. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/net/graphQl/MediaCategoriesBootstrap.kt:20, 29](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/MediaCategoriesBootstrap.kt#L20)
[feature-l/src/main/java/com/client/xvideos/l/net/graphQl/LandingPageAlbumSearch.kt:17](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/LandingPageAlbumSearch.kt#L17)
[feature-l/src/main/java/com/client/xvideos/l/net/graphQl/LandingPageAlbumTag.kt:16](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/LandingPageAlbumTag.kt#L16)
[feature-l/src/main/java/com/client/xvideos/l/net/AlbumTopHits.kt:31, 44](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumTopHits.kt#L31)
[feature-l/src/main/java/com/client/xvideos/l/repository/Repository.kt:103, 127, 150, 168, 183, 196, 234, 297, 311](../feature-l/src/main/java/com/client/xvideos/l/repository/Repository.kt#L103)

Штатные события отправки запросов `openURI`, обновления категорий и инициализации лендингов писались в `Timber.i` с отладочным префиксом `!!!`. Ошибки кэша и защиты от роботов логировались с тем же маркером. При ошибке декодирования `MediaCategoriesBootstrapResponse` исключение бесследно проглатывалось `getOrNull()`.

**Исправление:**
- Все информационные вызовы переведены на `Timber.d`.
- Удалены префиксы `!!!`.
- В `MediaCategoriesBootstrap` добавлен `Timber.w(e, ...)` при сбое десериализации JSON.

---

### D17 — Закомментированный отладочный код и устаревшие комментарии. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/repository/Repository.kt:104](../feature-l/src/main/java/com/client/xvideos/l/repository/Repository.kt#L104)
[feature-l/src/main/java/com/client/xvideos/l/net/graphQl/getLandingPageAlbumTag.kt:12](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/getLandingPageAlbumTag.kt#L12)

В `Repository.kt` сохранялась закомментированная строка вызова `Timber.i("!!! openURI() data:$data type:$type config:$config")`, а в `getLandingPageAlbumTag.kt` — закомментированные вызовы ручного `.replace()`.

**Исправление:**
- Закомментированные неиспользуемые строки удалены.

---

### T41 — Модульное тестирование безопасной сериализации GraphQL-запросов лендингов Luscious. Средняя.

[feature-l/src/test/java/com/client/xvideos/l/net/graphQl/LandingPageGraphQlTest.kt](../feature-l/src/test/java/com/client/xvideos/l/net/graphQl/LandingPageGraphQlTest.kt)

Генерация запросов лендингов поиска и тегов не была покрыта юнит-тестами на устойчивость к специальным символам и корректность структуры JSON.

**Исправление:**
- Создан тестовый класс `LandingPageGraphQlTest` (4 теста), проверяющий корректность полей `operationName`, `query`, `variables.id` и `variables.limit`, а также экранирование двойных кавычек, слэшей и переводов строк.

---

## Сводка верификации

- `detekt`: 0 предупреждений и ошибок в `:feature-l`.
- `testDebugUnitTest`: 100% юнит-тестов `:feature-l` зелёные, включая `LandingPageGraphQlTest`.
