# Код-ревью xvideos — проход 55

> **Срез:** `2f2d169` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `2f2d169`. Изменено 4 исходных файла в модулях `feature-r`, `feature-l`, `feature-x`, добавлен новый файл unit-тестов в `feature-r` (`ScreenRedFullScreenSMTest.kt`) и расширен unit-тест в `feature-x` (`SavedX_DownloadsTest.kt`).

Линзы:
- `UI` / `C` / Корректность временных меток A-B плеера: защита точек зацикливания от нечисловых (`Float.NaN`), бесконечных или отрицательных значений, а также клампинг к верхней границе продолжительности ролика в [ScreenRedFullScreenSM.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreenSM.kt).
- `C` / `T` / Устранение гонок при параллельных сканированиях файловой системы: отмена незавершённых фоновых задач `refreshJob` в [SavedL_Likes.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Likes.kt) и `SavedL_Collection.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt).
- `S` / `T` / Валидация целостности загрузок и потокобезопасность Compose State: проверка размера скачанного видео перед созданием метаданных в [SavedX_Downloads.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt) и потокобезопасное чтение Compose-списка в [SavedX_Favorites.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Favorites.kt).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### UI70 / C131 — Валидация и санитизация временных меток A-B повтора в `ScreenRedFullScreenSM`. Высокая.

[feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreenSM.kt:49-61](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreenSM.kt#L49)

В `ScreenRedFullScreenSM` методы выставления меток `setTimeA()` и `setTimeB()` напрямую присваивали `currentPlayerTime`. Если плеер находился в фазе буферизации, смены трека или возвращал нестандартные значения позиции (например, отрицательные, превышающие длину ролика или `Float.NaN`), эти значения попадали в состояние Compose и передавались в плеер. При отрицательных или нечисловых значениях логика A-B зацикливания плеера переставала корректно срабатывать.

**Исправление:**
- Вынесена функция санитизации `sanitizePointTime(time: Float, durationSec: Int): Float`.
- Все нечисловые и отрицательные значения клампятся в `0f`. Если длительность ролика известна (`durationSec > 0`), метка ограничивается сверху `durationSec.toFloat()`.
- Добавлен набор модульных тестов в [ScreenRedFullScreenSMTest.kt](file:///g:/xvideos2/feature-r/src/test/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreenSMTest.kt).

---

### C132 / T25 — Устранение гонок устаревания данных при параллельном обновлении коллекций и лайков L. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Likes.kt:88-107](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Likes.kt#L88)
[feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt:58-77, 211-245](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt#L58)

При частых операциях добавления/удаления или быстром переключении коллекций каждый вызов `refresh()`, `refreshCollectionList()` и `refreshDuplicates()` запускал новую независимую корутину на `Dispatchers.IO`. Если более раннее сканирование крупного каталога завершалось позже быстрого последующего запроса, устаревшие данные перезаписывали актуальный Compose-список.

**Исправление:**
В `SavedL_Likes` и `SavedL_Collection` введено отслеживание активных корутин через `Job` (`refreshJob`, `refreshCollectionJob`, `refreshItemsJob`, `refreshDuplicatesJob`). Перед запуском нового сканирования предыдущая незавершённая задача принудительно отменяется (`job?.cancel()`), гарантируя, что список формируется только актуальным запросом.

---

### S37 / T26 — Защита от повреждённых 0-байтовых файлов загрузок в X и синхронизация чтения Compose State. Высокая.

[feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt:125-135](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt#L125)
[feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Favorites.kt:75-84](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Favorites.kt#L75)

1. В `SavedX_Downloads` в коллбэке `onCompleted` статус «Скачано» выставлялся сразу, и создавался `.info`-файл без проверки реального размера записанного на диск `.mp4`. В случае отдачи сервером пустого ответа или обрыва потока в хранилище оставался пустой файл 0 байт, который попадал в список сохранённых и приводил к ошибке воспроизведения.
2. В `SavedX_Favorites.refresh()` трансформация `list.map { it.id }` выполнялась на `Dispatchers.IO`, хотя `list` является `SnapshotStateList`, модифицируемым на главном потоке, что создавало риск состояния гонки.

**Исправление:**
- В `SavedX_Downloads.onCompleted` перед формированием `.info` на `Dispatchers.IO` проверяется `file.exists() && file.length() > 0L`. При пустом или отсутствующем файле он удаляется, выставляется статус ошибки `percent.value = -3f` и выводится сообщение об ошибке.
- В `SavedX_Favorites.refresh()` чтение `list.map { it.id }` перенесено внутрь блока `withContext(Dispatchers.Main)`.
- В `SavedX_DownloadsTest.kt` добавлен тест фильтрации и отсечения 0-байтовых файлов.

---

## Итог верификации

- **Unit-тесты:** `./gradlew testDebugUnitTest` — успешно (все тесты по всем 5 модулям выполнены без ошибок).
- **Detekt:** `./gradlew detekt --rerun-tasks` — 0 ошибок и предупреждений по всем 5 модулям.
- **Архитектурные ограничения:** Замороженный P2P (`core/.../p2p/`) не изменялся.
