# Код-ревью xvideos — проход 47

> **Срез:** `9de0fea` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `9de0fea`. Изменено 5 файлов в модулях `core`, `feature-l`, `feature-r`, `feature-x`.
Линзы: жизненный цикл корутин фоновых воркеров (`T`), целостность скачиваемых файлов и метаданных (`C`), защита от 0-байтовых повреждений (`C`), валидация идентификаторов и коллбэков загрузчиков (`C`), UI-надёжность (`UI`). Граница P2P (`core/.../p2p/`) заморожена по решению владельца и не затрагивалась.

## Находки

### C110 — Ложное уведомление об ошибке и утечка временных файлов при отмене в `MediaDownloadWorker`. Высокая.

[core/src/main/java/com/client/xvideos/common/download/work/MediaDownloadWorker.kt:80-110, 140-155, 245](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/download/work/MediaDownloadWorker.kt#L80)

1. В `doWork()` блок `catch (e: Exception)` перехватывал `CancellationException` при остановке/отмене задачи WorkManager (или при потере сетевых констрейнтов), отправляя системное уведомление об ошибке (`showFailedNotification`) вместо чистой отмены и оставляя недокачанный `.tmp` файл на диске.
2. В `finalizeDownloadedFile()` отсутствовала проверка на 0-байтовый размер временного файла: пустой ответ от сервера (0 байт) переименовывался в целевой файл с записью метаданных `.info` и отображением уведомления об успешной загрузке.
3. В `downloadFile()` отсутствовала проверка на `null` у `response.body`.
**Исправление:**
- Добавлен приоритетный перехват `catch (e: CancellationException)` с удалением `tempFile` и пробросом исключения наружу для корректной обработки WorkManager.
- В блок `catch (e: Exception)` и проверку `isStopped` добавлено удаление `tempFile`.
- В `finalizeDownloadedFile()` добавлена проверка `if (targets.tempFile.length() == 0L) { targets.tempFile.delete(); throw IOException(...) }`.
- В `downloadFile()` добавлена проверка `response.body ?: throw IOException(...)`.

### UI51 — Передача 0-байтовых файлов в системную галерею в `ExpandMenuVM.saveToGallery`. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/ui/element/expandMenu/ExpandMenuVM.kt:156-163](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/element/expandMenu/ExpandMenuVM.kt#L156)

В функции `saveToGallery(item: PicsDetails)` локальный медиафайл выбирался через `?.takeIf { it.exists() }`. При наличии повреждённого или оборванного 0-байтового файла в папке лайка условие выполнялось, и пустой файл передавался в `GallerySaver.saveLocal`, вместо того чтобы корректно скачать полноценный файл через `lDownloadMediaToShareCache`.
**Исправление:** Условие дополнено проверкой размера: `?.takeIf { it.exists() && it.length() > 0L }`.

### C111 — Сбой десериализации `AppJson` при чтении 0-байтовых метаданных в `LSavedLikeMetadata`. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/featured/saved/LSavedLikeMetadata.kt:45-57](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/LSavedLikeMetadata.kt#L45)

В `readLSavedLikeMetadata(file: File)` проверка `if (!file.exists())` пропускала 0-байтовые файлы `metadata.json`. Вызов `file.readText()` возвращал пустую строку `""`, которая вызывала `SerializationException: Unexpected EOF` в `AppJson.decodeFromString` и заполняла журнал ошибок `Timber.e` стектрейсами при каждом обходе сохранённых лайков и коллекций L.
**Исправление:** Добавлены проверки `if (!file.exists() || file.length() == 0L) return null` и `if (text.isBlank()) return null` с предупреждением без спама стектрейсами.

### C112 — Потеря коллбэка `onComplete` при повторной загрузке кэшированного видео в `Downloader.downloadRedName`. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt:125-128](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt#L125)

В `downloadRedName(item, onComplete)` при обнаружении файла в кэше (`findVideoInDownload` == true) метод выводил информационный снекбар `SnackBar.info("Файл есть в кеше")`, но никогда не вызывал переданный коллбэк `onComplete()`. Из-за этого вызывающие компоненты (например, `DownloadRed.downloadItem`, диалоги и слушатели) зависали в ожидании завершения или не обновляли состояние списка загрузок.
**Исправление:** В ветку `else` добавлен вызов `scope.launch(Dispatchers.Main) { onComplete() }`.

### C113 — Загрязнение файловой системы и сбои при невалидных ID в `SavedX_Downloads`. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt:60-70, 81, 153, 174](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt#L60)

Методы `download()`, `saveToGallery()`, `delete()`, `contains()` и `localPosterPath()` не проверяли валидность идентификатора (`item.id <= 0L`). При вызове с невалидными ID запускались фоновые корутины, создавались фиктивные файлы `0.mp4`, `0.jpg`, `0.info`, а также выполнялись холостые файловые операции.
**Исправление:**
- Добавлены проверки `if (item.id <= 0L) return` в `download()`, `saveToGallery()` и `delete()`.
- Добавлены проверки `id > 0L` в `contains()` и `localPosterPath()`.
- Написан юнит-тест в [SavedX_DownloadsTest.kt](file:///g:/xvideos2/feature-x/src/test/java/com/client/xvideos/x/SavedX_DownloadsTest.kt).

## Статус

| Находка | Класс | Статус | Описание / Исправление |
| --- | --- | --- | --- |
| C110 | Core / WorkManager | Исправлен | Проброс `CancellationException`, очистка `.tmp` и 0-byte guard в `MediaDownloadWorker` |
| UI51 | Feature-L / ExpandMenu | Исправлен | Фильтрация 0-байтовых медиафайлов при сохранении в галерею в `ExpandMenuVM` |
| C111 | Feature-L / Metadata | Исправлен | Фильтрация 0-байтовых и пустых `metadata.json` в `readLSavedLikeMetadata` |
| C112 | Feature-R / Downloader | Исправлен | Гарантированный вызов `onComplete()` на Main при наличии файла в кэше в `Downloader` |
| C113 | Feature-X / Saved Downloads | Исправлен | Валидация `id > 0L` в методах `SavedX_Downloads` и юнит-тест |
