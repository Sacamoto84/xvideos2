# Код-ревью xvideos — проход 2

> **Срез:** `13c3bac` · **Статус:** закрыт · **Индекс:** [все документы](README.md)

Срез: `master` @ `13c3bac`, один модуль `:app`, 464 Kotlin-файла, 54 601 строка.

Отличия от [первого прохода](CODE_REVIEW_2026-07-30.md): применены правки первого ревью, обновлён version catalog (Kotlin `2.3.21` → `2.4.10`, AGP `9.2.1` → `9.3.1`, Hilt, Coil, Ktor, Compose BOM), `compose-stability-analyzer` поднят `0.8.0` → `0.12.0` — из-за этого чинилось падение компилятора (`ClassCastException` в `StabilityAnalyzerPluginRegistrar`: плагин 0.8.0 не знал про изменившийся в Kotlin 2.4 `FirExtensionRegistrarAdapter`).

> **Статус:** п.1, п.2, п.7 и п.3 исправлены. Проверка после правок: `assembleDebug`, `assembleRelease`, `testDebugUnitTest`, `lintDebug`, `lintRelease` — все BUILD SUCCESSFUL, lint без изменений (0 error / 156 warning). Контрольные счётчики: живых `printStackTrace` 15 → **0**; `catch(Exception)` в корутинах, показывающих ошибку пользователю, без обработки отмены 17 → **0**. Попутно найден и починен невалидный SQL в `AppDbHelper.empty()` — см. п.7.

## Проверка

```
:app:assembleDebug  :app:assembleRelease
:app:testDebugUnitTest
:app:lintDebug  :app:lintRelease
```
Всё — `BUILD SUCCESSFUL`. Lint: **0 error**, 156 warning, 9 hint (в первом проходе было 2 error / 180 warning; 21 `GradleDependency` ушли вместе с апдейтом зависимостей).

| Метрика | Было (проход 1) | Стало |
|---|---|---|
| Kotlin-файлов / строк | 470 / 54 800 | 464 / 54 601 |
| Gradle-модулей | 1 | 1 |
| Lint error / warning | 2 / 180 | **0** / 156 |
| Unit-тестов | 15 | 15 |
| ScreenModel | 50 | 36 |
| `Thread.sleep` в UI-пути | 1 | **0** |

---

## P0 — критично

### 1. `!!` на результате сети + `catch { printStackTrace() }` — Follow и Subscription молча не работают

`r/common/expand_menu_video/DropdownMenuItem_Follow.kt:36-38`
`r/common/expand_menu_video/DropdownMenuItem_Subscription.kt:41-43`

```kotlin
try {
    val a = redApi.invoke().readCreator(item.userName).getOrNull()
    savedRed.invoke().creators.add(a!!)
} catch (e: Exception) { e.printStackTrace() }
```

`readCreator` возвращает `Result<UserInfo>`. При любой сетевой ошибке `getOrNull()` даёт `null`, `a!!` кидает `NullPointerException`, а `catch (e: Exception)` его глотает. `printStackTrace()` пишет в stderr мимо Timber, поэтому в отфильтрованном логе не видно ничего.

**Сценарий:** нет сети (или 5xx от redgifs) → пользователь жмёт Follow → ничего не происходит. Ни изменения иконки, ни снекбара, ни строки в логе. Тот же путь у Subscription.

**Фикс:** `readCreator(...).onSuccess { creators.add(it) }.onFailure { SnackBar.error(...) }`, `!!` убрать, `printStackTrace()` заменить на `Timber.e(e, ...)`.

### 2. `catch (e: Exception)` глотает `CancellationException`

50 таких блоков в файлах с корутинами; из них **17** показывают ошибку пользователю или пишут state.

Самый наглядный — `l/ui/screens/screenAlbumList/ScreenAlbumListSM.kt:202`:

```kotlin
screenModelScope.launch(Dispatchers.IO) {
    try {
        val a = luscious.getAlbumList(page + 1, filter.value)
        ...
    } catch (e: Exception) {
        Timber.e(e, "!!! eee Error loading page $page")
        SnackBar.error(e.message ?: "Error loading page $page")
        bigList.put(page, AlbumListImplInfoAndListAndStatus(null, StatusAlbumList.BUSY))
    } finally { _isRequest.value = false }
}
```

**Сценарий:** пользователь листает список альбомов, страница подгружается, пользователь жмёт Back. Voyager уничтожает ScreenModel → `screenModelScope` отменяется → `getAlbumList` бросает `CancellationException` → он попадает в `catch (e: Exception)` → **снекбар с текстом отмены корутины всплывает уже на предыдущем экране**.

Остальные 16 мест с тем же паттерном: `SavedL_Collection.kt:64,158`, `SavedL_Likes.kt:93`, `Repository.kt:196`, `ExpandMenuVM.kt:133,165`, `ScreenLAlbumSM.kt:130`, `ScreenAlbumListSM.kt:110,154`, `R_Saved_NichesCaches.kt:86`, `R_SearchNiches.kt:36,53`, `GallerySaver.kt:42`, `ScreenRedProfileSM.kt:132`, `AndroidConnectivityObserver.kt:100`, `KtorRequestHandler.kt:136`.

**Фикс:** везде добавить `catch (e: CancellationException) { throw e }` перед общим catch. Правильный образец в проекте уже есть — `ScreenRedProfileSM.loadNextPage`.

### 3. Пароль Luscious по-прежнему в открытом виде — **исправлено**

Было: `common/settings/Settings.kt:98-99` — `SettingElementString` поверх общего файла настроек, пишется в `l/ui/screens/L_ScreenLogin.kt:88-89`.

Стало (после подключения `androidx.security:security-crypto 1.1.0`):

- `common/settings/SecureCredentialStore.kt` — отдельный файл `secure_credentials`, зашифрованный `EncryptedSharedPreferences` с мастер-ключом из Android Keystore (AES256_SIV для ключей, AES256_GCM для значений).
- `common/settings/element/SettingElementSecureString.kt` — элемент с тем же публичным API (`field: StateFlow<String>`, `setValue`), поэтому ни одно из 10 мест использования не менялось.
- `Settings.init(prefs, context)` переносит существующие `l_login`/`l_pass` из общего файла в зашифрованный и удаляет открытые копии. Миграция одноразовая: после неё ключей в обычных настройках нет.

Побочно: `SettingElementString.setValue` содержал `println("!!! setValue $value ...")` — то есть **пароль печатался в stdout при каждом сохранении**. Отладочный `println` удалён.

Известные ограничения, зафиксированные сознательно:

- `MasterKey` и `EncryptedSharedPreferences` помечены `@Deprecated` в security-crypto 1.1.0 (AndroidX свернула библиотеку, замены внутри неё нет). Работают; в файле стоит точечный `@Suppress("DEPRECATION")` с объяснением.
- Если Keystore недоступен (Compose Preview, экзотическая прошивка) или keyset повреждён, хранилище пересоздаётся, а при полном отказе секрет живёт только в памяти процесса. Пользователь введёт пароль заново — это лучше, чем запись открытым текстом.
- `SecureCredentialStore.createOrNull` вызывается из `App.onCreate` на main-потоке; первая генерация мастер-ключа в Keystore добавляет к холодному старту десятки миллисекунд. Ленивую инициализацию не делал: миграция должна отработать до первого чтения `l_pass`, а ленивый вариант даёт рекурсию между `securePref` и элементами.

### 4. R8 по-прежнему выключен

`app/build.gradle:66` — `minifyEnabled = false`. Подпись и `checkReleaseBuilds` в первом проходе починены, но релиз всё ещё собирается без обфускации и без shrink. Блокер — keep-правила: `-keep class kotlin.**`, `kotlinx.**`, `io.ktor.** { *; }` держат почти всё, а Gson-модели (`GifsInfo`, `NichesInfo`, `AlbumDetails`, `UserInfo`, `ItemsX`) не защищены вовсе.

---

### 4a. Неуникальные ключи в LazyLayout — краш при скролле альбома — **исправлено**

Найдено не ревью, а на устройстве: падение при прокрутке альбома 594392 (411 картинок).

```
java.lang.IllegalArgumentException: Key "https://ah-img.luscious.net/bugha/594392/
9222178551_01KBSTP26AFFCAWT1BS6XZXF1T.1680x0.jpg" was already used.
  at androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridMeasureKt.measure
```

У `PicsDetails` нет поля `id` — единственный идентификатор это `url_to_original`, причём `AlbumPicsDetails.normalizePictureUrls` подменяет его на `lBestThumbnailImageUrl()`. Одна и та же картинка, загруженная в альбом дважды, даёт две записи с идентичным URL, и `key = { index, item -> item.url_to_original ?: index }` возвращает один ключ для двух элементов. Фолбэк `?: index` закрывал только `null`, но не дубликаты.

Исправлено в четырёх местах одного флоу — ключ стал `"$url#$index"`:
`L_LazyRowPictureDetails.kt:137` (место падения), `L_FullScreenImage.kt` — `VerticalPager`, `HorizontalPager` и лента миниатюр (упали бы ровно так же при открытии этого альбома на весь экран). Индекс безопасен: страницы дописываются в хвост, порядок стабилен, идентичность уже показанных элементов сохраняется. Дедупликация не подходит — если картинка в альбоме действительно дважды, её и надо показать дважды.

Заодно укреплён `CustomBasicTextFieldContent.kt:160` (`key = { it.text }`): `R_SearchNiches` дедуплицирует выдачу сам, а `R_SearchExplorer` отдаёт ответ API как есть.

**Осталось непроверенным.** Тот же класс риска у списков, которые пагинируются с сервера и ключуются полем ответа: `R_ScreenNichesTab.kt:293` и `ScreenNiche.kt:263,285` (`key = { it.id }`). Если страницы когда-нибудь перекроются, падение будет идентичным. Не трогал — свидетельств дублей там нет.

## P1 — важно

### 5. Неатомарная замена Compose-списков — осталось 20 мест

`FileDB` в первом проходе починен (`Snapshot.withMutableSnapshot`), но тот же паттерн живёт ещё в 20 местах: `list.clear()` и следом `list.addAll(...)` на `mutableStateListOf`. Между двумя вызовами Compose успевает отрисовать пустой список — на длинных лентах это видимое мигание, а два параллельных обновления могут переплестись.

`SavedL_Collection.kt:70,164,177,193,328`, `SavedL_Likes.kt:99`, `AlbumPicsDetails.kt:241`, `AlbumTopHits.kt:42`, `LazyRowPictureDetailsHost.kt:44`, `L_ScreenSavedLikesTab.kt:178,184,189`, `ScreenAlbum.kt:140,143`, `R_Saved_Collection.kt:63`, `R_Saved_Likes.kt:55`, `R_Saved_NichesCaches.kt:69,118`, `SearchTab.kt:181`, `SavedX_Favorites.kt:64`, `DashboardsPaginatedListScreen.kt:80`.

Дешевле всего вынести хелпер рядом с `FileDB`:

```kotlin
fun <T> SnapshotStateList<T>.replaceAll(items: List<T>) =
    Snapshot.withMutableSnapshot { clear(); addAll(items) }
```

### 6. `MANAGE_EXTERNAL_STORAGE` + мёртвый SAF-слой

`AndroidManifest.xml:16-18` — полный доступ к хранилищу, блокер публикации в Play без обоснования. При этом `common/storage/SafFileSystem.kt` вырос до **16 KB** и по-прежнему не используется ни одной строкой. Либо доводить SAF-путь и уходить от разрешения, либо удалить файл — сейчас это самый большой кусок мёртвого кода в проекте.

### 7. `printStackTrace` вместо Timber — 15 живых мест — **исправлено**

`common/kdownloader/database/AppDbHelper.kt` (5), `common/kdownloader/internal/DownloadTask.kt` (5), `common/AppPath.kt` (2), два dropdown-меню из п.1. Ошибки БД загрузчика и работы с путями уходили в stderr мимо Timber: в логе, отфильтрованном по тегу приложения, их не было видно вообще.

Всё заменено на `Timber.e(e, ...)` с указанием операции.

**Что это сразу вскрыло.** `AppDbHelper.empty()` выполнял

```kotlin
db.execSQL("DELETE * FROM " + TABLE_NAME)
```

`DELETE * FROM` — невалидный SQLite (звёздочка в `DELETE` не допускается). Вызов всегда кидал `SQLiteException`, а `printStackTrace()` его гасил. Значит `DownloadDispatchers.cancelAll()` (`:85`) **никогда не очищал таблицу `downloads`** — записи об отменённых загрузках копились в БД неограниченно, и ни одной строки в логе об этом не было. Исправлено на `DELETE FROM`.

### 8. WebView не уничтожается, `WebScreen` — мёртвый

> **Закрыто целиком (проверено 2026-08-04).** `WebScreen.kt` из проекта удалён,
> `WebView` не используется нигде — вместе с предупреждением
> `SetJavaScriptEnabled` это неактуально.
>
> `onRelease` разобран в [проходе 3](CODE_REVIEW_2026-08-01.md), п. 5: добавлен
> в `ScreenX_VideoPlayerFullScreen.kt`; в `CMPlayer2.kt` намеренно не нужен —
> `PlayerView` там приходит из `rememberPlayerView` с собственным
> `DisposableEffect { playerView.player = null }`. В
> [проходе 4](CODE_REVIEW_2026-08-04.md) этот пункт был ошибочно переоткрыт
> из-за грепа с узким окном контекста, разбор ошибки — там же.

`WebScreen.kt` целиком не используется (ссылки только на самого себя), но именно он даёт lint-предупреждение `SetJavaScriptEnabled`: JS + DOM storage + сторонние cookies включены.

Шире: в проекте 3 вызова `AndroidView(...)` (`WebScreen.kt:21`, `CMPlayer2.kt:109`, `ScreenX_VideoPlayerFullScreen.kt:127`) и **ни одного `onRelease`**. Для `WebView` это утечка (WebView держит контекст и свой рендер-процесс); для `PlayerView` менее критично, но освобождение всё равно стоит делать явно.

**Фикс:** `WebScreen.kt` удалить; в оставшихся `AndroidView` добавить `onRelease`.

---

## P2 — архитектура

### 9. По-прежнему один модуль

464 файла, 54.6k строк, `include ':app'`. Четыре независимых мира: `common/` 143 файла (458 KB), `r/` 136 (570 KB), `l/` 102 (583 KB), `x/` 68 (299 KB). Любая правка перекомпилирует всё. Заготовка `//implementation project(':core-player')` так и висит закомментированной.

### 10. `gendreIds.kt` — 38 KB закомментированного JSON

`l/model/gendreIds.kt`: 1077 строк, из них **1064 — комментарий**, кода 2 строки, файл не используется нигде. Это дамп справочника жанров Luscious, вставленный в исходник. Место такому — в `res/raw/` или в тестовых фикстурах, если он вообще нужен.

### 11. Копипаста L↔R

14 пар файлов с одинаковыми именами (было 15): пять `DropdownMenuItem_*`, `CollectionsGrid`, `ScreenSaved`, `TabRow`, `Theme`, `UrlImage`, `useCaseShareFile`, `useCaseShareGifs`, `AlbumListFilter`, `ScreenFavoritesSM`.

### 12. Тройная реализация одного хелпера

`r/ui/profile/rememberVisibleRangePercentIgnoringFirstNForGrid.kt` — 22 KB, три композабла (`...ForGrid`, `...ForLazyColumn`, `...ForLazyStaggeredGrid`), используются 5 / 8 / 2 раза. Между версиями Grid и LazyColumn различается 26 строк из 117 — остальные 78 % идентичны. Логика вычисления видимого диапазона просится в одну функцию, параметризованную доступом к `layoutInfo`.

### 13. Compose-стабильность не проработана

183 data-класса, 101 свойство типа `List<...>`, `@Immutable` — 4, `@Stable` — 4, файла конфигурации стабильности нет. Плагин `stability-analyzer` подключён (теперь 0.12.0), но его вывод, судя по всему, никто не смотрит. Для приложения, где основной UI — длинные ленты видео, это самая дешёвая доступная оптимизация.

### 14. Локализации нет

680 кириллических литералов в 127 файлах, `strings.xml` — 1 запись. Языки в UI перемешаны (`"Like"` рядом с `"Группа добавлена"`).

### 15. Material 2 и Material 3 в одних экранах

26 предупреждений `UsingMaterialAndMaterial3Libraries`. Разные темы, разные `Scaffold`, разные `Surface` — отсюда же обе ошибки с игнорированием padding, исправленные в первом проходе.

---

## P3 — гигиена

### 16. Мёртвый код — 21 объявление, 8 файлов целиком

Целиком мёртвые файлы (~42 KB): `SafFileSystem.kt` (16 KB), `RedUrlVideoImageAndLongClickTikTok.kt` (6 KB), `ScreenLRootBottomNavigator.kt` (5 KB), `AnimatedZoomLayout.kt` (5 KB), `RedProfileTile.kt` (5 KB), `ScreenRedManageBlock.kt` (3 KB), `ScreenK.kt` (1 KB), `blockGetGifsByUserNameAsListString.kt` (1 KB). Плюс `WebScreen.kt` (п.8) и `gendreIds.kt` (п.10) — всего около **98 KB**.

Отдельные мёртвые функции: `getAlbumListGraphQL:153`, `NicheProfile:39`, `VerticalScrollbar1` / `VerticalScrollbar2` (две мёртвые реализации скроллбара), `applyAudioTrackSelection`, `applySubTitleTrackSelection`, `lMediaDownloadHeaders`, `lMediaUserAgent`, `writeCollectionMetadata`, `LandscapeOrientation`, `deleteFile`, `AppNetworkSpeedMonitor`, `CurrentTimeText`.

### 17. Lint — 156 warning

| Кол-во | Issue |
|---|---|
| 63 | `UnusedResources` — мёртвые ресурсы едут в APK |
| 32 | `PrivateResource` — internal-ресурсы библиотек, сломаются при их обновлении |
| 26 | `UsingMaterialAndMaterial3Libraries` |
| 9 | `AutoboxingStateCreation` — `mutableStateOf(0f)` вместо `mutableFloatStateOf` в `CustomSeekBar`, `MediaPlayerHost`, `UrlVideoLite` |
| 6 | `ModifierParameter` |
| 6 | `ObsoleteSdkInt` — проверки ниже `minSdk = 26` |
| 5 | `ConfigurationScreenWidthHeight` |
| 2 | `LogNotTimber`, `LockedOrientationActivity`, `DiscouragedApi` |

### 18. Тесты

15 файлов, покрывают `common/p2p` (10), `common/zip`, `r/common/downloader`. На 36 ScreenModel — ноль тестов, `androidTest` — ноль. Инфраструктура объявлена в каталоге (`uiautomator`, `benchmarkMacroJunit4`, compose-bom для тестов) и не используется.

Самое окупаемое: `FileDB` (атомарность записи, поведение на битом JSON), `ApiClient.withAuth` (перелогин по 401), `AppLockRepository` (backoff).

### 19. 215 `!!`

Из них разобранный в п.1 — реально стреляющий. Отдельно `L_ScreenSavedLikesTab.kt:174` — `item.url_to_original!!` на поле, которое в `PicsDetails` объявлено nullable.

---

## Порядок работ

1. **п.1** — `!!` + `printStackTrace` в Follow/Subscription. Полчаса, чинит молча не работающую функцию.
2. **п.2** — `catch (e: CancellationException) { throw e }` в 17 местах. Час, убирает ложные ошибки при уходе с экрана.
3. **п.7** — `printStackTrace` → `Timber` (15 мест). Полчаса.
4. **п.5** — хелпер `replaceAll` и 20 замен. Полдня.
5. **п.8, 16, 10** — чистка ~98 KB мёртвого кода, включая JS-WebView. День.
6. **п.3** — шифрование пароля Luscious с миграцией. Полдня.
7. **п.6** — решить судьбу `MANAGE_EXTERNAL_STORAGE` и SAF.
8. **п.4** — сузить keep-правила, включить R8, проверить release на устройстве.
9. **п.13** — стабильность Compose.
10. **п.9** — модуляризация, начиная с `:core-player`.
