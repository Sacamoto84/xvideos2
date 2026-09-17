# Код-ревью xvideos — проход 96

> **Срез:** `3f3ad05` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `3f3ad05`. Затронуты модули `:app`, `:feature-l`, `:feature-x`. Добавлена визуальная in-app кнопка выхода/возврата («Назад») в оверлее основного онлайн-видеоплеера X, обеспечен корректный проброс отступов `Scaffold` в корневом экране `ScreenRoot` и экране альбома `ScreenLAlbum` с полным удалением оставшихся подавлений `@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")`, внедрена каноническая нормализация ссылок пагинации дашбордов через `normalizeXUrl`, снижен уровень избыточного логирования в модулях сети и интерфейса, а также добавлены юнит-тесты на генерацию URL дашбордов и валидацию идентификаторов альбомов.

Линзы:
- `UI` / Добавление in-app кнопки возврата («Назад») в оверлей онлайн-видеоплеера X, корректный проброс отступов `Scaffold` в `ScreenLAlbum` и `ScreenRoot`, удаление устаревших и ложных аннотаций `@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")` ([ScreenX_VideoPlayer.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayer.kt), [ScreenAlbum.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenAlbum.kt), [ScreenSaved.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/ScreenSaved.kt), [ScreenRoot.kt](../app/src/main/java/com/client/xvideos/screenRoot/ScreenRoot.kt)).
- `C` / Каноническая нормализация URL дашбордов X (`normalizeXUrl`) и безопасное ограничение номеров страниц `coerceIn(0, 19999)` ([DashboardsPaginatedListScreen.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/DashboardsPaginatedListScreen.kt)).
- `L` / Перевод отладочных логов сетевых загрузок, кликов превью, жизненного цикла и списков стран в отладочный уровень `Timber.d` ([DashboardsPaginatedListScreen.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/DashboardsPaginatedListScreen.kt), [readHtmlFromURLDirect.kt](../feature-x/src/main/java/com/client/xvideos/x/feature/net/readHtmlFromURLDirect.kt), [readHtmlFromURLWebView.kt](../feature-x/src/main/java/com/client/xvideos/x/feature/net/readHtmlFromURLWebView.kt), [UrlVideoImageAndLongClickX.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/common/UrlVideoImageAndLongClickX.kt), [country.kt](../feature-x/src/main/java/com/client/xvideos/x/feature/country/country.kt), [L_ScreenSavedAlbumsTab.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/albums/L_ScreenSavedAlbumsTab.kt)).
- `Q` / Очистка устаревших записей в Detekt baseline ([config/detekt/baseline.xml](../config/detekt/baseline.xml)).
- `T` / Добавление модульных тестов `DashboardsUrlTest` и `ScreenLSavedAlbumsTest` ([DashboardsUrlTest.kt](../feature-x/src/test/java/com/client/xvideos/x/screens/dashboards/DashboardsUrlTest.kt), [ScreenLSavedAlbumsTest.kt](../feature-l/src/test/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/albums/ScreenLSavedAlbumsTest.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### UI125 — Отсутствие in-app кнопки возврата («Назад») в оверлее основного плеера X. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayer.kt:220](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayer.kt#L220)

В `ScreenX_VideoPlayer` при воспроизведении онлайн-видео поверх плеера отрисовывались только теги (`ComposeTags`) и нижняя панель управления (`X_PlayerBottomBar`). Пользователи с жестовой навигацией или в полноэкранном режиме были вынуждены использовать системный жест назад для выхода, при этом кнопка возврата присутствовала в `ScreenX_LocalVideoPlayer`, создавая несогласованность пользовательского опыта.

**Исправление:**
- В оверлей `ComposeVideoPlayer` добавлена кнопка возврата `IconButton` с иконкой `Icons.AutoMirrored.Filled.ArrowBack` и отступами `Modifier.statusBarsPadding().padding(8.dp)`.
- Кнопка анимированно появляется/скрывается синхронно с элементами управления плеера (`AnimatedVisibility(visible = !vm.isFullScreen || areControlsVisible)`).
- При клике в ландшафтном режиме кнопка возвращает плеер в портретный режим (`vm.exitFullScreen()`), а в портретном — закрывает экран плеера (`navigator.pop()`).
- Упрощён импорт `cafe.adriel.voyager.navigator.Navigator`.

---

### UI126 — Игнорирование отступов Scaffold в ScreenRoot и ScreenLAlbum, ложные подавления UnusedMaterial3ScaffoldPaddingParameter. Средняя.

[app/src/main/java/com/client/xvideos/screenRoot/ScreenRoot.kt:77, 108](../app/src/main/java/com/client/xvideos/screenRoot/ScreenRoot.kt#L77)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenAlbum.kt:82, 170, 341](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenAlbum.kt#L82)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/ScreenSaved.kt:57](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/ScreenSaved.kt#L57)

В корневом экране приложения `ScreenRoot` параметр `paddingValues` из `Scaffold` игнорировался, что маскировалось аннотацией `@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")`. В `ScreenLAlbum.Content()` нижний отступ от `Scaffold` (где отображается прогресс загрузки альбома) не передавался в `PullToRefreshBox`, из-за чего нижние элементы списка могли перекрываться индикатором. В `ScreenLAlbumPreviewBody` и `ScreenSaved` аннотации `@SuppressLint` являлись ложными подавлениями, так как код уже использовал отступы (`calculateBottomPadding()`).

**Исправление:**
- В `ScreenRoot` всё содержимое `Scaffold` обёрнуто в `Box(modifier = Modifier.fillMaxSize().padding(paddingValues))`.
- В `ScreenLAlbum.Content()` к `PullToRefreshBox` добавлен отступ `Modifier.padding(bottom = padding.calculateBottomPadding())`.
- Во всех проверенных файлах удалены аннотации `@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")` и неиспользуемые импорты `android.annotation.SuppressLint`.
- Очищен `config/detekt/baseline.xml` от устаревших записей с `@SuppressLint`.

---

### C17 — Отсутствие нормализации URL страниц в дашбордах X. Низкая.

[feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/DashboardsPaginatedListScreen.kt:62](../feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/DashboardsPaginatedListScreen.kt#L62)

В функции загрузки страниц `openNew` формировалась сырая строка `urlStart + if (...) ...`, без прогона через `normalizeXUrl`, несмотря на наличие импорта функции нормализации. Это могло приводить к расхождению с контрактом нормализации ссылок домена X.

**Исправление:**
- Логика построения URL вынесена в чистую функцию `internal fun buildDashboardUrl(numberScreen: Int): String`.
- URL страниц формируется с канонической нормализацией `normalizeXUrl(raw)` и безопасным ограничением диапазона `coerceIn(0, 19999)`.

---

### L30 — Засорение информационного уровня логов сетевыми событиями и дампами списков. Низкая.

[feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/DashboardsPaginatedListScreen.kt:65](../feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/DashboardsPaginatedListScreen.kt#L65)
[feature-x/src/main/java/com/client/xvideos/x/feature/net/readHtmlFromURLDirect.kt:52](../feature-x/src/main/java/com/client/xvideos/x/feature/net/readHtmlFromURLDirect.kt#L52)
[feature-x/src/main/java/com/client/xvideos/x/feature/net/readHtmlFromURLWebView.kt:51, 137](../feature-x/src/main/java/com/client/xvideos/x/feature/net/readHtmlFromURLWebView.kt#L51)
[feature-x/src/main/java/com/client/xvideos/x/screens/common/UrlVideoImageAndLongClickX.kt:62](../feature-x/src/main/java/com/client/xvideos/x/screens/common/UrlVideoImageAndLongClickX.kt#L62)
[feature-x/src/main/java/com/client/xvideos/x/feature/country/country.kt:225](../feature-x/src/main/java/com/client/xvideos/x/feature/country/country.kt#L225)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/albums/L_ScreenSavedAlbumsTab.kt:127, 133](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/albums/L_ScreenSavedAlbumsTab.kt#L127)

При парсинге стран метод `countryList.forEach` логировал каждую страну в `Timber.i`, перегружая logcat десятками записей при старте. В `readHtmlFromURLDirect`, `readHtmlFromURLWebView`, `UrlVideoImageAndLongClickX` и `ScreenLSavedAlbumsSM` рутинные события сети и инициализации также отправлялись в `Timber.i`.

**Исправление:**
- Все отладочные сообщения переведены в уровень `Timber.d`.

---

### T39 — Модульное тестирование URL дашбордов и валидации идентификаторов альбомов. Средняя.

[feature-x/src/test/java/com/client/xvideos/x/screens/dashboards/DashboardsUrlTest.kt](../feature-x/src/test/java/com/client/xvideos/x/screens/dashboards/DashboardsUrlTest.kt)
[feature-l/src/test/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/albums/ScreenLSavedAlbumsTest.kt](../feature-l/src/test/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/albums/ScreenLSavedAlbumsTest.kt)

Построение URL дашбордов с учётом граничных условий страниц и фильтрация сохранённых альбомов с валидными числовыми ID не имели модульных тестов.

**Исправление:**
- Создан тестовый класс `DashboardsUrlTest` (4 теста), проверяющий формирование адреса корневой страницы (индекс 0), сегментов пагинации `/new/{page}`, клампинг отрицательных индексов и верхнего порога (19999).
- Создан тестовый класс `ScreenLSavedAlbumsTest` (2 теста), проверяющий фильтрацию альбомов с валидными числовыми идентификаторами и отсечение пустых, нечисловых и пробельных ID.

---

## Сводка верификации

- `detekt`: 0 предупреждений и ошибок во всех модулях проекта (`:app`, `:core`, `:feature-l`, `:feature-r`, `:feature-x`).
- `testDebugUnitTest`: 140 задач выполнено, 100% юнит-тестов зелёные, включая новые наборы `DashboardsUrlTest` и `ScreenLSavedAlbumsTest`.
- `compileReleaseKotlin`: 61 задача выполнена, сборка релизных Kotlin-артефактов успешна.
