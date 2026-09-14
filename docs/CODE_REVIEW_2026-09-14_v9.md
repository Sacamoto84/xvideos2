# Отчёт о код-ревью: 14 сентября 2026 (проход 30, v9)

Срез: `master` после прохода 29.
Фокус прохода: мультилинзовое ревью по направлениям **Correctness**, **Concurrency / Lifecycle**, **Architecture / Performance**, **Clean Code** и **UI/UX**.

---

## Сводка находок

| ID | Линза | Серьёзность | Статус | Суть |
| --- | --- | --- | --- | --- |
| **C65** | Correctness / Compatibility | Средняя | **закрыт** | Ложная аннотация `@RequiresApi(Build.VERSION_CODES.S)` на `L_FullScreenImage.Content()`. |
| **C66** | Correctness / Lifecycle | Средняя | **закрыт** | `DownloadRequestQueue.cancelAll()` очищал мапу без вызова отмены на элементах, ломая нотификацию слушателей и очистку временных файлов. |
| **UI29** | UI / UX | Низкая | **закрыт** | Отсутствие плейсхолдера для пустого списка избранного в `ScreenFavorites.kt` (чёрный пустой экран). |
| **A11** | Architecture / Performance | Средняя | **закрыт** | Мёртвое поле `tagsList` и лишний сетевой запрос `refreshTagList()` на старте приложения (`SplashActivity` / `SavedRed`). |
| **A12** | Clean Code / Warning | Низкая | **закрыт** | Висячее неиспользуемое выражение `filterPictureCountStateCount` в `AlbumList.kt:158`. |
| **T35** | Concurrency / Lifecycle | Средняя | **закрыт** | Воспроизведение `exo` не ставилось на паузу перед выходом из `ScreenX_VideoPlayerFullScreen.kt`. |

---

## Подробное описание и решения

### C65 — Ложная аннотация `@RequiresApi(Build.VERSION_CODES.S)` в `L_FullScreenImage`
- **Файл:** [L_FullScreenImage.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/L_FullScreenImage.kt#L109)
- **Проблема:** Метод `Content()` класса `L_FullScreenImage` был помечен аннотацией `@RequiresApi(Build.VERSION_CODES.S)`. При этом проект поддерживает `minSdk = 29` (Android 10), а в самом экране нет никаких API, специфичных для Android 12+ (API 31). Аннотация создавала ложные предупреждения компилятора/линтера и искусственное ограничение совместимости.
- **Решение:** Аннотация `@RequiresApi(Build.VERSION_CODES.S)` и неиспользуемые импорты `android.os.Build`, `androidx.annotation.RequiresApi` удалены. Запись `detekt` baseline обновлена.

### C66 — Корректная отмена активных запросов в `DownloadRequestQueue.cancelAll()`
- **Файл:** [DownloadRequestQueue.kt](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadRequestQueue.kt#L97-L103), [DownloadDispatchers.kt](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadDispatchers.kt#L82-L90)
- **Проблема:** При вызове `cancelAll()` в `DownloadRequestQueue` метод просто делал `idRequestMap.clear()` и вызывал `downloader.cancelAll()`. Запросы, находящиеся в очереди или на паузе, не переводились в статус `Status.CANCELLED`, слушатели не получали коллбэк `onError("Cancelled")`, а временные файлы незавершённых загрузок не удалялись. Кроме того, в `DownloadDispatchers.cancel()` запуск корутины на `Dispatchers.Main` производился даже при `req.listener == null`, создавая ненужные аллокации и сбои в JVM тестах.
- **Решение:** `DownloadRequestQueue.cancelAll()` теперь итерирует все текущие запросы и вызывает `cancel(req.downloadId)`, переводя их в `Status.CANCELLED`, оповещая слушателей и очищая временные файлы перед обнулением скоупа и БД. В `DownloadDispatchers.cancel()` отправка коллбэка обёрнута в проверку `if (listener != null)`. Добавлен юнит-тест в [KDownloaderQueueTest.kt](../core/src/test/java/com/client/xvideos/common/kdownloader/KDownloaderQueueTest.kt).

### UI29 — Пустое состояние для экрана избранного `ScreenFavorites`
- **Файл:** [ScreenFavorites.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/favorites/ScreenFavorites.kt#L147-L165)
- **Проблема:** При `favorites.isEmpty()` экран рендерил пустой `LazyVerticalGrid`, отображая совершенно чёрный экран без каких-либо указаний для пользователя (в отличие от `ScreenSavedX.kt`, где отображается `Text("Пусто")`).
- **Решение:** Добавлена проверка `if (favorites.isEmpty())` с центрированным блоком `Box { Text("Пусто", color = Color.Gray, fontSize = 16.sp) }`.

### A11 — Удаление мертвого кода и лишнего HTTP-запроса на старте в `SavedRed`
- **Файл:** [SavedRed.kt](../feature-r/src/main/java/com/client/xvideos/r/common/saved/SavedRed.kt), [SplashActivity.kt](../app/src/main/java/com/client/xvideos/SplashActivity.kt#L114)
- **Проблема:** Поле `SavedRed.tagsList` и метод `refreshTagList()` нигде не читались и не использовались в UI (старый закомментированный код в `SearchTab.kt` был заменен). Тем не менее, `SplashActivity.initApp()` на каждом холодном старте приложения запускал отдельную `async`-задачу `savedRedInstance.refreshTagList()`, делающую ненужный HTTP-запрос к RedGifs tags API.
- **Решение:** Поле `tagsList`, метод `refreshTagList()`, импорт `TagInfo` и вызовы в `SavedRed.refreshAll()` и `SplashActivity.initApp()` полностью удалены. Холодный старт больше не тратит сетевой трафик на неиспользуемые теги.

### A12 — Удаление висячего выражения в `AlbumList.kt`
- **Файл:** [AlbumList.kt](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumList.kt#L158)
- **Проблема:** В методе `getAlbumListAggregations` на строке 158 находилось изолированное выражение `filterPictureCountStateCount` в блоке `try`, которое не являлось возвращаемым значением и вызывало варнинг компилятора об unused expression.
- **Решение:** Строка удалена.

### T35 — Остановка плеера ExoPlayer перед выходом из полноэкранного режима X
- **Файл:** [ScreenX_VideoPlayerFullScreen.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreen.kt#L174-L178)
- **Проблема:** В `exitWithExo()` вызывался `exit(exo.currentPosition)` с отправкой события позиции через `EventBus` и `navigator.pop()`. Поскольку плеер не ставился на паузу (`exo.pause()`), во время перехода назад происходило наложение аудио/видео с плеером на предыдущем экране `ScreenX_VideoPlayer`.
- **Решение:** В `exitWithExo()` сохраняется `currentPosition`, вызывается `exo.pause()`, и позиция передаётся в `exit(pos)`.

---

## Верификация
1. `./gradlew detekt` — 0 ошибок во всех модулях (`app`, `core`, `feature-l`, `feature-r`, `feature-x`).
2. `./gradlew testDebugUnitTest` — 100% тестов прошли успешно (140 задач, включая новый тест в `KDownloaderQueueTest`).
3. Границы `core/.../p2p/` не затронуты.
