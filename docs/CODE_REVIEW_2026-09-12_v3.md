# Код-ревью xvideos — проход 14

> **Срез:** `6bdca7d` · **Статус:** открыт · **Индекс:** [все документы](README.md)

База прохода — `master` (`6bdca7d`), текущий срез после закрытия всех замечаний 13-го прохода (`64e30e9`, `6bdca7d`).

Линзы: **подсистема in-process загрузок KDownloader (жизненный цикл Scope, lateinit в finally, утечки соединений, редиректы, потокобезопасность)**,
**файловая база FolderTable / FileDB (проверка ключей, защита от path traversal)**,
**ExoPlayer и жизненный цикл Compose (рассинхронизация состояния паузы при сворачивании)**,
**мониторинг трафика NetworkTrafficMonitor (корректность формул и единиц измерения)**,
**гигиена кодовой базы и устаревшие артефакты DI**.

Ключевой итог: **все замечания прохода 13 полностью закрыты**. Проект стабилен,
`detekt` на 100% зелёный во всех 5 модулях, unit-тесты (273 таски) проходят без единой ошибки,
`assembleRelease` с R8 minification и shrinkResources собирается успешно.
Новые находки сфокусированы на надёжности встроенного загрузчика `KDownloader`, исключении path traversal в `FolderTable`, исправлении рассинхронизации паузы плеера при возврате из фона и устранении ошибки удвоения скорости в мониторе трафика.

---

## Статус находок прохода 13 в текущем срезе

| Находка | Было в проходе 13 | Статус в проходе 14 | Комментарий к изменениям |
| --- | --- | --- | --- |
| **T9** | открыт (блокирующий I/O на Default) | **закрыт** (`64e30e9`) | `MediaDownloadWorker.doWork()` переведён на `withContext(Dispatchers.IO)` |
| **C16** | открыт (NPE на пустом ответе и зависание отмены) | **закрыт** (`64e30e9`) | Добавлена проверка `response.body` и привязка `call.cancel()` к отмене корутины воркера |
| **C17** | открыт (регистрозависимость DoH, утечка сокета, IDN) | **закрыт** (`64e30e9`) | Введена нормализация `IDN.toASCII(lowercase())`, сетевой вызов обёрнут в `execute().use`, добавлены unit-тесты |
| **UI8** | открыт (20 Гц поллинг времени плеера на паузе) | **закрыт** (`64e30e9`) | В `CMPPlayer2` добавлен guard `position != lastPosition` и выход из цикла при `config.isPause` |
| **S2** | открыт (path traversal в Downloader R) | **закрыт** (`64e30e9`) | Добавлены `isUnsafeItemName` и `requireInside(rootDir, creatorDir)` |
| **C18** | открыт (неатомарная запись .info и конфигов) | **закрыт** (`64e30e9`) | Все записи переведены на `File.writeTextAtomically` во всех модулях |
| **A7** | открыт (мёртвый searchHttpClient в X) | **закрыт** (`64e30e9`) | Функция `getSearchResults` помечена как `@Deprecated` |

---

## Новые находки

### S3 — `FolderTable`: уязвимость Path Traversal через `safeKeyRegex` (`.` и `..`). Высокая.

[FolderTable.kt:25, 86-90](../core/src/main/java/com/client/xvideos/common/fileDB/folder/FolderTable.kt#L25)

В `FolderTable` директория записи вычисляется следующим образом:
```kotlin
private val safeKeyRegex = Regex("[A-Za-z0-9._-]{1,120}")

private fun rowDir(key: String, createTableDir: Boolean): File {
    if (createTableDir) tableDir.mkdirs()
    val dirName = if (safeKeyRegex.matches(key)) key else key.toMD5()
    return File(tableDir, dirName)
}
```
Строки `.` и `..` состоят исключительно из символов `.` и имеют длину от 1 до 120 символов, поэтому `safeKeyRegex.matches("..")` возвращает `true`.
В результате:
- `rowDir("..")` возвращает `File(tableDir, "..")`, что разрешается в родительский каталог `tableDir` (каталог всей базы данных `AppPath.file_db`);
- последующий вызов `delete("..")` приводит к `rowDir.deleteRecursively()`, что **безвозвратно удаляет корень всей базы данных приложения** со всеми таблицами;
- вызов `upsert("..", ...)` создаёт файлы полей прямо в корне базы данных;
- вызов `delete(".")` удаляет саму таблицу `tableDir`.

*Лечение:* проверять ключ через `!isUnsafeItemName(key) && safeKeyRegex.matches(key)` и в случае несоответствия хэшировать в `key.toMD5()`. Дополнительно проверять `requireInside(tableDir, rowDir)`.

---

### T10 — `DownloadDispatchers`: вызов `scope.cancel()` в `cancelAll()` безвозвратно ломает запуск будущих загрузок. Высокая.

[DownloadDispatchers.kt:82-87](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadDispatchers.kt#L82-L87)

В методе `cancelAll()`:
```kotlin
fun cancelAll() {
    scope.cancel()
    dbScope.launch {
        dbHelper.empty()
    }
}
```
Вызов `scope.cancel()` безвозвратно переводит внутренний `SupervisorJob` в терминальное состояние `CANCELLED`.
В Kotlin Coroutines отменённый `CoroutineScope` не может быть перезапущен: любые последующие вызовы `scope.launch { execute(req) }` в `enqueue()` будут немедленно отменяться без выполнения тела. Если пользователь или логика экрана вызовет `cancelAll()`, текущий экземпляр `KDownloader` навсегда прекращает скачивать файлы до полного перезапуска процесса приложения.

*Лечение:* отменять только дочерние корутины через `scope.coroutineContext.cancelChildren()`, сохраняя сам `scope` активным для последующих вызовов.

---

### C19 — `DownloadTask`: падение с `UninitializedPropertyAccessException` в блоке `finally`. Средняя.

[DownloadTask.kt:33, 186, 274, 321](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt#L33)

В `DownloadTask` поток записи объявлен как:
```kotlin
private lateinit var outputStream: FileDownloadOutputStream
```
Инициализация происходит на строке 186:
```kotlin
this@DownloadTask.outputStream = FileDownloadRandomAccessFile.Companion.create(file)
```
А в блоке `finally`:
```kotlin
} finally {
    closeAllSafely(outputStream)
}
```
Если любая сетевая ошибка или исключение происходит до строки 186 (например, сбой DNS, таймаут соединения в `httpClient.connect(req)`, ошибка создания файла или неверный HTTP-код), выполнение переходит в секцию `finally`, где обращение к неинициализированному `outputStream` выбрасывает `UninitializedPropertyAccessException`. Это исключение вылетает из `finally`, затирая реальную ошибку сети (`IOException`) и приводя к крашу рабочей корутины загрузчика.
Аналогичная проблема существует и для поля `internal lateinit var job: Job` в `DownloadRequest.kt`, если `cancel()` вызывается до завершения планирования корутины.

*Лечение:* объявить `outputStream` как `FileDownloadOutputStream? = null` и проверять на `null` в `closeAllSafely()`; объявить `job` как nullable `Job? = null`.

---

### C20 — `DownloadTask` и `Utils`: утечка HTTP-соединения при HTTP 416 и сбой на относительных редиректах. Средняя.

[DownloadTask.kt:294-306](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt#L294-L306),
[Utils.kt:63-77](../core/src/main/java/com/client/xvideos/common/kdownloader/utils/Utils.kt#L63-L77)

1. В методе `checkIfFreshStartRequiredAndStart`:
   ```kotlin
   if (responseCode == Constants.HTTP_RANGE_NOT_SATISFIABLE || isETagChanged(model)) {
       ...
       httpClient = DefaultHttpClient().clone()
       httpClient.connect(req)
   ```
   Предыдущий `httpClient` уже открыл сетевое соединение в строке 135 (`httpClient.connect(req)`). При получении HTTP 416 ссылка перетирается новым экземпляром без вызова `httpClient.close()`, что оставляет открытый сетевой сокет и поток ответа брошенными в памяти.
2. В `getRedirectedConnectionIfAny`:
   ```kotlin
   while (isRedirection(code)) {
       ...
       req.url = (location)
       httpClient = DefaultHttpClient().clone()
       httpClient.connect(req)
   ```
   Если сервер возвращает относительный редирект (например, `Location: /video/stream.mp4`, что полностью легитимно по RFC 7231), присваивание `req.url = location` ломает URL: OkHttp в `Request.Builder().url(req.url)` выбрасывает `IllegalArgumentException: Expected URL scheme 'http' or 'https'`.
3. Кроме того, метод бросает `IllegalAccessException` (рефлексивное исключение времени выполнения) вместо `IOException`.

*Лечение:* закрывать предыдущий клиент перед пересозданием; нормализовать редиректы через `URI(req.url).resolve(location).toString()`; бросать `IOException`.

---

### UI9 — `rememberExoPlayerWithLifecycle`: захват устаревшего `isPause` возобновляет видео после паузы при возврате из фона. Средняя.

[rememberExoPlayerWithLifecycle.kt:119-125](../core/src/main/java/com/client/xvideos/common/videoplayer/rememberExoPlayerWithLifecycle.kt#L119-L125)

В `rememberExoPlayerWithLifecycle`:
```kotlin
var appInBackground by remember { mutableStateOf(false) }

DisposableEffect(key1 = lifecycleOwner, appInBackground) {
    val lifecycleObserver = getExoPlayerLifecycleObserver(exoPlayer, isPause, appInBackground) {
        appInBackground = it
    }
    lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
    onDispose { lifecycleOwner.lifecycle.removeObserver(lifecycleObserver) }
}
```
Параметр `isPause` не включён в список ключей `DisposableEffect`. Когда пользователь ставит видео на паузу (`isPause = true`), `DisposableEffect` не перезапускается, и зарегистрированный `lifecycleObserver` удерживает в замыкании старое значение `isPause = false`.
Если после паузы пользователь сворачивает приложение и возвращается в него (`ON_RESUME`):
```kotlin
private fun handleOnResume(...) {
    if (wasAppInBackground) {
        exoPlayer.playWhenReady = !isPause
    }
    ...
}
```
Считывается устаревшее значение `isPause == false`, из-за чего `playWhenReady` выставляется в `true`, и видео, которое пользователь явно поставил на паузу, самопроизвольно начинает играть.

*Лечение:* использовать `val currentIsPause by rememberUpdatedState(isPause)` и передавать актуальное состояние (или геттер `() -> Boolean`) в наблюдатель жизненного цикла.

---

### C21 — `NetworkTrafficMonitor`: двукратное завышение скорости сетевого трафика. Низкая.

[NetworkTrafficMonitor.kt:97-105](../core/src/main/java/com/client/xvideos/common/traficStatistic/NetworkTrafficMonitor.kt#L97-L105)

В расчёте скорости сети:
```kotlin
val timeDiff = (currentTime - previousTime) / timeout.toFloat()

val downloadSpeed = if (timeDiff > 0 && previousRxBytes > 0) {
    ((currentRxBytes - previousRxBytes) / timeDiff).toLong().coerceAtLeast(0L)
} else 0L
```
Значение `timeout` равно `2000L` миллисекунд. Дельта `currentTime - previousTime` составляет около `2000` мс.
Деление `(currentTime - previousTime) / timeout.toFloat()` даёт значение `~1.0` (число 2-секундных интервалов), а не число секунд (`~2.0`).
Деление объёма переданных байт на `timeDiff` (`1.0`) вычисляет объём за 2 секунды, а не скорость за секунду (`байт/с`). В результате скорость скачивания и отдачи, отображаемая в UI, в два раза выше фактической скорости передачи данных.

*Лечение:* делить дельту времени на `1000f` (`val timeDiffSec = (currentTime - previousTime) / 1000f`) для перевода в секунды.

---

### T11 — `DownloadRequestQueue`: непотокобезопасный `HashMap` в многопоточном диспетчере. Низкая.

[DownloadRequestQueue.kt:7](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadRequestQueue.kt#L7)

В `DownloadRequestQueue`:
```kotlin
private val idRequestMap: HashMap<Int, DownloadRequest> = hashMapOf()
```
Очередь запросов `idRequestMap` модифицируется и опрашивается из разных потоков: UI-потока (вызовы `enqueue`, `status`, `getRequestsByTag`, `cancel`) и фоновых потоков загрузки (прогресс, смена статусов). Использование несинхронизированного `HashMap` в параллельной среде несёт риск `ConcurrentModificationException` и повреждения внутренней структуры хэш-таблицы при одновременных операциях.

*Лечение:* заменить `HashMap` на `ConcurrentHashMap<Int, DownloadRequest>`.

---

### A8 — `:feature-r`: заброшенный файл `moduleKDownloader.kt` со 100% закомментированным кодом. Низкая.

[moduleKDownloader.kt:1-12](../feature-r/src/main/java/com/client/xvideos/r/common/downloader/di/moduleKDownloader.kt#L1-L12)

Файл `moduleKDownloader.kt` состоит исключительно из закомментированного старого Hilt-модуля (строки 3-11). Рабочий провайдер `KDownloader` перенесён в `LusciousModule.kt` в модуле `:feature-l`. Мёртвый файл создаёт путаницу при анализе DI-архитектуры загрузчиков.

*Лечение:* удалить файл `moduleKDownloader.kt`.

---

## Статус

| Находка | Класс | Статус | Коммит |
| --- | --- | --- | --- |
| S3 | безопасность | открыт | — |
| T10 | конкурентность / жизненный цикл | открыт | — |
| C19 | корректность | открыт | — |
| C20 | корректность | открыт | — |
| UI9 | интерфейс / UX | открыт | — |
| C21 | корректность | открыт | — |
| T11 | конкурентность | открыт | — |
| A8 | архитектура / гигиена | открыт | — |

---

## Проверка

```
./gradlew detekt --rerun-tasks --continue
BUILD SUCCESSFUL in 12s
12 actionable tasks: 12 executed
(detekt 100% зелёный во всех 5 модулях: :app, :core, :feature-l, :feature-r, :feature-x)

./gradlew test --continue
BUILD SUCCESSFUL in 57s
273 actionable tasks: 86 executed, 187 up-to-date
(Все 273 таски unit-тестов зелёные)

./gradlew assembleRelease
BUILD SUCCESSFUL in 1m 22s
234 actionable tasks: 44 executed, 190 up-to-date
(Release APK собран успешно, R8 minifyEnabled, dex desugaring и shrinkResources без сбоев)
```

---

## Что осталось открытым

- **P2P не трогать (решение владельца от 11.09.2026)**: тракт P2P заморожен, правки там не производятся.
- **T1, T2 (проход 7)**: `StorageCleanupGate` и архитектура `EventBus` оставлены как проектные соглашения.
- **A6 (проход 12)**: этапная миграция существующих загрузчиков на `WorkDownloadManager`.
