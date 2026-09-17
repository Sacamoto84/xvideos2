# Код-ревью: Итерация 119 (2026-09-17)

**Фокус ревью:** `:feature-x`, `:feature-r`, `:feature-l`, `:app` (каноническая нормализация URL в плеере и парсере X, защита пагинации профилей R от холостых запросов 404, очистка мёртвого кода и структуризация GraphQL в L, исправление тега логов подписок, миграция Surface на Material 3, модульные тесты).

---

## 1. Контекст и цели
1. Гарантировать приведение протокольно-относительных (`//cdn...`) и относительных URL видеопотоков X к валидным HTTPS-адресам в [parseHTML5Player.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/parcer/parseHTML5Player.kt), [ScreenX_VideoPlayerSM.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt) и [ScreenX_VideoPlayerFullScreenSM.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt).
2. Защитить источник пагинации профилей [ItemProfilePagingSource.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/pagin/ItemProfilePagingSource.kt) от холостых сетевых вызовов к невалидным URL (`/v2/users//search`), приводящих к ошибкам HTTP 404, и удалить закомментированный код.
3. Устранить copy-paste опечатку в теге логирования источника подписок [ItemSubscriptionsPagingSource.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/pagin/ItemSubscriptionsPagingSource.kt).
4. Перевести формирование тела GraphQL-запроса [getAlbumInfo.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/net/graphQl/getAlbumInfo.kt) на типобезопасный `buildJsonObject` из `kotlinx.serialization` и удалить ~130 строк закомментированного мёртвого кода устаревших запросов.
5. Заменить устаревший импорт Material 2 `androidx.compose.material.Surface` на Material 3 `androidx.compose.material3.Surface` в корневой активности [MainActivity.kt](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/MainActivity.kt).
6. Разработать модульные тесты: [ItemProfilePagingSourceTest.kt](file:///g:/xvideos2/feature-r/src/test/java/com/client/xvideos/r/common/pagin/ItemProfilePagingSourceTest.kt), [GetAlbumInfoGraphQLTest.kt](file:///g:/xvideos2/feature-l/src/test/java/com/client/xvideos/l/net/graphQl/GetAlbumInfoGraphQLTest.kt) и дополнить [XParsersTest.kt](file:///g:/xvideos2/feature-x/src/test/java/com/client/xvideos/x/parcer/XParsersTest.kt).

---

## 2. Выявленные замечания и исправления

### [C31] Поддержка протокольно-относительных ссылок в плеере X
- **Проблема:** В `parseHTML5Player.kt` функция `unescapeUrl()` раскодировала только экранирование слешей (`\/` в `/`). Если сайт отдавал протокольно-относительный URL потока (`//cdn...`), ExoPlayer / `MediaPlayerHost` не мог его воспроизвести из-за отсутствия схемы протокола (`Uri.parse("//...")`). Кроме того, `ScreenX_VideoPlayerSM` и `ScreenX_VideoPlayerFullScreenSM` пробрасывали сырой `hls` без канонической нормализации.
- **Решение:**
  - В [parseHTML5Player.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/parcer/parseHTML5Player.kt) функция `unescapeUrl()` нормализует результат через `normalizeXUrl(unescaped)`, автоматически приводя `//cdn...` к `https://cdn...`.
  - В [ScreenX_VideoPlayerSM.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt) и [ScreenX_VideoPlayerFullScreenSM.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt) поток-кандидат пропускается через `normalizeXUrl`.
- **Статус:** Исправлено.

### [C32] Защита от холостых запросов и HTTP 404 в `ItemProfilePagingSource`
- **Проблема:** В [ItemProfilePagingSource.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/pagin/ItemProfilePagingSource.kt) при пустом или состоящем из одних пробелов имени `profileName` отправлялся GET-запрос к RedGifs API по пути `/v2/users//search?...`, возвращавший 404 Not Found и приводящий к падению в `LoadResult.Error`. Также в файле оставался закомментированный блок `Order.FORCE_TEMP`.
- **Решение:**
  - Добавлена санитизация `val cleanProfileName = profileName.trim()`.
  - При `cleanProfileName.isBlank()` немедленно возвращается `LoadResult.Page(emptyList(), null, null)`.
  - Вызов `redApi.searchCreator` переведён на `cleanProfileName`.
  - Удалён мёртвый закомментированный блок.
- **Статус:** Исправлено.

### [L10] Copy-paste тег логов в `ItemSubscriptionsPagingSource`
- **Проблема:** В [ItemSubscriptionsPagingSource.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/pagin/ItemSubscriptionsPagingSource.kt) строка логирования выводила ошибочный тег `ItemLikesPagingSource::load()`.
- **Решение:** Тег исправлен на `ItemSubscriptionsPagingSource::load()`.
- **Статус:** Исправлено.

### [S63] Перевод `getAlbumInfo` на `buildJsonObject` и удаление мёртвого кода GraphQL
- **Проблема:** В [getAlbumInfo.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/net/graphQl/getAlbumInfo.kt) формирование JSON выполнялось через ручную склейку строк с заменой переводов строк и кавычек. Кроме того, в файле находилось ~130 строк закомментированных устаревших функций запросов (`getVideoInfo`, `albumSearchQuery`, `videoSearchQuery`, `landingPageQuery`).
- **Решение:**
  - Формирование JSON переписано с использованием DSL `kotlinx.serialization.json.buildJsonObject` (по аналогии с [getPicturesJson.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/net/graphQl/getPicturesJson.kt)).
  - Все ~130 строк закомментированного мёртвого кода удалены.
- **Статус:** Исправлено.

### [UI11] Миграция импорта `Surface` с Material 2 на Material 3 в `MainActivity`
- **Проблема:** В [MainActivity.kt](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/MainActivity.kt) корневой контейнер экрана использовал компонент `androidx.compose.material.Surface` из устаревшей библиотеки Compose Material 2, в то время как всё приложение использует Material 3.
- **Решение:** Импорт заменён на `androidx.compose.material3.Surface`.
- **Статус:** Исправлено.

### [T65] Модульные тесты для `ItemProfilePagingSource`, `getAlbumInfo` и `parseHTML5Player`
- **Проблема:** Отсутствовало изолированное модульное тестирование раннего возврата `ItemProfilePagingSource`, структуры полезной нагрузки `getAlbumInfo` и разбора протокольно-относительных ссылок в `parseHTML5Player`.
- **Решение:**
  - Создан [ItemProfilePagingSourceTest.kt](file:///g:/xvideos2/feature-r/src/test/java/com/client/xvideos/r/common/pagin/ItemProfilePagingSourceTest.kt): проверка мгновенной отдачи пустой страницы при пустом/пробельном `profileName`, а также вычисление `getRefreshKey`.
  - Создан [GetAlbumInfoGraphQLTest.kt](file:///g:/xvideos2/feature-l/src/test/java/com/client/xvideos/l/net/graphQl/GetAlbumInfoGraphQLTest.kt): проверка синтаксиса JSON, наличия секций query и variables с целевым `albumId`.
  - Добавлен тест в [XParsersTest.kt](file:///g:/xvideos2/feature-x/src/test/java/com/client/xvideos/x/parcer/XParsersTest.kt) на нормализацию `//cdn...` адресов в `parseHTML5Player`.
- **Статус:** Исправлено.

---

## 3. Сводная таблица находок

| ID | Модуль | Тип | Описание | Решение | Статус |
|:---|:---|:---|:---|:---|:---|
| **C31** | `:feature-x` | Баг / Сеть | Ошибка воспроизведения протокольно-относительных URL (`//cdn...`) в плеере X | `normalizeXUrl` в `unescapeUrl()` и ScreenModel плеера | **Закрыт** |
| **C32** | `:feature-r` | Корректность | Холостые запросы 404 при пустом `profileName` в `ItemProfilePagingSource` | Санитизация и ранний возврат пустой `Page` | **Закрыт** |
| **L10** | `:feature-r` | Логирование | Опечатка в имени тега логов `ItemSubscriptionsPagingSource` | Исправлен тег логирования | **Закрыт** |
| **S63** | `:feature-l` | Архитектура / Гигиена | Ручная сборка JSON и 130 строк закомментированного кода в `getAlbumInfo` | `buildJsonObject` + удаление мёртвого кода | **Закрыт** |
| **UI11** | `:app` | Compose / UI | Импорт Material 2 `Surface` вместо Material 3 в `MainActivity` | Замена импорта на `androidx.compose.material3.Surface` | **Закрыт** |
| **T65** | `:feature-r`, `:feature-l`, `:feature-x` | Тестирование | Отсутствие тестов для краевых случаев источников и парсера | Добавлены `ItemProfilePagingSourceTest`, `GetAlbumInfoGraphQLTest`, тесты `XParsersTest` | **Закрыт** |
