# Код-ревью xvideos — проход 16

> **Срез:** `0f3946b` · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода — `master` (`0f3946b`), текущий срез после закрытия всех замечаний 15-го прохода (`f4de960`, `0f3946b`).

Линзы: **безопасность файловых путей и утечки через FileProvider (`useCaseShareGifs`, `DownloadRed.delete`, `Downloader.downloadMissingFilesForRecovery`, `GallerySaver`)**,
**логика блокировки и риски перманентного lockout приложения (калькулятор-камуфляж с ведущими нулями и алфавитными PIN)**,
**конкурентность и сетевой слой KDownloader (локальный `Semaphore(4)`, дублирование `onError`, нарушение структурированной конкурентности при отмене, флуд главного потока колбэками прогресса)**,
**стабильность кэширования плееров X (перманентное кэширование пустой строки при сетевом сбое)**,
**потокобезопасность мутаций Snapshot State Compose на `Dispatchers.IO` (`SavedX_Favorites`, `ScreenAlbumListSM`)**,
**целостность файлового хранилища (атомарность записи полей `FolderTable`)**,
**UX подсистемы блокировок (отсутствие разблокировки в `ScreenRedManageBlock`)**,
**побитовые операции Kotlin (знаковое расширение байта в хешировании KDownloader)**.

Ключевой итог: **все 9 замечаний прохода 15 полностью закрыты**. Проект компилируется без замечаний, `detekt` на 100% зелёный во всех 5 модулях (`:app`, `:core`, `:feature-l`, `:feature-r`, `:feature-x`), unit-тесты (145 тасок) проходят без ошибок.
Новые находки сфокусированы на ликвидации оставшихся векторов Path Traversal и утечек файлов во внешние приложения, предотвращении блокировки пользователя камуфляжем, устранении перегрузки Main Looper и исправлении конкурентности загрузчика и Compose-состояний.

---

## Статус находок прохода 15 в текущем срезе

| Находка | Было в проходе 15 | Статус в проходе 16 | Комментарий к изменениям |
| --- | --- | --- | --- |
| **C22** | открыт (NPE в `ProgressInterceptor` при HTTP 304/204) | **закрыт** (`f4de960`) | Добавлена проверка `originalResponse.body ?: return originalResponse` |
| **S4** | открыт (Path Traversal в `blockItem` и неатомарная запись) | **закрыт** (`f4de960`) | Добавлены `isUnsafeItemName`, `requireInside` и `writeTextAtomically` |
| **T12** | открыт (гонка вызовов PIN в калькуляторе при повторном «=») | **закрыт** (`f4de960`) | Введён флаг `isVerifying` с мгновенным возвратом |
| **T13** | открыт (блокирующий I/O в `ScreenRedManageBlockSM`) | **закрыт** (`f4de960`) | Вызов `blockGetAllBlockedGifsInfo` вынесен на `Dispatchers.IO` |
| **C23** | открыт (неатомарная запись кэша тегов `TagsCache.write`) | **закрыт** (`f4de960`) | Запись переведена на `writeTextAtomically` |
| **C24** | открыт (сбой `CollectionDB.save` при отсутствии каталога) | **закрыт** (`f4de960`) | Добавлен предварительный `dir.mkdirs()` |
| **T14** | открыт (несинхронизированный `SimpleDateFormat` в `CrashLog`) | **закрыт** (`f4de960`) | Добавлен `synchronized(lock)` на форматирование и append |
| **C25** | открыт (дублирование дискового кэша OkHttp и Coil) | **закрыт** (`f4de960`) | Исключён дублирующий кэш OkHttp в пользу `DiskCache` Coil |
| **A9** | открыт (мёртвый код `ToastShow`, `CMPAudioPlayer`, опечатка) | **закрыт** (`f4de960`) | Удалены мёртвые файлы, исправлено имя `ScreenRedManageBlockSM.kt` |

---

## Проверенные области без замечаний (позитивный контроль)

1. **DNS-over-HTTPS (DoH) и Punycode:** Проверена реализация `AppDns` и `DohProvider`. Поддержка RFC 8484 (wire-format DNS запросы через `application/dns-message`), парсинг ответов A/AAAA записей, обработка IDN/Punycode в доменных именах и корректный фолбэк на `Dns.SYSTEM` при сбоях DoH работают надёжно.
2. **Резервное копирование и атомарный откат (`XlrBackupManager`):** Проверена процедура импорта/экспорта бэкапов. Временная распаковка в изолированный каталог, валидация манифеста и контрольных сумм до подмены данных, а также механизм отката (rollback) в случае прерывания процесса гарантируют защиту от повреждения хранилищ.
3. **Последовательная блокировка FileDB (`AppFileDatabase` / `FileDB`):** Проверены операции вставки, обновления и удаления записей. Локирование уровня экземпляра базы и строгая синхронизация списков предотвращают повреждение файлов базы данных при многопоточном обращении.

---

## Новые находки

### S5 — `useCaseShareGifs`: Path Traversal и утечка произвольных файлов приложения во внешние программы через FileProvider. Высокая.

[useCaseShareGifs.kt:14-19](../feature-r/src/main/java/com/client/xvideos/r/common/share/useCaseShareGifs.kt#L14-L19)

В функции отправки GIF-файла через системный диалог «Поделиться»:
```kotlin
fun useCaseShareGifs(context : Context, item: GifsInfo){
    val path = "${AppPath.r_cache_download}/${item.userName}/${item.id}.mp4"
    val file = File(path)

    try {
        if (file.exists()) {
            useCaseShareFile(context, file)
...
```
Поля `item.userName` и `item.id` формируются на основе данных из сети без очистки и валидации.
В отличие от загрузчика `DownloadRed`, где ранее были внедрены проверки `isUnsafeItemName` и `requireInside`, здесь валидация полностью отсутствует:
1. Если `item.userName` содержит последовательность вида `../../store` или `../../databases`, путь `path` выходит за пределы каталога кэша `r_cache_download`.
2. Функция `useCaseShareFile(context, file)` получает URI через Android `FileProvider` с флагом `FLAG_GRANT_READ_URI_PERMISSION`.
3. Любое внешнее приложение, выбранное пользователем в системном диалоге (или перехватившее интент), получает доступ на чтение к внутренним файлам приложения (базам данных, логам, конфигам).

*Лечение:* проверять входные компоненты пути через `!isUnsafeItemName(item.userName)` и `!isUnsafeItemName(item.id)`, нормализовать путь и гарантировать его нахождение внутри каталога загрузок через `requireInside(File(AppPath.r_cache_download), file)`.

---

### S6 — `DownloadRed.delete` и `Downloader.downloadMissingFilesForRecovery`: Path Traversal и удаление/создание файлов за пределами каталога кэша. Средняя.

[DownloadRed.kt:227-246](../feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt#L227-L246), [Downloader.kt:210-223](../feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt#L210-L223)

В методе `DownloadRed.delete`:
```kotlin
val userDirPath = AppPath.r_cache_download + SystemPathSeparator + item.userName
val userDir = File(userDirPath)

val path0 = AppPath.r_cache_download + SystemPathSeparator + item.userName + SystemPathSeparator + item.id + ".mp4"
val path1 = AppPath.r_cache_download + SystemPathSeparator + item.userName + SystemPathSeparator + item.id + ".info"
val path2 = AppPath.r_cache_download + SystemPathSeparator + item.userName + SystemPathSeparator + item.id + ".jpg"
File(path0).delete()
...
if (userDir.exists() && userDir.isDirectory) {
    val files = userDir.listFiles()
    if (files == null || files.isEmpty()) {
        userDir.delete()
    }
}
```
А также в `Downloader.downloadMissingFilesForRecovery`:
```kotlin
val folderPath = AppPath.r_cache_download + "/" + item.userName
File(folderPath).mkdirs()
val videoFile = File(folderPath, "${item.id}.mp4")
```
В основных методах загрузки `DownloadRed.download` проверка `isUnsafeItemName` была добавлена, но методы `delete` и `downloadMissingFilesForRecovery` были упущены:
- При вызове `delete` с модифицированным `item.userName` (например, `../../files`) метод попытается удалить файлы и саму директорию `userDir`, если она опустеет.
- При вызове `downloadMissingFilesForRecovery` создаются директории по произвольному пути через `File(folderPath).mkdirs()`.

*Лечение:* добавить проверки `!isUnsafeItemName(item.userName)` и `!isUnsafeItemName(item.id)` и валидацию `requireInside(File(AppPath.r_cache_download), ...)` в `delete` и `downloadMissingFilesForRecovery`.

---

### S7 — `GallerySaver`: отсутствие валидации имени файла (Path Traversal при сохранении в галерею). Средняя.

[GallerySaver.kt:38-83](../core/src/main/java/com/client/xvideos/common/gallery/GallerySaver.kt#L38-L83)

В методе сохранения файла из сети во временный каталог перед публикацией в MediaStore:
```kotlin
val tmpDir = File(appContext.cacheDir, "gallery_tmp").apply { mkdirs() }
val tmpFile = File(tmpDir, fileName)
...
val request = kDownloader.newRequestBuilder(url, tmpDir.absolutePath, fileName).build()
```
Параметр `fileName` передаётся напрямую из вызывающих слоёв (где он может формироваться из URL или данных сети).
Если `fileName` содержит относительные сегменты (`../../malicious.mp4`), файл временной загрузки `tmpFile` будет создан за пределами изолированного каталога `gallery_tmp`.

*Лечение:* санитизировать `fileName` с помощью `File(fileName).name`, проверять `!isUnsafeItemName(fileName)` и валидировать вхождение через `requireInside(tmpDir, tmpFile)`.

---

### C26 — Необратимая блокировка приложения (Lockout) через калькулятор-камуфляж при алфавитном пароле или PIN-коде с ведущими нулями. Критическая.

[AppLockSection.kt:111-124, 338-341](../app/src/main/java/com/client/xvideos/screenSettings/components/AppLockSection.kt#L111-L124), [CalculatorState.kt:53-63, 151-156](../app/src/main/java/com/client/xvideos/calculator/CalculatorState.kt#L53-L63)

В настройках защиты приложения `AppLockSection`:
1. Поле ввода мастер-пароля `PasswordSettingField` сконфигурировано как обычное текстовое поле:
   ```kotlin
   keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
       keyboardType = KeyboardType.Text,
       imeAction = ImeAction.Done
   )
   ```
   Пользователь может установить буквенно-цифровой пароль (например, `Secret123` или `pass_2026`).
2. Переключатель «Маскировка под калькулятор» проверяет только флаг `passwordSet`, но не проверяет формат пароля.
3. После включения камуфляжа приложение при запуске открывает `CalculatorScreen`. На клавиатуре калькулятора доступны **только цифры 0–9** и базовые математические операции. Ввести буквы или спецсимволы невозможно.
4. Дополнительно: в `CalculatorState.onDigit` при вводе первой цифры:
   ```kotlin
   } else if (displayValue == "0") {
       displayValue = digit
   }
   ```
   Если у пользователя установлен числовой PIN-код с ведущими нулями (например, `0042` или `0123`), ввести ведущий ноль невозможно — нажатие нуля при `displayValue == "0"` игнорируется, а нажатие следующей цифры заменяет `"0"` на эту цифру (получается `"123"` вместо `"0123"`).
   В результате верификация по нажатию «=» сверяет введённую строку с реальным PIN и завершается неудачей.

Пользователь оказывается навсегда заблокирован в экране калькулятора без возможности зайти в приложение или отключить камуфляж, теряя доступ ко всей локальной базе и загрузкам.

*Лечение:*
1. В `AppLockSection` разрешать включение камуфляжа только в том случае, если пароль состоит строго из цифр без ведущего нуля, либо предупреждать пользователя и блокировать переключатель с понятным сообщением.
2. В `PasswordSettingField` при активном камуфляже (или при установке PIN для камуфляжа) принудительно использовать `KeyboardType.NumberPassword`.
3. В `CalculatorState` поддержать ввод пин-кодов или явно запретить установку ведущих нулей в PIN.

---

### C27 — `DownloadTask`: ложные дублирующие вызовы `onError` и продолжение выполнения после HTTP 4xx/5xx. Высокая.

[DownloadTask.kt:149-175, 263-277](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt#L149-L175)

В методе исполнения загрузки `DownloadTask.run`:
```kotlin
if (!isSuccessful()) {
    listener.onError("Wrong link")
}

setResumeSupportedOrNot()
totalBytes = req.totalBytes
...
inputStream = redirectedClient.getInputStream()
if (inputStream == null) {
    return@withContext
}
```
1. При получении неудачного HTTP-кода ответа (403, 404, 500) условие `!isSuccessful()` срабатывает и вызывает `listener.onError("Wrong link")`.
2. Однако оператор `return@withContext` **отсутствует**! Метод продолжает выполнение, определяет resume, модифицирует модель в БД и пытается открыть `redirectedClient.getInputStream()`.
3. У стандартного `HttpURLConnection` метод `getInputStream()` при статусах 4xx/5xx выбрасывает `FileNotFoundException` или `IOException`.
4. Исключение попадает в блок `catch (e: Exception)` строки 269, где происходит повторный сброс состояния `req.reset()` и **второй вызов** `listener.onError(e.toString())`.
5. Слушатель прогресса получает два конфликтующих события ошибки с разными сообщениями, а в логах фиксируется паразитный стек-трейс `FileNotFoundException`.

*Лечение:* немедленно прерывать выполнение при неуспешном коде:
```kotlin
if (!isSuccessful()) {
    req.status = Status.FAILED
    listener.onError("Wrong link (HTTP $responseCode)")
    return@withContext
}
```

---

### C28 — `ScreenX_VideoPlayerSM` и `ScreenX_VideoPlayerFullScreenSM`: перманентное кэширование пустой строки при сетевом сбое. Средняя.

[ScreenX_VideoPlayerSM.kt:94-98](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt#L94-L98), [ScreenX_VideoPlayerFullScreenSM.kt:63-67](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt#L63-L67)

В логике инициализации плееров раздела X:
```kotlin
val res = db.cacheUrlStringRam.get(url)
val s = if (res == null) {
    val content = readHtmlFromURLDirect(url)
    db.cacheUrlStringRam.put(url, content)
    content
} else {
    res.content
}
```
Функция `readHtmlFromURLDirect(url)` при любой ошибке сети (таймаут, отсутствие связи, DNS error) возвращает пустую строку `""`.
Если произошёл кратковременный сетевой сбой:
1. `content` равен `""`.
2. В RAM-кэш `db.cacheUrlStringRam.put(url, "")` записывается пустая строка для данного `url`.
3. Все последующие попытки воспроизвести видео (повторный вход в плеер, смена ориентации экрана, полноэкранный режим) читают из кэша сохранённую пустую строку `""` без выполнения повторных запросов в сеть.
4. Видео перестаёт воспроизводиться вплоть до полного перезапуска процесса приложения.

*Лечение:* сохранять результат в кэш только при успешной загрузке непустого контента:
```kotlin
val content = readHtmlFromURLDirect(url)
if (content.isNotBlank()) {
    db.cacheUrlStringRam.put(url, content)
}
```

---

### C29 — `Utils.getUniqueId`: знаковое расширение байта в Kotlin при формировании hex-строки хеша. Низкая.

[Utils.kt:94-98](../core/src/main/java/com/client/xvideos/common/kdownloader/utils/Utils.kt#L94-L98)

В утилите вычисления идентификатора запроса загрузки `getUniqueId`:
```kotlin
val hex = StringBuilder(hash.size * 2)
for (b in hash) {
    if (b and 0xFF.toByte() < 0x10) hex.append("0")
    hex.append(Integer.toHexString((b and 0xFF.toByte()).toInt()))
}
return hex.toString().hashCode()
```
1. Выражение `0xFF.toByte()` в Kotlin равно `-1` (так как тип `Byte` знаковый: от `-128` до `127`).
2. Операция `b and (-1).toByte()` возвращает исходный `b`.
3. При последующем вызове `.toInt()` для отрицательного байта (от `0x80` до `0xFF`) происходит знаковое расширение (sign extension), например байт `0x8A` превращается в `Int` со значением `-118` (`0xFFFFFF8A`).
4. Метод `Integer.toHexString(-118)` возвращает 8 символов (`"ffffff8a"`), а не 2 символа (`"8a"`), что искажает и раздувает hex-строку.

*Лечение:* выполнять маскирование `0xFF` над `Int`: `Integer.toHexString(b.toInt() and 0xFF)`, либо использовать стандартное форматирование `"%02x".format(b)`.

---

### T15 — `DownloadTask.downloadSemaphore`: семафор объявлен как экземплярное свойство и не ограничивает параллелизм загрузок. Высокая.

[DownloadTask.kt:97-100](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt#L97-L100), [DownloadDispatchers.kt:31-33](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadDispatchers.kt#L31-L33)

В классе `DownloadTask`:
```kotlin
class DownloadTask(
    private val req: DownloadRequest,
    private val dbHelper: DbHelper
) {
    ...
    // Семафор на 8 разрешений
    private val downloadSemaphore = Semaphore(4)

    suspend fun run(listener: DownloadRequest.Listener) {
        downloadSemaphore.withPermit {
            ...
```
При постановке каждого запроса на загрузку диспетчер `DownloadDispatchers` создаёт **новый отдельный экземпляр** `DownloadTask(request, dbHelper)`.
Каждый созданный экземпляр `DownloadTask` имеет свой собственный, независимый `downloadSemaphore = Semaphore(4)`.
В результате семафор захватывается на объекте, существующем в единственном числе для данной корутины, и реальное количество параллельных сетевых загрузок ничем не ограничено.

*Лечение:* перенести `downloadSemaphore` в `companion object` класса `DownloadTask`, либо сделать его общим полем в `DownloadDispatchers` и передавать по ссылке в конструктор задачи.

---

### T16 — `DownloadTask` и `DownloadDispatchers`: перегрузка главного потока (UI Looper Flooding) недросселированными колбэками прогресса на каждый буфер 4 КБ. Высокая.

[DownloadTask.kt:207-245](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt#L207-L245), [DownloadDispatchers.kt:51-59](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadDispatchers.kt#L51-L59)

В цикле чтения сетевого потока `DownloadTask`:
```kotlin
do {
    val byteCount = inputStream!!.read(buff, 0, BUFFER_SIZE) // BUFFER_SIZE = 4096
    ...
    outStream.write(buff, 0, byteCount)
    req.downloadedBytes = req.downloadedBytes + byteCount
    ...
    var progress = 0
    if (totalBytes > 0) {
        progress = ((req.downloadedBytes * 100) / totalBytes).toInt()
    }
    listener.onProgress(progress)
} while (true)
```
И в `DownloadDispatchers`:
```kotlin
private fun executeOnMainThread(block: () -> Unit) {
    scope.launch(Dispatchers.Main) {
        block()
    }
}
```
1. Размер буфера `BUFFER_SIZE` составляет 4096 байт (4 КБ).
2. На каждой итерации цикла без каких-либо проверок вызывается `listener.onProgress(progress)`.
3. `DownloadDispatchers` на каждый вызов порождает новую корутину через `scope.launch(Dispatchers.Main)`.
4. При скачивании видео размером 100 МБ цикл совершает более **25 000 итераций**. В очередь главного потока Android (`Looper.getMainLooper()`) за секунды отправляется 25 000 задач.
5. Это приводит к забиванию очереди сообщений главного потока, пропуску кадров рендеринга (jank / frame drops) и риску возникновения ANR.

*Лечение:* дросселировать (throttle) отправку прогресса: вызывать `listener.onProgress` только при изменении целочисленного процента (`progress != lastReportedProgress`) и с ограничением по времени (не чаще раза в 100–200 мс).

---

### T17 — `SavedX_Favorites` и `ScreenAlbumListSM`: прямая модификация Snapshot State Compose из пула `Dispatchers.IO`. Средняя.

[SavedX_Favorites.kt:31-67](../feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Favorites.kt#L31-L67), [ScreenAlbumListSM.kt:179-220](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumListSM.kt#L179-L220)

1. В классе `SavedX_Favorites`:
   ```kotlin
   val list = favoritesDb.list // SnapshotStateList
   val favoriteIds = mutableStateSetOf<Long>()

   fun add(item: ItemsX) {
       scope.launch(Dispatchers.IO) {
           favoritesDb.insert(...)
               .onSuccess {
                   list.add(item)
                   favoriteIds.add(item.id)
   ```
2. В `ScreenAlbumListSM`:
   ```kotlin
   val bigList = mutableStateMapOf<Int, AlbumListImplInfoAndListAndStatus>()

   fun loadAlbumList(page: Int) {
       screenModelScope.launch(Dispatchers.IO) {
           ...
           bigList.put(page, ...)
   ```
Коллекции `mutableStateListOf`, `mutableStateSetOf` и `mutableStateMapOf` принадлежат механизму снапшотов Jetpack Compose.
Их прямая мутация из фоновых потоков пула `Dispatchers.IO` (без перехода на главный поток или явного управления `Snapshot.withMutableSnapshot`) приводит к гонкам применения снапшотов (snapshot conflict), потерянным рекомпозициям и неконсистентному отображению списков в UI.

*Лечение:* переключать контекст на `Dispatchers.Main` перед мутацией наблюдаемых Compose-структур данных.

---

### T18 — `DownloadTask`: перехват `CancellationException` и нарушение контракта структурированной конкурентности. Средняя.

[DownloadTask.kt:263-268](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt#L263-L268)

В блоке обработки исключений `DownloadTask.run`:
```kotlin
} catch (e: CancellationException) {
    deleteTempFile()
    req.reset()
    req.status = Status.FAILED
    listener.onError(e.toString())
    return@withContext
} catch (e: Exception) {
```
1. Класс `CancellationException` перехватывается, и статус задачи выставляется в `Status.FAILED` вместо `Status.CANCELLED`.
2. Слушателю отправляется событие `onError(e.toString())`, сигнализируя об ошибке загрузки вместо штатной отмены.
3. Главное: исключение `CancellationException` **поглощается** (не пробрасывается дальше). Это нарушает механизм структурированной конкурентности корутин Kotlin, не позволяя родительским скоупам и Job корректно завершить отмену.

*Лечение:* выставить `req.status = Status.CANCELLED`, вызвать `listener.onError("Cancelled")` (или специальный колбэк отмены) и повторно выбросить `throw e`.

---

### A10 — `FolderTable.writeField`: нестандартная реализация записи с риском повреждения данных вместо общего `writeTextAtomically`. Низкая.

[FolderTable.kt:113-123](../core/src/main/java/com/client/xvideos/common/fileDB/folder/FolderTable.kt#L113-L123)

В `FolderTable`:
```kotlin
private fun writeField(rowDir: File, field: String, value: String) {
    val target = File(rowDir, fieldFileName(field))
    val tmp = File(rowDir, "${target.name}.tmp")
    tmp.writeText(value, Charsets.UTF_8)
    if (target.exists() && !target.delete()) {
        throw IllegalStateException("Cannot replace file: ${target.absolutePath}")
    }
    if (!tmp.renameTo(target)) {
        throw IllegalStateException("Cannot commit file: ${target.absolutePath}")
    }
}
```
1. Используется детерминированное имя временного файла `"${target.name}.tmp"`.
2. Операция удаления `target.delete()` выполняется перед `tmp.renameTo(target)`. В случае сбоя или падения процесса между `delete` и `renameTo` файл поля безвозвратно теряется.
3. В модуле `:core` существует проверенный стандарт проекта `File.writeTextAtomically(value)` (`AtomicWrite.kt`), создающий уникальный временный файл и гарантирующий безопасную замену.

*Лечение:* заменить кастомную реализацию на `target.writeTextAtomically(value)`.

---

### UI10 — `ScreenRedManageBlock`: отсутствие возможности разблокировки элементов в интерфейсе менеджера блокировок. Низкая.

[ScreenRedManageBlock.kt:57-75](../feature-r/src/main/java/com/client/xvideos/r/ui/manager_block/ScreenRedManageBlock.kt#L57-L75), [BlockRed.kt:22-60](../feature-r/src/main/java/com/client/xvideos/r/common/block/BlockRed.kt#L22-L60)

В экране управления блокировками `ScreenRedManageBlock`:
- Пользователю выводится список заблокированных GIF-элементов `LazyColumn(items(blockList))`.
- Однако в элементе списка отображаются только миниатюра, имя автора и ID.
- Ни в элементе списка, ни в объекте `BlockRed` нет действия «Разблокировать» (удалить файл блокировки). Заблокированный по ошибке элемент невозможно вернуть в ленту через UI без ручной очистки дискового каталога приложения.

*Лечение:* добавить в `BlockRed` функцию `unblockItem(item: GifsInfo)` (удаляющую соответствующий `.block`-файл) и добавить кнопку удаления блокировки (иконку корзины/крестика) в строку списка `ScreenRedManageBlock`.

---

## Статус

| Находка | Класс | Статус | Коммит |
| --- | --- | --- | --- |
| S5 | безопасность / file provider leak | закрыт | рабочее дерево |
| S6 | безопасность / path traversal | закрыт | рабочее дерево |
| S7 | безопасность / path traversal | закрыт | рабочее дерево |
| C26 | корректность / app lockout | закрыт | рабочее дерево |
| C27 | корректность / сеть | закрыт | рабочее дерево |
| C28 | корректность / кэш | закрыт | рабочее дерево |
| C29 | корректность / битовые операции | закрыт | рабочее дерево |
| T15 | конкурентность / семафор | закрыт | рабочее дерево |
| T16 | производительность / UI looper | закрыт | рабочее дерево |
| T17 | конкурентность / compose snapshots | закрыт | рабочее дерево |
| T18 | конкурентность / coroutine cancellation | закрыт | рабочее дерево |
| A10 | архитектура / атомарность I/O | закрыт | рабочее дерево |
| UI10 | UI/UX / управление блокировками | закрыт | рабочее дерево |

---

## Проверка

```
./gradlew testDebugUnitTest detekt
BUILD SUCCESSFUL in 32s
145 actionable tasks: 30 executed, 115 up-to-date
(Все 145 тасок unit-тестов и detekt во всех 5 модулях зелёные)

./gradlew assembleRelease
BUILD SUCCESSFUL in 1m 52s
234 actionable tasks: 73 executed, 161 up-to-date
(Релизный APK успешно собран с R8, сжатием ресурсов и desugaring)
```

---

## Что осталось открытым

- **P2P не трогать (решение владельца от 11.09.2026)**: тракт P2P заморожен, правки там не производятся.
- **T1, T2 (проход 7)**: `StorageCleanupGate` и архитектура `EventBus` оставлены как проектные соглашения.
- **A6 (проход 12)**: этапная миграция существующих загрузчиков на `WorkDownloadManager`.
