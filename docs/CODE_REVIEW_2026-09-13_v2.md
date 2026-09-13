# Код-ревью xvideos — проход 17

> **Срез:** `94cd6b2` · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода — `master` (`94cd6b2`).
Линзы:
- **Безопасность файловых путей и защита от Path Traversal** (`DownloadRed.saveToGallery`, `Downloader.findVideoInDownload`, `PicsDetails.lSavedFileName`, `lDownloadMediaToShareCache`);
- **Корректность и жизненный цикл загрузок KDownloader** (`DownloadTask`, `DownloadDispatchers`, `AppDbHelper`);
- **Потокобезопасность и циклы перезагрузок Compose** (`CountryState`, `DashboardsPaginatedListScreen`);
- **Отзывчивость UI и фоновый парсинг DOM** (`ScreenX_VideoPlayerSM`, `ScreenX_VideoPlayerFullScreenSM`);
- **Предотвращение утечек памяти слушателей и стабильность переименования файлов при коллизиях**.

Ключевой итог: **все 10 дефектов 17-го прохода устранены**. Все 5 модулей (`:app`, `:core`, `:feature-l`, `:feature-r`, `:feature-x`) собираются с 0 ошибок компиляции, detekt на 100% зелёный (0 warnings), все 146 unit-тестов успешно проходят.

---

## Находки

### S8 — `DownloadRed.saveToGallery` и `Downloader.findVideoInDownload`: Path Traversal и потенциальное копирование произвольных файлов приложения в общую галерею. Высокая.

[DownloadRed.kt:107-124](../feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt#L107-L124), [Downloader.kt:296-302](../feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt#L296-L302)

В методе `saveToGallery(item)`:
```kotlin
val fileName = "r_${item.userName}_${item.id}.mp4"
val local = File("${AppPath.r_cache_download}/${item.userName}/${item.id}.mp4")
if (local.exists()) {
    GallerySaver.saveLocal(appContext, local, fileName)
    return
}
```
Если поля `userName` или `id` из сети содержали последовательности обхода каталогов (`../../`), формировался путь за пределами `r_cache_download`. При наличии целевого файла `GallerySaver.saveLocal` копировал внутренние файлы приложения в публичное хранилище `MediaStore` (галерею).
Аналогично в `Downloader.findVideoInDownload(id, name)` проверка существования файла выполнялась без валидации `isUnsafeItemName` и `requireInside`.

*Исправление:* добавлены проверки `isUnsafeItemName(item.userName)`, `isUnsafeItemName(item.id)` и `requireInside(baseDir, local)` перед доступом к локальному файлу и копированием в галерею. Метод `findVideoInDownload` возвращает `false` при любых небезопасных путях.

---

### S9 — `PicsDetails.lSavedFileName` и `lDownloadMediaToShareCache`: Path Traversal через поле `album` из сети. Высокая.

[PicsDetailsMedia.kt:71-74](../feature-l/src/main/java/com/client/xvideos/l/model/PicsDetailsMedia.kt#L71-L74), [lDownloadMediaToShareCache.kt:21-34](../feature-l/src/main/java/com/client/xvideos/l/featured/share/lDownloadMediaToShareCache.kt#L21-L34)

В `PicsDetails.lSavedFileName()`:
```kotlin
fun PicsDetails.lSavedFileName(): String? {
    val sourceName = lDownloadUrl()?.lUrlFileName()?.takeIf { it.isNotBlank() } ?: return null
    return "${width}_${height}_${is_animated}_${album}_$sourceName"
}
```
Сетевое значение `album` интерполировалось в имя сохраняемого файла без экранирования. Если `album` содержал `../../` или разделители путей, итоговое имя файла в `lDownloadMediaToShareCache` приводило к записи за пределами `AppPath.l_cacheDownload`, а последующий `useCaseShareFile` мог передавать эти файлы во внешние приложения.

*Исправление:* в `lSavedFileName` сырые значения `album` и `sourceName` очищаются от слэшей, символов `..` и недопустимых символов файловой системы. В `lDownloadMediaToShareCache` добавлена строгая проверка `isUnsafeItemName(fileName)` и `requireInside(rootDir, file)`. Добавлен юнит-тест `lSavedFileName нейтрализует path traversal и слеши в альбоме`.

---

### C30 — `DownloadTask`: проваливание отменённой задачи в `onCompleted` и переименование повреждённого файла. Высокая.

[DownloadTask.kt:224-259](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt#L224-L259)

В цикле чтения данных `DownloadTask`:
```kotlin
if (!isActive || req.job?.isActive == false) {
    deleteTempFile()
    req.reset()
    // отсутствовала установка req.status = Status.CANCELLED и вызов onError
}
```
При отмене корутины/Job происходил выход из проверки без установки статуса отмены и без немедленного прерывания выполнения. Поток исполнения проваливался ниже цикла, вызывая `renameFileName(tempPath, path)`, `listener.onCompleted()` и выставляя `req.status = Status.COMPLETED` для недокачанного или удалённого файла.

*Исправление:* при `!isActive || req.job?.isActive == false` выставляется `req.status = Status.CANCELLED`, вызывается `listener.onError("Cancelled")`, временный файл удаляется и выполнение немедленно завершается через `return@withContext`. Аналогичная проверка добавлена непосредственно перед финальным вызовом `renameFileName`.

---

### C31 — `ScreenX_VideoPlayerSM` и `ScreenX_VideoPlayerFullScreenSM`: строка `"null"` вместо пустой строки ломает проверки UI и передаётся в ExoPlayer. Средняя.

[ScreenX_VideoPlayerSM.kt:110](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt#L110), [ScreenX_VideoPlayerFullScreenSM.kt:75](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt#L75)

Выражение `passedHLS = a.value?.videoHLS.toString()` и `passedString = a.value?.videoHLS.toString()` при `null`-значении `videoHLS` превращалось в строку `"null"`. В UI проверки вида `vm.passedHLS == ""` или `passedString.isNotEmpty()` пропускали `"null"`, передавая фиктивный URL в `ExoPlayer`, что приводило к ошибке источника медиа вместо отображения заглушки/сообщения об ошибке.

*Исправление:* заменено на `config?.videoHLS.orEmpty()`. При отсутствии ссылки HLS-переменная остаётся строго пустой строкой `""`.

---

### T19 — `DashboardsPaginatedListScreen`: бесконечный цикл перезагрузки экрана и мутация состояния Compose на `Dispatchers.IO`. Высокая.

[DashboardsPaginatedListScreen.kt:51-92](../feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/DashboardsPaginatedListScreen.kt#L51-L92), [country.kt:68-70](../feature-x/src/main/java/com/client/xvideos/x/feature/country/country.kt#L68-L70)

В `DashboardsPaginatedListScreen`:
1. `LaunchedEffect` был привязан к `key2 = CountryState.current`.
2. Функция `openNew`, вызываемая внутри эффекта на `Dispatchers.IO`, выполняла `parseSiteCountryFlag(html)?.let { CountryState.current = it }`.
3. Запись в `CountryState.current` (Compose Snapshot state) происходила на фоновом потоке, что нарушает правила потокобезопасности Compose.
4. Изменение `CountryState.current` немедленно инвалидировало ключ `LaunchedEffect`, отменяя текущую корутину и перезапуская `openNew` по кругу, порождая избыточную нагрузку на CPU, сеть и циклическое создание WebView.

*Исправление:*
- В `CountryState` добавлен явный счётчик переключений пользователем `var userSelectionEpoch: Int by mutableIntStateOf(0)`.
- `userSelectionEpoch` инкрементируется на `Dispatchers.Main` только при явном клике по стране в `ComposeCountry`.
- `LaunchedEffect` в `DashboardsPaginatedListScreen` переведён на ключи `pageIndex` и `CountryState.userSelectionEpoch`.
- `CountryState.current` обновляется на Main-потоке без рекурсивного перезапуска загрузки страницы.
- Добавлена обработка ошибок с `SnackBar.error` при сетевом сбое.

---

### T20 — `ScreenX_VideoPlayerSM` и `ScreenX_VideoPlayerFullScreenSM`: тяжёлый синхронный парсинг HTML/DOM на главном потоке. Средняя.

[ScreenX_VideoPlayerSM.kt:104-108](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt#L104-L108), [ScreenX_VideoPlayerFullScreenSM.kt:73-74](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt#L73-L74)

Методы `parserItemVideo(s)`, `parseHTML5Player(...)` и `parserItemVideoTags(s)` выполняют тяжеловесный DOM-парсинг Jsoup всей HTML-страницы видео. В обеих моделях экранов этот вызов происходил на потоке `screenModelScope` (`Dispatchers.Main.immediate`), вызывая заметный фриз интерфейса при переходе на экран плеера.

*Исправление:* парсинг HTML и тегов вынесен в `withContext(Dispatchers.Default)`. На Main возвращаются уже готовые DTO-объекты.

---

### T21 — `DownloadTask`: повреждение пути временного файла при наличии точек в каталоге или имени (`tempPath.split(".")[0]`). Средняя.

[DownloadTask.kt:108](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt#L108)

При разрешении коллизий неактуальных записей БД код выполнял:
```kotlin
file = File(tempPath.split(".")[0] + "_2." + file.extension)
```
Метод `split(".")` разбивал строку пути по первой же точке. Если в пути присутствовали точки (например, пакет Android `/data/user/0/com.client.xvideos/` или точка в имени файла), строка обрезалась, создавая файл в искажённой директории.

*Исправление:* путь перестраивается корректно с сохранением каталога и исходного расширения: `File(parent, "${file.nameWithoutExtension}_2.${file.extension}")`.

---

### T22 — `DownloadDispatchers`: дублирование вызова `onError("Cancelled")` при отмене задачи. Низкая.

[DownloadDispatchers.kt:81-86](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadDispatchers.kt#L81-L86)

При отмене загрузки `cancel(req)` диспетчер синхронно вызывал `req.listener?.onError("Cancelled")`. Одновременно сама задача `DownloadTask`, перехватив отмену корутины, также вызывала `listener.onError("Cancelled")`, приводя к двойному уведомлению UI и двойным SnackBar/Toast.

*Исправление:* диспетчер уведомляет слушателя напрямую только если задача была приостановлена (`wasPaused`), а для активных задач отмену обрабатывает сама корутина `DownloadTask` ровно один раз.

---

### A11 — `DownloadDispatchers`: утечка ссылок на слушатели и UI-замыкания в терминальных состояниях. Средняя.

[DownloadDispatchers.kt:42-54](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadDispatchers.kt#L42-L54)

Объект `DownloadRequest` может сохраняться в очереди или переиспользоваться, при этом поле `listener` удерживало ссылку на лямбды экранов, фрагментов или активити. В терминальных состояниях `onCompleted` и `onError` ссылка `listener` не очищалась, что создавало утечки памяти UI-контекстов при завершении загрузок.

*Исправление:* в обработчиках `onCompleted` и `onError` поле `request.listener = null` обнуляется сразу после отправки терминального события на главный поток.

---

### A12 — `AppDbHelper`: блокирующий ввод-вывод на вызывающем потоке, интерполяция строк в SQL и небезопасная работа с курсорами. Средняя.

[AppDbHelper.kt:25-160](../core/src/main/java/com/client/xvideos/common/kdownloader/database/AppDbHelper.kt#L25-L160)

В реализации `AppDbHelper`:
1. Операции с базой данных `insert`, `update`, `updateProgress`, `remove`, `getUnwantedModels`, `empty` не гарантировали выполнение на `Dispatchers.IO`.
2. В `find` и `remove` аргументы конкатенировались напрямую в строку SQL запроса (`id = " + id`).
3. Курсоры не использовали идиоматичный `use { ... }`, что при исключениях могло приводить к утечке дескрипторов базы данных.

*Исправление:* все методы переведены на `withContext(Dispatchers.IO)`, запросы параметризованы через массив аргументов `selectionArgs`, курсоры обёрнуты в безопасный `.use { ... }`.

---

## Статус

| Находка | Класс | Статус | Файлы |
| --- | --- | --- | --- |
| **S8** | безопасность | закрыт | `DownloadRed.kt`, `Downloader.kt` |
| **S9** | безопасность | закрыт | `PicsDetailsMedia.kt`, `lDownloadMediaToShareCache.kt`, `LPureFunctionsTest.kt` |
| **C30** | корректность | закрыт | `DownloadTask.kt` |
| **C31** | корректность | закрыт | `ScreenX_VideoPlayerSM.kt`, `ScreenX_VideoPlayerFullScreenSM.kt` |
| **T19** | конкурентность | закрыт | `country.kt`, `DashboardsPaginatedListScreen.kt`, `GlobalStateTest.kt` |
| **T20** | производительность / UI | закрыт | `ScreenX_VideoPlayerSM.kt`, `ScreenX_VideoPlayerFullScreenSM.kt` |
| **T21** | корректность / пути | закрыт | `DownloadTask.kt` |
| **T22** | конкурентность | закрыт | `DownloadDispatchers.kt` |
| **A11** | архитектура / память | закрыт | `DownloadDispatchers.kt` |
| **A12** | архитектура / БД | закрыт | `AppDbHelper.kt` |

---

## Проверка

```powershell
./gradlew testDebugUnitTest
# Результат: BUILD SUCCESSFUL in 11s, 146 unit tests passed, 0 failures

./gradlew detekt
# Результат: BUILD SUCCESSFUL in 2s, 0 weighted issues across all 5 modules (:app, :core, :feature-l, :feature-r, :feature-x)
```

## Что осталось открытым

- P2P-тракт заморожен решением владельца от 11.09.2026.
- Все дефекты прохода 17 закрыты полностью.
