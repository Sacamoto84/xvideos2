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

## Добор: остаток списка, кроме R8 и модулей

Второй заход по этому же ревью. Пункты 2, 3, 5 и 6 из «что осталось» закрыты.

### 8. Аудит `!!` — цифра «204» была неверной

Все три прошлых прохода писали про «215 / 204 `!!`». Это **артефакт подсчёта
через grep**: в проекте принято префиксовать логи строкой `"!!! ..."`, и
`grep -o '!!'` считал их наравне с оператором. Разбор с вырезанием строковых
литералов и комментариев даёт:

| | |
|---|---|
| всего вхождений `!!` | 204 |
| из них внутри строк логов | 162 |
| в закомментированном коде | 23 |
| **настоящих not-null assertion** | **19**, из них 6 — в многострочных логах → **13** |

Из 13 безопасны по построению и оставлены: `getSystemService<ConnectivityManager>()!!`
(сервис есть всегда), `result.exceptionOrNull()!!` под `if (isFailure)`,
`sharedPrefs.getString(name, default)!!` с non-null дефолтом (×2),
`inputStream!!.read()` внутри цикла чтения, `activeController!!` под проверкой
в том же выражении.

Исправлены четыре, где падение реально:

- **`DownloadTask.kt:330`** — `inputStream!!.close()` внутри `catch (e: IOException)`.
  Если загрузка сорвалась до открытия потока, это NPE, а **NPE не IOException**:
  он пролетал мимо собственного catch и валил `closeAllSafely` целиком, вместе с
  недоделанной уборкой. Стало `inputStream?.close()`.
- **`TikTokRow1.kt:87`** — `videoItem.urls.poster!!`, а `poster` у redgifs
  опционален (`String? = null` в `URL1`). Первый же ролик без постера — краш
  в пейджере. Стало `poster ?: thumbnail`, как уже сделано в
  `RedUrlVideoImageAndLongClick` и `...TikTok`.
- **`R_Screen_Root.kt:144`** — `collectionItemGifInfo!!` вообще без проверки:
  диалог открыт, элемент успели сбросить — краш по тапу на коллекцию.
- **`L_ScreenSavedLikesTab.kt:174`** — `item.url_to_original!!` при удалении,
  а поле опционально (именно поэтому ключи LazyLayout строятся с фолбэком).

Плюс три места, где `!!` стоял под корректной проверкой, но читал значение
повторным вызовом (`if (savedRed() != null) ... savedRed()!!`): переписаны на
одно чтение в локальную переменную — `R_ScreenNichesTab.kt:296`,
`R_Screen_Root.kt:121`, `SearchTab.kt:107`.

### 9. `R_Saved_NichesCaches.list` → snapshot-список

`list` был обычным `mutableListOf`, а рекомпозиция держалась на ручном
`version++`. Переведён на `mutableStateListOf` + `replaceWith`.

`version` **оставлен намеренно**: подписку теперь даёт сам список, но `version`
работает ключом `remember` для фильтрации и сортировки в `R_ScreenNichesTab.kt:131` —
гонять её на каждое чтение дорого.

Поле `size` (дублировавшее `list.size` ради Compose) удалено, читатель в
`AppSettingsScreen.kt:291` переведён на `list.size`.

### 10. `CMPlayer2` держал экран включённым всегда

`keepScreenOn = true` выставлялся один раз в `LaunchedEffect` и не снимался
никогда — устройство не засыпало ни на паузе, ни после ухода с экрана.
Заменено на `DisposableEffect(playerView, isPause)` с `keepScreenOn = !isPause`
и сбросом в `onDispose`.

### 11. Мусор в корне удалён

`CODE_REVIEW_REPORT.md`, `CODE_REVIEW_REPORT_NEW.md`, `DETAILED_CODE_REVIEW.md`,
`PROJECT_CODE_ANALYSIS_REPORT.md`, `R8_Configuration_Analysis.md`,
`project_flowchart.md`, `Описалово.graphml`, `query` — 1500+ строк.

`project_flowchart.md` был не просто устаревшим, а неверным: описывал Permission
Flow с экраном запроса разрешений, которых нет с коммита `ab1f0b6`.
`R8_Configuration_Analysis.md` — выход скилла `skills/performance/r8-analyzer`,
он его пересоздаёт при следующем запуске. История в git сохранена.

### 12. R8 включён

`minifyEnabled = true`, `proguard-rules.pro` переписан. Размер APK:

| | |
|---|---|
| без R8 | 90 530 680 Б (86.3 МБ) |
| с R8 | 18 424 176 Б (17.6 МБ) |

Переименовано 890 классов приложения, вырезано 2249.

**`shrinkResources` намеренно оставлен выключенным** — ресурсы режутся по
другим правилам, и мешать два независимых источника отказов в одном изменении
не стоит. Это следующий шаг.

#### Что было не так со старым файлом правил

- `-keep class kotlin.** { *; }`, `kotlinx.**`, `androidx.compose.** { *; }`,
  `coil.** { *; }`, `dagger.** { *; }`, `androidx.datastore.**` — дублировали
  consumer-правила, которые библиотеки везут внутри артефактов. Не чинили
  ничего, но выключали шринк на всей библиотеке. Проверено: `kotlin-reflect`
  в зависимостях нет, datastore не используется ни одной строкой.
- Правила и `-dontwarn` на пакеты `com.client.common.**`, `com.redgifs.**`,
  `com.example.**` — этих пакетов в проекте не существует, остатки прежней
  раскладки.
- Два десятка `-dontwarn` на собственные классы приложения.

#### Главное: `@SerializedName` защищает не всё

Общее правило вида «сохранить классы с полями `@SerializedName`» выглядит
достаточным, но **шесть типов, которые Gson сопоставляет по именам полей, этой
аннотации не имеют** и под правило не попадали:

| Тип | Что бы сломалось |
|---|---|
| `common.p2p.P2pManifest`, `P2pManifestFile` | протокол P2P между телефонами |
| `l.featured.saved.LSavedLikeMetadata`, `LSavedLikePreview` | метаданные сохранённых лайков |
| `l.featured.saved.LCollectionConfig` | конфиг коллекции |
| `l.net.LAlbumBundleCache` | дисковый кэш альбома |
| `x.model.ItemsX` | `.info` файлы загрузок X |
| `l.model.AlbumListFilter` | фильтр в запросах |

Обфускация не стабильна между сборками. Без явных keep обновление приложения
сделало бы нечитаемым всё, что записала предыдущая версия, а два телефона с
разными сборками перестали бы понимать друг друга по P2P. Отказ был бы
**молчаливым**: JSON разбирается, поля остаются null, в логе ни строки.

Добавлены явные keep для этих шести плюс пакеты моделей целиком
(`l.model`, `r.model`, `x.model`, `common.collectionDB.model`) — страховка от
той же ошибки в модели, которую добавят потом.

Проверено по `mapping.txt` и `usage.txt`: классы и геттеры мапятся сами в
себя, из защищённых типов вырезан только пустой `<clinit>`.

Отдельно добавлены правила для kotlinx.serialization (`x/search/model`,
`l/model/UserProfile`) и `-keepattributes SourceFile,LineNumberTable` — R8
инлайнит и склеивает методы, без этого атрибута номера строк в стектрейсах
теряются даже без обфускации.

#### Первый прогон на устройстве: краш на старте (исправлено)

Релиз падал ещё до первого экрана:

```
java.lang.RuntimeException: Unable to get provider androidx.startup.InitializationProvider:
  Failed to create an instance of class androidx.work.impl.WorkDatabase
    at androidx.work.WorkManagerInitializer.b(SourceFile:66)
```

**Корень.** Room ищет сгенерированную реализацию базы по имени и создаёт её
рефлексией: `Class.forName("<База>_Impl").getDeclaredConstructor().newInstance()`.
R8 этого вызова не видит. Он заключил, что `WorkDatabase_Impl` никогда не
инстанцируется, и вырезал у него конструктор без аргументов, а следом — как
ставшие недостижимыми — `createInvalidationTracker()` и `clearAllTables()`.
Подтверждено по `usage.txt`: в списке удалённого стояло ровно
`public void <init>()`.

Почему не спасло consumer-правило: `androidx.room:room-runtime:2.5.0`
(приходит транзитивно через `androidx.work`, самим приложением WorkManager не
используется) везёт

```
-keep class * extends androidx.room.RoomDatabase
```

— без блока членов. В семантике R8 это сохраняет имя класса, но не его члены.
В Room 2.6+ правило починили, дописав `{ <init>(); }`. Эта же версия
добавлена в `proguard-rules.pro`.

Проверено после пересборки: `<init>()` и `createInvalidationTracker()` ушли из
списка удалённого, других Room-`_Impl` с потерянным конструктором нет.
`rawWorkInfoDao()` и `clearAllTables()` остаются вырезанными, но **с обеих
сторон** — и абстрактное объявление, и переопределение, — поэтому
`AbstractMethodError` невозможен.

Проверялась и вторая гипотеза — Tink под `androidx.security:security-crypto`,
на котором лежат пароли Luscious. **Отклонена:** `tink-android:1.8.0` везёт
правило для своего shaded-protobuf (`GeneratedMessageLite`), consumer-правила
`security-crypto` на месте.

#### Обязательно перед выпуском

Зелёная сборка здесь **не доказывает работоспособность** — Gson и Ktor ломаются
в рантайме, а не на компиляции. Нужен прогон на устройстве по путям, которые
задействуют рефлексию:

1. L: список альбомов, открытие альбома, сохранение в коллекцию, лайки;
2. R: лента gifs, ниши (обновление кэша), поиск, профиль;
3. X: поиск (там kotlinx.serialization), загрузка, «в галерею»;
4. загрузка файла и чтение `.info` после перезапуска;
5. P2P между двумя устройствами;
6. бэкап/восстановление.

Архивировать `mapping.txt` **не требуется**, пока выключена обфускация
(см. п. 13). Если включите обратно — требование возвращается.

### 13. Обфускация выключена по решению владельца

`-dontobfuscate` в `proguard-rules.pro`. R8 продолжает вырезать неиспользуемый
код, но ничего не переименовывает.

| Конфигурация | APK |
|---|---|
| без R8 | 90 530 680 Б (86.3 МБ) |
| R8, обфускация выключена | 19 473 075 Б (18.6 МБ) |
| R8 полностью | 18 424 176 Б (17.6 МБ) |

То есть переименование даёт последний 1 МБ из 68 сэкономленных. Взамен
стектрейсы из релиза читаются как есть, `mapping.txt` для расшифровки не нужен
и архивировать его на каждый выпуск не надо.

Проверено: переименовано 0 классов приложения, `ExpandMenuType` мапится сам в
себя.

**Keep-правила остаются обязательными.** Они защищают от вырезания, а не от
переименования. Проверено на этой сборке: `<init>()` у `WorkDatabase_Impl` на
месте благодаря правилу Room, у защищённых DTO вырезан только пустой
`<clinit>`. Без правила Room краш на старте повторился бы и без обфускации.

Включить обратно — убрать строку `-dontobfuscate` и вернуть архивирование
`mapping.txt` на каждую выпущенную сборку.

## Что осталось

1. **Один модуль** — 473 файла в `:app`. Любая правка перекомпилирует всё.
   Распил на `common` / `l` / `r` / `x` — отдельная ветка: Hilt-графы,
   циклические зависимости между разделами.
2. **`shrinkResources`** — не включён (см. п. 12).
3. **Сужение keep для Ktor** — `io.ktor.**` и `kotlinx.coroutines.**` пока
   держатся целиком: цена ошибки там отказ всей сети. Сужать после того, как
   текущая конфигурация подтвердится на устройстве.
3. Локализации нет, Material 2 и 3 в одних экранах, копипаста L↔R — без
   изменений со второго прохода.
