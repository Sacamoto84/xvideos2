# Код-ревью xvideos — проход 3

Дата: 01.08.2026. Ветка `refactor/structure-cleanup`, база `ffd353d`.

Проход сфокусирован на одном классе дефектов, который во втором проходе был
описан, но исправлен лишь частично: **отмена корутины, которая доезжает до
пользователя как ошибка**. Плюс завершена начатая работа по атомарной замене
Compose-списков.

## Проверка

- `:app:compileDebugKotlin` — OK
- `:app:testDebugUnitTest` — 62 теста, 0 падений (было 58, добавлено 8, удалено 4 не было)
- `:app:detekt` — OK, новых нарушений сверх baseline нет

## Что исправлено

### 1. `CancellationException` глотался в 13 местах (P0)

Второй проход насчитал 17 таких мест; часть закрыли точечно, остальные остались.
Пересчёт по коду: broad-catch без проброса отмены нашёлся в 32 местах, из них
в корутинном контексте и с последствиями для пользователя — 13. Остальные 19
оказались либо ложными срабатываниями (`ApiClient.withAuth` — проброс есть, он
просто дальше 8 строк вверх), либо корректными (`closeAllSafely` глушит ошибки
закрытия намеренно; `LMediaPersist` уже делает `throw e` после уборки).

Исправлено — везде добавлен `catch (e: CancellationException) { throw e }`
перед общим catch, по образцу уже принятому в проекте (`ScreenRedProfileSM`):

| Файл | Что было видно пользователю |
|---|---|
| `P2pReceiveController.kt:114` | отмена scope → экран приёма показывал `ReceiveState.Error` |
| `P2pShareController.kt:186` | то же для `ShareState.Error` |
| `AlbumList.kt:174,237` | отмена уезжала как `Result.failure` → снекбар на уже другом экране |
| `LandingPageAlbumSearch.kt:22`, `LandingPageAlbumTag.kt:22` | то же |
| `Repository.kt:120,164,226` | то же, три ветки `openURI`/`postJsonValidated` |
| `AlbumTopHits.kt:36` | ловился весь `Throwable`, включая отмену |
| `getSearchResults.kt:41` | отмена возвращалась как `null` и трактовалась как «ничего не найдено» |
| `DownloadRed.kt:63` | отмена логировалась как «Ошибка при загрузке» |

Два места хуже прочих, потому что там отмена не просто путала лог, а не
останавливала работу:

- `R_SearchExplorer.kt:31` — catch стоял **внутри** `searchText.collect`.
  Отмена scope гасилась, сборщик оставался жив и продолжал крутиться на
  отменённой корутине.
- `R_Saved_Subscriptions.kt:94` — catch внутри `forEach` по авторам. После
  отмены цикл шёл дальше и дёргал сеть по всем оставшимся подпискам.

### 2. `runCatching` — та же дыра, 7 мест (P0)

`runCatching` ловит `Throwable`, то есть тоже проглатывает `CancellationException`.
Из 46 использований 7 оборачивают suspend-работу.

Добавлен `common/util/runCatchingCancellable.kt` — то же самое, но отмена всегда
летит наружу. Применён там, где последствия реальные:

- `LDownloadRecovery.kt:48,67` — массовое восстановление недокачанных файлов.
  Отмена помечала текущий файл как «не скачан» и цикл шёл дальше по **всему**
  списку кандидатов.
- `LMediaPersist.kt:336,347,354` — загрузка media и превью; `previewSources.forEach`
  так же не прерывался.
- `BlockRed.kt:50` — отмена показывала снекбар «Ошибка блокировки».
- `SavedX_Downloads.kt:134` — `resolveDirectVideoUrl` возвращал `null`, то есть
  отмена выглядела как «не удалось получить ссылку».

Оставлены как есть: `Repository.kt:317`, `AlbumInfo.kt:77`, `SavedX_Downloads.kt:118` —
внутри только парсинг/запись файла, suspend-вызовов нет.

Тесты: `RunCatchingCancellableTest` — 4 теста, включая проверку, что цикл
загрузки после `cancel()` действительно останавливается.

### 3. `getOrNull()!!` в обновлении ниш (P0)

`R_Saved_NichesCaches.refresh()`:

```kotlin
val res = redApi.explorer.getExplorerNiches(page = 1, count = 100).getOrNull()
val pages = res!!.pages.coerceAtLeast(1)
```

Ровно тот же паттерн, что во втором проходе чинили в Follow/Subscription: при
любой сетевой ошибке `getOrNull()` даёт `null`, `!!` кидает NPE, общий catch его
ловит и показывает снекбар **«Ошибка обновления java.lang.NullPointerException»**.
То же на `res2!!` внутри цикла по страницам.

Заменено на `getOrElse { error("...") }` с текстом настоящей причины. Семантика
обрыва сохранена намеренно: записать на диск неполный список ниш под видом
полного хуже, чем не обновиться.

### 4. Неатомарная замена Compose-списков — 20 мест (P1)

Второй проход починил `FileDB` инлайн и оставил 20 мест с `list.clear()` +
`list.addAll(...)` на `mutableStateListOf`. Между двумя записями Compose успевает
отрисовать пустой список.

Добавлен `common/util/replaceWith.kt` — общий хелпер с той же логикой, что была
захардкожена в `FileDB` (включая fallback, если вложенный mutable-снапшот
недоступен). `FileDB` переведён на него, 18 строк инлайна ушли.

Применён в: `SavedL_Collection` (5), `SavedL_Likes`, `AlbumTopHits`,
`AlbumPicsDetails`, `LazyRowPictureDetailsHost`, `R_Saved_Collection`,
`R_Saved_Likes`, `SearchTab`, `DashboardsPaginatedListScreen`, `FileDB` (2).

Тесты: `ReplaceWithTest` — 4 теста, главный проверяет через
`Snapshot.registerApplyObserver`, что подписчик видит **одну** публикацию сразу
с итоговым содержимым, а не промежуточный пустой список.

Не тронуто осознанно:

- `R_Saved_NichesCaches:70,121` — там `list` это обычный `mutableListOf`, а
  рекомпозиция держится на ручном счётчике `version++`. Хелпер не применим (см. п. 2 остатков).
- `SavedX_Favorites:64` — `mutableStateSetOf`, не список.
- 3 закомментированных места (`SearchTab:186,190`, `TagsPaginatedListScreen:35`).

Побочно вскрылось и исправлено:

- **`DashboardsPaginatedListScreen:80`** — `l.clear()` стоял **перед** сетевым
  вызовом (`l.addAll(openNew(pageIndex)...)`), то есть лента была пустой всё
  время запроса, а не мгновение. Загрузка вынесена вперёд, замена — после.
- **`L_ScreenSavedLikesTab:177-191`** — три функции повторяли `clear()`+`addAll()`
  вручную, хотя у `LazyRowPictureDetailsHost` уже есть
  `replaceFilteredPictures()` с проверкой `hasSameItems`. Переведены на неё,
  заодно ушёл мёртвый `if (...isNotEmpty()) ...clear()` (clear пустого списка —
  no-op) и висячее выражение `host.filteredPic`. То же в `ScreenAlbum:137-145`,
  там же удалён неиспользуемый `newFilteredNoAnimatedPics`.

### 5. `AndroidView` без `onRelease` (P1)

`ScreenX_VideoPlayerFullScreen.kt:127` создаёт `PlayerView` прямо в `factory` и
ставит ему `player = exo`. Плеер держит ссылку на view, view — на контекст
Activity, и ничто их не разрывало. Добавлен `onRelease = { it.player = null }`.

Второй `AndroidView` (`CMPlayer2.kt:109`) **не трогал**: там view приходит из
`rememberPlayerView`, у которого уже есть `DisposableEffect { playerView.player = null }`.
`onRelease` был бы дублированием.

### 6. Мёртвый `gendreIds.kt` (P2)

`l/model/gendreIds.kt` — 1077 строк, из которых объявлений **ноль**: файл
целиком это `package` + один KDoc-комментарий с дампом справочника жанров.
Удалён.

### 7. Мелочи

- `DownloadRed.kt:157` — `println` вместо Timber при чтении битого `.info`
  (последний остаток от чистки `printStackTrace` во втором проходе).
- `P2pShareController.kt:73` — `kotlinx.coroutines.CancellationException` по
  полному имени приведён к импорту, как в остальном файле.

## Что осталось (не делал)

Списком, по убыванию важности. Все пункты подтверждены по коду на 01.08.2026.

1. **R8 выключен** (`app/build.gradle:83`, `minifyEnabled = false`). Тянется с
   первого прохода. Блокер прежний: keep-правила держат почти всё
   (`kotlin.**`, `kotlinx.**`, `io.ktor.**`), а Gson-модели не защищены вовсе —
   после включения обфускации они молча перестанут парситься. Нужна ревизия
   `proguard-rules.pro` целиком, это отдельная задача с проверкой на устройстве.
2. **`R_Saved_NichesCaches.list` — `mutableListOf` вместо `mutableStateListOf`.**
   Рекомпозиция держится на ручном `version++`. Работает, но это ловушка: любой
   новый composable, который прочитает `list` и не прочитает `version`, просто
   не будет обновляться. Перевод на snapshot-список поведение только улучшит,
   но затрагивает всех читателей — отдельной задачей.
3. **204 `!!` по main.** Хот-споты: `ScreenRedProfile.kt` (19),
   `Repository.kt` (14), `AlbumList.kt` (13), `AndroidConnectivityObserver.kt` (12),
   `FileDB.kt` (11). Проверенного паттерна `getOrNull()` → `!!` больше не
   осталось (проверил скриптом — 0 совпадений), но остальные не аудировал.
4. **Один модуль** — 474 файла в `:app`. Любая правка перекомпилирует всё.
5. **`CMPlayer2.kt:103`** — `playerView.keepScreenOn = true` выставляется и
   никогда не снимается: экран не гаснет и на паузе. Не утечка, но UX-баг.
6. **Мусор в корне репозитория** — `CODE_REVIEW_REPORT.md`,
   `CODE_REVIEW_REPORT_NEW.md`, `DETAILED_CODE_REVIEW.md`,
   `PROJECT_CODE_ANALYSIS_REPORT.md`, `R8_Configuration_Analysis.md`,
   `project_flowchart.md`, `Описалово.graphml`, `query`. Все устарели и
   перекрыты `docs/`. Кандидаты на удаление.
7. Локализации нет, Material 2 и 3 в одних экранах, копипаста L↔R — без
   изменений со второго прохода.
