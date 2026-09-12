# Код-ревью xvideos — проход 13

> **Срез:** `6361302` · **Статус:** открыт · **Индекс:** [все документы](README.md)

База прохода — `master` (`6361302`), текущий срез после закрытия замечаний 12-го прохода (`fe01a55`, `6361302`).

Линзы: **подсистема фоновых загрузок WorkManager (диспетчеризация, отмена, обработка тела)**,
**подсистема DNS-over-HTTPS (DoH: нормализация хостов, Punycode, безопасность соединений)**,
**Media3-плеер (нагрузка на CPU/Compose при паузе, синхронизация таймеров)**,
**сохранение загрузок и метаданных в :feature-r, :feature-l, :feature-x (path traversal, атомарность)**,
**состояние гейтов сборки (detekt, release build, unit-тесты)**.

Ключевой итог: **все замечания проходов 11 и 12 полностью закрыты**. Проект компилируется,
`detekt` на 100% зелёный во всех 5 модулях, unit-тесты (273 таски) проходят без единой ошибки,
`assembleRelease` с R8 minification и shrinkResources собирается успешно.
Новые находки сфокусированы на диспетчеризации I/O потоков воркера, надёжности парсинга DoH,
устранении избыточных рекомпозиций плеера на паузе и атомарности сохранения метаданных.

---

## Статус находок прохода 12 в текущем срезе

| Находка | Было в проходе 12 | Статус в проходе 13 | Комментарий к изменениям |
| --- | --- | --- | --- |
| **S1** | открыт (path traversal в воркере) | **закрыт** (`fe01a55`) | Добавлена проверка `isUnsafeItemName` в конструкторе `DownloadWorkRequest` и `requireInside` в `MediaDownloadWorker.prepareFiles` |
| **C12** | открыт (битые cookie из-за `split(";")`) | **закрыт** (`fe01a55`) | Сериализация заголовков переведена на JSON (`AppJsonCompact`) с фолбэком на старый формат |
| **C13** | открыт (вечный цикл при HTTP 416) | **закрыт** (`fe01a55`) | В `MediaDownloadWorker` при HTTP 416 временный `.tmp` удаляется и загрузка повторяется с 0-го байта |
| **C14** | открыт (ложный DoH bypass по подстроке) | **закрыт** (`fe01a55`) | Сравнение хоста кастомного DoH переведено на точное `URI(customUrl).host == host` |
| **C15** | открыт (безлимитный кэш DNS) | **закрыт** (`fe01a55`) | Введён лимит кэша на 256 записей с очисткой по TTL и LRU-вытеснением |
| **T8** | открыт (параллельный PBKDF2 в калькуляторе) | **закрыт** (`fe01a55`) | Добавлен синхронный guard-флаг `isVerifying` в `CalculatorState` |
| **UI7** | открыт (конфликт FLAG_SECURE и скриншотов) | **закрыт** (`fe01a55`) | `FLAG_SECURE` сделан динамическим: очищается в `onResume` и включается в `onPause` при активном `blur_recent_tasks` |
| **A6** | открыт (изолированный WorkDownloadManager) | **принят** | Зафиксирован в бэклоге для этапной миграции загрузчиков |

---

## Новые находки

### T9 — `MediaDownloadWorker`: блокирующий I/O в `CoroutineWorker.doWork()` на `Dispatchers.Default`. Средняя.

[MediaDownloadWorker.kt:47](../core/src/main/java/com/client/xvideos/common/download/work/MediaDownloadWorker.kt#L47)

В WorkManager класс `CoroutineWorker.doWork()` по умолчанию выполняется на пуле потоков `Dispatchers.Default` (CPU-bound пул с числом потоков, равным количеству процессорных ядер).
Внутри `MediaDownloadWorker` осуществляются длительные блокирующие операции:
- `downloadOkHttpClient.newCall(request).execute()` (блокирующее сетевое рукопожатие);
- `input.read(buffer)` и `output.write(buffer)` (потоковое чтение и запись гигабайтных медиа-файлов на диск).

Блокирование рабочих потоков `Dispatchers.Default` дисковым и сетевым вводом-выводом приводит к голоданию других корутин приложения (включая расчёты Compose и фоновые бизнес-операции).

*Лечение:* переопределить `coroutineContext` воркера на `Dispatchers.IO` либо обернуть тело `doWork()` в `withContext(Dispatchers.IO)`.

---

### C16 — `MediaDownloadWorker`: потенциальный NPE при пустом ответе и отсутствие отмены OkHttp Call при остановке воркера. Средняя.

[MediaDownloadWorker.kt:209-226](../core/src/main/java/com/client/xvideos/common/download/work/MediaDownloadWorker.kt#L209-L226),
[MediaDownloadWorker.kt:303](../core/src/main/java/com/client/xvideos/common/download/work/MediaDownloadWorker.kt#L303)

1. В OkHttp свойство `response.body` является аннотированным как `@Nullable` (`ResponseBody?`). В текущем коде:
   ```kotlin
   val responseBody = response.body
   val totalBytes = calculateTotalBytes(isResume, resumeOffset, responseBody.contentLength())
   ```
   Если сервер возвращает ответ без тела (например, HTTP 204/304), обращение к `responseBody.contentLength()` завершится `NullPointerException` вместо информативного `IOException`.
2. В цикле скачивания:
   ```kotlin
   while (input.read(buffer).also { bytesRead = it } != -1) {
       if (isStopped) return
   ...
   ```
   Если воркер отменяется системой или пользователем (`cancelWorkById`), флаг `isStopped` проверяется только *после* завершения блокирующего вызова `input.read(buffer)`. При медленном или зависшем соединении этот вызов будет блокировать поток до 60 секунд (сокетный таймаут), удерживая соединение.

*Лечение:* проверять `response.body ?: throw IOException("Empty body")` и привязывать отмену сетевого запроса к отмене корутины (`coroutineContext.job.invokeOnCompletion { call.cancel() }`).

---

### C17 — `AppDns`: регистрозависимость кэша, утечка сокета при ошибке парсинга DoH и отсутствие поддержки IDN/Punycode. Средняя.

[AppDns.kt:69](../core/src/main/java/com/client/xvideos/common/net/doh/AppDns.kt#L69),
[AppDns.kt:205-214](../core/src/main/java/com/client/xvideos/common/net/doh/AppDns.kt#L205-L214)

1. По спецификации RFC 1035 доменные имена в DNS регистронезависимы. В `AppDns` переменная `cleanHost` не приводится к нижнему регистру: хосты `api.redgifs.com` и `API.REDGIFS.COM` сохраняются в `cache` как разные ключи, вызывая лишние сетевые запросы DoH.
2. В методе `fetchDohAnswers`:
   ```kotlin
   val response = dohHttpClient.newCall(request).execute()
   if (!response.isSuccessful) {
       val code = response.code
       response.close()
       throw IOException("DoH HTTP error $code from $endpoint")
   }
   val body = response.body.string()
   val dohResponse = json.decodeFromString<DohResponse>(body)
   ```
   Если `json.decodeFromString` выбросит исключение (например, шлюз DoH вернул HTML-страницу ошибки или капчу), тело ответа не будет закрыто через `response.use`, что приводит к утечке соединений OkHttp.
3. Если запрашивается интернационализованное имя домена (IDN, содержащее не-ASCII символы), URL-строка `name=$hostname` сформирует невалидный `Request`, вызывающий `IllegalArgumentException` в OkHttp.

*Лечение:* нормализовать хост через `IDN.toASCII(cleanHost.lowercase())`, оборачивать вызов в `dohHttpClient.newCall(request).execute().use { ... }` с безопасным чтением `response.body?.string()`.

---

### UI8 — `CMPPlayer2`: холостой 20 Гц поллинг времени и лишние рекомпозиции при паузе видео. Низкая.

[CMPlayer2.kt:68-75](../core/src/main/java/com/client/xvideos/common/videoplayer/util/CMPlayer2.kt#L68-L75)

В `CMPPlayer2` запущен LaunchedEffect опроса позиции воспроизведения:
```kotlin
LaunchedEffect(exoPlayer) {
    flow {
        while (isActive) {
            emit((exoPlayer.currentPosition / 1000f).coerceAtLeast(0f))
            delay(50)
        }
    }.collectLatest { callbacks.currentTime(it) }
}
```
Корутина опрашивает плеер каждые 50 миллисекунд (20 раз в секунду) непрерывно, даже когда плеер стоит на паузе (`config.isPause == true`). При каждом тике вызывается `callbacks.currentTime(it)`, что в `StaticPlayer` обновляет Compose-состояние `playerHost.currentTime` и вызывает избыточные рекомпозиции элементов управления (слайдера и текста таймера).
В соседнем `RedPooledVideoPlayer` эта проблема уже решена проверкой `if (position != lastPosition)`.

*Лечение:* проверять изменение позиции перед вызовом `callbacks.currentTime` либо приостанавливать таймер при `config.isPause`.

---

### S2 — `Downloader` (:feature-r): отсутствие проверки `userName` на path traversal при загрузке медиа. Низкая.

[Downloader.kt:57](../feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt#L57),
[Downloader.kt:109](../feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt#L109)

В модуле `:feature-r` папка креатора создаётся конкатенацией строки:
```kotlin
val p = AppPath.r_cache_download + "/" + item.userName
File(p).mkdirs()
```
Если имя пользователя получено из ненадёжного источника или сформировано с относительными сегментами (`../../`), файлы медиа могут быть записаны за пределы каталога `r_cache_download`.

*Лечение:* валидировать `isUnsafeItemName(item.userName)` и проверять границы через `requireInside(File(AppPath.r_cache_download), File(p))`.

---

### C18 — `Downloader`, `SavedX_Downloads`, `LCollectionFs`: неатомарная запись метаданных и конфигураций. Низкая.

[Downloader.kt:86](../feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt#L86),
[SavedX_Downloads.kt:120](../feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt#L120),
[LCollectionFs.kt:191](../feature-l/src/main/java/com/client/xvideos/l/featured/saved/LCollectionFs.kt#L191)

В проекте реализована специальная утилита `writeTextAtomically` для исключения повреждения JSON-файлов при внезапном закрытии процесса. Однако сохранение `.info`-файлов в R и X, а также запись `LCollectionConfig` в L до сих пор используют стандартный `writeText(...)`, что при обрыве питания или принудительной остановке приложения оставляет битые JSON-файлы («битый .info»).

*Лечение:* перевести сохранение метаданных и конфигов на `writeTextAtomically(...)`.

---

### A7 — `:feature-x`: неиспользуемый экземпляр Ktor-клиента и мёртвый метод `getSearchResults`. Низкая.

[getSearchResults.kt:16-59](../feature-x/src/main/java/com/client/xvideos/x/search/getSearchResults.kt#L16-L59)

В модуле `:feature-x` инициализируется процессный `searchHttpClient` и объявлен метод `getSearchResults(query: String)`. Метод нигде в проекте не вызывается и не имеет потребителей в UI, удерживая ресурсы под неиспользуемый стек соединений.

*Лечение:* удалить неиспользуемый код либо запланировать интеграцию в поисковый экран X.

---

## Статус

| Находка | Класс | Статус | Коммит |
| --- | --- | --- | --- |
| T9 | конкурентность | открыт | — |
| C16 | корректность | открыт | — |
| C17 | корректность | открыт | — |
| UI8 | интерфейс / UX | открыт | — |
| S2 | безопасность | открыт | — |
| C18 | корректность | открыт | — |
| A7 | архитектура / гигиена | открыт | — |

---

## Проверка

```
./gradlew detekt --continue
BUILD SUCCESSFUL in 1m 10s
12 actionable tasks: 12 up-to-date
(detekt 100% зелёный во всех 5 модулях: :app, :core, :feature-l, :feature-r, :feature-x)

./gradlew test --continue
BUILD SUCCESSFUL in 4s
273 actionable tasks: 273 up-to-date
(Все 273 таски unit-тестов зелёные)

./gradlew assembleRelease
BUILD SUCCESSFUL in 1m 29s
234 actionable tasks: 32 executed, 202 up-to-date
(Release APK собран успешно, R8 minifyEnabled, dex desugaring и shrinkResources без сбоев)
```

---

## Что осталось открытым

- **P2P не трогать (решение владельца от 11.09.2026)**: тракт P2P заморожен, правки там не производятся.
- **T1, T2 (проход 7)**: `StorageCleanupGate` и архитектура `EventBus` оставлены как проектные соглашения.
- **A6 (проход 12)**: этапная миграция существующих загрузчиков на `WorkDownloadManager`.
