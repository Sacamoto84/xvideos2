# Код-ревью xvideos — проход 45

> **Срез:** `dc6cace` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `dc6cace68d6f5ad8df9521fae39d5b06497ec097`. Изменено 5 файлов в модулях `core`, `feature-r`, `feature-x`.
Линзы: жизненный цикл корутин и плеера (`T`), целостность файловых баз данных (`C`), защита от 0-байтовых повреждений (`C`), валидация пользовательского ввода и границ отрезка A-B (`UI`), отказоустойчивость интерфейса (`UI`). Граница P2P (`core/.../p2p/`) заморожена по решению владельца и не затрагивалась.

## Находки

### T49 — Перехват и подавление `CancellationException` при смене URL в `rememberExoPlayerWithLifecycle`. Высокая.

[core/src/main/java/com/client/xvideos/common/videoplayer/rememberExoPlayerWithLifecycle.kt:118-125](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/videoplayer/rememberExoPlayerWithLifecycle.kt#L118)

В эффекте `LaunchedEffect(url)` блок `try { ... } catch (e: Exception)` перехватывал базовый класс `Exception`.
При штатной смене видеоролика или уходе с экрана корутина отменялась, порождая `CancellationException`. Блок catch перехватывал отмену как ошибку воспроизведения, вызывал `error(MediaPlayerError.PlaybackError("StandaloneCoroutine was cancelled"))` и отправлял ложное сообщение об ошибке в UI.
**Исправление:** Добавлен явный перехват `catch (e: CancellationException) { throw e }` перед общим обработчиком `Exception`.

### C103 — Сбой десериализации и спам в логи при наличии 0-байтовых файлов в `CollectionDB.readAllCollections`. Средняя.

[core/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt:199-208](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt#L199)

При аварийном завершении приложения или внезапном отключении питания на диске мог остаться 0-байтовый файл `.collection`.
При вызове `readAllCollections` метод читал файл целиком (`file.readText() -> ""`) и передавал пустую строку в парсер `json.decodeFromString`, что приводило к `SerializationException: Unexpected EOF` и выводу стектрейса в `Timber.e` при каждом открытии коллекций.
**Исправление:** Добавлен пропуск `if (file.length() == 0L || text.isBlank()) return@mapNotNull null`. Покрыто тестом в [CollectionDBTest.kt](file:///g:/xvideos2/core/src/test/java/com/client/xvideos/common/collectionDB/CollectionDBTest.kt).

### C104 — Отсутствие защиты от 0-байтовых файлов в `FileDB.read` и `FileDB.refresh`. Средняя.

[core/src/main/java/com/client/xvideos/common/fileDB/FileDB.kt:132, 163](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/fileDB/FileDB.kt#L132)

В `FileDB.read(nameFile)` проверка `if (!file.exists())` не учитывала размер файла. Чтение 0-байтового файла завершалось `SerializationException` вместо ожидаемого `FileNotFoundException`. В `refresh()` обход файлов пытался десериализовать пустые файлы и писал ошибки в лог.
**Исправление:**
- В `read` условие расширено: `if (!file.exists() || file.length() == 0L) return Result.failure(...)`.
- В `refresh` добавлена фильтрация: `if (file.length() == 0L || jsonString.isBlank()) return@mapNotNull null`.

### UI48 — Рассинхронизация состояния кнопки A-B при невалидном интервале в `FeedControls_Container_Line0`. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/bottom_bar/FeedControls_Container_Line0.kt:65, 92, 112](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/bottom_bar/FeedControls_Container_Line0.kt#L65)

Если точка B была меньше или равна точке A (`vm.timeB <= vm.timeA`), нажатие на кнопку A-B включало `vm.enableAB = true`, кнопка окрашивалась в ярко-зелёный цвет, создавая видимость рабочего режима, хотя плеер игнорировал зацикливание (`timeB > timeA`). Кроме того, выставление точек A или B друг за друга при включенном повторе ломало воспроизведение без индикации.
**Исправление:**
- При включении A-B добавлена валидация с показом предупреждения: `if (!vm.enableAB && vm.timeB <= vm.timeA) SnackBar.warning(...) else vm.enableAB = !vm.enableAB`.
- При смещении точек A или B так, что `timeB <= timeA`, флаг `enableAB` автоматически сбрасывается в `false`.

### UI49 — Отсутствие возможности повтора при пустом ответе в `TagsPaginatedListScreen`. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/screens/tags/atom/TagsPaginatedListScreen.kt:93-98](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/tags/atom/TagsPaginatedListScreen.kt#L93)

Когда `loaded.isEmpty()` (например, временный сбой сети или пустая выдача сервера), экран выводил надпись «Видео не найдены» без кнопки «Повторить». Пользователь оказывался заблокирован на пустом экране без возможности обновить контент.
**Исправление:** Добавлена кнопка `Button(onClick = { retryTrigger++ }) { Text("Повторить") }` в состояние пустого списка по аналогии с состоянием ошибки.

## Статус

| Находка | Класс | Статус | Описание / Исправление |
| --- | --- | --- | --- |
| T49 | Core / Video Player | Исправлен | Проброс `CancellationException` в `rememberExoPlayerWithLifecycle` при смене URL |
| C103 | Core / CollectionDB | Исправлен | Фильтрация 0-байтовых `.collection` файлов в `readAllCollections` |
| C104 | Core / FileDB | Исправлен | Защита от 0-байтовых файлов в `FileDB.read` и `FileDB.refresh` |
| UI48 | Feature-R / Video Player | Исправлен | Валидация точек A-B и автосброс некорректного диапазона в `FeedControls_Container_Line0` |
| UI49 | Feature-X / Tags | Исправлен | Добавлена кнопка «Повторить» при пустом результате в `TagsPaginatedListScreen` |
