# Раскладка на раскрытых складных телефонах

Дата: 2026-10-14  
Статус: дизайн утверждён  

---

## 1. Контекст и постановка проблемы

Приложение рассчитано на портретный телефон: `MainActivity` и `LockActivity`
объявлены с `screenOrientation="portrait"`, сетки почти везде с жёсткими двумя
колонками, одноколоночный контент растянут на всю ширину.

У приложения `targetSdk = 37`. С Android 16 при `targetSdk` от 36 система на
дисплеях, у которых меньшая сторона не короче 600dp, **игнорирует**
`screenOrientation`, `resizeableActivity` и ограничения соотношения сторон, а
также запросы ориентации из кода (`requestedOrientation`). На Android 16 от
этого можно было отказаться флагом в манифесте, на Android 17 такой возможности
нет. Поэтому на внутреннем экране складного телефона приложение уже сейчас
получает почти квадратное окно в любой ориентации, и портретный замок там не
работает.

Размеры окна на внутреннем экране:

| устройство | портрет | альбомная |
|---|---|---|
| Pixel 10 Pro Fold (AVD `Pixel_10_Pro_Fold`, Android 17) | ~852×883dp | ~883×852dp |
| складные с внутренним экраном около 700dp | ~700×840dp | ~840×700dp |

Внешний экран — около 340–445dp в ширину, то есть обычный телефон.

На таком окне:

- две колонки дают карточки шириной 350–440dp, на экран попадает мало;
- горизонтальные ряды L (топ, поиск, страница тега) считают ширину карточки как
  `screenWidthDp / 3` и получают карточки по ~280dp;
- списки, шапки, строки поиска и ряды вкладок нижних панелей растянуты
  на всю ширину;
- клавиатура калькулятора из квадратных кнопок (`CalcButton`, `aspectRatio(1)`,
  четыре в ряд) на ширине 852–883dp получается выше окна.

Изменение конфигурации у обеих Activity объявлено в `configChanges`: при
раскрытии и сложении Activity не пересоздаётся, Compose перекомпоновывает
экран с новым размером окна.

На устройстве это не проверялось: адаптация делается заранее.

### Целевое поведение

1. На окне шириной меньше 600dp (телефон, внешний экран) разметка не меняется
   ни на пиксель.
2. На окне шириной от 600dp число колонок в сетках растёт с шириной, а размер
   карточки остаётся близким к телефонному.
3. Одноколоночный контент (списки, шапки, поля, ряды нижних панелей) не шире
   640dp и стоит по центру; фон и баннеры остаются на всю ширину.
4. Клавиатура калькулятора целиком помещается в окно в обеих ориентациях
   внутреннего экрана.
5. При раскрытии, сложении и повороте внутреннего экрана прокрутка и выбранная
   вкладка сохраняются.

---

## 2. Решения

| Решение | Альтернативы | Обоснование |
|---|---|---|
| **Адаптивные размеры, навигация прежняя** | Боковая панель (NavigationRail) на широком окне; две панели «список + детали» | Решение владельца. Закрывает растянутые сетки и раскладку, телефон не задевает. Боковая панель требует переделать три разные нижние панели с дополнительными элементами; две панели конфликтуют со стеком Voyager. |
| **Ширина окна, а не экрана** | `LocalConfiguration.screenWidthDp` | Окно правильно ведёт себя и в разделённом экране; `screenWidthDp` описывает весь экран. |
| **Три класса ширины по границам Material** | Плавная формула от ширины в dp | Классы проще проверить и протестировать. Границы 600 и 840dp — стандарт Material и `material3-adaptive`, библиотека уже подключена в `:core`. |
| **Пользовательский выбор колонок + 0 / +1 / +2** | Число пропорционально ширине; ровно выбор | Решение владельца. Выбор сохраняется как есть, на широком окне добавляется запас. Пропорциональная формула давала 7–8 колонок из четырёх. |
| **Прибавка колонок только при отрисовке** | Записывать эффективное число в `host.columns` | `LazyRow123Host.normalizeColumns` обрезает значение до 2..4, а кнопка выбора должна показывать выбор пользователя. |
| **Лимит списка — горизонтальный `contentPadding`** | `widthIn(max)` на самом списке | С отступом список свайпается по всей ширине окна, а не только в центральной полосе. |
| **Сторож на жёсткие колонки и размеры экрана** | Договорённость | Новая сетка иначе снова получит `GridCells.Fixed(2)`. Тот же приём, что у `AutofillExclusionTest`. |

---

## 3. Дизайн

### 3.1. Основа в `:core`

Пакет `common/ui/adaptive/` в `:core`:

```kotlin
enum class WidthClass { Compact, Medium, Expanded }

/** Compact < 600dp, Medium 600–839dp, Expanded ≥ 840dp. */
fun widthClassOf(widthDp: Int): WidthClass

/** Единственная точка, которая читает размер окна. */
@Composable
fun currentWidthClass(): WidthClass
```

`widthClassOf` и все правила ниже — чистые функции без Compose, они
тестируются на JVM. `currentWidthClass()` берёт ширину окна из
`material3-adaptive` (`currentWindowAdaptiveInfo()`) или из
`LocalWindowInfo`; конкретный API выбирает план по версии библиотеки в
`composeBom`.

Где `Compact`, там все правила возвращают нынешние значения. Адаптация
включается только начиная с `Medium`.

Класс ширины решает окно, округлённое вниз (`windowWidthDp`). Когда Compact
и окно, и `Configuration.screenWidthDp`, ширина берётся из конфигурации:
на новых API `screenWidthDp` округляется к ближайшему, и без этого карточки
рядов L на телефоне стали бы на пиксель уже прежних.

### 3.2. Сетки и ряды

**а) Сетки с жёсткими двумя колонками.** Правило:

```kotlin
fun gridColumns(widthClass: WidthClass): Int   // 2 / 3 / 4
```

| модуль | файл |
|---|---|
| X | `favorites/molecule/FavoritesContent.kt` |
| X | `history/molecule/HistoryGrid.kt` |
| X | `channel/molecule/ChannelVideosPager.kt` |
| X | `dashboards/molecule/DashboardsPaginatedListContent.kt` |
| X | `actresses/molecule/ActressesIndexGrid.kt` |
| X | `tags/molecule/TagsPaginatedListScreen.kt` — ряды собираются вручную через `chunked`; ориентация заменяется классом ширины |
| L | `screenAlbumList/molecule/AlbumListPageGrid.kt` |
| L | `explorer/tab/saved/albums/L_ScreenSavedAlbumsTab.kt` |
| L | `explorer/tab/saved/collection/CollectionsGrid.kt` |
| L | `explorer/tab/saved/subscribedAlbums/molecule/SubscribedAlbumsGrid.kt` |
| core | `common/collectionDB/ui/CollectionsGrid.kt` |

`TagsPaginatedListScreen` сейчас даёт 4 колонки в альбомной ориентации. На
телефоне её не бывает из-за портретного замка, так что поведение телефона не
меняется.

**б) Сетки, где колонки выбирает пользователь.** Правило:

```kotlin
fun userGridColumns(selected: Int, widthClass: WidthClass): Int   // selected + 0 / 1 / 2
```

- R — `ui/lazyrow123/LazyRow123.kt` (ленты, лайки, коллекции, ниша; выбор
  2..4, на широком окне максимум 6).
- L — `ui/element/lazyRowPictureDetails/L_LazyRowPictureDetails.kt` (фото
  альбома, лайков, лайков с сервера, коллекций; выбор из разрешённых в
  настройках, у альбома — значение `host.columns` по умолчанию).

Выбор хранится в настройках и в `host.columns` без изменений, кнопка выбора
показывает его же. Эффективное число идёт только в
`GridCells.Fixed(...)` / `StaggeredGridCells.Fixed(...)` и во всё, что
считается от числа колонок при отрисовке: буферы предзагрузки
`bufferBefore` / `bufferAfter` у L и ключи эффектов, которые перезапускаются
при смене колонок (возврат из полноэкранного режима у R прокручивает по
индексу элемента и от числа колонок не зависит).

**в) Горизонтальные ряды L.** Правило:

```kotlin
fun rowVisibleItems(widthClass: WidthClass): Int   // 3 / 4 / 5
```

Ширина карточки = (ширина окна − 8dp) / `rowVisibleItems`. Файлы:
`explorer/tab/albumTopHits/L_ScreenAlbumTopHits.kt`,
`explorer/tab/albumSearch/L_ScreenAlbumSearch.kt`,
`albumLandingTag/ScreenLAlbumLandingTag.kt`. `screenWidthDp` из них уходит.

### 3.3. Одноколоночный контент

Лимит — **640dp**, только на `Medium` и `Expanded`.

```kotlin
const val CONTENT_MAX_WIDTH_DP = 640

/** Отступ с каждой стороны: 0, пока контейнер не шире лимита. */
fun contentSidePaddingDp(containerWidthDp: Int): Int   // max(0, (w − 640) / 2)

/** widthIn(max = 640.dp) + центрирование в родителе. */
fun Modifier.adaptiveContentWidth(): Modifier
```

**а) Списки** — горизонтальный `contentPadding` из `contentSidePaddingDp`:

| модуль | где |
|---|---|
| app | настройки и бэкап (`screenSettings/`), меню выбора раздела (`screenRoot/MenuScreen.kt`) |
| core | P2P: `ScreenP2pSend.kt`, `ScreenP2pReceive.kt` — только разметка |
| X | `saved/molecule/X_SavedList.kt`, `subscriptions/molecule/X_SubscriptionsList.kt`, `search/molecule/SearchHistoryView.kt`, `search/molecule/SearchSuggestionsView.kt`, `feature/country/molecule/ComposeCountry.kt` |
| R | `explorer/tab/niches/R_ScreenNichesTab.kt`, `explorer/tab/saved/tab/R_Screen_CreatorsTab.kt`, `explorer/tab/saved/tab/R_Screen_Saved_DownloadTab.kt`, `explorer/tab/saved/tab/savedNiche/SavedNichesTab.kt`, `explorer/tab/search/molecule/SearchCreatorsList.kt`, `manager_block/molecule/BlockedUsersList.kt`, `search/molecule/SuggestionList.kt` |
| L | `screenAlbumList/molecule/AlbumListFilterOverlay.kt` — заодно `screenHeightDp` заменяется высотой окна |

`adaptiveContentWidth()` — узел `LayoutModifierNode`; решение «оставить или
ограничить» принимает чистая функция `contentWidthConstraints`.

**б) Шапки, верхние панели, поля ввода** — `adaptiveContentWidth()` на
текстовые блоки, кнопки и поле; фон, баннер и разделитель под панелью
остаются на всю ширину. Шапка, которая в списке идёт первой строкой (X —
«Сохранённое»), и в пустом списке стоит в той же полосе:

- X — шапка канала (`channel/molecule/ChannelHeader*.kt`, баннер на всю
  ширину), верхние панели и строки поиска (`search/molecule/SearchTopBar.kt`,
  `history/molecule/HistoryTopBar.kt`, `actresses/molecule/ActressesIndexTopBar.kt`);
- R — профиль (`profile/molecule/RedProfileCreaterInfo.kt`, `TagsBlock.kt`),
  шапка ниши (`niche/molecule/NicheHeaderContent.kt`), поиск
  (`explorer/tab/search/molecule/SearchTopBar.kt`), верхние панели коллекций и
  блокировок;
- L — шапка альбома (`screenAlbum/molecule/ScreenLAlbumDetailsHeader.kt`),
  верхние панели тега и коллекций.

**в) Нижние панели** — фон и разделитель на всю ширину, ряд вкладок и
вторичные элементы (страницы, колонки, индикатор загрузки) через
`adaptiveContentWidth()`. У общего `TabRow` собственный разделитель
`SecondaryTabRow` выключен и нарисован на всю ширину под рядом:

- X — `dashboards/ScreenXDashBoards.kt` (`DashboardSecondaryControls`,
  `TabRow`), `dashboards/bottomBar/DashboardControlsRow.kt`,
  `channel/molecule/ChannelPagerBottomBar.kt`;
- L — `explorer/molecule/L_ExplorerBottomBar.kt`,
  `explorer/tab/saved/molecule/L_SavedBottomBar.kt`,
  `screenAlbumList/bottomBar/AlbumListBottomBar.kt`;
- R — `explorer/molecule/ExplorerBottomBar.kt`, нижние панели вкладок
  (`GifsTabBottomBar.kt`, `NichesBottomBar.kt`, `SavedBottomBar.kt`),
  `niche/NicheBottomBar.kt`.

Нижние панели плеера X сюда не входят (раздел 4).

**г) Без изменений.** Диалоги: 54 из 60 файлов с `Dialog(` идут на
системной ширине, которую ограничивает сама платформа; у остальных шести
(фильтры альбомов L) уже стоит `widthIn(max = 420–460dp)`. Выпадающие меню.

Конкретный набор файлов в а)–в) уточняет план по месту: таблицы перечисляют
места, которые нашёл поиск, а в плане каждое проверяется на окне 852dp.

### 3.4. Калькулятор и экран замка

Клавиатура калькулятора (`calculator/molecule/CalculatorKeypad.kt`) —
четыре квадратные кнопки в ряд, пять рядов. Её ширина:

```kotlin
/** Меньшее из 420dp и ширины, при которой клавиатура влезает по высоте. */
fun keypadWidthDp(windowWidthDp: Int, keypadAvailableHeightDp: Int): Int
```

Клавиатура стоит по центру; дисплей калькулятора — в пределах той же ширины.
На `Compact` функция возвращает ширину окна, как сейчас. В низком широком
окне кнопка не меньше 48dp (граница действует ниже 460dp высоты): иначе
ниже ~224dp клавиатура сжималась бы в ноль. Это первый экран
при запуске под маскировкой, поэтому он проверяется в обеих ориентациях
внутреннего экрана.

Форма замка и форма входа L — не шире 420dp вне `Compact`
(`capOutsideCompactDp`); фон и прокрутка — на всю ширину.

---

## 4. Вне рамок

- **Плеер X.** Запросы `SENSOR_LANDSCAPE` / `PORTRAIT` из
  `OrientationAndSystemBarsEffect` и `util.android.kt` на внутреннем экране
  система игнорирует: полноэкранный режим работает в текущей ориентации.
  На внешнем экране всё как сейчас. Отдельная задача, если понадобится.
- Полноэкранные просмотрщики: лента R (`RedFullScreenFeed`), просмотр фото L.
- Боковая панель навигации и две панели.
- Положение «полураскрыт» (tabletop / book) и учёт линии сгиба
  (`FoldingFeature`).
- Отказ от портретного замка на телефонах: на окне меньше 600dp замок
  по-прежнему работает.

---

## 5. Тесты и проверка

### 5.1. Юнит-тесты (`:core`, JVM)

- `widthClassOf`: 599 → Compact, 600 → Medium, 839 → Medium, 840 → Expanded.
- `gridColumns`: 2 / 3 / 4.
- `userGridColumns`: для каждого выбора R (2, 3, 4) и для крайних значений L —
  `+0` / `+1` / `+2`.
- `rowVisibleItems`: 3 / 4 / 5.
- `contentSidePaddingDp`: 411 → 0, 640 → 0, 700 → 30, 852 → 106.
- `keypadWidthDp`: Compact — ширина окна; широкое окно с запасом по высоте —
  420; широкое низкое окно — ширина, при которой пять рядов квадратных кнопок
  равны доступной высоте; очень низкое окно — кнопки 48dp.
- `windowWidthDp`: Compact — конфигурация; вне Compact — окно вниз; при
  отстающей конфигурации класс решает окно.
- `contentWidthConstraints`: родитель не шире лимита — `null`; шире — ширина
  не больше лимита, высота прежняя. Модификаторы с тем же лимитом равны.

### 5.2. Сторож (`app/src/test/.../arch/AdaptiveLayoutTest.kt`)

Читает исходники через `ProjectSources`, как `AutofillExclusionTest`, и
запрещает в UI-коде:

- `GridCells.Fixed(<число>)` и `StaggeredGridCells.Fixed(<число>)`, в том
  числе с именем аргумента; `GridItemSpan(<число>)`, кроме одной ячейки;
  `FlowRow(maxItemsInEachRow = <число>)` и `chunked(<число>)`;
- `LocalConfiguration.current.screenWidthDp`, `.screenHeightDp`,
  `.orientation`.

Исключения перечислены явно с причиной: пакет `common/ui/adaptive/`, функции
`@Preview`, плеер X (раздел 4), цифровая клавиатура (три кнопки в ряду) и
разбор hex-строки по два символа. Тест падает в обе стороны: и на новом
нарушении, и на исключении, которое больше ничего не исключает.

### 5.3. Превью

У затронутых atom/molecule остаются минимальные превью по правилам проекта.
Отдельных превью под размер складного телефона не добавляется.

### 5.4. Эмулятор

AVD `Pixel_10_Pro_Fold` (Android 17):

1. **Внешний экран (Compact).** Снимки главных экранов X / L / R, настроек,
   меню и калькулятора до и после правки совпадают. Та же сверка — на
   телефонном `emulator-5554`.
2. **Внутренний экран, портрет и альбомная (Expanded).** Сетки 4 колонки
   (пользовательские — выбор + 2), ряды L по 5 карточек, списки, шапки и ряды
   нижних панелей не шире 640dp, клавиатура калькулятора целиком в окне.
3. **Medium.** На внутреннем экране `adb shell wm density 475` даёт окно
   ~700dp в ширину, как у складных с внутренним экраном около 700dp: сетки
   3 колонки, ряды L по 4 карточки. После проверки — `wm density reset`.
4. **Переходы.** Раскрытие, сложение и поворот на открытом экране: прокрутка
   и выбранная вкладка сохраняются, сетка перестраивается без пересоздания
   Activity.

P2P (отправка и приём) и «Поделиться» на устройстве не проверяются — правило
проекта; P2P-экраны меняются только в разметке.

### 5.5. Гейт

`gradlew.bat testDebugUnitTest detekt :app:lintRelease` целиком перед каждым
коммитом; в отчёт идёт фактический результат.
