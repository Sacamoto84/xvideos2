# Код-ревью xvideos — проход 41

> **Срез:** `a47bbf1` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `a47bbf12e2e6739b9afba200becf86ed227e1b96`. Изменено 6 файлов в модулях `core`, `feature-x` и `feature-l`.
Линзы: сохранение в системную галерею и MediaStore (`C`), целостность и предотвращение утечек дескрипторов бэкапов (`C`), парсинг метаданных загрузок (`C`), предотвращение создания 0-байтовых файлов при скачивании (`C`), валидация P2P-экспорта (`C`). Граница P2P (`core/.../p2p/`) заморожена по решению владельца и не затрагивалась.

## Находки

### C89 — `GallerySaver.saveLocal` и `saveFromUrl` сохраняют 0-байтовые файлы в системную галерею. Высокая.

[core/src/main/java/com/client/xvideos/common/gallery/GallerySaver.kt:49, 113](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/gallery/GallerySaver.kt#L49)

В `GallerySaver.kt` методы `saveLocal` и `saveFromUrl` выполняют публикацию медиа в общую системную галерею (`Movies/xvideos_download/` или `Pictures/xvideos_download/`) через Android `MediaStore`.
1. `saveLocal` проверял только `exists(appContext, cleanFileName)` в MediaStore, но не проверял размер и целостность исходного файла `src`. Повреждённый или прерванный 0-байтовый файл (`src.length() == 0L`) копировался в MediaStore, засоряя галерею пользователя чёрными невоспроизводимыми превью.
2. В `saveFromUrl` скачанный во временную папку `tmpFile` перед публикацией не проверялся на `length() > 0L`. В случае сбоя или возврата сервером пустого ответа в галерею публиковалась пустышка.
**Исправление:**
- В `saveLocal` добавлен guard: `if (!src.exists() || src.length() == 0L)` с выводом понятного сообщения `SnackBar.error("Файл повреждён или отсутствует")`.
- В `saveFromUrl` добавлена проверка `if (!tmpFile.exists() || tmpFile.length() == 0L)` перед вызовом `publish`.

### C90 — Утечка файловых дескрипторов ContentResolver при ошибках заголовка в `XlrBackupManager` и `XlrChunkedCrypto`. Высокая.

[core/src/main/java/com/client/xvideos/common/backup/XlrBackupManager.kt:434-460](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/backup/XlrBackupManager.kt#L434), [core/src/main/java/com/client/xvideos/common/backup/XlrChunkedCrypto.kt:250-270](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/backup/XlrChunkedCrypto.kt#L250), [core/src/test/java/com/client/xvideos/common/backup/XlrChunkedCryptoTest.kt:156](file:///g:/xvideos2/core/src/test/java/com/client/xvideos/common/backup/XlrChunkedCryptoTest.kt#L156)

1. В `XlrBackupManager.openZipInputStream` открывался входящий поток `context.contentResolver.openInputStream(uri)`. Если файл оказывался пустым (`read < 4`), требовал пароль (`password == null`), имел неподдерживаемый формат или бросал ошибку, метод выбрасывал исключение без закрытия `rawInput`. В Android открытие потоков через `ContentResolver` создаёт нативные файловые дескрипторы в системных процессах Storage Access Framework / DocumentsProvider, что приводило к утечке дескрипторов (file descriptor leak) и последующим ошибкам `EMFILE: Too many open files`.
2. В конструкторе `XlrEncryptedInputStream.init`, если заголовок или версия оказывались некорректными, выбрасывался `XlrCorruptedBackupException` без закрытия нижележащего потока `dataInput`.
**Исправление:**
- Вызовы обёрнуты в безопасные блоки `try-catch`, гарантированно вызывающие `rawInput.close()` и `dataInput.close()` при любой ошибке до завершения инициализации потока.
- Добавлен юнит-тест `битый заголовок закрывает нижележащий поток и бросает XlrCorruptedBackupException` в `XlrChunkedCryptoTest`.

### C91 — 0-байтовые файлы `.info` вызывают `SerializationException` и гонки `delete` в `SavedX_Downloads`. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt:174-210](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt#L174)

1. В методе `SavedX_Downloads.refresh` фильтр файлов метаданных `allFiles.filter { it.isFile && it.extension == "info" }` не проверял размер файла. При внезапном прерывании процесса пустой 0-байтовый `.info` файл приводил к постоянным ошибкам десериализации `SerializationException: Expected start of the object '{', but had 'EOF'` на каждый `refresh()`.
2. В методе `delete` вызов `refresh()` запускался в параллельной корутине вместо синхронного перечитывания состояния внутри текущего блока IO.
**Исправление:**
- Фильтр дополнен: `it.extension == "info" && it.length() > 0L`.
- Логика перечитывания вынесена в `loadFromDisk()` и синхронно вызывается из `delete()` перед выводом сообщения пользователю.

### C92 — Создание 0-байтовых файлов при пустом ответе сервера в `LMediaPersist.lDownloadToFile`. Высокая.

[feature-l/src/main/java/com/client/xvideos/l/featured/saved/LMediaPersist.kt:195-200](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/LMediaPersist.kt#L195)

В `LMediaPersist.lDownloadToFile`, если сервер возвращал статус 200 OK с пустым телом ответа (0 байт) или соединение преждевременно закрывалось без заголовка `Content-Length`, временный файл переименовывался в целевой, и на диске оставался 0-байтовый повреждённый файл медиа.
**Исправление:** Добавлена проверка `if (downloadedBytes == 0L) throw IOException("Download failed: empty response body (0 bytes) from $url")`, предотвращающая запись пустых файлов на диск.

### C93 — Экспорт повреждённых 0-байтовых альбомов в P2P в `LAlbumExporter`. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/featured/saved/LAlbumExporter.kt:25-28](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/LAlbumExporter.kt#L25)

В `LAlbumExporter.export` проверка `if (savedFile.exists())` упаковывала файл альбома в P2P-пакет без проверки его размера. Если на диске находился повреждённый 0-байтовый `.album`, получателю отправлялся битый файл вместо свежей сериализации альбома.
**Исправление:** Проверка дополнена: `if (savedFile.exists() && savedFile.length() > 0L)`. При наличии пустого файла экспорт падает на ветку повторной сериализации из модели `AlbumDetails`.

## Статус

| Находка | Класс | Статус | Описание / Исправление |
| --- | --- | --- | --- |
| C89 | Core / Gallery | Исправлен | `GallerySaver` проверяет `length() > 0L` для локальных и скачанных файлов |
| C90 | Core / Backup | Исправлен | Устранена утечка дескрипторов `ContentResolver` при ошибках бэкапов, добавлен тест |
| C91 | Feature-X / Saved | Исправлен | Фильтрация 0-байтовых `.info` и синхронизация `delete` в `SavedX_Downloads` |
| C92 | Feature-L / Storage | Исправлен | Защита от создания 0-байтовых файлов в `lDownloadToFile` при пустом ответе сервера |
| C93 | Feature-L / P2P Export | Исправлен | Проверка `length() > 0L` при экспорте альбома в `LAlbumExporter` |
