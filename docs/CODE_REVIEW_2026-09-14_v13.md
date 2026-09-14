# Отчёт о код-ревью: 14 сентября 2026 (проход 34, v13)

Срез: `master` (`a03105e`)
Фокус прохода: мультилинзовое ревью по направлениям **Correctness**, **Concurrency / Lifecycle**, **Architecture / Performance**, **Code Hygiene / Detekt**.

---

## Сводка находок

| ID | Линза | Серьёзность | Статус | Суть |
| --- | --- | --- | --- | --- |
| **T40** | Concurrency / Network Handover | Высокая | **закрыт** | В `AndroidConnectivityObserver` при смене сети (Wi-Fi ↔ сотовая) коллбэк `onLost` старой сети безусловно сбрасывал состояние подключения в `false`, не проверяя активную сеть; `getSystemService` содержал жесткий `!!`; оставлены отладочные префиксы `"!!! 999"`. |
| **C74** | Correctness / UI Data | Высокая | **закрыт** | В `UrlImage` ссылки по протоколу `http://` ошибочно распознавались как локальные файлы и приводили к сбою Coil; параметр `albumName` отсутствовал в ключах `remember(url, urlGif)`. |
| **C75** | Correctness / Device Compatibility | Средняя | **закрыт** | В `vibrateWithPatternAndAmplitude` небезопасный каст `as Vibrator` приводил к NPE/CCE на устройствах без сервиса; отсутствовали проверки `hasVibrator()` и `hasAmplitudeControl()`, а также поддержка `VibratorManager` на Android 12+ (API 31+). |
| **C76** | Correctness / Resilience | Высокая | **закрыт** | В `DownloadTask` блок `catch (e: Exception)` безусловно удалял скачанный временный файл и запись из БД при сетевых сбоях даже для серверов с поддержкой докачки (`isResumeSupported == true`). |
| **A16** | Performance / Redundant Parsing | Средняя | **закрыт** | В `ScreenX_VideoPlayerSM` разбор страницы видео выполнялся через `Jsoup.parse()` дважды подряд (`parserItemVideo` и `parserItemVideoTags`). Переведено на однократный разбор `Document`. |
| **H1** | Code Hygiene / Detekt | Средняя | **закрыт** | Падение задачи `:core:detekt` (7 ошибок) из-за неиспользуемых импортов `Build`/`NetworkRequest` и несовпадения хэша baseline для `UrlImage` после правок порядка аргументов. |

---

## Подробное описание и решения

### T40 — Устойчивость сетевого состояния при хэндовере и безопасная инициализация в `AndroidConnectivityObserver`
- **Файл:** [AndroidConnectivityObserver.kt](../core/src/main/java/com/client/xvideos/common/connectivityObserver/AndroidConnectivityObserver.kt)
- **Проблема:**
  1. Метод `onLost(network)` принимал сеть, которая отключилась. При переключении с Wi-Fi на сотовую связь или наоборот Android сначала уведомляет о доступности новой сети, а затем вызывает `onLost` для старой. Метод `onLost` безусловно выставлял `_isConnected.value = false`, замораживая статус приложения в «нет сети» при наличии активного соединения.
  2. Метод `onAvailable(network)` не запрашивал возможности сети сразу, рассчитывая только на `onCapabilitiesChanged`.
  3. Поле `connectivityManager` инициализировалось через `context.getSystemService<ConnectivityManager>()!!` — падение в рантайме при недоступности системного сервиса.
  4. В логах оставались временные префиксы `"!!! 999"`.
- **Решение:** 
  1. `connectivityManager` сделан nullable с безопасным вызовом.
  2. В `onAvailable` при наличии возможностей сети флаг соединения обновляется сразу.
  3. В `onLost` добавлена проверка: если отключившаяся сеть не является текущей активной (`currentActive != network`), состояние обновляется на основе возможностей актуальной сети.
  4. Логирование переведено на `Timber.d` без тестовых маркеров.

### C74 — Поддержка протокола `http://` и учёт `albumName` в `UrlImage`
- **Файл:** [UrlImage.kt](../core/src/main/java/com/client/xvideos/common/coil/UrlImage.kt)
- **Проблема:**
  1. `remember(url, urlGif)` определял сетевой URL только через `url.startsWith("https://")`. При передаче ссылок по обычному HTTP (`http://...` — локальные прокси, зеркала, перенаправления) ссылка попадала в ветку `File(url)`, вызывая сбой `filePath == null` или `FileNotFoundException` в Coil.
  2. Ключи кэширования `remember(url, urlGif)` не включали параметр `albumName`, используемый внутри лямбды для папки `l_likes`.
- **Решение:**
  1. Проверка адреса расширена до `url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)`.
  2. В `remember` добавлен ключ `albumName`: `remember(url, urlGif, albumName)`.
  3. В проверке иконки анимации учтён протокол HTTP.

### C75 — Безопасный виброотклик и поддержка Android 12+ в `vibrateWithPatternAndAmplitude`
- **Файл:** [vibrateWithPatternAndAmplitude.kt](../core/src/main/java/com/client/xvideos/common/vibrate/vibrateWithPatternAndAmplitude.kt)
- **Проблема:**
  1. Вызов `context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator` падал с исключениями приведения/null на планшетах или эмуляторах.
  2. На Android 12+ (API 31+) `VIBRATOR_SERVICE` устарел в пользу `VibratorManager`.
  3. Вызов `VibrationEffect.createWaveform(pattern, amplitudes, -1)` на устройствах без поддержки регулировки амплитуды приводил к ошибкам драйвера вибромотора.
- **Решение:**
  1. Добавлено получение `VibratorManager` на API 31+ с корректным fallback на `VIBRATOR_SERVICE`.
  2. Добавлена проверка `vibrator.hasVibrator()`.
  3. Добавлена проверка `vibrator.hasAmplitudeControl()`: для устройств без регулировки амплитуды генерируется стандартный `createWaveform(pattern, -1)`.
  4. Вызов обёрнут в `runCatching` с записью в `Timber.w`.

### C76 — Сохранение недокачанных файлов при сбоях сети в `DownloadTask`
- **Файл:** [DownloadTask.kt](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt)
- **Проблема:** В блоке `catch (e: Exception)` при сетевом исключении (`SocketTimeoutException`, разрыв Wi-Fi) методы `deleteTempFile()` и `removeNoMoreNeededModelFromDatabase()` вызывались безусловно. Это приводило к безвозвратному удалению уже скачанных гигабайт видео даже для серверов, отдающих `206 Partial Content` (`isResumeSupported == true`).
- **Решение:** Удаление файла и записи из БД теперь выполняется только если задача была явно отменена (`wasCancelled`) либо если сервер не поддерживает докачку (`!isResumeSupported`). Для поддерживающих серверов прогресс сохраняется в БД для последующего возобновления.

### A16 — Однократный разбор DOM дерева в видеоплеере X
- **Файлы:** [parserItemVideo.kt](../feature-x/src/main/java/com/client/xvideos/x/parcer/parserItemVideo.kt), [parserItemVideoTags.kt](../feature-x/src/main/java/com/client/xvideos/x/parcer/parserItemVideoTags.kt), [ScreenX_VideoPlayerSM.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt)
- **Проблема:** При загрузке страницы видео в плеере X вызывались `parserItemVideo(s)` и `parserItemVideoTags(s)`. Обе функции независимо вызывали `Jsoup.parse(s)` над одной и той же HTML строкой, удваивая расход CPU и выделение памяти.
- **Решение:** Реализованы перегрузки `parserItemVideo(document: Document)` и `parserItemVideoTags(document: Document)`. В `ScreenX_VideoPlayerSM.loadVideo()` разбор выполняется один раз через `Jsoup.parse(s)` в пуле `Dispatchers.Default`.

### H1 — Восстановление чистоты Detekt во всех модулях
- **Файлы:** [AppLockAutofill.kt](../core/src/main/java/com/client/xvideos/common/applock/AppLockAutofill.kt), [util.android.kt](../core/src/main/java/com/client/xvideos/common/videoplayer/util/util.android.kt), [UrlImage.kt](../core/src/main/java/com/client/xvideos/common/coil/UrlImage.kt), [AndroidConnectivityObserver.kt](../core/src/main/java/com/client/xvideos/common/connectivityObserver/AndroidConnectivityObserver.kt)
- **Проблема:** Задача `:core:detekt` завершалась с ошибкой: 5 неиспользуемых импортов (`Build`, `NetworkRequest`) и расхождение baseline по правилам `LongMethod` / `CyclomaticComplexMethod` для `UrlImage`.
- **Решение:** Неиспользуемые импорты удалены; для функции `UrlImage` добавлены соответствующие `@Suppress`. Сборка `:core:detekt` и корневая задача `detekt` стали полностью «зелёными».

---

## Верификация
1. `./gradlew detekt` — 0 ошибок и предупреждений во всех 5 модулях проекта (`app`, `core`, `feature-l`, `feature-r`, `feature-x`).
2. `./gradlew testDebugUnitTest` — 100% тестов пройдены успешно (140 задач, включая новые тесты `VibrateSafetyTest` и `XParsersTest`).
3. Код и логика P2P (`core/.../p2p/`) не затрагивались согласно правилу репозитория.
