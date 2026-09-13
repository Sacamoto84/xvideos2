# Код-ревью xvideos — проход 18

> **Срез:** `94cd6b2` · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода — `master` (`94cd6b2`).
Линзы:
- **Корректность персистентности и жизненного цикла коллекций Compose** (`SavedX_Favorites`, `SavedL_Albums`, `R_Saved_Likes`, `R_Saved_Creator`, `R_Saved_Niches`);
- **Безопасность парсинга и устойчивость к ошибкам типов URL/ID** (`Luscious.getAlbum`);
- **Управление памятью очереди фонового загрузчика** (`DownloadRequestQueue`, `KDownloader`);
- **Надёжность файловых операций и предотвращение потери пользовательских данных** (`Utils.renameFileName`);
- **Корректность сопоставления сохранённых элементов и медиа-ресурсов** (`ExpandMenuVM`, `LCollectionFs`).

Ключевой итог: **все 5 дефектов 18-го прохода устранены**. Все 5 модулей (`:app`, `:core`, `:feature-l`, `:feature-r`, `:feature-x`) собираются с 0 ошибок компиляции, detekt на 100% зелёный (0 warnings), все 149 unit-тестов успешно проходят.

---

## Находки

### C32 — Десинхронизация и краш Compose по дублирующимся ключам в saved-хранилищах. Высокая.

[SavedX_Favorites.kt:31-61](../feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Favorites.kt#L31-L61), [SavedL_Albums.kt:22-74](../feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Albums.kt#L22-L74), [R_Saved_Likes.kt:19-36](../feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Likes.kt#L19-L36), [R_Saved_Creator.kt:16-37](../feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Creator.kt#L16-L37), [R_Saved_Niches.kt:15-35](../feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Niches.kt#L15-L35)

В механизмах сохранения всех трёх продуктовых веток (`X`, `L`, `R`) обнаружен системный дефект:
1. **Дублирование ключей в Compose:** в `add(item)` вызывался `list.add(item)` без предварительного удаления элемента с тем же идентификатором. При повторном клике пользователя или параллельных запросах в `SnapshotStateList` появлялись два элемента с идентичным ID. На экранах с сетками (например, `ScreenFavorites.kt:153`: `items(favorites, key = { it.id })`) Compose падал с фатальным `IllegalArgumentException: Key <id> was already used`.
2. **Десинхронизация при удалении:** в `remove(item)` (например, в `SavedX_Favorites`, `SavedL_Albums`, `R_Saved_Niches`) вызывался `list.remove(item)`, выполняющий проверку `equals()` по всем полям `data class`. Модели данных (`ItemsX`, `AlbumDetails`, `NichesInfo`) содержат динамические счётчики (`views`, `subscribers`, `gifs`, `modified`, `number_of_pictures`). Если элемент был открыт из обновлённой сетевой ленты, его поля не совпадали с ранее сохранёнными на диск, и `list.remove(item)` возвращал `false`. В результате файл с диска удалялся, а в UI элемент оставался навечно до перезапуска приложения.
3. **Блокировка UI при дисковом I/O:** в `SavedL_Albums.kt` вызовы `albumDb.insert` и `albumDb.delete` выполнялись напрямую на потоке вызова (Main UI thread) без корутинного контекста `Dispatchers.IO`.

*Исправление:*
- Во всех хранилищах операции сделаны идемпотентными по ключу: в `add()` выполняется `list.removeAll { it.id == item.id }; list.add(item)`.
- В `remove()` элементы удаляются строго по уникальному идентификатору: `list.removeAll { it.id == item.id }`.
- В `SavedL_Albums.kt` файловые операции перенесены на `Dispatchers.IO`, а мутации списка `list` — на `Dispatchers.Main`.
- Добавлен юнит-тест `SavedX_FavoritesTest` для проверки удаления по ID при несовпадающих метаданных и предотвращения дублирования.

---

### C33 — `Luscious.getAlbum`: выброс `NumberFormatException` на некорректных URL или больших ID. Средняя.

[Luscious.kt:42-50, 72-78](../feature-l/src/main/java/com/client/xvideos/l/net/Luscious.kt#L42-L50)

В методе `getAlbum(albumInput: Any)`:
```kotlin
val id = when (albumInput) {
    is Int -> albumInput.toString()
    is Long -> albumInput.toString()
    is String -> extractIdFromUrl(albumInput) ?: albumInput
    else -> throw IllegalArgumentException("albumInput must be Int or String")
}
return AlbumInfo(id.toInt(), download, repository, requestScope)
```
1. Если строка не совпадала с исходным шаблоном `Regex("/albums/[^_]+_(\\d+)")` (например, прямой ID с пробелами, URL вида `/albums/12345/` или URL с несколькими подчеркиваниями в названии), строка передавалась в `id.toInt()` и выбрасывала `NumberFormatException`.
2. Если передан `Long` больше `Int.MAX_VALUE`, `toInt()` падал с переполнением.
3. Текст исключения `albumInput must be Int or String` игнорировал поддерживаемый тип `Long`.

*Исправление:*
- Разбор переведён на безопасный `toIntOrNull()`, `Long` валидируется в диапазоне `0..Int.MAX_VALUE`.
- Регулярное выражение `extractIdFromUrl` расширено до `/albums/(?:[^/]*_)?(\\d+)`, поддерживающего как составные названия с произвольным числом подчеркиваний, так и прямые числовые пути альбомов.
- При невалидном вводе выбрасывается строгое `IllegalArgumentException("Invalid album ID: $albumInput")`.
- Добавлены unit-тесты в `LPureFunctionsTest`.

---

### C34 — `DownloadRequestQueue.idRequestMap`: неограниченная утечка памяти. Высокая.

[DownloadRequestQueue.kt:7, 58-77](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadRequestQueue.kt#L7), [KDownloader.kt:35-56](../core/src/main/java/com/client/xvideos/common/kdownloader/KDownloader.kt#L35-L56)

В `DownloadRequestQueue`:
```kotlin
private val idRequestMap = java.util.concurrent.ConcurrentHashMap<Int, DownloadRequest>()
```
Каждый поставленный в очередь запрос регистрировался в `idRequestMap[request.downloadId] = request`.
Удаление происходило исключительно при ручной отмене через `cancel(id)` или `cancelAll()`. При штатном завершении загрузки (`onCompleted`) или при ошибке (`onError`) объект `DownloadRequest` оставался в таблице навсегда.
`DownloadRequest` удерживает URL, заголовки, пути к файлам на диске и ссылки на слушатели. В ходе долгой сессии пользователя таблица непрерывно разрасталась в памяти.

*Исправление:*
- В `DownloadRequestQueue` и `KDownloader` добавлен метод `remove(id: Int)`.
- В `DownloadRequestQueue.enqueue` выставляется статус `Status.QUEUED`.
- В `KDownloader.enqueue` слушатель обёрнут в декоратор, гарантированно вызывающий `reqQueue.remove(req.downloadId)` в блоке `finally` после завершения обработки терминальных статусов `onCompleted` и `onError`.
- Добавлен юнит-тест `KDownloaderQueueTest`.

---

### T23 — `Utils.renameFileName`: безвозвратное уничтожение скачанного файла в блоке `finally` при сбое `renameTo`. Высокая.

[Utils.kt:25-43](../core/src/main/java/com/client/xvideos/common/kdownloader/utils/Utils.kt#L25-L43)

В функции `renameFileName(oldPath, newPath)`:
```kotlin
@Throws(IOException::class)
fun renameFileName(oldPath: String, newPath: String) {
    val oldFile = File(oldPath)
    try {
        val newFile = File(newPath)
        if (newFile.exists()) {
            if (!newFile.delete()) {
                throw IOException("Deletion Failed")
            }
        }
        if (!oldFile.renameTo(newFile)) {
            throw IOException("Rename Failed")
        }
    } finally {
        if (oldFile.exists()) {
            oldFile.delete()
        }
    }
}
```
`File.renameTo` возвращает `false` при переносе между разными точками монтирования (внутренняя память / SD-карта) или кратковременном удержании дескриптора системой сканирования медиа.
При возврате `false` выбрасывался `IOException("Rename Failed")`, после чего блок `finally` безусловно удалял `oldFile`.
Пользователь полностью терял уже скачанный многомегабайтный файл без возможности восстановления.

*Исправление:*
- Если `renameTo` завершается неуспешно, выполняется `oldFile.copyTo(newFile, overwrite = true)`, и `oldFile` удаляется только после успешного завершения копирования.
- Если копирование падает с ошибкой, исходный файл не удаляется, сохраняя возможность восстановления.
- Устранено безусловное удаление исходного файла из блока `finally`.
- Добавлен unit-тест `renameFileName successfully renames source file to destination` в `KDownloaderQueueTest`.

---

### C35 — `ExpandMenuVM` не находит локально сохранённые лайки для анимированных/видео элементов. Средняя.

[ExpandMenuVM.kt:150-195](../feature-l/src/main/java/com/client/xvideos/l/ui/element/expandMenu/ExpandMenuVM.kt#L150-L195), [LCollectionFs.kt:297-322](../feature-l/src/main/java/com/client/xvideos/l/featured/saved/LCollectionFs.kt#L297-L322)

1. В `ExpandMenuVM.kt:152, 190` поиск локальной папки лайка производился только по полю `item.url_to_original`. Для анимированных картинок и видео в `PicsDetails` поле `url_to_original` часто равно `null`, а сетевой адрес находится в `url_to_video` или резолвится через `lDownloadUrl()`. В результате `saveToGallery` никогда не находил уже скачанный локальный файл и всегда заново выкачивал его по сети через `lDownloadMediaToShareCache`, а `startP2p` не мог построить готовый бандл и скачивал файл в outbox.
2. В `LCollectionFs.kt:298-320` функция `lFindLikeFolder` не сверяла `url` с `metadata.sourcePreviewUrl` и `previewFiles[].sourceUrl`, а также выполняла `File(url)` с canonical path resolution для сетевых HTTP URL.

*Исправление:*
- В `ExpandMenuVM` поиск папки переведён на fallback-цепочку: `item.url_to_original ?: item.url_to_video ?: item.lDownloadUrl()`.
- В `LCollectionFs.kt` добавлено сопоставление по `metadata.sourcePreviewUrl` и превью-ссылкам, а проверка `lIsInside` пропускается для сетевых HTTP/HTTPS URL.

---

## Статус

| Находка | Класс | Статус | Коммит |
| --- | --- | --- | --- |
| C32 | корректность | закрыт | рабочий дифф |
| C33 | корректность | закрыт | рабочий дифф |
| C34 | корректность / память | закрыт | рабочий дифф |
| T23 | конкурентность / потеря данных | закрыт | рабочий дифф |
| C35 | корректность | закрыт | рабочий дифф |

---

## Проверка

```
./gradlew testDebugUnitTest --no-daemon
BUILD SUCCESSFUL in 27s
140 actionable tasks: 19 executed, 121 up-to-date
149 tests completed, 0 failed

./gradlew detekt --no-daemon
BUILD SUCCESSFUL in 11s
12 actionable tasks: 4 executed, 8 up-to-date
0 detekt issues
```

---

## Что осталось открытым

- Тракт P2P (`core/.../p2p/`) заморожен по решению владельца от 11.09.2026.
- Архитектурные соглашения T1 (`StorageCleanupGate`) и T2 (`EventBus sequential`) сохранены без изменений.
