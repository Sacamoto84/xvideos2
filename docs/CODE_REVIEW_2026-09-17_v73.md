# Код-ревью xvideos — проход 94

> **Срез:** `94e4c62` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `94e4c62`. Затронуты модули `:app`, `:core`, `:feature-l`, `:feature-r`. Выполнена системная ревизия обработки отступов `Scaffold` и удаление устаревших подавлений `@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")`, декомпозирован экран поиска альбомов Luscious с выносом компонентов `AlbumSearchInputField` и `AlbumSearchSectionBlock`, устранены однобуквенные переменные в кастомном лэйауте тегов, очищены подавления `@SuppressLint("DefaultLocale")` в утилитах форматирования, а также добавлены наборы модульных тестов для фильтрации поиска альбомов и строкового форматирования чисел.

Линзы:
- `UI` / Устранение избыточных `Scaffold`, корректный проброс отступов `paddingValues` в контейнеры содержимого, удаление устаревших подавлений `@SuppressLint`, вынос вычислений `screenWidth` и декомпозиция вёрстки ([L_ScreenAlbumSearch.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumSearch/L_ScreenAlbumSearch.kt), [R_Screen_Root.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/root/R_Screen_Root.kt), [MenuScreen.kt](../app/src/main/java/com/client/xvideos/screenRoot/MenuScreen.kt), [ScreenRedFullScreen.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt), [ScreenExplorer.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/ScreenExplorer.kt), [ScreenSaved.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/ScreenSaved.kt), [SearchTab.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/search/SearchTab.kt), [R_ScreenNichesTab.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/niches/R_ScreenNichesTab.kt)).
- `S` / Чистка однобуквенных переменных `val p` и `val btnPl` в лэйауте тегов ([TagBlock.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/profile/tags/TagBlock.kt)).
- `C` / Удаление ложных подавлений `@SuppressLint("DefaultLocale")` при явном указании `Locale.US` ([toPrettyCount.kt](../core/src/main/java/com/client/xvideos/common/util/toPrettyCount.kt)).
- `L` / Перевод логов жизненного цикла в отладочный уровень `Timber.d` ([L_ScreenAlbumSearch.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumSearch/L_ScreenAlbumSearch.kt)).
- `T` / Добавление модульных тестов `ToPrettyCountTest` и `L_ScreenAlbumSearchTest` ([ToPrettyCountTest.kt](../core/src/test/java/com/client/xvideos/common/util/ToPrettyCountTest.kt), [L_ScreenAlbumSearchTest.kt](../feature-l/src/test/java/com/client/xvideos/l/ui/screens/explorer/tab/albumSearch/L_ScreenAlbumSearchTest.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### UI123 — Игнорирование отступов Scaffold, избыточные контейнеры и ложные подавления UnusedMaterial3ScaffoldPaddingParameter. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumSearch/L_ScreenAlbumSearch.kt:80](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumSearch/L_ScreenAlbumSearch.kt#L80)
[feature-r/src/main/java/com/client/xvideos/r/ui/root/R_Screen_Root.kt:51, 95](../feature-r/src/main/java/com/client/xvideos/r/ui/root/R_Screen_Root.kt#L51)
[app/src/main/java/com/client/xvideos/screenRoot/MenuScreen.kt:55, 101](../app/src/main/java/com/client/xvideos/screenRoot/MenuScreen.kt#L55)
[feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt:76](../feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt#L76)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/ScreenExplorer.kt:58](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/ScreenExplorer.kt#L58)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/ScreenSaved.kt:69](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/ScreenSaved.kt#L69)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/search/SearchTab.kt:62](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/search/SearchTab.kt#L62)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/niches/R_ScreenNichesTab.kt:192](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/niches/R_ScreenNichesTab.kt#L192)

В `R_Screen_Root` и `MenuScreen` параметр `padding` из `Scaffold` не передавался в дочерние контейнеры, что маскировалось аннотацией `@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")`. В `L_ScreenAlbumSearch` `Scaffold` использовался без баров и плавающих кнопок как фоновый контейнер. На экранах `ScreenRedFullScreen`, `ScreenExplorer`, `ScreenSaved`, `SearchTab`, `R_ScreenNichesTab` аннотации `@SuppressLint` висели исторически, хотя реальный код уже использовал отступы (`paddingValues.calculateBottomPadding()`).

**Исправление:**
- В `L_ScreenAlbumSearch` пустой `Scaffold` заменён на `Box(modifier = Modifier.fillMaxSize().background(Theme.background))`, вычисление `screenWidth` вынесено из списка, добавлены стабильные ключи `key`.
- Экран `L_ScreenAlbumSearch` декомпозирован на `AlbumSearchInputField` и `AlbumSearchSectionBlock`, что устранило предупреждение Detekt `LongMethod`.
- В `R_Screen_Root` отступы `Scaffold` проброшены в `Box(modifier = Modifier.padding(padding))`, аннотация `@SuppressLint` удалена, строковое сравнение `collection != ""` заменено на `collection.isNotEmpty()`.
- В `MenuScreen` отступы `paddingValues` проброшены в `Column(modifier = Modifier.padding(paddingValues))`.
- Во всех проверенных экранах удалены ложные подавления `@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")` и неиспользуемые импорты `Scaffold` и `SuppressLint`.

---

### S45 — Однобуквенные переменные элементов в кастомном SubcomposeLayout. Низкая.

[feature-r/src/main/java/com/client/xvideos/r/ui/profile/tags/TagBlock.kt:57, 71, 104, 119](../feature-r/src/main/java/com/client/xvideos/r/ui/profile/tags/TagBlock.kt#L57)

В алгоритме расчёта геометрии чипов тегов `TagsBlock` использовались однобуквенные и сокращённые переменные `val p` и `val btnPl` для промежуточных замеров элементов `Placeable`.

**Исправление:**
- Переменные переименованы в понятные `placeable` и `buttonPlaceable` как при замере (`measure`), так и при размещении (`placeRelative`).

---

### C15 — Устаревшие подавления DefaultLocale при явной передаче Locale.US. Низкая.

[core/src/main/java/com/client/xvideos/common/util/toPrettyCount.kt:14, 38, 62, 86](../core/src/main/java/com/client/xvideos/common/util/toPrettyCount.kt#L14)

В файле расширений форматирования чисел `toPrettyCount.kt` все вызовы `String.format` явно используют `Locale.US`. Аннотации `@SuppressLint("DefaultLocale")` над всеми четырьмя функциями являлись устаревшим артефактом и вызывали избыточные предупреждения инспекции кода.

**Исправление:**
- Удалены все 4 аннотации `@SuppressLint("DefaultLocale")` и неиспользуемый импорт `android.annotation.SuppressLint`.

---

### T37 — Покрытие тестами утилит форматирования и генерации поисковых фильтров Luscious. Средняя.

[core/src/test/java/com/client/xvideos/common/util/ToPrettyCountTest.kt](../core/src/test/java/com/client/xvideos/common/util/ToPrettyCountTest.kt)
[feature-l/src/test/java/com/client/xvideos/l/ui/screens/explorer/tab/albumSearch/L_ScreenAlbumSearchTest.kt](../feature-l/src/test/java/com/client/xvideos/l/ui/screens/explorer/tab/albumSearch/L_ScreenAlbumSearchTest.kt)

Функции форматирования `Long.toPrettyCount()`, `toPrettyCount2()`, `toPrettyCount3()`, `toPrettyCountInt()` и логика преобразования секций поиска альбомов в фильтры `createAlbumSearchFilter` не имели выделенного тестового покрытия.

**Исправление:**
- Создан тестовый класс `ToPrettyCountTest` (8 тестов), проверяющий корректность строкового форматирования, диапазоны тысяч, миллионов, миллиардов, точность знаков после запятой и обработку отрицательных чисел.
- Функция `createAlbumSearchFilter` выделена как чистая `internal`-функция и покрыта 4 тестами в `L_ScreenAlbumSearchTest`, проверяющими маппинг разделов ("Manga", "Picture Sets", произвольные категории) и корректный тримминг поисковых строк.

---

## Сводка верификации

- `detekt`: 0 предупреждений и ошибок во всех модулях проекта (`:app`, `:core`, `:feature-l`, `:feature-r`, `:feature-x`).
- `testDebugUnitTest`: 140 задач выполнено, все юнит-тесты успешно пройдены (100%), включая новые наборы `ToPrettyCountTest` и `L_ScreenAlbumSearchTest`.
- `compileReleaseKotlin`: 61 задача выполнена, чистая сборка релизных Kotlin-артефактов без ошибок.
