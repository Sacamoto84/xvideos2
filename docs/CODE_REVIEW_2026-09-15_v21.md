# Код-ревью xvideos — проход 42

> **Срез:** `a47bbf1` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `a47bbf12e2e6739b9afba200becf86ed227e1b96`. Изменено 6 файлов в модулях `feature-r`, `feature-x`, `core`, `feature-l` и `app`.
Линзы: производительность отрисовки Canvas (`T`), отмена параллельных корутин и устранение гонок плеера (`T`), устойчивость фоновой загрузки (`T`), защита от ложной блокировки экрана доступа (`S`), устранение блокировок UI-потока и сохранность каталогов (`UI`). Граница P2P (`core/.../p2p/`) заморожена по решению владельца и не затрагивалась.

## Находки

### T46 — Падение производительности и перегрузка Canvas при отрисовке делений таймлайна в `CanvasTimeDurationLine`. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/ui/video/CanvasTimeDurationLine.kt:120-137](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/video/CanvasTimeDurationLine.kt#L120)

В компоненте `CanvasTimeDurationLine1` цикл отрисовки делений таймлайна выполнялся посекундно: `for (i in 0..duration)`.
Для длинных видеороликов (10–60 минут) `duration` составляет от 600 до 3600 секунд. На каждом кадре рекомпозиции и тике таймера Canvas выполнял тысячи вызовов `drawLine`, линии толщиной 2dp полностью накладывались друг на друга (при плотности меньше 2-3px на секунду), создавая экстремальную нагрузку на графический конвейер Compose и вызывая лаги UI и просадки частоты кадров (FPS drop).
**Исправление:** Реализован адаптивный шаг отрисовки делений (`stepSec` от 1 до 300 секунд), вычисляемый на основе доступной ширины полосы воспроизведения и минимального шага в 6dp: деления всегда читаемы и не перегружают Canvas.

### T47 — Гонка параллельных запусков загрузки видео в `ScreenX_VideoPlayerSM` и `ScreenX_VideoPlayerFullScreenSM`. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt:97-104](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt#L97), [feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt:68-75](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt#L68)

В обоих ScreenModel плеера вызов `loadVideo()` запускал асинхронную корутину через `screenModelScope.launch` без сохранения ссылки на `Job` и без отмены предыдущего запуска.
При нажатии пользователем кнопки повтора при ошибке («Попробовать снова») или быстрых повторных вызовах параллельные сетевые запросы конкурировали между собой за чтение HTML, парсинг DOM и обновление состояний плеера, вызывая состояние гонки (race condition).
**Исправление:** В обоих классах введена переменная `loadJob: Job?` и обязательная отмена предыдущей задачи `loadJob?.cancel()` перед стартом новой загрузки.

### T48 — Повреждённые 0-байтовые `.info` файлы блокировали повторную запись метаданных в `Downloader.downloadMissingFiles`. Высокая.

[feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt:113, 201](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt#L113)

1. В методе `downloadMissingFiles` проверка `if (!infoFile.exists())` проверяла только наличие файла. Если в результате аварийного завершения на диске оставался 0-байтовый `.info` файл, условие не выполнялось, файл не перезаписывался валидными метаданными и оставался навечно повреждённым.
2. В методе `downloadRedName` запись файла `writeTextAtomically` не была обёрнута в `runCatching`. При исключении файловой системы (например, закончившееся место на диске) колбэк `withContext(Dispatchers.Main) { onComplete() }` никогда не вызывался.
**Исправление:**
- Проверка дополнена: `if (!infoFile.exists() || infoFile.length() == 0L)`.
- Запись в `downloadRedName` обёрнута в `runCatching { ... }.onFailure { Timber.e(...) }`, обеспечивая гарантированный вызов `onComplete()`.

### S13 — Ложный учет пустых вводов как неудачных попыток доступа в `AppLockScreen`. Высокая.

[core/src/main/java/com/client/xvideos/common/applock/AppLockScreen.kt:163](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/applock/AppLockScreen.kt#L163)

В экране блокировки приложения `AppLockScreen` метод `submit()` при пустом значении поля ввода `password == ""` не прерывал выполнение, а запускал проверку `onUnlock("")`, которая выполняла 120 000 итераций криптографического алгоритма PBKDF2, закономерно проваливалась и вызывала `AppLockRepository.registerFailedAttempt(context)`. Случайное нажатие Enter на клавиатуре или кнопки разблокировки списывало попытку ввода и блокировало пользователя на 30–60 секунд.
**Исправление:** В метод `submit()` добавлен ранний возврат: `if (password.isEmpty()) return`.

### UI44 — Сериализация JSON и дисковый ввод/вывод на UI-потоке в `SavedL_Albums` и удаление корня `r_cache_download` в `AppSettingsScreen`. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Albums.kt:57-64](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Albums.kt#L57), [app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt:171-175](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt#L171)

1. В `SavedL_Albums.addAndPicsDetails` сериализация сотен объектов `PicsDetails` через `AppJson.encodeToString` и последующая запись в базу `db.lAlbumPictureCache.put(...)` вызывались на главном потоке `Dispatchers.Main` после показа снекбара, вызывая задержку кадра и подлагивание анимации UI.
2. В `AppSettingsScreen` при очистке папки загрузок `File(AppPath.r_cache_download).deleteRecursively()` удалялась сама корневая папка, оставляя файловую систему в состоянии, когда папка отсутствовала вплоть до перезапуска приложения.
**Исправление:**
- В `SavedL_Albums` вызовы `AppJson.encodeToString` и `db.lAlbumPictureCache.put` выполняются на пуле `Dispatchers.IO` в блоке `runCatching` до переключения на главный поток.
- В `AppSettingsScreen` после `deleteRecursively()` добавлен вызов `mkdirs()`, восстанавливающий директорию.

## Статус

| Находка | Класс | Статус | Описание / Исправление |
| --- | --- | --- | --- |
| T46 | Feature-R / Timeline | Исправлен | Адаптивный шаг делений таймлайна в `CanvasTimeDurationLine`, устранение лагов Canvas |
| T47 | Feature-X / Video Player | Исправлен | Отмена предыдущей задачи `loadJob?.cancel()` при повторном запуске плеера |
| T48 | Feature-R / Downloader | Исправлен | Перезапись 0-байтовых `.info` и `runCatching` при сохранении в `Downloader` |
| S13 | Core / AppLock | Исправлен | Защита от ложного штрафа и зависания PBKDF2 при пустом вводе в `AppLockScreen` |
| UI44 | Feature-L & App / UI & FS | Исправлен | Перенос сериализации кэша на `Dispatchers.IO` и сохранение папки загрузок в `AppSettingsScreen` |
