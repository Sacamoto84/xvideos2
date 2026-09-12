# Код-ревью xvideos — проход 12

> **Срез:** `c1c8df4` · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода — `master` (`c1c8df4`), 17 коммитов со среза прохода 11 (`6cbf5b0`),
35 изменённых файлов, +2867 / −117 строк.

Линзы: **аудит закрытия находок прохода 11**, **подсистема DNS-over-HTTPS (DoH)**,
**фоновые загрузки WorkManager**, **камуфляж-калькулятор и защита окна в Recent Apps**,
**состояние гейтов сборки (detekt, release build, R8/shrinkResources, unit-тесты)**.

Ключевой итог: **все замечания прохода 11 (C10, T7, UI6, C11, A3, UI1, UI2, C5)
полностью устранены**. Проект компилируется, `minifyReleaseWithR8` и `shrinkResources`
успешно отрабатывают, `detekt` на 100% зелёный во всех 5 модулях, unit-тесты
(273 таски) проходят без единой ошибки.
Новая функциональность (DoH, WorkManager, калькулятор) в целом выполнена качественно,
но содержит ряд дефектов корректности в обработке путей, заголовков и кэша.

---

## Статус находок прохода 11 в текущем срезе

| Находка | Было в проходе 11 | Статус в проходе 12 | Комментарий к изменениям |
| --- | --- | --- | --- |
| **C10** | открыт (`:feature-l:detekt` FAILED) | **закрыт** | Неиспользуемые импорты удалены. Все модули `:app`, `:core`, `:feature-l`, `:feature-r`, `:feature-x` проходят `detekt` без ошибок |
| **T7** | открыт (мутация `MutableState` на IO) | **закрыт** | В [R_Screen_CollectionTab.kt:168](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_CollectionTab.kt#L168) и [L_Screen_CollectionTab.kt:175](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/L_Screen_CollectionTab.kt#L175) состояние диалога сбрасывается синхронно на Main, операции делегированы в `ScreenSavedCollectionSM` |
| **UI6** | открыт (тумблер-призрак выреза) | **закрыт** | [getTopInsetDp.kt:35](../core/src/main/java/com/client/xvideos/common/util/getTopInsetDp.kt#L35) теперь реактивно читает `Settings.useCutoutPadding` и отключает отступ, если тумблер выключен |
| **C11** | открыт (нет тестов `SafePath`) | **закрыт** | Добавлен [SafePathTest.kt](../core/src/test/java/com/client/xvideos/common/io/SafePathTest.kt), проверяющий `normalizeRelativePath`, `requireInside` и `isUnsafeItemName` |
| **A3** | в работе (модели R и L) | **закрыт** | Модели :feature-r и :feature-l переведены на `@Serializable` с лояльными парсерами `RJson`/`LJson`. Добавлены `LSerializationCompatibilityTest` и `RSerializationCompatibilityTest` |
| **UI1** | открыт (статика навигации R) | **закрыт** | `RNavigationState` переведён на `CompositionLocalProvider` без статического синглтона |
| **A1** | открыт (сторож синглтонов) | **закрыт** | Сторож `GlobalStateTest` расширен и контролирует неизменность глобальных изменяемых точек |
| **UI2** | открыт (дисковый I/O коллекций) | **закрыт** | Все дисковые операции вынесены в `ScreenSavedCollectionSM` на `Dispatchers.IO` |
| **C5** | заморожен (P2P тест) | **закрыт** | `P2pReceiveControllerTest` обновлён под `authenticationDigits`, тесты компилируются |

---

## Новые находки

### S1 — `MediaDownloadWorker`: отсутствие проверки `fileName` и `metaFileName` на path traversal. Средняя.

[MediaDownloadWorker.kt:66-68](../core/src/main/java/com/client/xvideos/common/download/work/MediaDownloadWorker.kt#L66-L68),
[MediaDownloadWorker.kt:96-98](../core/src/main/java/com/client/xvideos/common/download/work/MediaDownloadWorker.kt#L96-L98)

В новом компоненте фоновых загрузок целевой файл и файл метаданных создаются напрямую:
```kotlin
val targetFile = File(dir, fileName)
val tempFile = File(dir, "$fileName.tmp")
...
val infoName = metaFileName ?: "${targetFile.nameWithoutExtension}.info"
File(dir, infoName).writeText(metaContent, Charsets.UTF_8)
```
Имена `fileName` и `metaFileName` передаются через `inputData` из `DownloadWorkRequest`. Если источник запроса сформирует имя с относительными сегментами (`../../...`) или недопустимыми символами, файл может быть записан за пределы каталога загрузок (`destDir`).

В проекте уже есть утилиты защиты `SafePath.isUnsafeItemName()` и `requireInside(dir, targetFile)`, но в `MediaDownloadWorker` они не вызываются.

*Лечение:* перед началом скачивания проверять входные имена:
```kotlin
require(!isUnsafeItemName(fileName)) { "Небезопасное имя файла: $fileName" }
metaFileName?.let { require(!isUnsafeItemName(it)) { "Небезопасное имя info-файла: $it" } }
requireInside(dir, targetFile)
```

*Чего нет:* эксплуатация возможна только при программной передаче некорректного `DownloadWorkRequest`.

---

### C12 — `DownloadWorkRequest`: искажение HTTP-заголовков с точкой с запятой при сериализации через `split(";")`. Средняя.

[DownloadWorkRequest.kt:47-49](../core/src/main/java/com/client/xvideos/common/download/work/DownloadWorkRequest.kt#L47-L49),
[DownloadWorkRequest.kt:71-79](../core/src/main/java/com/client/xvideos/common/download/work/DownloadWorkRequest.kt#L71-L79)

При сохранении HTTP-заголовков в `Data` WorkManager используется наивное объединение через точку с запятой:
```kotlin
val serializedHeaders = headers.entries.joinToString(";") { "${it.key}=${it.value}" }
builder.putString(KEY_HEADERS, serializedHeaders)
```
И обратный разбор:
```kotlin
return headersString.split(";").mapNotNull { entry ->
    val split = entry.split("=", limit = 2)
    if (split.size == 2) split[0].trim() to split[1].trim() else null
}.toMap()
```
Если значение любого заголовка содержит точку с запятой (стандартно для `Cookie: sid=123; user=abc`, а также `Accept: text/html; charset=utf-8`), `split(";")` разбивает значение заголовка на части и превращает сегменты cookie в самостоятельные фиктивные HTTP-заголовки. Воркер отправит искажённые заголовки, что приведёт к ошибкам авторизации (401/403) при загрузке закрытых медиафайлов.

*Лечение:* сериализовать `headers` через `AppJsonCompact.encodeToString(headers)` и десериализовать через `AppJsonCompact.decodeFromString<Map<String, String>>(...)`.

---

### C13 — `MediaDownloadWorker`: постоянный сбой загрузки при HTTP 416 (Range Not Satisfiable) из-за неудаляемого `.tmp`. Средняя.

[MediaDownloadWorker.kt:157-169](../core/src/main/java/com/client/xvideos/common/download/work/MediaDownloadWorker.kt#L157-L169),
[MediaDownloadWorker.kt:109-117](../core/src/main/java/com/client/xvideos/common/download/work/MediaDownloadWorker.kt#L109-L117)

При докачке файла размер существующего `.tmp` подставляется в заголовок `Range: bytes=$resumeOffset-`:
```kotlin
val resumeOffset = if (tempFile.exists()) tempFile.length() else 0L
val request = buildDownloadRequest(urlString, headers, resumeOffset)
val response = downloadOkHttpClient.newCall(request).execute()
```
Если файл на сервере изменился или предыдущая загрузка оборвалась так, что размер `.tmp` оказался больше или равен длине удалённого файла, сервер возвращает HTTP 416.
В блоке проверки:
```kotlin
if (!isOk && !isResume) {
    throw IOException("Сервер вернул HTTP $responseCode: $message")
}
```
выбрасывается исключение, которое перехватывается в `doWork()` и завершает задачу `Result.failure()`. Однако сам `tempFile` на диске остаётся нетронутым.
В результате все последующие попытки пользователя скачать этот же файл будут отправлять тот же ошибочный `Range` и снова гарантированно падать с HTTP 416 до тех пор, пока пользователь вручную не очистит диск.

*Лечение:* обрабатывать код 416 отдельно: удалять `tempFile` и запрашивать файл заново с нулевого байта.

---

### C14 — `AppDns`: подстрочное сопоставление хоста в `isDohServerHost` ошибочно обходит DoH. Низкая.

[AppDns.kt:251-253](../core/src/main/java/com/client/xvideos/common/net/doh/AppDns.kt#L251-L253)

В методе `isDohServerHost` для кастомного DoH провайдера выполняется проверка:
```kotlin
if (provider == DohProvider.CUSTOM) {
    val customUrl = runCatching { Settings.doh_custom_url.field.value }.getOrDefault("")
    if (customUrl.contains(host, ignoreCase = true)) return true
}
```
Метод проверяет, входит ли имя запрашиваемого хоста подстрокой в полный URL резолвера (включая схему, порт, путь и query-параметры).
Если пользователь ввел, например, `https://dns.nextdns.io/myprofile`, то любой реальный хост, содержащий подстроку `"dns"`, `"nextdns"`, `"io"` или имя профиля (например, `dns.google`, `service.io`), вернёт `true` и будет безусловно перенаправлен на незащищённый системный DNS оператора (`Dns.SYSTEM`).

*Лечение:* извлекать имя хоста через `java.net.URI(customUrl).host` и сравнивать через `equals` с `host`.

---

### C15 — `AppDns`: неограниченный рост кэша DNS (`ConcurrentHashMap`) без очистки устаревших записей по размеру. Низкая.

[AppDns.kt:65](../core/src/main/java/com/client/xvideos/common/net/doh/AppDns.kt#L65),
[AppDns.kt:99-104](../core/src/main/java/com/client/xvideos/common/net/doh/AppDns.kt#L99-L104)

`cache` объявлен как `ConcurrentHashMap<String, CacheEntry>()`. Записи из него удаляются только по требованию: если поступил запрос именно на тот хост, чей `expiresAtMs` уже прошёл.
Если приложение в течение сессии обращается к сотням уникальных поддоменов CDN (Luscious/RedGifs/Xvideos распределённые хранилища), просроченные записи остаются в памяти и никогда не вытесняются, так как повторного обращения к ним может не быть.

*Лечение:* ограничить максимальный размер кэша (например, 256 записей) или использовать LRU-структуру с автоматическим вытеснением.

---

### T8 — `CalculatorScreen`: параллельные запуски криптографической проверки PIN при частом клике «=». Низкая.

[CalculatorState.kt:148-155](../app/src/main/java/com/client/xvideos/calculator/CalculatorState.kt#L148-L155)

В `onEquals` запуск корутины проверки PIN происходит без подавления повторных нажатий:
```kotlin
scope.launch {
    if (isCandidatePin && onUnlock(codeToTest)) return@launch
    ...
}
```
Проверка PBKDF2 (120 000 итераций) выполняется в фоновом пуле и занимает сотни миллисекунд. Если пользователь или злоумышленник быстро нажимает кнопку «=» несколько раз, запускается несколько параллельных тяжелых вычислений хеша, что приводит к двойному или тройному учету неудачной попытки в `AppLockRepository.registerFailedAttempt`.

*Лечение:* добавить флаг `isVerifying` в `CalculatorState`, игнорируя повторные вызовы до завершения проверки (аналогично тому, как это сделано в `AppLockScreen.submit()`).

---

### A6 — Инфраструктура `WorkDownloadManager` изолирована и не используется вызывающими экранами. Низкая.

[WorkDownloadManager.kt](../core/src/main/java/com/client/xvideos/common/download/work/WorkDownloadManager.kt),
[Downloader.kt](../feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt),
[SavedX_Downloads.kt](../feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt),
[GallerySaver.kt](../core/src/main/java/com/client/xvideos/common/gallery/GallerySaver.kt)

В `:core` создана современная инфраструктура загрузок на базе `WorkManager` и `ForegroundService` (`WorkDownloadManager`, `MediaDownloadWorker`, `DownloadWorkModule`).
Однако реальные компоненты загрузки (`Downloader` в :feature-r, `SavedX_Downloads` в :feature-x, `GallerySaver`) до сих пор напрямую обращаются к устаревшему `KDownloader` (in-process). Новая подсистема пока не связана ни с одним UI-потребителем и представляет собой задел на будущее.

*Лечение:* сформировать этапный план миграции существующих загрузчиков на `WorkDownloadManager`.

---

### UI7 — Конфликт `FLAG_SECURE` и программного размытия в диспетчере задач. Низкая.

[MainActivity.kt:144-150](../app/src/main/java/com/client/xvideos/MainActivity.kt#L144-L150),
[AppLockSection.kt:137-148](../app/src/main/java/com/client/xvideos/screenSettings/components/AppLockSection.kt#L137-L148)

Тумблер «Защита в диспетчере задач» (включён по умолчанию) выставляет флаг `FLAG_SECURE` на всё окно `MainActivity`:
```kotlin
LaunchedEffect(blurRecentTasks, isAppLocked) {
    if (blurRecentTasks || isAppLocked) {
        window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    } else {
        window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
```
Следствия:
1. Пользователь полностью лишён возможности делать скриншоты в приложении, а также транслировать экран через scrcpy или Chromecast, хотя настройка описана как защита превью в Recent Apps.
2. При наличии аппаратного `FLAG_SECURE` система Android вообще не создаёт снимок окна (показывает чёрный экран или иконку приложения). Соответственно, программное наложение `Modifier.blur(25.dp)` и `MinimizedPrivacyOverlay()` при уходе в фон никогда не попадает в снимок системы и является избыточным вычислением.

---

## Статус

| Находка | Класс | Статус | Коммит |
| --- | --- | --- | --- |
| S1 | безопасность | **закрыт** | `fe01a55` |
| C12 | корректность | **закрыт** | `fe01a55` |
| C13 | корректность | **закрыт** | `fe01a55` |
| C14 | корректность | **закрыт** | `fe01a55` |
| C15 | корректность | **закрыт** | `fe01a55` |
| T8 | конкурентность | **закрыт** | `fe01a55` |
| A6 | архитектура | **принят** (арх. план миграции) | — |
| UI7 | интерфейс / UX | **закрыт** | `fe01a55` |

---

## Проверка

```
./gradlew detekt --rerun-tasks --continue
BUILD SUCCESSFUL in 3s
12 actionable tasks: 12 executed
(detekt 100% зелёный во всех 5 модулях: :app, :core, :feature-l, :feature-r, :feature-x)

./gradlew test --continue
BUILD SUCCESSFUL in 41s
273 actionable tasks: 73 executed, 200 up-to-date
(Все unit-тесты, включая GlobalStateTest, CalculatorStateTest, AppDnsTest, DownloadWorkRequestTest, SafePathTest, тесты сериализации L и R, зелёные)

./gradlew assembleRelease
BUILD SUCCESSFUL in 1m 29s
234 actionable tasks: 32 executed, 202 up-to-date
(Release APK собран успешно, R8 minifyEnabled, dex desugaring и shrinkResources отработали без сбоев)
```

---

## Что осталось открытым

- **P2P не трогать (решение владельца от 11.09.2026)**: тракт P2P заморожен, правки там не производятся.
- **T1, T2 (проход 7)**: `StorageCleanupGate` и архитектура `EventBus` оставлены как проектные соглашения.
