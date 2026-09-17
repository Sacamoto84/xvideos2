# Код-ревью xvideos — проход 92

> **Срез:** `94e4c62` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `94e4c62`. Затронуты модули `:app`, `:core`, `:feature-l`. Выполнена гигиена логирования исключений в GraphQL Luscious, устранён избыточный `Scaffold` и вынесены повторные вычисления размеров экрана из списков в `ScreenLAlbumLandingTag`, очищены однобуквенные переменные в настройках и диалогах фильтрации, а также добавлено 100% покрытие модульными тестами парсера URL фильтров топа альбомов.

Линзы:
- `L` / Логирование исключений GraphQL Luscious с полной трассировкой стека ошибки через `Timber.e(e, ...)` вместо информационного `Timber.i` ([LandingPageAlbumTag.kt](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/LandingPageAlbumTag.kt), [LandingPageAlbumSearch.kt](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/LandingPageAlbumSearch.kt)).
- `UI` / Устранение избыточного `Scaffold` и подавления `@SuppressLint`, добавление ключей элементов списка и вынос повторного чтения конфигурации экрана из `FlowRow` ([ScreenLAlbumLandingTag.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/albumLandingTag/ScreenLAlbumLandingTag.kt)).
- `S` / Чистка однобуквенных переменных `val d` (тема диалога), `val s` (аннотированный текст), `val q` (запрос/bootstrap), `val f` (фильтр) в коде настроек и фильтров ([BackupComponents.kt](../app/src/main/java/com/client/xvideos/screenSettings/backup/BackupComponents.kt), [AppLockSection.kt](../app/src/main/java/com/client/xvideos/screenSettings/components/AppLockSection.kt), [LavenderDialog.kt](../core/src/main/java/com/client/xvideos/common/theme/LavenderDialog.kt), [AlbumListFilterTags.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumListFilterTags.kt), [AlbumListFilterGenres.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumListFilterGenres.kt), [AlbumFilterTagsDialog.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterTagsDialog.kt), [AlbumFilterGenresDialog.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterGenresDialog.kt), [MediaCategoriesBootstrap.kt](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/MediaCategoriesBootstrap.kt), [AlbumTopHits.kt](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumTopHits.kt), [AlbumList.kt](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumList.kt), [L_ScreenAlbumTopHits.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumTopHits/L_ScreenAlbumTopHits.kt), [ScreenLAlbumLandingTag.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/albumLandingTag/ScreenLAlbumLandingTag.kt)).
- `T` / Добавление модульного теста `L_ScreenAlbumTopHitsTest` для проверки функции десериализации параметров фильтрации из URL топа альбомов ([L_ScreenAlbumTopHitsTest.kt](../feature-l/src/test/java/com/client/xvideos/l/ui/screens/explorer/tab/albumTopHits/L_ScreenAlbumTopHitsTest.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### L11 — Потеря контекста ошибок при логировании исключений GraphQL Luscious. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/net/graphQl/LandingPageAlbumTag.kt:29](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/LandingPageAlbumTag.kt#L29)
[feature-l/src/main/java/com/client/xvideos/l/net/graphQl/LandingPageAlbumSearch.kt:28](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/LandingPageAlbumSearch.kt#L28)

В обработчиках сетевых запросов GraphQL для тегов и поиска альбомов Luscious блок `catch (e: Exception)` перехватывал ошибку и выводил её на информационном уровне `Timber.i("landingPageAlbumSearch: ${e.message}")`. При этом стек исключения терялся, а фильтрация логов по ошибкам (`Timber.e`) пропускала сбои сети и парсинга.

**Исправление:**
- Заменено логирование на `Timber.e(e, "landingPageAlbumTag error")` и `Timber.e(e, "landingPageAlbumSearch error")`.
- Переименована переменная `val q` в понятное `query`.

---

### UI121 — Избыточный Scaffold, подавление UnusedMaterial3ScaffoldPaddingParameter и вычисления в LazyColumn. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/albumLandingTag/ScreenLAlbumLandingTag.kt:94-135, 201-236](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/albumLandingTag/ScreenLAlbumLandingTag.kt#L94)

Экран лендинга тегов альбомов `ScreenLAlbumLandingTag` и его превью оборачивались в `Scaffold(containerColor = Theme.background)`, не содержащий баров или плавающих кнопок, что вызывало подавление предупреждений `@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")`. Кроме того, внутри элементов `items(...)` для каждой секции заново вызывался `LocalConfiguration.current.screenWidthDp.dp`, а у `items` отсутствовал ключ `key`.

**Исправление:**
- `Scaffold` заменён на легковесный `Box(modifier = Modifier.fillMaxSize().background(Theme.background))`.
- Удалены аннотации `@SuppressLint` и неиспользуемый импорт `Scaffold`.
- Вычисление `val screenWidth = LocalConfiguration.current.screenWidthDp.dp` вынесено наружу из списка элементов.
- В вызовы `items(...)` добавлен стабильный ключ `key`.
- В `ScreenLAlbumLandingTagSM.filter()` убрана промежуточная переменная `val f` с прямым возвратом созданного `AlbumListFilter`.

---

### S44 — Однобуквенные переменные в темах диалогов, настройках и фильтрах. Низкая.

[app/src/main/java/com/client/xvideos/screenSettings/backup/BackupComponents.kt:410, 508](../app/src/main/java/com/client/xvideos/screenSettings/backup/BackupComponents.kt#L410)
[app/src/main/java/com/client/xvideos/screenSettings/components/AppLockSection.kt:448](../app/src/main/java/com/client/xvideos/screenSettings/components/AppLockSection.kt#L448)
[core/src/main/java/com/client/xvideos/common/theme/LavenderDialog.kt:50](../core/src/main/java/com/client/xvideos/common/theme/LavenderDialog.kt#L50)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumListFilterTags.kt:33](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumListFilterTags.kt#L33)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumListFilterGenres.kt:33](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumListFilterGenres.kt#L33)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterTagsDialog.kt:66](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterTagsDialog.kt#L66)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterGenresDialog.kt:66](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterGenresDialog.kt#L66)
[feature-l/src/main/java/com/client/xvideos/l/net/graphQl/MediaCategoriesBootstrap.kt:37](../feature-l/src/main/java/com/client/xvideos/l/net/graphQl/MediaCategoriesBootstrap.kt#L37)
[feature-l/src/main/java/com/client/xvideos/l/net/AlbumTopHits.kt:28](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumTopHits.kt#L28)
[feature-l/src/main/java/com/client/xvideos/l/net/AlbumList.kt:47](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumList.kt#L47)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumTopHits/L_ScreenAlbumTopHits.kt:169](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumTopHits/L_ScreenAlbumTopHits.kt#L169)

Во множестве диалогов и компонентов встречались однобуквенные переменные `val d = Theme.DialogLavande`, `val s = buildAnnotatedString { ... }`, `val q = ...`, `val i = ...`, снижающие читаемость кода.

**Исправление:**
- `d` переименована в `dialogTheme`.
- `s` переименована в `annotatedText`, `annotatedTag`, `annotatedGenre`.
- `q` переименована в `query` (или упразднена в `MediaCategoriesBootstrap`).
- `i` переименована в `separatorIndex`.

---

### T36 — Отсутствие тестов для парсера URL топа альбомов albumListFilterFromTopHitsUrl. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumTopHits/L_ScreenAlbumTopHits.kt:164](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumTopHits/L_ScreenAlbumTopHits.kt#L164)

Функция `albumListFilterFromTopHitsUrl` отвечает за преобразование клика по категории/тегу на экране топа альбомов в полноценный объект `AlbumListFilter` с параметрами URL-декодирования и разбиения тегов по знаку `+`. Приватная видимость мешала тестированию, а логика не была покрыта юнит-тестами.

**Исправление:**
- Видимость `albumListFilterFromTopHitsUrl` изменена с `private` на `internal`.
- Создан набор модульных тестов `L_ScreenAlbumTopHitsTest` (5 тестов), проверяющий:
  1. Дефолтный фильтр при отсутствии query-параметров.
  2. Корректный парсинг всех параметров (`display`, `album_type`, `audience_ids`, декодирование `tagged`).
  3. Откат некорректного `album_type` к дефолту (`Pictures`).
  4. Очистку пустых токенов и лишних плюсов в тегах.
  5. Корректную обработку параметров без знака равенства `=`.

---

## Сводка верификации

- `detekt`: 0 предупреждений и ошибок во всех модулях проекта (`:app`, `:core`, `:feature-l`, `:feature-r`, `:feature-x`).
- `testDebugUnitTest`: 140 задач выполнено, все юнит-тесты успешно пройдены (100%), включая новый набор `L_ScreenAlbumTopHitsTest`.
- `compileReleaseKotlin`: 61 задача выполнена, чистая сборка релизных Kotlin-артефактов без ошибок.
