# Код-ревью проекта xvideos — 30.07.2026

Ревьюируемый срез: `master` @ `71f93fa`, один Gradle-модуль `:app`, 470 Kotlin-файлов, ~54 800 строк.

Проверено: сборка и unit-тесты (`:app:testDebugUnitTest` — BUILD SUCCESSFUL), Android Lint (`:app:lintDebug` — 2 error, 180 warning, 9 hint), ручной разбор архитектуры, конкурентности, слоя данных, сети, безопасности и мёртвого кода.

---

## Статус исправлений

Пункты ниже помечены по состоянию на 30.07.2026, после прохода правок.
Проверка после правок: `assembleDebug`, `assembleRelease`, `testDebugUnitTest`, `lintDebug`, `lintRelease` — все BUILD SUCCESSFUL, lint: **0 error** (было 2).

| № | Пункт | Статус |
|---|---|---|
| 1 | Release подписывается debug-ключом, R8 выключен | **частично** — подпись читается из `keystore.properties`, `checkReleaseBuilds` включён; `minifyEnabled` намеренно оставлен `false` (см. ниже) |
| 2 | `versionCode`/`versionName` | **исправлено** — `15919` / `159.19` |
| 3 | Пароль Luscious в plaintext | **не исправлено** — нужен новый модуль и миграция, см. ниже |
| 4 | `Thread.sleep` в `clear()` | **исправлено** — отмена job вместо busy-wait |
| 5 | Разрешения foreground-сервиса без сервиса | **исправлено** — убраны из манифеста |
| 6 | `MANAGE_EXTERNAL_STORAGE` | **не исправлено** — продуктовое решение |
| 7 | `EventBus` теряет порядок событий | **исправлено** — однопоточный диспетчер |
| 8 | `FileDB` не атомарен / не потокобезопасен | **исправлено** — temp+rename, `synchronized`, атомарная публикация в snapshot |
| 9 | Полный пересканинг каталога на unlike | **исправлено** |
| 10 | Дисковый I/O на main в `SavedL_Likes.remove` | **исправлено** — вынесено на IO |
| 11 | `HttpClient` на каждый запрос | **исправлено** — общий клиент |
| 12 | WebView-скрапер без таймаута | **исправлено** — `withTimeoutOrNull`, привязка к вызывающей корутине |
| 13–18 | Архитектура (модули, копипаста, состояние, стабильность, i18n) | **не исправлено** — крупные работы |
| 19 | 2 ошибки lint (Scaffold padding) | **исправлено** |
| 21 | Мёртвый код | **частично** — удалено 6 файлов + `AppLockActivity` |
| 23 | Мёртвый `EXTRA_REQUIRE_APP_LOCK` | **исправлено** |
| 24 | Устаревшие `@OptIn`, теневой параметр, `.gitignore` | **исправлено** |
| 25 | Тесты | **не исправлено** |

### Почему `minifyEnabled` оставлен выключенным

Включение R8 без прогона на устройстве почти наверняка сломает Gson-десериализацию: правила в `proguard-rules.pro` сейчас держат `kotlin.**`, `kotlinx.**` и `io.ktor.**` целиком, а модели ответов (`GifsInfo`, `NichesInfo`, `AlbumDetails`, `UserInfo`, `ItemsX` и др.) не защищены `-keep` вовсе. Это отдельная задача: сузить keep-правила, добавить правила для моделей, собрать release и проверить на устройстве все три раздела.

### Почему пароль Luscious не зашифрован

Нужен `androidx.security:security-crypto` (новая зависимость — офлайн-сборка на текущем кэше Gradle упадёт) плюс миграция уже сохранённых у пользователей значений. Без миграции первый же запуск после обновления разлогинит всех в разделе L.

---

## Сводка

| Метрика | Значение |
|---|---|
| Gradle-модулей | 1 (`:app`) |
| Kotlin-файлов / строк | 470 / 54 800 |
| Пакеты верхнего уровня | `common` 145 файлов (458 KB), `r` 139 (575 KB), `l` 103 (584 KB), `x` 68 (297 KB) |
| Файлов в корневом пакете | 9 (в т.ч. `AppSettingsScreen.kt` — 65 KB) |
| Unit-тестов | 15 файлов (только `common/p2p`, `common/zip`, `r/common/downloader`) |
| androidTest | 0 |
| Lint | 2 error / 180 warning / 9 hint |
| Зависимостей в `app/build.gradle` | 135 деклараций + 103 закомментированные строки |

**Общая оценка.** Код заметно лучше, чем можно ожидать от single-module проекта такого размера: видны следы предыдущих прицельных фиксов (комментарии `P1:`/`P3:`/`P4:`, объяснения почему убран trust-all TLS, почему `GlobalScope` заменён на управляемый scope). Криптография app-lock и авторизация в `ApiClient` сделаны грамотно. Основные проблемы — не в отдельных строчках, а в конфигурации релиза, в отсутствии границ между фичами и в двух несовместимых стандартах работы с состоянием/диском, живущих в одном проекте.

---

## Что сделано хорошо

- **`r/network/http/ApiClient.kt`** — образцовая работа с анонимным токеном: `@Volatile` + double-checked под `Mutex`, единая обёртка `withAuth` с однократным перелогином на 401, защита от «шторма логинов» через сравнение с `previousToken`. Четыре копии retry-логики схлопнуты в одну.
- **`common/applock/AppLockRepository.kt`** — PBKDF2WithHmacSHA256, 120 000 итераций, случайная 16-байтная соль, сравнение через `MessageDigest.isEqual` (constant-time), персистентный экспоненциальный backoff, переживающий убийство процесса. Сделано правильно.
- **`res/xml/network_security_config.xml`** — `cleartextTrafficPermitted="false"`, доверие к ISRG Root X1 вместо глобального отключения проверки TLS. Комментарии в `App.kt:138` и `CoilImageLoaderFactory.kt:72` фиксируют, что trust-all удалён сознательно.
- **`common/videoplayer/rememberExoPlayerWithLifecycle.kt`** — единый владелец жизненного цикла плеера, `release()` в `DisposableEffect`, `LoadControl` и `TrackSelector` не пересоздаются на рекомпозиции.
- **`AppSettingsScreen.kt`** — при 1577 строках разбит на ~30 мелких приватных композаблов, читается нормально.
- Hilt + Voyager + assisted injection применены последовательно; KDoc на русском в ключевых местах — по делу, а не для галочки.

---

## P0 — Критично

### 1. Release-сборка подписывается debug-ключом, R8 выключен

`app/build.gradle:53-56`

```groovy
release {
    minifyEnabled = false
    //shrinkResources = true
    proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    signingConfig = signingConfigs.debug
}
```

Три проблемы разом:
- **Debug-подпись у release.** Такой APK нельзя опубликовать; хуже — debug-ключ общеизвестен, любой может собрать «обновление» с той же подписью.
- **`minifyEnabled = false`.** Ни обфускации, ни удаления мёртвого кода. При 135 зависимостях (media3 целиком, Ktor, Coil, Lottie, Nearby, Hilt) это десятки лишних мегабайт и весь исходный нейминг наружу.
- **`lint { checkReleaseBuilds = false }`** (`:76`) — lint для release не запускается никогда, то есть release-специфичные проблемы (в т.ч. R8-правила) не ловятся в принципе.

`proguard-rules.pro` при этом уже написан и содержит агрессивные `-keep class kotlin.**`, `-keep class kotlinx.**`, `-keep class io.ktor.** { *; }` — эти три правила сами по себе съедают большую часть выигрыша от R8 и требуют сужения после того, как minify включат.

**Действие:** вынести `signingConfigs.release` в `signing.properties` (вне git), включить `minifyEnabled` + `shrinkResources`, вернуть `checkReleaseBuilds = true`, сузить keep-правила до конкретных data-классов Gson.

### 2. `versionCode` заморожен на 1, `versionName` отстал на релиз

`app/build.gradle:27-28`

```groovy
versionCode = 1
versionName = "158.0"
```

Последние коммиты — `159.19`, `159.18`, `159.17`. То есть версия в коммитах ведётся вручную и в `build.gradle` не попадает. При `versionCode = 1` Android откажется ставить обновление поверх — апдейты невозможны в принципе.

**Действие:** генерировать `versionCode`/`versionName` из одного источника (git tag или `version.properties`), добавить проверку в CI.

### 3. Пароль от Luscious хранится в открытом виде

`common/settings/Settings.kt:98-99` → пишется в `l/ui/screens/L_ScreenLogin.kt:88-89`, читается в `l/repository/Repository.kt:104-105`.

```kotlin
val l_login by lazy { SettingElementString(pref, "l_login", "") }
val l_pass  by lazy { SettingElementString(pref, "l_pass",  "") }
```

Обычный `SharedPreferences`, plaintext. Асимметрия налицо: собственный код-доступ приложение защищает PBKDF2 со 120k итераций, а чужой пароль от внешнего сервиса кладёт рядом открытым текстом. `allowBackup="false"` снижает риск, но не закрывает его (root, ADB на debug-сборке, любой файловый менеджер с MANAGE_EXTERNAL_STORAGE — который у этого же приложения есть).

**Действие:** `EncryptedSharedPreferences` (androidx.security-crypto) или, лучше, хранить только session-cookie вместо пароля.

### 4. Блокировка потока `Thread.sleep` в цикле ожидания

`r/ui/profile/ScreenRedProfileSM.kt:189-195`

```kotlin
fun clear() {
    while (isLoading.value) {
        Thread.sleep(100)
    }
    _list.update { emptyList() }
    _tags.update { emptySet() }
}
```

`clear()` — не `suspend`, вызывается напрямую из `onClick` композабла (`r/ui/ui/atom/GifTypes_Control.kt:32`), то есть с main-потока. Busy-wait по `isLoading` блокирует UI на всё время сетевого запроса.

Хуже: `loadNextPage` (`:163`) выставляет `isLoading = false` в `finally`, и это возобновление происходит на `screenModelScope` (Voyager — `Dispatchers.Main.immediate`). Если main заблокирован в `Thread.sleep`, `finally` никогда не выполнится → **вечный дедлок main-потока**, а не просто фриз.

Сейчас не стреляет ровно по одной причине: **`loadNextPage` не вызывается ниоткуда** — мёртвый публичный метод, `isLoading` всегда `false`, цикл выходит на первой проверке. Как только пагинацию подключат обратно — гарантированный ANR.

**Действие:** сделать `clear()` suspend-функцией и заменить busy-wait на отмену job'а загрузки (`loadJob?.cancelAndJoin()`), либо просто убрать ожидание и полагаться на `_list.update`. Заодно решить судьбу мёртвого `loadNextPage`.

---

## P1 — Важно

### 5. Разрешения foreground-сервиса объявлены, сервиса нет

`AndroidManifest.xml:38-40` объявляет `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE`, `POST_NOTIFICATIONS`. При этом в проекте **ни одного класса `Service`** (grep по `: Service()`, `startForegroundService`, `startForeground(` — пусто), и в манифесте нет тега `<service>`.

Соответственно настройка `p2p_background_receive` (`Settings.kt:47`, включается в `MainActivity` строкой `toggleP2pService(applicationContext, true)`) фоновой не является — `toggleP2pService` (`common/p2p/P2pReceiveManager.kt:184`) просто дёргает объект-синглтон, KDoc которого сам это признаёт: «Работает только пока приложение активно (не в фоне)».

Итог: пользователю обещан фоновый приём, которого нет; в манифесте висят три чувствительных разрешения без применения (лишний повод для отказа при ревью в сторе).

**Действие:** либо реализовать реальный `ForegroundService` и объявить его, либо убрать разрешения и переименовать настройку так, чтобы она не обещала фон.

### 6. `MANAGE_EXTERNAL_STORAGE` + `requestLegacyExternalStorage`

`AndroidManifest.xml:16-18, 61`. Полный доступ ко всему хранилищу — блокер публикации в Google Play без отдельного обоснования. При этом в проекте уже лежит `common/storage/SafFileSystem.kt`, который **никем не используется** (см. п. 21) — то есть SAF-путь начинали делать и бросили.

**Действие:** оценить, реально ли нужен полный доступ, или хватит SAF/MediaStore + `AppPath` во внутреннем хранилище.

### 7. `EventBus` не гарантирует порядок событий

`common/eventBus/EventBus.kt:36-39`

```kotlin
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

fun postEvent(event: Event) {
    Timber.i("!!! ~~~ EventBus.postEvent $event")
    scope.launch { _events.emit(event) }
}
```

Каждый `postEvent` запускает **отдельную корутину на многопоточном `Dispatchers.IO`** — две подряд отправки могут доехать до подписчиков в обратном порядке. Для `Event.P2pTransferUpdate.Progress` это означает скачущий назад прогресс-бар в `MainActivity.P2pBackgroundOverlay`, для `ShowSnackBar` — перепутанный порядок сообщений.

Буфер `extraBufferCapacity = 1024` уже есть, значит `emit` практически никогда не приостанавливается.

**Действие:** `_events.tryEmit(event)` напрямую, без `launch`. Либо, если приостановка всё же нужна, `Dispatchers.IO.limitedParallelism(1)`.

### 8. `FileDB` — не потокобезопасен и не атомарен

`common/fileDB/FileDB.kt`. Базовый слой персистентности для шести хранилищ (`R_Saved_Likes`, `R_Saved_Niches`, `R_Saved_Creator`, `R_Saved_Subscriptions`, `SavedL_Albums`, `SavedX_Favorites`).

**8a. Мутация Compose-состояния без переключения потока и без синхронизации** (`:113-115`):

```kotlin
list.clear()
list.addAll(loaded)
```

`list` — `mutableStateListOf`, вызывается из IO-потока (`SavedRed.scope` = `Dispatchers.IO`). Две операции не атомарны: UI может отрисовать промежуточное пустое состояние (мигание списка), а два параллельных `refresh()` — переплестись.

**8b. Неатомарная запись** (`:33`, `:52`):

```kotlin
file.writeText(json, Charsets.UTF_8)
```

Обрыв процесса в момент записи оставляет обрезанный JSON. При следующем `refresh()` он молча отбрасывается (`mapNotNull` + `catch → null`, `:105-111`) — элемент просто исчезает без единого следа для пользователя.

**Действие:** писать во временный файл + `renameTo`; мутацию `list` делать под `Mutex` и на `Dispatchers.Main`.

Обратите внимание: рядом, в `l/featured/saved/SavedL_Likes.kt:78-95`, ровно та же задача решена **правильно** — чтение на IO, `withContext(Dispatchers.Main) { listUrl.clear(); listUrl.addAll(items) }`. То есть в проекте сосуществуют два стандарта, и `FileDB` — устаревший.

### 9. Полное пересканирование каталога на каждое удаление

`r/common/saved/R_Saved_Likes.kt:36-42`

```kotlin
fun remove(item: GifsInfo) {
    likesDb.delete(item.id)...
    refresh()   // listFiles() + Gson-парсинг ВСЕХ файлов
}
```

`add()` при этом делает `list.add(safeItem)` — O(1). Асимметрия: unlike стоит O(n) чтений с диска, like — ноль. На коллекции в несколько тысяч лайков это секунды.

**Действие:** `list.remove(item)` по аналогии с `add()`.

### 10. Дисковый I/O на вызывающем потоке

`l/featured/saved/SavedL_Likes.kt:59-76` — `remove(url)` выполняет `folder.deleteRecursively()` / `file.delete()` синхронно, без `scope.launch(Dispatchers.IO)`, хотя соседние `add()` и `refresh()` в том же классе это делают. Вызовы приходят из UI. `deleteRecursively()` по папке с медиа — это уже не микросекунды.

### 11. `HttpClient` создаётся на каждый запрос

`x/feature/net/readHtmlFromURLDirect.kt:16`

```kotlin
val client = HttpClient(OkHttp) { ... }
```

Новый пул соединений и пул потоков на каждый вызов, нулевое переиспользование keep-alive. Клиент корректно закрывается в `finally`, так что утечки нет — есть чистая потеря производительности.

Плюс глушение ошибки (`:35-37`): при исключении функция возвращает `""`, и вызывающий не отличит пустую страницу от сетевого сбоя.

**Действие:** вынести клиент в синглтон/DI; возвращать `Result<String>`.

### 12. WebView-скрапер без таймаута и с неуправляемым scope

`x/feature/net/readHtmlFromURLWebView.kt:23`

```kotlin
suspendCancellableCoroutine { continuation ->
    CoroutineScope(Dispatchers.Main).launch { ... }
```

Неуправляемый scope внутри `suspendCancellableCoroutine`. Если `onPageFinished` не придёт никогда (редирект в бесконечный лоадер, капча, оборванная сеть) — корутина висит вечно, WebView не уничтожается. Логика `destroyWebView()` через `AtomicBoolean` написана аккуратно, но её просто некому вызвать.

**Действие:** обернуть в `withTimeout(...)`; вместо своего scope использовать `Dispatchers.Main` как контекст вызывающей корутины.

---

## P2 — Архитектура

### 13. Один модуль на 55 тысяч строк

`settings.gradle` содержит ровно `include ':app'`. Внутри — четыре почти независимых мира: `r/` (RedGifs, 139 файлов), `l/` (Luscious, 103), `x/` (Xvideos, 68), `common/` (145). Любая правка в любом из них перекомпилирует всё.

Заготовка модуляризации уже была и откачена — в `app/build.gradle` висит `//implementation project(':core-player')` с комментарием «Этап 2 рефакторинга плеера».

**Действие:** `:core-common`, `:core-player`, `:feature-r`, `:feature-l`, `:feature-x`. Это даст и скорость инкрементальной сборки, и компилятор в роли охранника границ — сейчас ничто не мешает `l/` дёргать внутренности `r/`.

### 14. Девять файлов в корневом пакете

`App.kt`, `MainActivity.kt` (19.5 KB), `AppSettingsScreen.kt` (65 KB), `AppLockActivity.kt`, `PermissionScreenActivity.kt`, `SplashActivity.kt`, `HapticDemoScreen.kt`, `WebScreen.kt`, `template.kt`. Экраны и демки не должны лежать рядом с `Application`.

### 15. Копипаста между `l/` и `r/`

15 пар файлов с совпадающими именами. Пять из них — прямая копипаста меню, разошедшаяся правками:

| Файл | L | R | строк расхождения |
|---|---|---|---|
| `DropdownMenuItem_AddCollection` | 43 | 51 | 57 |
| `DropdownMenuItem_Download` | 36 | 45 | 41 |
| `DropdownMenuItem_RemoveFromCollection` | 44 | 84 | 67 |
| `DropdownMenuItem_SaveToGallery` | 35 | 27 | 22 |
| `DropdownMenuItem_Share` | 36 | 52 | 42 |

Плюс дубли `CollectionsGrid.kt`, `ScreenSaved.kt`, `TabRow.kt`, `Theme.kt`, `UrlImage.kt`, `useCaseShareFile.kt`, `useCaseShareGifs.kt`.

**Действие:** общий `common/expandMenu` с параметризацией по источнику; `common/theme/Theme.kt` оставить один (второй, `ui/theme/Theme.kt`, — огрызок шаблона на 1.8 KB).

### 16. Две несовместимые модели состояния в ScreenModel

Из 50 ScreenModel: 23 держат состояние в `mutableStateOf`, 10 — в `StateFlow`, часть — в обоих сразу. `ScreenRedProfileSM` — характерный пример: `_list: MutableStateFlow` соседствует с `creator by mutableStateOf` и `order by mutableStateOf`.

Дополнительно `_list` объявлен как `val _list` — **public** (`:69`), при наличии рядом публичного read-only `list`. Подчёркивание намекает на приватность, модификатора нет. То же в `tagsSelect` (`:76`).

**Действие:** выбрать один подход (`StateFlow` предпочтительнее — не тянет Compose-runtime в слой состояния и тестируется без Compose-правил), закрыть `_`-поля `private`.

### 17. Compose-стабильность не проработана

- 164 data-класса, 101 свойство типа `List<...>`
- `@Immutable` — 4 использования, `@Stable` — 4
- `kotlinx-collections-immutable` подключён, но встречается 15 раз
- Плагин `stability.analyzer` подключён (`app/build.gradle:14`), файла конфигурации стабильности нет

`List<T>` для компилятора Compose нестабилен → все композаблы, принимающие модели с такими полями, рекомпозируются при каждом кадре родителя. В приложении, где основной UI — длинные ленты видео и картинок, это самая дешёвая доступная оптимизация.

**Действие:** `ImmutableList` в UI-моделях либо `@Immutable` на них; добавить `stability_config.conf`.

### 18. Локализации нет

680 строковых литералов с кириллицей в 129 файлах; `res/values/strings.xml` содержит **одну** строку. Языки в UI перемешаны: `SnackBar.success("Like")` и рядом `SnackBar.info("Группа добавлена")`, `SnackBar.error("Ошибка добавления лайка")`.

**Действие:** если мультиязычность не нужна — хотя бы привести UI к одному языку. Если нужна — выносить в ресурсы придётся всё сразу.

---

## P3 — Гигиена

### 19. `lintDebug` падает: 2 error

- `x/screens/tags/ScreenTags.kt:37` — `UnusedMaterial3ScaffoldPaddingParameter`: `{ _ -> }` игнорирует padding от `Scaffold`, контент может уехать под системные панели.
- Парная `UnusedMaterialScaffoldPaddingParameter` (Material 2).

### 20. Топ lint-предупреждений

| Кол-во | Issue | Комментарий |
|---|---|---|
| 63 | `UnusedResources` | мёртвые drawable/строки в APK |
| 32 | `PrivateResource` | использование internal-ресурсов библиотек — сломается при их обновлении |
| 26 | `UsingMaterialAndMaterial3Libraries` | M2 и M3 в одном экране; напр. `MainActivity` тянет `material.Surface` и `material3.Text` одновременно |
| 21 | `GradleDependency` | устаревшие версии |
| 9 | `AutoboxingStateCreation` | `mutableStateOf(0)` вместо `mutableIntStateOf(0)` — боксинг на каждой записи |
| 6 | `ObsoleteSdkInt` | проверки версий ниже `minSdk = 26` |
| 3 | `LockedOrientationActivity` | `screenOrientation="portrait"` жёстко |
| 1 | `SetJavaScriptEnabled` | `WebScreen.kt:23` — JS в WebView; проверить, что туда не попадают недоверенные URL |

### 21. Мёртвый код — 26 неиспользуемых публичных объявлений

Самое заметное:

- **`AppLockActivity`** (11.7 KB) — объявлена в `AndroidManifest.xml:75`, но `startActivity` для неё нет нигде; замок теперь рисуется прямо в `MainActivity` через композабл `AppLockScreen`.
- **`r/common/video/player_with_menu/RedVideoPlayerWithMenuContent.kt`** — форк живого `r/common/video/RedVideoPlayerWithMenuContent.kt` (там функция называется `RedVideoPlayerWithMenu` и используется в `ScreenRedFullScreen.kt:306`). Форк разошёлся: потерял `BufferChange`-событие, приобрёл параметры меню. Никем не вызывается — только закомментированная строка в `TikTokStyleVideoFeed.kt:92`.
- `common/storage/SafFileSystem.kt`, `common/buildMediaSource.kt`, `common/ComposableLifecycle.kt`, `common/traficStatistic/AppNetworkSpeedMonitor.kt`
- `x/screens/k/ScreenK.kt` (`ScreenDashBoards`), `r/ui/manager_block/ScreenRedManageBlock.kt`
- Две реализации скроллбара: `VerticalScrollbar1` (`ScreenRedProfile.kt:202`) и `VerticalScrollbar2` (`atom/VerticalScrollbar.kt:54`) — обе мёртвые
- `l/net/graphQl/getAlbumListGraphQL.kt:153`, `l/featured/saved/fileNameToPicsDetails.kt`, `l/model/PicsDetailsMedia.kt:85,93`

### 22. Половина `app/build.gradle` — комментарии

103 закомментированные строки на 135 деклараций зависимостей: Facebook SDK, Firebase BOM + Analytics + Auth, play-services-auth, androidx.credentials, закомментированный блок `signingConfigs` **с паролями от keystore в открытом виде** (`:35-49`). Плагин `google-gms-google-services` при этом подключён и `app/google-services.json` закоммичен, хотя все Firebase-зависимости выключены.

### 23. Мёртвый intent-extra

`MainActivity.kt:133` объявляет `EXTRA_REQUIRE_APP_LOCK`, `SplashActivity.kt:70` его кладёт — **никто не читает**. Комментарий в `MainActivity:167-170` честно объясняет почему (правильное решение: доверять только `AppLockRepository.shouldShowLock`). Раз extra не нужен — убрать оба конца.

### 24. Прочее

- Устаревшие `@OptIn(DelicateCoroutinesApi::class)` на функциях, где `GlobalScope` давно убран: `r/common/saved/R_Saved_Likes.kt:44`, `R_Saved_Niches.kt:38`.
- `FileDB.read(nameFile, clazz)` (`:79`) принимает `clazz` параметром, дублируя одноимённое поле конструктора (`:13`) — теневое перекрытие, легко передать не тот тип и получить `ClassCastException` в рантайме.
- `.kotlin/` не в `.gitignore` — постоянный мусор в `git status`.
- В корне репозитория закоммичены 5 отчётов прошлых ревью (`CODE_REVIEW_REPORT.md`, `CODE_REVIEW_REPORT_NEW.md`, `DETAILED_CODE_REVIEW.md`, `PROJECT_CODE_ANALYSIS_REPORT.md`, `R8_Configuration_Analysis.md`), а также `query` (6 байт) и `Описалово.graphml`. Место им в `docs/` или в корзине.

### 25. Тестов почти нет

15 unit-тестов покрывают `common/p2p` (10 файлов), `common/zip`, `r/common/downloader`. Слои `r/`, `l/`, `x/` — ноль тестов. `androidTest` — ноль. При этом в `libs.versions.toml` объявлены `uiautomator`, `benchmarkMacroJunit4`, `androidx-compose-bom` для тестов — инфраструктура есть, использования нет.

Минимально окупаемое: тесты на `FileDB` (атомарность записи, поведение на битом JSON), на `ApiClient.withAuth` (перелогин по 401), на `AppLockRepository` (backoff, constant-time сравнение).

---

## Рекомендуемый порядок работ

1. **Релизная конфигурация** (P0 №1, №2) — полдня, разблокирует возможность вообще выпускать сборки.
2. **`Thread.sleep` в `clear()`** (P0 №4) — полчаса, снимает мину.
3. **Шифрование пароля Luscious** (P0 №3) — полдня.
4. **`EventBus.tryEmit`** (P1 №7) — 10 минут, чинит прыгающий прогресс P2P.
5. **`FileDB`: атомарная запись + mutex** (P1 №8) — день, устраняет молчаливую потерю сохранённых элементов.
6. **Разрешения без сервиса** (P1 №5, №6) — решить: делаем фоновый сервис или убираем разрешения и обещание в UI.
7. **Чистка мёртвого кода** (P3 №21, №22, №23) — день, минус ~2000 строк и минус путаница «какой из двух плееров живой».
8. **Две ошибки lint + `checkReleaseBuilds = true`** (P3 №19) — час.
9. **Стабильность Compose** (P2 №17) — самый дешёвый прирост производительности лент.
10. **Модуляризация** (P2 №13) — крупная работа, планировать отдельно; начать с извлечения `:core-player` (заготовка уже была).
