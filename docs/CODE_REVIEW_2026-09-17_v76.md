# Код-ревью xvideos — проход 97

> **Срез:** `063f8cf` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `063f8cf`. Затронут модуль `:feature-r`. Выполнена очистка сетевого слоя Red: удалена устаревшая аннотация `@SuppressLint("CheckResult")` в конфигурации Ktor/OkHttp `ApiClient`, переменная `USER_AGENT` переведена в `const val` с удалением соответствующего исключения из Detekt baseline, из сигнатур методов `RedApi` (`readCreator`, `searchCreator`, `getNiche`, `getNiches`, `getNichesRelated`, `getNichesTopCreators`, `getNichesTopTags`) удалены хардкодные тестовые значения по умолчанию, рутинные логи авторизации, кэширования и запросов переведены из информационного уровня `Timber.i` в отладочный `Timber.d`, а также добавлен набор модульных тестов `RedApiRouteTest`.

Линзы:
- `S` / Удаление ложных аннотаций `@SuppressLint("CheckResult")` и хардкодных тестовых параметров по умолчанию в методах `RedApi` ([ApiClient.kt](../feature-r/src/main/java/com/client/xvideos/r/network/http/ApiClient.kt), [RedApi.kt](../feature-r/src/main/java/com/client/xvideos/r/network/api/RedApi.kt)).
- `Q` / Перевод `USER_AGENT` в `const val` и очистка устаревшей записи `MayBeConst` из Detekt baseline ([ApiClient.kt](../feature-r/src/main/java/com/client/xvideos/r/network/http/ApiClient.kt), [baseline.xml](../config/detekt/baseline.xml)).
- `L` / Перевод рутинных сетевых логов авторизации, обращений к кэшу и запросов в `Timber.d` без отладочных маркеров `!!!` ([ApiClient.kt](../feature-r/src/main/java/com/client/xvideos/r/network/http/ApiClient.kt), [RedApi.kt](../feature-r/src/main/java/com/client/xvideos/r/network/api/RedApi.kt)).
- `T` / Добавление модульных тестов построения и экранирования маршрутов Red ([RedApiRouteTest.kt](../feature-r/src/test/java/com/client/xvideos/r/network/http/RedApiRouteTest.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### S47 — Ложная аннотация @SuppressLint("CheckResult") и тестовые значения по умолчанию в RedApi. Низкая.

[feature-r/src/main/java/com/client/xvideos/r/network/http/ApiClient.kt:35](../feature-r/src/main/java/com/client/xvideos/r/network/http/ApiClient.kt#L35)
[feature-r/src/main/java/com/client/xvideos/r/network/api/RedApi.kt:149, 174, 235, 241, 261, 268, 281](../feature-r/src/main/java/com/client/xvideos/r/network/api/RedApi.kt#L149)

В `ApiClient.kt` свойство `client` было помечено `@SuppressLint("CheckResult")`, хотя вызовы в блоке конфигурации OkHttp не требовали подавления. В методах репозитория `RedApi` (`readCreator`, `searchCreator`, `getNiche`, `getNiches`, `getNichesRelated`, `getNichesTopCreators`, `getNichesTopTags`) в качестве значений по умолчанию использовались тестовые хардкодные строки (например, `"lilijunex"`, `"pumped-pussy"`), что нарушало типобезопасность контракта при случайном пропуске аргументов вызывающей стороной.

**Исправление:**
- Удалена аннотация `@SuppressLint("CheckResult")` и неиспользуемый импорт `android.annotation.SuppressLint` в `ApiClient.kt`.
- Удалены тестовые строки по умолчанию в методах `RedApi`, сделав аргументы идентификаторов авторов и ниш строго обязательными.

---

### Q13 — Неконстантное поле USER_AGENT и подавление в Detekt baseline. Низкая.

[feature-r/src/main/java/com/client/xvideos/r/network/http/ApiClient.kt:32](../feature-r/src/main/java/com/client/xvideos/r/network/http/ApiClient.kt#L32)
[config/detekt/baseline.xml](../config/detekt/baseline.xml)

Поле `USER_AGENT` в `ApiClient` являлось обычным `val`, из-за чего в `baseline.xml` годами сохранялось подавление правила `MayBeConst`.

**Исправление:**
- Поле объявлено как `const val USER_AGENT`.
- Из `config/detekt/baseline.xml` удалена запись `MayBeConst:ApiClient.kt$ApiClient$val USER_AGENT`.

---

### L31 — Засорение информационного уровня логов сетевой авторизацией и обращениями к кэшу. Низкая.

[feature-r/src/main/java/com/client/xvideos/r/network/http/ApiClient.kt:115, 119, 150, 160, 164](../feature-r/src/main/java/com/client/xvideos/r/network/http/ApiClient.kt#L115)
[feature-r/src/main/java/com/client/xvideos/r/network/api/RedApi.kt:139, 346, 350, 357](../feature-r/src/main/java/com/client/xvideos/r/network/api/RedApi.kt#L139)

В `ApiClient` и `RedApi` штатные события логина, получения токена, запросов и чтения из локального кэша логировались с префиксом `!!!` на уровне `Timber.i`, а предупреждения и нефатальные ошибки кэша — на уровне `Timber.e`.

**Исправление:**
- Штатные сетевые события и обращения к кэшу переведены на `Timber.d`.
- Удалены отладочные префиксы `!!!`.
- Повреждения кэша и сетевые ретраи логируются через `Timber.w`.

---

### T40 — Модульное тестирование маршрутизации и эскейпинга RedApiRouteTest. Средняя.

[feature-r/src/test/java/com/client/xvideos/r/network/http/RedApiRouteTest.kt](../feature-r/src/test/java/com/client/xvideos/r/network/http/RedApiRouteTest.kt)

Маршрутизация `Route` для запросов лент, тегов и авторов не имела специализированного модульного тестового покрытия граничных случаев.

**Исправление:**
- Добавлен тестовый класс `RedApiRouteTest` (4 теста), покрывающий построение параметров поиска гифок, списки тегов через запятую, экранирование спецсимволов в логинах авторов и безопасную обработку пустых параметров.

---

## Сводка верификации

- `detekt`: 0 предупреждений и ошибок в `:feature-r`.
- `testDebugUnitTest`: 100% юнит-тестов `:feature-r` зелёные, включая `RedApiRouteTest`.
