# Категории X — план реализации

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** вкладка «Категории» на главном экране X со списком категорий сайта для выбранной ориентации и экран категории с листанием страниц и четырьмя фильтрами.

**Architecture:** список категорий берётся из меню HTML-страницы, которую сайт отдаёт после
`/switch-sexual-orientation/<siteKey>/straight`. Этот запрос идёт отдельным клиентом с
cookie, которые живут только в этом вызове, и кешируется в памяти по ориентации
(`XRubricCatalog`). Экран категории повторяет экран тегов. Страницы, кеш и сброс при смене
фильтров живут в обычном классе `XRubricFeed`, который тестируется без Android, а
ScreenModel тонкий.

**Tech Stack:** Kotlin, Jetpack Compose, Voyager + Hilt (`ScreenModel`), Ktor с движком
OkHttp, Jsoup, JUnit4, kotlinx-coroutines-test (без `runTest`).

**Spec:** [docs/superpowers/specs/2026-10-24-x-categories-screen-design.md](../specs/2026-10-24-x-categories-screen-design.md)

## Global Constraints

- Пути ниже относительно `feature-x/src/main/java/com/client/xvideos/x/` (код) и
  `feature-x/src/test/java/com/client/xvideos/x/` (тесты), если не сказано иное.
- Название сайта нигде не пишется: ни в коде, ни в логах, ни в тестах, ни в коммитах.
  Перед каждым коммитом добавленные строки (без `package`/`import`) проверяются поиском
  без учёта регистра. Запрещены и имена, которые в нижнем регистре дают название сайта
  (пакет `com.client.xvideos` уже принят и не в счёт).
- В тестовых данных только нейтральные подписи («Категория Б», `Cat_B`), никаких
  настоящих названий категорий.
- Один Composable — один файл, имя файла = имя функции. В `atom/` и `molecule/` у каждого
  файла есть лёгкий `@Preview`. Молекулы и атомы не принимают ScreenModel.
- Сетка — `GridCells.Fixed(adaptiveGridColumns())`. Шапки и ряды панелей —
  `adaptiveContentWidth()` после фона. `GridCells.Fixed(<число>)` и `screenWidthDp` запрещены.
- Позиции прокрутки и страница пейджера живут в ScreenModel как `LazyListPosition` /
  `LazyGridPosition` / `Int`, а не как `Lazy*State` / `PagerState` и не в `remember`.
- `map.getOrPut(...) { mutableIntStateOf(0) }` в composable оборачивается в
  `remember(key) { … }`: иначе `:app:lintRelease` падает с `UnrememberedMutableState`.
- Ключи элементов ленивых списков и пейджера — `Int` или `String` (их сохраняет Bundle).
- В новых тестах `feature-x` нет `runTest` и `Dispatchers.setMain`: синхронный код —
  через `runBlocking`, корутины ScreenModel — через свой `TestCoroutineScheduler`.
- Гейт перед каждым коммитом из PowerShell:
  `cmd /c ".\gradlew.bat testDebugUnitTest detekt :app:lintRelease --continue > build\gate.log 2>&1"; "exit=$LASTEXITCODE"`.
  Ожидается `exit=0`. Если падают только `KDownloaderQueueTest` или
  `ScreenX_ChannelSMPageTest`, это известные нестабильные тесты: перезапустить и указать
  это в отчёте.
- Коммиты — в `master`, без push, только явными путями (`git add -- <пути>`). Перед первым
  коммитом спросить владельца. Чужие незакоммиченные файлы в коммит не берутся.

## Review Focus

1. Ответ страницы на старые фильтры пришёл после смены фильтра. Он не должен попасть ни в
   заголовок, ни в кеш новой выдачи (тест в задаче 4).
2. Slug категории с дефисами и цифрами (`/c/A-b_c-7`): id — число после **последнего**
   дефиса, slug — всё до него (тест в задаче 2).
3. Нет сети при первом открытии вкладки: показывается ошибка, а «Повторить» идёт в сеть
   заново. Ошибка не кешируется (тесты в задачах 2 и 6).
4. Ориентацию переключили, пока грузится прежний список: показывается список последней
   выбранной ориентации, а не того запроса, что ответил позже (тест в задаче 6).
5. Фильтры дали страницу без роликов: она не кешируется, «Повторить» снова идёт в сеть
   (тест в задаче 4).

---

### Task 1: Запрос со своими cookie

**Files:**
- Create: `feature/net/OwnSessionCookieJar.kt`
- Modify: `feature/net/readHtmlFromURLDirect.kt` (сборка `htmlClient`, новая функция рядом с `fetchHtml`)
- Test: `feature/net/OwnSessionCookieJarTest.kt`

**Interfaces:**
- Produces: `suspend fun fetchHtmlInOwnSession(url: String): String`. Ошибки те же, что
  у `fetchHtml`: `IOException`, `HttpStatusException`.

- [ ] **Step 1: Написать падающий тест `OwnSessionCookieJarTest`**

```kotlin
private val site = "https://www.example.test/".toHttpUrl()

@Test fun `cookie из ответа уходят в следующий запрос того же сайта`() {
    val jar = OwnSessionCookieJar()
    jar.saveFromResponse(site, listOf(Cookie.parse(site, "session_token=abc; Path=/")!!))
    assertEquals(listOf("abc"), jar.loadForRequest("https://www.example.test/gay".toHttpUrl()).map { it.value })
}
@Test fun `чужой сайт cookie не получает`() { /* то же сохранение; loadForRequest("https://other.test/") — пусто */ }
@Test fun `у двух экземпляров свои cookie`() { /* сохранили в первый — второй для site пуст */ }
@Test fun `cookie с Max-Age=0 не отдаётся`() { /* "session_token=abc; Max-Age=0; Path=/" — пусто */ }
@Test fun `cookie с тем же именем заменяется`() { /* abc, потом def — ровно один, def */ }
```

- [ ] **Step 2: Запустить и убедиться, что тест падает**

Run: `cmd /c ".\gradlew.bat :feature-x:testDebugUnitTest --tests *OwnSessionCookieJarTest"`
Expected: FAIL — `OwnSessionCookieJar` не найден.

- [ ] **Step 3: Реализовать `internal class OwnSessionCookieJar : okhttp3.CookieJar`**

Хранит cookie в памяти экземпляра. Повтор по `name`+`domain`+`path` заменяет старое
значение. `loadForRequest` отдаёт только `cookie.matches(url)` с `expiresAt` больше
текущего времени. В KDoc написать, зачем он нужен: запрос со своей сессией не трогает
`XWebViewCookieJar`.

- [ ] **Step 4: Вынести сборку клиента и добавить `fetchHtmlInOwnSession`**

В `readHtmlFromURLDirect.kt`:
- `private fun buildXHtmlClient(cookieJar: CookieJar): HttpClient` — нынешняя
  конфигурация `htmlClient` (AppDns, перехватчик техобслуживания, таймауты, редиректы,
  заголовки по умолчанию), но с параметром `cookieJar`;
- `private val htmlClient by lazy { buildXHtmlClient(XWebViewCookieJar) }`;
- `suspend fun fetchHtmlInOwnSession(url: String): String`: клиент
  `buildXHtmlClient(OwnSessionCookieJar())` на один вызов, закрывается через `use`.
  Проверка адреса и статуса — как в `fetchHtml`. В KDoc: вызовов единицы за процесс
  (меню трёх ориентаций), поэтому отдельный клиент на вызов оправдан, а cookie этой сессии
  не попадают ни в WebView, ни в общий клиент.

- [ ] **Step 5: Тесты зелёные**

Run: `cmd /c ".\gradlew.bat :feature-x:testDebugUnitTest --tests *OwnSessionCookieJarTest"`
Expected: PASS (5 тестов).

- [ ] **Step 6: Гейт и коммит**

Гейт из Global Constraints, затем:
`feat(x): запрос страницы X в отдельной сессии сайта`

---

### Task 2: Рубрики — модель, разбор меню, каталог

**Files:**
- Create: `feature/rubric/XRubric.kt`
- Create: `feature/rubric/rubricMenuUrl.kt`
- Create: `feature/rubric/XRubricCatalog.kt`
- Create: `parcer/parserRubricMenu.kt`
- Test: `parcer/ParserRubricMenuTest.kt`, `feature/rubric/XRubricCatalogTest.kt`

**Interfaces:**
- Consumes: `fetchHtmlInOwnSession(url)` (задача 1), `XCategory.siteKey`, `urlStart`.
- Produces:
  - `@Immutable data class XRubric(val id: Int, val slug: String, val title: String)`;
  - `fun parserRubricMenu(html: String): List<XRubric>` — порядок сайта, без сортировки;
  - `fun buildRubricMenuUrl(orientation: XCategory): String`;
  - `@Singleton class XRubricCatalog internal constructor(private val fetch: suspend (String) -> String)`
    с `@Inject constructor() : this(::fetchHtmlInOwnSession)`;
    `suspend fun load(orientation: XCategory): ImmutableList<XRubric>` и
    `fun cached(orientation: XCategory): ImmutableList<XRubric>?`.

- [ ] **Step 1: Написать падающий тест `ParserRubricMenuTest`**

```kotlin
private val menu = """
    <ul class="sub-list" id="main-cats-sub-list">
      <li class="dyn"><a href="/lang/one">Язык</a></li>
      <li class="dyntop-cat top-cat-2"><a href="/c/Cat_B-12">Категория Б</a></li>
      <li class="dyn"><a href="/gay?fmc=1">Ориентация</a></li>
      <li class="dyntop-cat top-cat-1"><a href="/c/A-b_c-7">Категория &amp; А</a></li>
      <li class="dyntop-cat top-cat-3"><a href="/c/Cat_B-12">Дубль</a></li>
      <li class="dyn"><a href="/tags">Все</a></li>
    </ul>
    <a href="/c/Outside-99">Вне меню</a>
""".trimIndent()

@Test fun `из меню берутся только категории, в порядке сайта, без дублей`() {
    assertEquals(
        listOf(XRubric(12, "Cat_B", "Категория Б"), XRubric(7, "A-b_c", "Категория & А")),
        parserRubricMenu(menu),
    )
}
@Test fun `id — число после последнего дефиса`() { /* второй элемент: id 7, slug "A-b_c" */ }
@Test fun `без меню — пустой список`() { assertEquals(emptyList<XRubric>(), parserRubricMenu("<html></html>")) }
```

- [ ] **Step 2: Написать падающий тест `XRubricCatalogTest`** (через `runBlocking`)

```kotlin
private fun menuOf(vararg titles: String) = titles.mapIndexed { i, t ->
    """<li class="dyntop-cat"><a href="/c/R$i-$i">$t</a></li>"""
}.joinToString("", """<ul id="main-cats-sub-list">""", "</ul>")

@Test fun `адрес меню — смена ориентации из свежей сессии`() {
    assertEquals("$urlStart/switch-sexual-orientation/straight/straight", buildRubricMenuUrl(XCategory.STRAIGHT))
    assertEquals("$urlStart/switch-sexual-orientation/gay/straight", buildRubricMenuUrl(XCategory.GAY))
    assertEquals("$urlStart/switch-sexual-orientation/shemale/straight", buildRubricMenuUrl(XCategory.TRANS))
}
@Test fun `список — по алфавиту`() {
    // Только кириллица: порядок латиницы относительно кириллицы на JVM и Android разный.
    val catalog = XRubricCatalog { menuOf("яблоко", "Арбуз", "банан") }
    assertEquals(listOf("Арбуз", "банан", "яблоко"), runBlocking { catalog.load(XCategory.STRAIGHT) }.map { it.title })
}
@Test fun `повторная загрузка той же ориентации не идёт в сеть`() { /* счётчик вызовов fetch == 1; cached(STRAIGHT) не null */ }
@Test fun `у каждой ориентации свой запрос`() { /* load GAY и TRANS — fetch получил оба адреса buildRubricMenuUrl */ }
@Test fun `ошибка не кешируется`() { /* первый fetch бросает IOException — load бросает; второй отдаёт меню — load возвращает список; cached до успеха == null */ }
@Test fun `пустое меню — IOException`() { /* fetch отдаёт "<html></html>" — assertThrows<IOException>; cached == null */ }
```

- [ ] **Step 3: Запустить и убедиться, что тесты падают**

Run: `cmd /c ".\gradlew.bat :feature-x:testDebugUnitTest --tests *ParserRubricMenuTest --tests *XRubricCatalogTest"`
Expected: FAIL — символы не найдены.

- [ ] **Step 4: Реализовать модель, разбор, адрес и каталог**

- `parserRubricMenu`: Jsoup, `#main-cats-sub-list a[href]`, href по регулярке
  `^/c/([^/]+)-(\d+)$`, подпись — `text().trim()`, пустые подписи пропускаются,
  `distinctBy { it.id }`.
- `buildRubricMenuUrl`: `"$urlStart/switch-sexual-orientation/${orientation.siteKey}/straight"`.
- `XRubricCatalog.load`: сначала кеш (`ConcurrentHashMap<XCategory, ImmutableList<XRubric>>`),
  потом `fetch` → разбор на `Dispatchers.Default`. Пустой результат —
  `IOException("Меню категорий не разобрано")`. Сортировка —
  `Collator.getInstance(Locale("ru"))` по `title`. В кеш попадает только успешный непустой
  список. В лог — только класс ошибки, без адреса.

- [ ] **Step 5: Тесты зелёные**

Run: та же команда. Expected: PASS (3 + 6 тестов).

- [ ] **Step 6: Гейт и коммит**

`feat(x): список категорий X из меню сайта по ориентации`

---

### Task 3: Фильтры и адрес страницы категории

**Files:**
- Create: `screens/rubric/model/XRubricFilters.kt`
- Create: `screens/rubric/model/RubricPageUrl.kt`
- Test: `screens/rubric/model/RubricPageUrlTest.kt`

**Interfaces:**
- Produces:
  - `sealed interface XRubricFilterOption { val segment: String?; val title: String }`;
  - `enum class XRubricSort`, `XRubricPeriod`, `XRubricDuration`, `XRubricQuality`
    (`: XRubricFilterOption`) — значения ниже;
  - `enum class XRubricFilterGroup(val title: String, val options: List<XRubricFilterOption>)`:
    `SORT("Сортировка")`, `PERIOD("Период")`, `DURATION("Длительность")`, `QUALITY("Качество")`;
    `fun selectedIn(filters: XRubricFilters): XRubricFilterOption`;
  - `@Immutable data class XRubricFilters(val sort: XRubricSort = XRubricSort.RELEVANCE, val period: XRubricPeriod = XRubricPeriod.ANY, val duration: XRubricDuration = XRubricDuration.ANY, val quality: XRubricQuality = XRubricQuality.ANY)`
    с `fun with(option: XRubricFilterOption): XRubricFilters` (меняет только свою группу) и
    `val pathPrefix: String` (`"s:…/m:…/d:…/q:…/"`, только не по умолчанию, всегда в
    этом порядке);
  - `fun buildRubricPageUrl(id: Int, slug: String, index: Int, filters: XRubricFilters): String`.

Значения (сегмент — подпись). Подписи значений по умолчанию у длительности и качества —
мой выбор: «Все» на двух кнопках подряд было бы непонятно.

| enum | значения |
|---|---|
| `XRubricSort` | `RELEVANCE(null, "Актуальность")`, `NEWEST("s:uploaddate", "Самые свежие")`, `RATING("s:rating", "Рейтинг")`, `LENGTH("s:length", "Длина")`, `VIEWS("s:views", "Просмотров")` |
| `XRubricPeriod` | `ANY(null, "В любое время")`, `DAYS_3("m:today", "Последние 3 дня")`, `WEEK("m:week", "Эта неделя")`, `MONTH("m:month", "Этот месяц")`, `MONTHS_3("m:3month", "Последние 3 месяца")`, `MONTHS_6("m:6month", "Последние 6 месяцев")` |
| `XRubricDuration` | `ANY(null, "Любая длина")`, `MIN_1_3("d:1-3min", "1–3 мин")`, `MIN_3_10("d:3-10min", "3–10 мин")`, `MIN_10_20("d:10-20min", "10–20 мин")`, `MIN_20_PLUS("d:20min_more", "20+ мин")` |
| `XRubricQuality` | `ANY(null, "Любое качество")`, `HD("q:hd", "720p+")`, `FULL_HD("q:1080P", "1080p+")` |

- [ ] **Step 1: Написать падающий тест `RubricPageUrlTest`**

```kotlin
@Test fun `без фильтров — /c/slug-id/N`() {
    assertEquals("$urlStart/c/Cat_B-12/0", buildRubricPageUrl(12, "Cat_B", 0, XRubricFilters()))
}
@Test fun `один фильтр — его сегмент перед slug`() {
    assertEquals("$urlStart/c/s:rating/Cat_B-12/3", buildRubricPageUrl(12, "Cat_B", 3, XRubricFilters(sort = XRubricSort.RATING)))
}
@Test fun `все четыре — в порядке s, m, d, q независимо от порядка выбора`() {
    val filters = XRubricFilters().with(XRubricQuality.HD).with(XRubricDuration.MIN_10_20)
        .with(XRubricPeriod.WEEK).with(XRubricSort.VIEWS)
    assertEquals("$urlStart/c/s:views/m:week/d:10-20min/q:hd/Cat_B-12/1", buildRubricPageUrl(12, "Cat_B", 1, filters))
}
@Test fun `with меняет только свою группу`() {
    val f = XRubricFilters(sort = XRubricSort.RATING).with(XRubricPeriod.MONTH)
    assertEquals(XRubricSort.RATING, f.sort); assertEquals(XRubricPeriod.MONTH, f.period)
}
@Test fun `возврат к значению по умолчанию убирает сегмент`() {
    assertEquals("", XRubricFilters(sort = XRubricSort.RATING).with(XRubricSort.RELEVANCE).pathPrefix)
}
@Test fun `выбранное значение группы`() {
    assertEquals(XRubricPeriod.WEEK, XRubricFilterGroup.PERIOD.selectedIn(XRubricFilters(period = XRubricPeriod.WEEK)))
}
```

- [ ] **Step 2: Запустить и убедиться, что тест падает**

Run: `cmd /c ".\gradlew.bat :feature-x:testDebugUnitTest --tests *RubricPageUrlTest"`
Expected: FAIL — символы не найдены.

- [ ] **Step 3: Реализовать фильтры и адрес**

`buildRubricPageUrl` = `"$urlStart/c/${filters.pathPrefix}$slug-$id/$index"`.

- [ ] **Step 4: Тест зелёный**

Run: та же команда. Expected: PASS (6 тестов).

- [ ] **Step 5: Гейт и коммит**

`feat(x): фильтры и адрес страницы категории X`

---

### Task 4: Выдача категории и её ScreenModel

**Files:**
- Create: `screens/rubric/model/XRubricFeed.kt`
- Create: `screens/rubric/ScreenXRubricSM.kt` (ScreenModel и Hilt-модуль фабрики)
- Test: `screens/rubric/model/XRubricFeedTest.kt`

**Interfaces:**
- Consumes: `buildRubricPageUrl`, `XRubricFilters`, `XRubricFilterGroup`, `XRubricFilterOption`
  (задача 3), `parserScreenTags`, `PageCache`, `ModelScreenTag`, `readHtmlFromURLDirect`.
- Produces:
  - `@Stable class XRubricFeed(private val id: Int, private val slug: String, private val fetch: suspend (String) -> String)`:
    `var filters: XRubricFilters` (снаружи только чтение), `var screen: ModelScreenTag`
    (снаружи только чтение, начальное `ModelScreenTag("", "", emptyList())`),
    `val listPositions: MutableMap<Int, LazyListPosition>`,
    `val shownPerRow: MutableMap<Int, MutableIntState>`, `var currentPage: Int`,
    `fun cachedItems(index: Int): List<ItemsX>?`,
    `fun applyFilters(new: XRubricFilters): Boolean` (false — те же фильтры, ничего не
    тронуто), `suspend fun loadPage(index: Int): ModelScreenTag`;
  - `@Stable class ScreenXRubricSM @AssistedInject constructor(@Assisted id: Int, @Assisted slug: String, val saved: SavedX) : ScreenModel`
    с `@AssistedFactory interface Factory : ScreenModelFactory { fun create(id: Int, slug: String): ScreenXRubricSM }`,
    `val feed: XRubricFeed`, `var openGroup: XRubricFilterGroup?` (снаружи только чтение),
    `fun toggleGroup(group: XRubricFilterGroup)`, `fun selectFilter(option: XRubricFilterOption)`,
    и делегаты для меню роликов, как у `ScreenTagsViewModel`: `isFavorite`, `addFavorite`,
    `removeFavorite`, `download`, `saveToGallery`.

- [ ] **Step 1: Написать падающий тест `XRubricFeedTest`** (через `runBlocking`)

```kotlin
private val page = """
    <html><body>
      <h2 class="page-title">Категория Б<span class="sub">(1 000 результатов)</span></h2>
      <div class="pagination "><ul><li><a href="/c/Cat_B-12/4" class="last-page">5</a></li></ul></div>
      <div id="content"><div class="mozaique cust-nb-cols">
        <div class="frame-block thumb-block" data-id="555123">
          <p class="title"><a href="/video555123/clip" title="Ролик"><span class="duration">15 min</span></a></p>
        </div>
      </div></div>
    </body></html>
""".trimIndent()
private val emptyPage = page.replace(Regex("""<div class="frame-block.*?</div>\s*""", RegexOption.DOT_MATCHES_ALL), "")

@Test fun `страница грузится по адресу текущих фильтров`() { /* loadPage(0): адрес "$urlStart/c/Cat_B-12/0"; screen.lastPage == 5 */ }
@Test fun `загруженная страница берётся из кеша`() { /* два loadPage(1) — один fetch */ }
@Test fun `смена фильтров сбрасывает выдачу`() {
    // loadPage(2); listPositions[2] = LazyListPosition(3); currentPage = 2
    // applyFilters(XRubricFilters(sort = XRubricSort.RATING)) == true
    // listPositions пусто, shownPerRow пусто, currentPage == 0, screen.title0 == ""
    // loadPage(2) снова идёт в сеть: "$urlStart/c/s:rating/Cat_B-12/2"
}
@Test fun `те же фильтры — ничего не сбрасывается`() { /* applyFilters(XRubricFilters()) == false; кеш цел (fetch не вызван повторно) */ }
@Test fun `ответ на старые фильтры не попадает в новую выдачу`() {
    // fetch ждёт CompletableDeferred<String>; launch { loadPage(0) } в runBlocking;
    // пока ждёт — applyFilters(sort = RATING); затем complete(page); join
    // screen.title0 == "" и следующий loadPage(0) снова вызывает fetch
}
@Test fun `страница без роликов не кешируется`() { /* fetch отдаёт emptyPage: два loadPage(0) — два вызова fetch */ }
@Test fun `пустой ответ — IOException`() { /* fetch отдаёт "" — assertThrows<IOException> */ }
```

- [ ] **Step 2: Запустить и убедиться, что тест падает**

Run: `cmd /c ".\gradlew.bat :feature-x:testDebugUnitTest --tests *XRubricFeedTest"`
Expected: FAIL — `XRubricFeed` не найден.

- [ ] **Step 3: Реализовать `XRubricFeed`**

Кеш — `private var pages = PageCache<ModelScreenTag>(maxPages = 20)`, у `PageCache` нет
`clear()`. `applyFilters` заменяет `pages` новым экземпляром, очищает позиции, ставит
`currentPage = 0` и пустой `screen`. Защита от гонки в `loadPage`:

```kotlin
val requested = filters
val cache = pages
cache[index]?.let { return it }
// fetch → пустая строка: IOException; разбор parserScreenTags на Dispatchers.Default
if (requested == filters && index == 0 && (screen.lastPage <= 1 || screen.title0.isEmpty())) screen = result
if (result.items.isNotEmpty()) cache[index] = result   // ответ на старые фильтры уходит в выброшенный кеш
```

- [ ] **Step 4: Реализовать `ScreenXRubricSM` и Hilt-модуль**

`feed = XRubricFeed(id, slug, ::readHtmlFromURLDirect)`. `init` и успешный
`applyFilters` запускают загрузку страницы 0 в `screenModelScope`, как в
`ScreenTagsViewModel.init`: отмена пробрасывается, остальные ошибки — `Timber.w` без адреса.
`selectFilter(option)` закрывает `openGroup` и вызывает
`feed.applyFilters(feed.filters.with(option))`. `toggleGroup` открывает группу или
закрывает её, если она уже открыта. Модуль — `ScreenModuleRubric` по образцу
`ScreenModuleTags`.

- [ ] **Step 5: Тесты зелёные**

Run: `cmd /c ".\gradlew.bat :feature-x:testDebugUnitTest --tests *XRubricFeedTest"`
Expected: PASS (7 тестов).

- [ ] **Step 6: Гейт и коммит**

`feat(x): выдача категории X со сбросом по фильтрам`

---

### Task 5: Экран категории

**Files:**
- Move: `screens/actresses/atom/FilterButton.kt` → `screens/common/atom/FilterButton.kt`
  (пакет `…x.screens.common.atom`, импорт в `screens/actresses/molecule/ActressesFilterBar.kt`)
- Create: `screens/rubric/atom/XRubricOptionRow.kt`
- Create: `screens/rubric/molecule/XRubricFilterBar.kt`
- Create: `screens/rubric/molecule/XRubricHeader.kt`
- Create: `screens/rubric/ScreenXRubric.kt`
- Test: `screens/rubric/ScreenXRubricSerializationTest.kt`

**Interfaces:**
- Consumes: `ScreenXRubricSM` и `XRubricFeed` (задача 4), `XRubricFilterGroup`,
  `XRubricFilters`, `XRubricFilterOption` (задача 3), `TagsPaginatedListScreen`,
  `TagsHeader`, `TagCellActions`, `BottomListDashBoardNavigationButtons2`,
  `rememberLocalVideoOf`, `playerScreenFor`, `getTopInsetDp`.
- Produces: `class ScreenXRubric(val id: Int, val slug: String, val title: String) : Screen`
  с `key = "ScreenXRubric:$id"`.

- [ ] **Step 1: Написать падающий тест `ScreenXRubricSerializationTest`**

Как в `ScreenTagsSerializationTest`: после `roundTrip(ScreenXRubric(12, "Cat_B", "Категория Б"))`
у восстановленного экрана `id == 12`, `slug == "Cat_B"`, `title == "Категория Б"`,
`key == "ScreenXRubric:12"`.

- [ ] **Step 2: Запустить и убедиться, что тест падает**

Run: `cmd /c ".\gradlew.bat :feature-x:testDebugUnitTest --tests *ScreenXRubricSerializationTest"`
Expected: FAIL — `ScreenXRubric` не найден.

- [ ] **Step 3: Перенести `FilterButton`**

Перенести файл (`git mv`) и поправить пакет и импорт. Код кнопки не меняется. Перед
коммитом проверить `git diff --cached`: переименование не должно уехать в чужой коммит.

- [ ] **Step 4: Атом и молекулы**

- `XRubricOptionRow(title: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier)` —
  строка варианта, выбранный отмечен галочкой.
- `XRubricFilterBar(filters: XRubricFilters, openGroup: XRubricFilterGroup?, onToggleGroup: (XRubricFilterGroup) -> Unit, onSelect: (XRubricFilterOption) -> Unit, modifier: Modifier = Modifier)` —
  ряд из четырёх `FilterButton` с горизонтальной прокруткой (подпись кнопки —
  `group.selectedIn(filters).title`). Под рядом в `AnimatedVisibility` — варианты
  открытой группы из `XRubricOptionRow`. Ряд и панель — `adaptiveContentWidth()` после фона.
- `XRubricHeader(title: String, title0: String, title1: String, topCutout: Dp, filterBar: @Composable () -> Unit, modifier: Modifier = Modifier)` —
  `TagsHeader(tag = title, title0, title1, topCutout)` и под ним `filterBar()`.

- [ ] **Step 5: Экран `ScreenXRubric.kt`**

В файле только Voyager-класс и корневая функция `ScreenXRubricContent` — по образцу
`ScreenTags` / `ScreenTagsContent`. Отличия:
- ScreenModel: `getScreenModel<ScreenXRubricSM, ScreenXRubricSM.Factory> { it.create(id, slug) }`;
- пейджер пересоздаётся при смене фильтров:
  `val pagerState = key(feed.filters) { rememberPagerState(initialPage = feed.currentPage) { feed.screen.lastPage.coerceAtLeast(1) } }`,
  `LaunchedEffect(pagerState)` записывает `currentPage` в `feed`;
- ключ страницы пейджера — строка `"${feed.filters.pathPrefix}#$pageIndex"`: со сменой
  фильтров композиция страниц (`remember(pageIndex)` в `TagsPaginatedListScreen`)
  создаётся заново;
- `listPositions` / `shownPerRow` / `cachedItems` берутся из `feed`, шапка —
  `XRubricHeader` с `XRubricFilterBar`;
- `ScreenXRubricContent` получает развёрнутое состояние и колбэки, а не ScreenModel.
  Если параметров станет больше двенадцати — `@Suppress("LongParameterList")`, как у
  `ScreenXDashBoardsContent`.

- [ ] **Step 6: Тест зелёный**

Run: та же команда, что в шаге 2. Expected: PASS.

- [ ] **Step 7: Гейт и коммит**

`feat(x): экран категории X с фильтрами`. Перенос `FilterButton` идёт в этот же коммит.

---

### Task 6: Вкладка «Категории» на главном экране

**Files:**
- Create: `screens/rubrics/XRubricsSM.kt` (ScreenModel, `XRubricsState`, Hilt-модуль)
- Create: `screens/rubrics/atom/XRubricTile.kt`
- Create: `screens/rubrics/molecule/XRubricsGrid.kt`
- Create: `screens/rubrics/ScreenXRubricsTabContent.kt`
- Modify: `screens/dashboards/molecule/DashboardSecondaryControls.kt` (константы вкладок и ветка)
- Modify: `screens/dashboards/ScreenXDashBoards.kt` (иконка, ScreenModel, слот вкладки)
- Test: `screens/rubrics/XRubricsSMTest.kt`

**Interfaces:**
- Consumes: `XRubricCatalog`, `XRubric` (задача 2), `ScreenXRubric` (задача 5),
  `XCategoryState.current`, `LazyGridPosition`, `TagsStateMessage`.
- Produces:
  - `sealed interface XRubricsState { data object Loading; data object Failed; data class Ready(val rubrics: ImmutableList<XRubric>) }`;
  - `@Stable class XRubricsSM internal constructor(private val catalog: XRubricCatalog, private val scopeOverride: CoroutineScope?) : ScreenModel`
    с `@Inject constructor(catalog: XRubricCatalog) : this(catalog, null)`,
    `var state: XRubricsState` (снаружи только чтение), `fun show(orientation: XCategory)`,
    `fun retry()`, `fun gridPosition(orientation: XCategory): LazyGridPosition` (своя позиция
    у каждой ориентации — решение пункта 8 спеки);
  - константы `MAIN_TAB_DASHBOARDS = 0`, `MAIN_TAB_RUBRICS = 1`, `MAIN_TAB_SAVABLE = 2`,
    `MAIN_TAB_SEARCH = 3`.

- [ ] **Step 1: Написать падающий тест `XRubricsSMTest`**

Свой `TestCoroutineScheduler` + `StandardTestDispatcher`, scope —
`CoroutineScope(dispatcher)`. Каталог — `XRubricCatalog { url -> … }`, ответ по адресу
управляется вручную через `CompletableDeferred`. `scheduler.advanceUntilIdle()` между
шагами.

```kotlin
@Test fun `показ ориентации грузит её список`() { /* show(STRAIGHT): Loading → после advance Ready(1 рубрика) */ }
@Test fun `уже загруженный список показывается сразу`() { /* второй XRubricsSM на том же каталоге: после show(STRAIGHT) сразу Ready, без advance */ }
@Test fun `ошибка — Failed, retry грузит заново`() { /* первый ответ — IOException → Failed; retry() + advance → Ready */ }
@Test fun `побеждает последняя выбранная ориентация`() {
    // show(GAY) (ответ ждёт), show(TRANS), ответ TRANS, затем ответ GAY; advance
    // state == Ready со списком TRANS
}
@Test fun `повторный show той же ориентации не перезагружает`() { /* fetch вызван один раз */ }
```

- [ ] **Step 2: Запустить и убедиться, что тест падает**

Run: `cmd /c ".\gradlew.bat :feature-x:testDebugUnitTest --tests *XRubricsSMTest"`
Expected: FAIL — `XRubricsSM` не найден.

- [ ] **Step 3: Реализовать `XRubricsSM`**

`scope` — `scopeOverride ?: screenModelScope`. `show(orientation)`:
- если та же ориентация и не `Failed`, ничего не делает;
- иначе отменяет прежнюю загрузку;
- `catalog.cached(orientation)` есть — сразу `Ready`;
- нет — `Loading` и `launch { state = Ready(catalog.load(orientation)) }`. Отмена
  пробрасывается, остальные ошибки — `Failed` и `Timber.w` с классом ошибки.

`retry()` повторяет `show` для последней ориентации, минуя проверку «та же». Модуль —
`@Binds @IntoMap @ScreenModelKey(XRubricsSM::class)`, как у `ScreenXSearchSM`.

- [ ] **Step 4: Атом, молекула, корневая функция вкладки**

- `XRubricTile(title: String, onClick: () -> Unit, modifier: Modifier = Modifier)` —
  плитка высотой 56dp, подпись по центру, до двух строк с многоточием, скруглённый фон в
  цветах модуля.
- `XRubricsGrid(state: XRubricsState, gridState: LazyGridState, topCutout: Dp, onOpen: (XRubric) -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier)`:
  - `Loading` — только `CircularProgressIndicator` по центру;
  - `Failed` — `TagsStateMessage("Категории не загрузились", onRetry)` по центру;
  - `Ready` — `LazyVerticalGrid(GridCells.Fixed(adaptiveGridColumns()))`, ключ —
    `rubric.id`, отступы 8dp, сверху ещё `topCutout`.
- `ScreenXRubricsTabContent(vm: XRubricsSM, category: XCategory, onOpenRubric: (XRubric) -> Unit, modifier: Modifier = Modifier)`
  (корень вкладки, как `ScreenXSearchTabContent`): `LaunchedEffect(category) { vm.show(category) }`,
  затем `XRubricsGrid` с `vm.gridPosition(category).rememberState()` и `getTopInsetDp()`.

- [ ] **Step 5: Подключить вкладку к главному экрану**

- `DashboardSecondaryControls.kt`: новые значения констант. Ветка `MAIN_TAB_RUBRICS -> Unit`
  стоит до `else`, второго ряда у вкладки нет.
- `ScreenXDashBoards.kt`:
  - `mainTabs`: `Icons.Outlined.Category` вторым элементом;
  - в `Content()`: `val rubricsVm: XRubricsSM = getScreenModel()`;
  - в `ScreenXDashBoardsContent` новый параметр `rubricsTab: @Composable () -> Unit` и
    ветка `MAIN_TAB_RUBRICS -> rubricsTab()`. Слот собирается в `Content()`:
    `ScreenXRubricsTabContent(vm = rubricsVm, category = XCategoryState.current, onOpenRubric = { navigator.push(ScreenXRubric(it.id, it.slug, it.title)) })`;
  - `vm.mainTab = 0` в `onBack` заменить на `MAIN_TAB_DASHBOARDS`.

- [ ] **Step 6: Тесты зелёные**

Run: `cmd /c ".\gradlew.bat :feature-x:testDebugUnitTest --tests *XRubricsSMTest"`
Expected: PASS (5 тестов).

- [ ] **Step 7: Гейт и коммит**

Гейт целиком, включая сторожей `app/.../arch` (`AdaptiveLayoutTest`, `ScrollStateOwnershipTest`,
`GlobalStateTest`): `exit=0`. Коммит — `feat(x): вкладка «Категории» на главном экране X`.

---

### Task 7: Проверка на эмуляторе и запись итога в спеку

**Files:**
- Modify: `docs/superpowers/specs/2026-10-24-x-categories-screen-design.md` (раздел
  «Как реализовано» под статусом: коммиты и что проверено)

- [ ] **Step 1: Собрать debug и поставить на `Pixel_10_Pro_Fold` (emulator-5554)**

Run: `cmd /c ".\gradlew.bat :app:installDebug"`
Expected: `BUILD SUCCESSFUL`, `Installed on 1 device`.

- [ ] **Step 2: Пройти сценарии спеки (раздел «Проверка»)**

Вкладка:
- три ориентации дают разные списки;
- без сети — ошибка, «Повторить» загружает.

Категория:
- листание страниц;
- каждый фильтр по отдельности и сочетание всех четырёх;
- возврат из плеера на ту же страницу и позицию.

Сессия:
- после показа списков «Геи» и «Трансы» лента на главной остаётся в своей ориентации;
- переключатель на сайте в WebView не сдвинулся.

Складной:
- внешний экран, внутренний в портрете и альбомной;
- Medium: `adb shell wm density 475`, потом `wm density reset`;
- сложение и раскрытие посреди прокрутки: вкладка и позиция сохраняются.

Каждый сценарий — снимок экрана (`screencap -d`) или текст из дампа.

- [ ] **Step 3: Записать итог в спеку**

Под статусом добавить раздел «Как реализовано (дата)»: sha коммитов задач 1–6, что
проверено на эмуляторе, что не проверено. Статус спеки: «реализован в `<sha>`…`<sha>`».

- [ ] **Step 4: Коммит**

`docs(spec): итог реализации экрана категорий X`
