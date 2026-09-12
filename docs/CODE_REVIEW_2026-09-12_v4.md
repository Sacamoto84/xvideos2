# Код-ревью xvideos — проход 15

> **Срез:** `eebb12a` · **Статус:** закрыт (`f4de960`) · **Индекс:** [все документы](README.md)

База прохода — `master` (`eebb12a`), текущий срез после закрытия всех замечаний 14-го прохода (`14243d7`, `eebb12a`).

Линзы: **сетевой слой и обработка изображений Coil (NPE в интерцепторе при пустом теле ответа, двойное дисковое кэширование)**,
**безопасность файловых путей (Path Traversal в подсистеме блокировок R)**,
**конкурентность и защита от брутфорса калькулятора-камуфляжа (гонка параллельных вызовов PBKDF2 при повторных нажатиях «=»)**,
**жизненный цикл и блокирующий I/O в ScreenModel (синхронное чтение файлов блокировок на UI-потоке)**,
**атомарность записи кэша ниш и надёжность CollectionDB при отсутствии каталога**,
**потокобезопасность локального журнала ошибок CrashLog (SimpleDateFormat и гонки append/trim)**,
**гигиена кодовой базы и мёртвый код**.

Ключевой итог: **все замечания прохода 14 полностью закрыты**. Проект стабилен,
`detekt` на 100% зелёный во всех 5 модулях, unit-тесты (273 таски) проходят без единой ошибки,
`assembleRelease` с R8 minification и shrinkResources собирается успешно.
Новые находки сфокусированы на предотвращении краша Coil при ответах HTTP 304/204, исключении Path Traversal в use case блокировок R, защите калькулятора от исчерпания попыток из-за параллельных верификаций, устранении блокирующего I/O в ScreenModel и оптимизации дискового кэша.

---

## Статус находок прохода 14 в текущем срезе

| Находка | Было в проходе 14 | Статус в проходе 15 | Комментарий к изменениям |
| --- | --- | --- | --- |
| **S3** | открыт (path traversal через `.` и `..` в `FolderTable`) | **закрыт** (`14243d7`) | Добавлены `!isUnsafeItemName` и `requireInside`, добавлен юнит-тест |
| **T10** | открыт (уничтожение scope в `cancelAll`) | **закрыт** (`14243d7`) | `scope.cancel()` заменён на `scope.coroutineContext.cancelChildren()` |
| **C19** | открыт (`lateinit` в finally `DownloadTask`/`DownloadRequest`) | **закрыт** (`14243d7`) | Поля переведены на `null`, `closeAllSafely` защищён от NPE |
| **C20** | открыт (утечка сокета и сбои редиректов в `DownloadTask`) | **закрыт** (`14243d7`) | Добавлен `httpClient?.close()`, `Location` резолвится через `URI.resolve()`, бросается `IOException` |
| **UI9** | открыт (рассинхронизация паузы плеера при возврате из фона) | **закрыт** (`14243d7`) | `getExoPlayerLifecycleObserver` переведён на `isPause: () -> Boolean` и `rememberUpdatedState` |
| **C21** | открыт (удвоение скорости в `NetworkTrafficMonitor`) | **закрыт** (`14243d7`) | Формула переведена на деление на секунды (`/ 1000f`) |
| **T11** | открыт (непотокобезопасный `HashMap` в `DownloadRequestQueue`) | **закрыт** (`14243d7`) | Заменён на `ConcurrentHashMap<Int, DownloadRequest>` |
| **A8** | открыт (заброшенный файл `moduleKDownloader.kt`) | **закрыт** (`14243d7`) | Файл удалён из репозитория |

---

## Новые находки

### C22 — `ProgressInterceptor`: краш с `NullPointerException` при HTTP 304 (Not Modified) и 204 (No Content). Высокая.

[ProgressInterceptor.kt:15-21](../core/src/main/java/com/client/xvideos/common/coil/ProgressInterceptor.kt#L15-L21)

В сетевом перехватчике Coil `ProgressInterceptor`:
```kotlin
val originalResponse = chain.proceed(chain.request())
val url = chain.request().url.toString()

return originalResponse.newBuilder()
    .body(
        ProgressResponseBody(originalResponse.body) { bytesRead, contentLength, done ->
            progressListener(url, bytesRead, contentLength, done)
        } as ResponseBody
    )
    .build()
```
В спецификации HTTP и OkHttp `originalResponse.body` является `@Nullable ResponseBody?` — например, при ответах `304 Not Modified` (условные запросы с кэшем ETag / If-Modified-Since) или `204 No Content`.
Конструктор `ProgressResponseBody`:
```kotlin
class ProgressResponseBody(
    private val responseBody: ResponseBody,
    ...
```
принимает non-null `ResponseBody`. Kotlin генерирует проверку аргумента `Intrinsics.checkNotNullParameter(responseBody, "responseBody")`.
При получении HTTP 304 от сервера (что регулярно происходит при наличии кэша изображений) перехватчик выбрасывает `NullPointerException`, приводя к сбою загрузки изображения.

*Лечение:* если `originalResponse.body == null`, немедленно возвращать `originalResponse` без оборачивания:
```kotlin
val body = originalResponse.body ?: return originalResponse
```

---

### S4 — `useCase/blockItem`: отсутствие валидации `userName` и `id` (уязвимость Path Traversal) и неатомарная запись. Средняя.

[blockItem.kt:17-29](../feature-r/src/main/java/com/client/xvideos/r/common/block/useCase/blockItem.kt#L17-L29), [blockGetGifsInfoByUserName.kt:10](../feature-r/src/main/java/com/client/xvideos/r/common/block/useCase/blockGetGifsInfoByUserName.kt#L10), [blockGetGifsByUserNameAsListString.kt:8](../feature-r/src/main/java/com/client/xvideos/r/common/block/useCase/blockGetGifsByUserNameAsListString.kt#L8)

В функции блокировки GIF-элементов:
```kotlin
val blockDir = File(AppPath.r_block, item.userName)
if (!blockDir.exists()) {
    val created = blockDir.mkdirs()
    ...
}
val blockFile = File(blockDir, "${item.id}.block")
val json = AppJson.encodeToString(item)
blockFile.writeText(json, Charsets.UTF_8)
```
Поля `item.userName` и `item.id` приходят напрямую из сетевой модели `GifsInfo`.
1. Имена не проверяются через `isUnsafeItemName` и `requireInside(rootDir, blockDir)`. Передача строки вида `../../` в `userName` позволяет создавать файлы блокировок за пределами каталога `r_block`.
2. Запись файла `.block` выполняется через неатомарный `writeText`, что несёт риск повреждения файла при прерывании процесса.
3. В `blockGetGifsInfoByUserName` и `blockGetGifsByUserNameAsListString` имя `userName` также используется без валидации `isUnsafeItemName`, а параметр `userName` в первой функции содержит странный хардкод `userName: String = "lilijunex"`.

*Лечение:* валидировать входные параметры через `isUnsafeItemName`, проверять вхождение через `requireInside(File(AppPath.r_block), blockDir)` и `requireInside(blockDir, blockFile)`, а запись перевести на `writeTextAtomically`.

---

### T12 — `CalculatorState.onEquals`: гонка параллельных верификаций PIN-кода и ложные блокировки из-за отсутствия guard `isVerifying`. Средняя.

[CalculatorState.kt:150-170](../app/src/main/java/com/client/xvideos/calculator/CalculatorState.kt#L150-L170)

В экране-камуфляже калькулятора вычисление хэша кода доступа `PBKDF2` занимает сотни миллисекунд (120 000 итераций).
В методе `onEquals`:
```kotlin
val isCandidatePin = previousValue == null &&
    pendingOperation == null &&
    codeToTest.length >= 4 &&
    codeToTest.all { it.isDigit() }

if (isCandidatePin) {
    isVerifying = true
}

scope.launch {
    if (isCandidatePin) {
        val success = try {
            onUnlock(codeToTest)
        } finally {
            isVerifying = false
        }
        if (success) return@launch
    }
...
```
Несмотря на наличие флага `isVerifying`, в самом начале метода `onEquals` отсутствует guard:
```kotlin
if (isVerifying) return
```
Если пользователь быстро нажимает кнопку «=» несколько раз подряд (что часто случается при задержке отклика), запускается несколько параллельных корутин верификации.
Каждая корутина в случае ошибки вызывает `AppLockRepository.registerFailedAttempt(this@MainActivity)`, из-за чего одно ошибочное нажатие засчитывается как 2–3 неудачные попытки, досрочно блокируя пользователя на 30+ секунд.

*Лечение:* добавить проверку `if (isVerifying) return` в начале `onEquals`.

---

### T13 — `ScreenRedManageBlockSM`: блокирующий опрос диска на UI-потоке в `init` и дублирование состояния. Средняя.

[ScreenRedMnageBlockSM.kt:20-22](../feature-r/src/main/java/com/client/xvideos/r/ui/manager_block/ScreenRedMnageBlockSM.kt#L20-L22)

В `ScreenRedManageBlockSM`:
```kotlin
class ScreenRedManageBlockSM @Inject constructor() : ScreenModel {
    val _blockList = MutableStateFlow<List<GifsInfo>>(emptyList())
    val blockList: StateFlow<List<GifsInfo>> = _blockList

    init {
        _blockList.value = blockGetAllBlockedGifsInfo()
    }
}
```
1. Конструктор и блок `init` `ScreenModel` выполняются на главном потоке (UI). Вызов `blockGetAllBlockedGifsInfo()` выполняет синхронный обход дерева каталогов на диске, открытие и десериализацию всех `.block` файлов на главном потоке, вызывая фриз интерфейса при переходе на экран.
2. В проекте уже есть синглтон `BlockRed`, который асинхронно управляет списком блокировок на `Dispatchers.IO` (`val blockList: StateFlow<List<GifsInfo>>`). `ScreenRedManageBlockSM` не использует его, создавая изолированную копию, которая не обновляется при изменении блокировок.

*Лечение:* заинжектить `BlockRed` в `ScreenRedManageBlockSM` и делегировать `val blockList: StateFlow<List<GifsInfo>> = blockRed.blockList`.

---

### C23 — `R_Saved_NichesCaches`: неатомарное обновление кэша через предварительное удаление файла. Средняя.

[R_Saved_NichesCaches.kt:89-94](../feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_NichesCaches.kt#L89-L94)

В процессе обновления ниш:
```kotlin
val json = AppJson.encodeToString(niches)
val file = cacheFile()
if (file.exists()) {
    file.delete()
}
file.writeText(json)
```
Перед записью файл кэша явно удаляется, а затем записывается обычным `writeText`.
Если процесс приложения прерывается системой (OOM killer, свайп из Recent Apps) в момент между `delete()` и завершением `writeText()`, кэш ниш оказывается либо полностью утрачен, либо повреждён. При следующем запуске `readFromDisk` терпит сбой и очищает список.

*Лечение:* убрать предварительное `delete()` и перевести сохранение на `file.writeTextAtomically(json)`.

---

### C24 — `CollectionDB.readAllCollections`: выброс `IOException` при отсутствии каталога. Низкая.

[CollectionDB.kt:193](../core/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt#L193)

В `CollectionDB.readAllCollections()`:
```kotlin
val root = File(path)
if (!root.exists()) throw IOException("Каталог коллекций не найден: ${root.absolutePath}")
```
Если каталог коллекций ещё не был создан (чистая установка приложения или удаление пользователем), метод возвращает `Result.failure(IOException(...))`.
Вызывающий код в `R_Saved_Collection.kt:92`:
```kotlin
val a = collectionDb.readAllCollections()
if (a.isSuccess) {
    ...
} else {
    SnackBar.error("Ошибка чтения коллекций ${a.exceptionOrNull()?.message}")
}
```
показывает пользователю красный SnackBar об ошибке чтения коллекций при совершенно нормальном сценарии первого запуска.

*Лечение:* если каталог отсутствует, возвращать `Result.success(emptyList())`.

---

### T14 — `CrashLog`: использование несинхронизированного `SimpleDateFormat` и отсутствие лока на операциях записи. Низкая.

[CrashLog.kt:36, 89, 114-139](../core/src/main/java/com/client/xvideos/common/log/CrashLog.kt#L36)

В объекте `CrashLog`:
1. Экземпляры `timeFormat` и `format` используют класс `java.text.SimpleDateFormat`, который по спецификации Java не является потокобезопасным. При параллельных вызовах `Timber.e(...)` из фоновых потоков возможно повреждение внутреннего календаря и искажение дат.
2. Функция `appendEntry` производит `appendText(entry)`, а затем `trimToLimit(target, maxBytes)` (выполняющий `readText` и `writeText`). Без синхронизации параллельные ошибки из разных потоков могут перезаписывать друг друга или повреждать лог.

*Лечение:* защитить операции записи и форматирования дат единым объектом блокировки `synchronized(lock)`.

---

### C25 — `CoilImageLoaderFactory`: дублирование дискового кэша между OkHttp Cache и Coil DiskCache. Низкая.

[CoilImageLoaderFactory.kt:61-69, 108-117](../core/src/main/java/com/client/xvideos/common/coil/CoilImageLoaderFactory.kt#L61-L69)

В конфигурации `CoilImageLoaderFactory`:
1. Настроен дисковый кэш OkHttp: `cache(okhttp3.Cache(httpCacheDir(appContext), diskCacheMaxBytes))`.
2. Одновременно настроен собственный дисковый кэш Coil 3: `diskCache { DiskCache.Builder().directory(imageCacheDir(appContext)).maxSizeBytes(diskCacheMaxBytes).build() }`.
Оба кэша включены одновременно и имеют лимит до 2 ГБ каждый. Каждое загруженное изображение кэшируется дважды: как сырой HTTP-ответ в `http_cache` и как файл в `image_cache`, удваивая потребление дискового пространства (до 4 ГБ вместо 2 ГБ).

*Лечение:* согласно документации Coil, при использовании собственного `DiskCache` дисковый кэш `OkHttp` следует отключить (или разделить их роли).

---

### A9 — Мёртвый код и артефакты в `util`, `manager_block` и `App.kt`. Низкая.

- [ToastShow.kt:1-14](../core/src/main/java/com/client/xvideos/common/util/ToastShow.kt#L1-L14): файл состоит из 100% закомментированного устаревшего кода `GlobalScope.launch { Toast.makeText(...) }`, нигде не импортируется.
- [CMPAudioPlayer.android.kt:1-62](../core/src/main/java/com/client/xvideos/common/videoplayer/util/CMPAudioPlayer.android.kt#L1-L62): файл содержит неиспользуемый дубликат `createPlayerListener` с `didEndAudio` и приватный extension `PlayerSpeed.toFloat()`.
- [App.kt:129-178](../app/src/main/java/com/client/xvideos/App.kt#L129-L178): 45 строк закомментированных неиспользуемых блоков перехватчиков и слушателей событий.
- [ScreenRedMnageBlockSM.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/manager_block/ScreenRedMnageBlockSM.kt): опечатка в имени файла (`Mnage` вместо `Manage`).

*Лечение:* удалить неиспользуемые мёртвые файлы и закомментированные фрагменты, исправить опечатку в имени файла.

---

## Статус

| Находка | Класс | Статус | Коммит |
| --- | --- | --- | --- |
| C22 | корректность | закрыт | `f4de960` |
| S4 | безопасность / path traversal | закрыт | `f4de960` |
| T12 | конкурентность / app-lock | закрыт | `f4de960` |
| T13 | конкурентность / lifecycle | закрыт | `f4de960` |
| C23 | корректность / атомарность | закрыт | `f4de960` |
| C24 | корректность / устойчивость | закрыт | `f4de960` |
| T14 | конкурентность / потокобезопасность | закрыт | `f4de960` |
| C25 | производительность / ресурсы | закрыт | `f4de960` |
| A9 | архитектура / гигиена | закрыт | `f4de960` |

---

## Проверка

```
./gradlew detekt --rerun-tasks --continue
BUILD SUCCESSFUL in 13s
12 actionable tasks: 12 executed
(detekt 100% зелёный во всех 5 модулях: :app, :core, :feature-l, :feature-r, :feature-x)

./gradlew test --continue
BUILD SUCCESSFUL in 1m 4s
273 actionable tasks: 76 executed, 197 up-to-date
(Все 273 таски unit-тестов зелёные)

./gradlew assembleRelease
BUILD SUCCESSFUL in 1m 37s
234 actionable tasks: 32 executed, 202 up-to-date
(Release APK собран успешно, R8 minifyEnabled, dex desugaring и shrinkResources без сбоев)
```

---

## Что осталось открытым

- **P2P не трогать (решение владельца от 11.09.2026)**: тракт P2P заморожен, правки там не производятся.
- **T1, T2 (проход 7)**: `StorageCleanupGate` и архитектура `EventBus` оставлены как проектные соглашения.
- **A6 (проход 12)**: этапная миграция существующих загрузчиков на `WorkDownloadManager`.
