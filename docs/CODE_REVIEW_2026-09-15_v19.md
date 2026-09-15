# Код-ревью xvideos — проход 40

> **Срез:** `a47bbf1` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `a47bbf12e2e6739b9afba200becf86ed227e1b96`. Изменено 6 файлов в модулях `app`, `feature-r` и `feature-l`.
Линзы: стабильность мультимедиа и потоков воспроизведения (`T`), корректность загрузчика (`T`), безопасность экрана аутентификации (`S`), отказоустойчивость запуска приложения (`A`), чистота и надёжность UI (`UI`). Граница P2P (`core/.../p2p/`) заморожена по решению владельца и не затрагивалась.

## Находки

### T44 — Бесконечный цикл перемотки плеера при некорректном A-B зацикливании в `RedPooledVideoPlayer`. Высокая.

[feature-r/src/main/java/com/client/xvideos/r/feature/screen/red/player/RedPooledVideoPlayer.kt:127-142](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/feature/screen/red/player/RedPooledVideoPlayer.kt#L127)

В плеере A-B зацикливание (`enableAB`) запускало периодический корутин-таймер (50 мс), который при `player.currentPosition >= timeB` выполнял `player.seekTo(timeA)`.
1. Если `timeA >= timeB` (например, при установке метки A позже текущей B или значениях по умолчанию), плеер попадал в бесконечный шторм вызовов `seekTo` каждые 50 мс, замораживая UI-поток и ExoPlayer.
2. `LaunchedEffect(player, enableAB)` не учитывал изменение `timeA` и `timeB`, из-за чего корутина не перезапускалась при динамической корректировке меток.
**Исправление:**
- Добавлено строгое условие `if (timeB > timeA && player.currentPosition >= timeB) player.seekTo(timeA)`.
- Ключи `LaunchedEffect` расширены: `LaunchedEffect(player, enableAB, timeA, timeB)`.

### T45 — Небезопасный вызов колбэка `onComplete` не из главного потока и учёт 0-байтовых `.info` в `DownloadRed`. Высокая.

[feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt:294, 342](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt#L294)

1. Метод `DownloadRed.recoverIncompleteDownloads` выполнял фоновое сканирование незавершённых загрузок в корутине на пуле `Dispatchers.IO` и вызывал переданный UI-колбэк `onComplete(report)` напрямую на фоновом потоке, что приводило к сбоям при обновлении Compose State/UI.
2. В методе `refreshDownloadList` происходил поиск файлов описания метаданных загрузок `it.name.endsWith(".info")`. При сбое или внезапном завершении процесса оставался 0-байтовый `.info` файл, который десериализовался с ошибкой `SerializationException` или искажал счётчики.
**Исправление:**
- Вызов `onComplete(report)` перенесён на `withContext(Dispatchers.Main)`.
- В `refreshDownloadList` фильтр файлов дополнен проверкой размера: `it.name.endsWith(".info") && it.length() > 0L`.

### S12 — Двойной штраф попыток ввода и преждевременная блокировка калькулятора в `CalculatorState`. Высокая.

[app/src/main/java/com/client/xvideos/ui/screens/screenCalculator/CalculatorState.kt:66-96](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/ui/screens/screenCalculator/CalculatorState.kt#L66), [CalculatorScreen.kt:33](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/ui/screens/screenCalculator/CalculatorScreen.kt#L33), [MainActivity.kt:94](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/MainActivity.kt#L94), [CalculatorStateTest.kt:71](file:///g:/xvideos2/app/src/test/java/com/client/xvideos/ui/screens/screenCalculator/CalculatorStateTest.kt#L71)

В режиме скрытия (Camouflage Calculator) нажатие клавиши `=` проверяло три варианта пароля (`raw`, `trimmed` и `evalResult`) последовательным вызовом `attemptUnlock(candidate)`.
1. Функция `attemptUnlock` внутри себя вызывала `onUnlockFailed()` при каждом несовпадении! Из-за этого при единственном нажатии `=` счётчик ошибок увеличивался сразу 3 раза вместо 1, приводя к моментальной блокировке пользователя на 30–60 секунд после всего 1-2 нажатий.
2. Коллбэк `onUnlockFailed` вызывался даже тогда, когда пользователь просто производил стандартные вычисления (например, `2+2=`), если ни один кандидат не совпал с секретным кодом разблокировки.
**Исправление:**
- Вызов `onUnlockFailed()` вынесен из `attemptUnlock` наружу. Регистрация неудачной попытки теперь вызывается ровно один раз после исчерпания всех трёх кандидатов.
- Добавлен юнит-тест `CalculatorStateTest: нажимая = при неверном пароле onUnlockFailed вызывается ровно 1 раз`.

### A18 — Фатальный краш при холодном старте приложения при сбое инициализации в `SplashActivity`. Высокая.

[app/src/main/java/com/client/xvideos/SplashActivity.kt:80-92](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/SplashActivity.kt#L80)

В `SplashActivity` метод `initApp()` инициализировал критические зависимости: базы данных Room, предзагрузку кэшей и компонентов. Ранее вызов `initApp()` выполнялся без блока перехвата исключений:
```kotlin
lifecycleScope.launch {
    initApp()
    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
    finish()
}
```
Любая непредвиденная ошибка на этапе старта (повреждение файла кэша SQLite, отсутствие места на диске, OOM) приводила к мгновенному падению приложения при запуске без шанса для пользователя попасть в настройки или интерфейс с очисткой данных.
**Исправление:**
- Вызов `initApp()` обёрнут в безопасный блок `try-catch (e: Throwable)` с пробросом `CancellationException` и подробным логированием `Timber.e(e, "SplashActivity initApp failed")`.
- Обеспечен гарантированный переход в `MainActivity` даже при сбое вторичных подсистем при старте.

### UI43 — Мёртвый закомментированный код в `ScreenRedTopThisWeekSM` и нестабильный ключ элементов в `AlbumFilterGenresDialog`. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/feature/screen/red/topThisWeek/ScreenRedTopThisWeekSM.kt:33-72](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/feature/screen/red/topThisWeek/ScreenRedTopThisWeekSM.kt#L33), [feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterGenresDialog.kt:294](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterGenresDialog.kt#L294)

1. В `ScreenRedTopThisWeekSM.kt` оставался блок устаревшего закомментированного кода объёмом 40+ строк, нарушавший читаемость и засорявший статический анализ.
2. В `AlbumFilterGenresDialog.kt` в `LazyColumn` использовался ключ `key = { it.id }`. Однако для кастомных или парсируемых жанров `id` мог быть пустым (`""`), вызывая коллизию ключей `IllegalArgumentException: Key was already used` при рендере списка.
**Исправление:**
- Удалён мёртвый закомментированный код в `ScreenRedTopThisWeekSM.kt`.
- Ключ в диалоге жанров усилен: `key = { it.id.ifBlank { it.title } }`.

## Статус

| Находка | Класс | Статус | Описание / Исправление |
| --- | --- | --- | --- |
| T44 | Feature-R / Video Player | Исправлен | Устранение бесконечного цикла `seekTo` при `timeA >= timeB` в A-B плеере |
| T45 | Feature-R / Downloader | Исправлен | Безопасная диспетчеризация `onComplete` на `Dispatchers.Main` и фильтрация 0-байтовых `.info` |
| S12 | App / Camouflage | Исправлен | Устранение двойного штрафа попыток в `CalculatorState`, добавлен юнит-тест |
| A18 | App / Lifecycle | Исправлен | Защита от фатального краша в `SplashActivity.initApp()` с логированием |
| UI43 | Feature-R & L / UI | Исправлен | Очистка мертвого кода в `ScreenRedTopThisWeekSM` и уникальный ключ в `AlbumFilterGenresDialog` |
