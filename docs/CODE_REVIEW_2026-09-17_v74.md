# Код-ревью xvideos — проход 95

> **Срез:** `3f3ad05` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `3f3ad05`. Затронуты модули `:core`, `:feature-r`. Выполнена комплексная очистка устаревших и ложных аннотаций `@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")` в экранах сохранённого Red (коллекции, загрузки, лайки, авторы, ниши), устранён избыточный контейнер `Scaffold` в `SubscriptionsTabContent`, удалены устаревшие подавления `@SuppressLint("UnusedBoxWithConstraintsScope")` и `@OptIn(ExperimentalMaterialApi::class)` в плеере, обеспечена безопасность форматирования времени `formatMinSec` с явной локалью `Locale.US` и защитой от отрицательных значений, вычищен мёртвый закомментированный код, скорректированы уровни логирования и добавлены юнит-тесты.

Линзы:
- `UI` / Устранение ложных и избыточных подавлений `@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")`, замена пустого `Scaffold` на `Box` в подписках ([R_Screen_CollectionTab.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_CollectionTab.kt), [R_Screen_Saved_DownloadTab.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_DownloadTab.kt), [R_Screen_Saved_LikesTab.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_LikesTab.kt), [R_Screen_Saved_SubscriptionsTab.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_SubscriptionsTab.kt), [R_Screen_CreatorsTab.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_CreatorsTab.kt), [SavedNichesTab.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/savedNiche/SavedNichesTab.kt), [SavedCollectionName.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/collection/SavedCollectionName.kt)).
- `C` / Безопасность строкового форматирования `formatMinSec` (`Locale.US`, `coerceAtLeast(0)`) и удаление подавления `@SuppressLint("DefaultLocale")` ([util.android.kt](../core/src/main/java/com/client/xvideos/common/videoplayer/util/util.android.kt)).
- `S` / Удаление устаревших подавлений `@SuppressLint("UnusedBoxWithConstraintsScope")`, `@OptIn(ExperimentalMaterialApi::class)`, `@OptIn(DelicateCoroutinesApi::class)` и неиспользуемых импортов ([VideoPlayerWithMenuContent.kt](../core/src/main/java/com/client/xvideos/common/videoplayer/ui/VideoPlayerWithMenuContent.kt), [PlayerSpeedButton.kt](../core/src/main/java/com/client/xvideos/common/videoplayer/ui/component/PlayerSpeedButton.kt), [SavedCollectionName.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/collection/SavedCollectionName.kt)).
- `D` / Удаление закомментированного мёртвого кода ([VideoPlayerWithMenuContent.kt](../core/src/main/java/com/client/xvideos/common/videoplayer/ui/VideoPlayerWithMenuContent.kt), [R_Screen_Saved_LikesTab.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_LikesTab.kt)).
- `L` / Перевод отладочных логов жизненного цикла и жестов в уровень `Timber.d` ([R_Screen_CollectionTab.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_CollectionTab.kt), [SavedCollectionName.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/collection/SavedCollectionName.kt), [VideoPlayerWithMenuContent.kt](../core/src/main/java/com/client/xvideos/common/videoplayer/ui/VideoPlayerWithMenuContent.kt)).
- `T` / Добавление модульных тестов форматирования продолжительности медиа `FormatMinSecTest` ([FormatMinSecTest.kt](../core/src/test/java/com/client/xvideos/common/videoplayer/util/FormatMinSecTest.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### UI124 — Ложные и устаревшие подавления UnusedMaterial3ScaffoldPaddingParameter в табах сохранённого Red. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_CollectionTab.kt:76](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_CollectionTab.kt#L76)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_DownloadTab.kt:85](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_DownloadTab.kt#L85)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_LikesTab.kt:57](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_LikesTab.kt#L57)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_SubscriptionsTab.kt:125](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_SubscriptionsTab.kt#L125)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_CreatorsTab.kt:96](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_CreatorsTab.kt#L96)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/savedNiche/SavedNichesTab.kt:66](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/savedNiche/SavedNichesTab.kt#L66)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/collection/SavedCollectionName.kt:64](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/collection/SavedCollectionName.kt#L64)

На экранах `R_Screen_Saved_DownloadTab`, `R_Screen_CreatorsTab`, `SavedNichesTab` и `SavedCollectionName` отступы `Scaffold` уже корректно использовались кодом (`padding.calculateTopPadding()` и `Modifier.padding(padding)`), однако на методах `Content()` оставались исторические аннотации `@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")`. В `R_Screen_CollectionTab` и `R_Screen_Saved_LikesTab` компонент `Scaffold` отсутствовал вовсе (контейнером служил `Box`), но аннотация подавления продолжала висеть. В `SubscriptionsTabContent` компонент `Scaffold` использовался без баров и кнопок исключительно для заливки фона.

**Исправление:**
- В `SubscriptionsTabContent` избыточный `Scaffold` заменён на легковесный `Box(modifier = Modifier.fillMaxSize().background(Theme.background))`, удалены импорт `Scaffold` и аннотация `@SuppressLint`.
- Во всех перечисленных экранах удалены ложные аннотации `@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")` и неиспользуемые импорты `android.annotation.SuppressLint`.

---

### C16 — Потенциальная локалезависимость и некорректная обработка отрицательных значений в formatMinSec. Средняя.

[core/src/main/java/com/client/xvideos/common/videoplayer/util/util.android.kt:18-35](../core/src/main/java/com/client/xvideos/common/videoplayer/util/util.android.kt#L18)

Функция `formatMinSec(value: Int)` использовала системный `String.format(...)` без явного указания `Locale.US`, что маскировалось подавлением `@SuppressLint("DefaultLocale")`. В локалях с альтернативными цифровыми символами (арабский, фарси и др.) строковое представление таймкодов плеера могло искажаться. Кроме того, при отрицательных значениях (например, сбойных таймкодах или неинициализированной длительности) формировались некорректные строки.

**Исправление:**
- Значение времени санитизировано с помощью `value.coerceAtLeast(0)`.
- Все вызовы форматирования переведены на явную `Locale.US`: `String.format(Locale.US, ...)`.
- Удалены аннотация `@SuppressLint("DefaultLocale")` и неиспользуемый импорт `android.annotation.SuppressLint`.

---

### S46 — Устаревшие подавления аннотаций в компонентах видеоплеера и коллекций. Низкая.

[core/src/main/java/com/client/xvideos/common/videoplayer/ui/VideoPlayerWithMenuContent.kt:34-35](../core/src/main/java/com/client/xvideos/common/videoplayer/ui/VideoPlayerWithMenuContent.kt#L34)
[core/src/main/java/com/client/xvideos/common/videoplayer/ui/component/PlayerSpeedButton.kt:39](../core/src/main/java/com/client/xvideos/common/videoplayer/ui/component/PlayerSpeedButton.kt#L39)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/collection/SavedCollectionName.kt:63](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/collection/SavedCollectionName.kt#L63)

В `VideoPlayerWithMenuContent` присутствовали неиспользуемые аннотации `@SuppressLint("UnusedBoxWithConstraintsScope")` (в файле нет `BoxWithConstraints`) и `@OptIn(ExperimentalMaterialApi::class)`. В `PlayerSpeedButton` аннотация `@SuppressLint("UnusedBoxWithConstraintsScope")` являлась устаревшей. В `SavedCollectionName` сохранялась устаревшая аннотация `@OptIn(DelicateCoroutinesApi::class)`.

**Исправление:**
- Удалены устаревшие подавления аннотаций и сопутствующие импорты `SuppressLint`, `ExperimentalMaterialApi` и `DelicateCoroutinesApi`.
- Исправлена опечатка в комментарии в `SavedCollectionName.kt`.

---

### D16 — Закомментированный отладочный код и устаревшие поля. Низкая.

[core/src/main/java/com/client/xvideos/common/videoplayer/ui/VideoPlayerWithMenuContent.kt:89-93](../core/src/main/java/com/client/xvideos/common/videoplayer/ui/VideoPlayerWithMenuContent.kt#L89)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_LikesTab.kt:54-55, 86, 89](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_LikesTab.kt#L54)

В `VideoPlayerWithMenuContent` сохранялся закомментированный блок `if (playerHost.poster) { ... }`. В `R_Screen_Saved_LikesTab` оставались закомментированные строчки старых настроек колонок (`columnSelect`) и навигации (`currentIndexGoto`).

**Исправление:**
- Полностью удалены закомментированные фрагменты кода.

---

### L29 — Неверные уровни логирования событий жизненного цикла. Низкая.

[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_CollectionTab.kt:89](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_CollectionTab.kt#L89)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/collection/SavedCollectionName.kt:75](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/collection/SavedCollectionName.kt#L75)
[core/src/main/java/com/client/xvideos/common/videoplayer/ui/VideoPlayerWithMenuContent.kt:51](../core/src/main/java/com/client/xvideos/common/videoplayer/ui/VideoPlayerWithMenuContent.kt#L51)

Отладочные логи `BackHandler` и рекомпозиции выводились в информационный уровень `Timber.i` с отладочными префиксами `iii` и `@@@`.

**Исправление:**
- Логи переведены в уровень `Timber.d`, устранены отладочные префиксы.

---

### T38 — Модульное тестирование форматирования продолжительности formatMinSec. Средняя.

[core/src/test/java/com/client/xvideos/common/videoplayer/util/FormatMinSecTest.kt](../core/src/test/java/com/client/xvideos/common/videoplayer/util/FormatMinSecTest.kt)

Функция форматирования таймкодов `formatMinSec` и соответствующее расширение `Int.formatMinSec()` не имели модульных тестов.

**Исправление:**
- Создан тестовый класс `FormatMinSecTest` (5 тестов), проверяющий нулевую продолжительность ("00:00"), санитизацию отрицательных значений, ведущие нули секунд (< 1 мин), формат минут и секунд (< 1 ч) и корректное разделение часов, минут и секунд (>= 1 ч), включая граничные значения (60, 3599, 3600, 360000).

---

## Сводка верификации

- `:core:testDebugUnitTest`: выполнено успешно, все юнит-тесты зелёные (100%), включая новый набор `FormatMinSecTest`.
- `:feature-r:testDebugUnitTest`: выполнено успешно, все тесты модуля feature-r пройдены.
