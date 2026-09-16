# Код-ревью xvideos — проход 58

> **Срез:** `5858e47` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `5858e47`. Изменено 5 исходных файлов в модуле `feature-r`, расширены unit-тесты в `feature-r` (`DownloadedVideoKeysTest.kt`).

Линзы:
- `T` / Устранение гонок и отмена незавершённых задач (in-flight job cancellation) при обновлении и восстановлении загрузок в [DownloadRed.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt), [R_Saved_Likes.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Likes.kt), [R_Saved_Creator.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Creator.kt), [R_Saved_Niches.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Niches.kt).
- `IO` / Вынос дисковых операций создания каталогов с Main-потока в [DownloadRed.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt).
- `P` / Исключение повторного скачивания существующих превью в [Downloader.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### T29 / IO41 — Блокирующий дисковый I/O в ensureDownloaded и отсутствие отмены гонок в DownloadRed. Высокая.

[feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt:70-105, 175-220](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt#L70)

1. В методе `DownloadRed.ensureDownloaded` вызов `downloader.downloadRedName(item)` происходил непосредственно на главном потоке `Dispatchers.Main`. Внутри `downloadRedName` синхронно вызывались `requireInside`, `findVideoInDownload` и `creatorDir.mkdirs()`, создавая риск блокировки UI и ANR.
2. Методы `refreshDownloadList()` и `recoverIncompleteDownloads()` запускались без отслеживания активных `Job`. При частых вызовах (например, после серии скачиваний или многократных тапах пользователя) параллельно запускалось несколько полных обходов диска с JSON-декодированием всех `.info`-файлов, что приводило к взаимной перезаписи состояний `StateFlow` неактуальными результатами.

**Исправление:**
- Вызов `downloader.downloadRedName` в `ensureDownloaded` обёрнут в `withContext(Dispatchers.IO)`. В `downloadItem` корутина переведена на `Dispatchers.IO`.
- Добавлены поля `refreshJob: Job?` и `recoveryJob: Job?` с вызовом `cancel()` перед запуском новых операций сканирования.

---

### P36 — Повторная загрузка превью и неперехваченные ошибки в Downloader.downloadRedName. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt:83-90](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt#L83)

В методе `Downloader.downloadRedName` превью скачивалось через `kDownloader.enqueue(requestImage)` без проверки наличия файла на диске и без колбэков ошибок/старта. Если превью `${item.id}.jpg` уже присутствовало на диске, оно скачивалось повторно, расходуя трафик.

**Исправление:**
- Добавлена проверка `!previewFile.exists() || previewFile.length() == 0L`.
- Переиспользован метод `enqueuePreview` с централизованной обработкой ошибок и логированием.

---

### T30 — Гонки при фоновом обновлении и чтении SnapshotStateList в хранилищах R. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Likes.kt:60-75](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Likes.kt#L60)
[feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Creator.kt:75-82](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Creator.kt#L75)
[feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Niches.kt:50-58](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Niches.kt#L50)

В `R_Saved_Likes.refresh()` чтение снимка `list.toList()` выполнялось на `Dispatchers.IO`. Кроме того, во всех трёх классах вызовы `refresh()` не отменяли предыдущие запущенные обновления.

**Исправление:**
- Добавлено `refreshJob?.cancel()` во всех трёх репозиториях.
- В `R_Saved_Likes` чтение списка `list.toList()` перенесено в `withContext(Dispatchers.Main)`.
- В `DownloadedVideoKeysTest.kt` добавлен тест для пустого списка файлов.

---

## Итог верификации

- **Unit-тесты:** `./gradlew testDebugUnitTest` — успешно (все тесты по всем 5 модулям выполнены без ошибок).
- **Detekt:** `./gradlew detekt --rerun-tasks` — 0 ошибок и предупреждений по всем 5 модулям.
- **Архитектурные ограничения:** Замороженный P2P (`core/.../p2p/`) не изменялся.
