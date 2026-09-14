# Код-ревью xvideos — проход 28

> **Срез:** `master` · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода — `master` (коммит `0c438bc` после прохода 27).
Линзы:
- **Жизненный цикл, конкурентность и обработка сетевых сбоев KDownloader** (`DownloadTask.kt`, `KDownloaderQueueTest`);
- **Удержание актуальных замыканий (preventing stale closures) и коллбэков ExoPlayer Compose** (`CMPlayer2.kt`);
- **Границы индексов и предотвращение падений при постраничной навигации в Jetpack Compose** (`ScreenDashBoardsBottomNavigationButtons.kt`, `X_BottomNavigationButtonsTest.kt`);
- **Адаптивность UI и доступность экрана блокировки при активной экранной клавиатуре IME** (`AppLockScreen.kt`);
- **Изоляция слоёв архитектуры и защита межмодульных кэшей данных** (`Repository.kt`, `LusciousModule.kt`, `AppFileDatabase.kt`);
- **Соблюдение архитектурного запрета на изменение P2P-транспорта** (`core/.../p2p/` не модифицировался).

Ключевой итог: **все 6 находок 28-го прохода устранены**. Репозиторий полностью собирается, detekt чист (0 замечаний), все unit-тесты успешно проходят (140 задач, 0 ошибок).

---

## Находки

### C59 — Проверка HTTP 416 Range Not Satisfiable до считывания кода ответа и молчаливое зависание при `inputStream == null` в `DownloadTask`. Высокая.

[DownloadTask.kt:125-175, 330-355](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt#L125-L175)

В конвейере загрузки файлов `DownloadTask.kt` выявлены две критические ошибки управления состоянием:
1. Вызов `checkIfFreshStartRequiredAndStart(model)` производился до получения `responseCode` от перенаправленного HTTP-подключения (`responseCode = redirectedClient.getResponseCode()`). В результате на момент проверки поле `responseCode` всегда содержало начальное значение `0`, и условие `responseCode == Constants.HTTP_RANGE_NOT_SATISFIABLE` (416) всегда оценивалось как `false`. Если сервер возвращал 416 (например, размер частично скачанного файла на диске равен либо превышает размер файла на сервере), задача не сбрасывала прогресс и падала с кодом ошибки `!isSuccessful()` ("Wrong link") вместо штатного перезапуска загрузки с байта 0.
2. При сбое получения потока `redirectedClient.getInputStream() == null` метод выполнял немедленный `return@withContext`. Статус задачи оставался `Status.RUNNING`, `listener.onError(...)` не вызывался, из-за чего задача навсегда зависала в очереди загрузчика без уведомления пользователя.
3. При ошибке `!isSuccessful()` не очищались временный файл и запись в БД для невозобновляемых загрузок.

*Исправление:*
- Опрос `redirectedClient.getResponseCode()` и `eTag` вынесен до вызова `checkIfFreshStartRequiredAndStart(model)`.
- В случае ответа 416 или изменения ETag прогресс сбрасывается (`req.downloadedBytes = 0`), временный файл удаляется, и подключение перезапускается с 0 байта.
- При `inputStream == null` ресурсы безопасно закрываются, временные файлы и БД очищаются, статус переводится в `Status.FAILED`, и вызывается `listener.onError("Failed to obtain input stream")`.
- При `!isSuccessful()` гарантированно вызывается закрытие ресурсов и очистка временных файлов.

---

### T31 — Неуправляемый `dbScope` и асинхронный запуск операций SQLite без ожидания завершения в `DownloadTask`. Средняя.

[DownloadTask.kt:40-85](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt#L40-L85)

В `DownloadTask` каждый новый экземпляр создавал собственный `CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1)...)`, который нигде не отменялся.
Методы `createAndInsertNewModel()` и `removeNoMoreNeededModelFromDatabase()` объявлялись как `suspend`, но внутри асинхронно запускали `dbScope.launch`. Это приводило к состоянию гонки: при завершении или отмене загрузки удаление записи из SQLite происходило в фоне без ожидания, из-за чего последующая немедленная проверка или перезапуск загрузки читали устаревший `DownloadModel`.

*Исправление:*
- Неуправляемый `dbScope` удалён.
- Операции `dbHelper.insert` и `dbHelper.remove` выполняются напрямую с ожиданием завершения через `withContext(Dispatchers.IO)`.

---

### T32 (C60) — Удержание устаревших замыканий `callbacks` и `config.url` в `DisposableEffect` и `LaunchedEffect` плеера `CMPlayer2`. Высокая.

[CMPlayer2.kt:55-150](../core/src/main/java/com/client/xvideos/common/videoplayer/util/CMPlayer2.kt#L55-L150)

Плеер `exoPlayer` в `CMPPlayer2` сохраняется между рекомпозициями через `rememberExoPlayerWithLifecycle(config.url, context, ...)`.
Блок `DisposableEffect(key1 = exoPlayer)` привязан исключительно к ключу `exoPlayer` и не перезапускается при рекомпозиции родителя.
1. Слушатель `createPlayerListener` захватывал начальные экземпляры лямбд `callbacks.totalTime`, `didEndVideo`, `error`, `poster` и значение `config.url`. При обновлении стейта родительского экрана или коллбэков плеер продолжал вызывать устаревшие замыкания первой композиции.
2. В `LaunchedEffect(exoPlayer, config.isPause)` и `LaunchedEffect(isBuffering)` вызовы `callbacks.currentTime` и `callbacks.bufferCallback` также использовали захваченную ссылку `callbacks`.

*Исправление:*
- В `CMPPlayer2` добавлены `val currentCallbacks by rememberUpdatedState(callbacks)` и `val currentConfig by rememberUpdatedState(config)`.
- В `createPlayerListener` и во все эффекты переданы динамические делегаты вызова `currentCallbacks` и `currentConfig.url`.

---

### C61 — Ошибка выхода за границы индексов `scrollToPage(max)` и off-by-one смещение в `ScreenDashBoardsBottomNavigationButtons`. Высокая.

[ScreenDashBoardsBottomNavigationButtons.kt:78-170](../feature-x/src/main/java/com/client/xvideos/x/screens/common/bottomKeyboard/ScreenDashBoardsBottomNavigationButtons.kt#L78-L170), [X_BottomNavigationButtonsTest.kt:1-68](../feature-x/src/test/java/com/client/xvideos/x/X_BottomNavigationButtonsTest.kt#L1-L68)

В компоненте нижней навигации страниц `BottomListDashBoardNavigationButtons2`:
1. Кнопка перехода вперёд `>` вызывала `onChange.invoke((value + 1).coerceIn(0, max))`. Когда пользователь находился на последней странице (`value == max - 1`), клик на `>` отправлял индекс `max`. В `ScreenXDashBoards` (`vm.pagerState.scrollToPage(it)`) и в `ScreenTags` (`pagerState.animateScrollToPage(it)`) число страниц равно `max`, поэтому допустимый диапазон — `0 until max` (`0..max-1`). Попытка перейти на страницу `max` приводила к выбросу `IllegalArgumentException` и падению приложения.
2. Кнопки `<` и `>` оставались кликабельными на граничных страницах, повторно диспатча некорректные события.
3. В `LaunchedEffect(value)` вычислялось `indexToScroll = value + 1`, что смещало фокус на 1 страницу вперёд в 0-индексированном ряду `LazyRow`, а на последней странице давало `indexToScroll = max`, выходя за границы списка размером `max` и приводя к ошибке расчёта смещения центрирования.

*Исправление:*
- Добавлен расчёт `maxPageIndex = max.coerceAtLeast(1) - 1`.
- Кнопка `<` отключается при `value <= 0`, кнопка `>` отключается при `value >= maxPageIndex`.
- Переход вперёд ограничен `(value + 1).coerceIn(0, maxPageIndex)`.
- `indexToScroll` скорректирован на `value.coerceIn(0, maxPageIndex)`.
- Написан юнит-тест `X_BottomNavigationButtonsTest`, проверяющий граничные значения навигации.

---

### UI26 — Недоступность поля ввода и кнопки «Разблокировать» в `AppLockScreen` на компактных экранах и в альбомной ориентации при IME. Средняя.

[AppLockScreen.kt:274-285](../core/src/main/java/com/client/xvideos/common/applock/AppLockScreen.kt#L274-L285)

В `AppLockScreenContent` контейнер `Column` с логотипом, заголовком, полем ввода пароля и кнопкой «Разблокировать» имел фиксированную высоту элементов (~360dp) и модификатор `.imePadding()`.
В альбомной ориентации устройства или на экранах с небольшим разрешением появление экранной клавиатуры оставляет видимую область высотой ~120-180dp. Из-за отсутствия вертикального скролла текстовое поле и кнопка «Разблокировать» вытеснялись за нижний край экрана, делая невозможным ввод пароля и разблокировку приложения.

*Исправление:*
- В `Column` добавлен модификатор `Modifier.fillMaxWidth().verticalScroll(rememberScrollState())`.

---

### A9 — Нарушение изоляции слоёв и стирание общего кэша страниц `cacheUrlStringRam` из репозитория Luscious. Средняя.

[Repository.kt:78-85](../feature-l/src/main/java/com/client/xvideos/l/repository/Repository.kt#L78-L85), [LusciousModule.kt:26-33](../feature-l/src/main/java/com/client/xvideos/l/di/LusciousModule.kt#L26-L33)

В `feature-l` репозиторий `Repository.kt` перешёл на внутренний LRU-кэш `ramCache = LinkedHashMap(...)`. Однако в классе оставалось поле `legacyCacheUrlStringRamDao = fileDb.cacheUrlStringRam`, а в блоке `init` вызывался метод `clearRamDao()`, который выполнял `legacyCacheUrlStringRamDao.deleteAll()`.
Таблица `fileDb.cacheUrlStringRam` принадлежит `AppFileDatabase` и активно используется модулем `feature-x` (`ScreenX_VideoPlayerSM` и `ScreenX_VideoPlayerFullScreenSM`) для кэширования разобранных HTML-страниц плеера в течение сессии. В результате при каждом открытии раздела Luscious происходила несанкционированная очистка кэша видеоплеера XVideos.

*Исправление:*
- Из `Repository.kt` удалены `legacyCacheUrlStringRamDao`, блок `init` и метод `clearRamDao()`.
- Из конструктора `Repository` и `LusciousModule.kt` удалены неиспользуемые параметры `scope` и `context`.

---

## Статус

| Находка | Класс | Статус | Коммит |
| --- | --- | --- | --- |
| C59 | корректность | закрыт | рабочее дерево |
| T31 | конкурентность / ЖЦ | закрыт | рабочее дерево |
| T32 (C60) | конкурентность / ЖЦ | закрыт | рабочее дерево |
| C61 | корректность / UI | закрыт | рабочее дерево |
| UI26 | UI / доступность | закрыт | рабочее дерево |
| A9 | архитектура | закрыт | рабочее дерево |

---

## Проверка

```bash
./gradlew detekt
# BUILD SUCCESSFUL in 1s (0 issues across all 5 modules)

./gradlew testDebugUnitTest
# BUILD SUCCESSFUL in 52s (140 actionable tasks, 100% unit tests passed)
```

Автоматические тесты:
- `X_BottomNavigationButtonsTest` — проверка граничных условий навигации (первая страница, середина, последняя страница, лента из одной страницы, некорректный max);
- `KDownloaderQueueTest` — проверка очереди и операций с файлами в KDownloader;
- Все тесты модулей `:app`, `:core`, `:feature-l`, `:feature-r`, `:feature-x` зелёные.

## Что осталось открытым

- P2P-тракт (`core/.../p2p/`) остаётся замороженным по решению владельца от 11.09.2026.
