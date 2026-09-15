# Код-ревью xvideos — проход 43

> **Срез:** `a47bbf1` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `a47bbf12e2e6739b9afba200becf86ed227e1b96`. Изменено 5 файлов в модулях `core`, `feature-x`, `feature-r`.
Линзы: устойчивость загрузок и целостность медиа (`C`), валидация путей и URI (`C`), надежность парсинга поисковой выдачи (`C`), безопасность и целостность избранного (`C`), изоляция дискового кэша от сетевых исключений (`C`). Граница P2P (`core/.../p2p/`) заморожена по решению владельца и не затрагивалась.

## Находки

### C94 — Завершение загрузки с сохранением 0-байтовых пустышек при пустом ответе сервера в `DownloadTask`. Критическая.

[core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt:310-320](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt#L310)

Если удалённый сервер возвращал 200 OK с пустым телом ответа или соединение обрывалось до отправки первого байта (`req.downloadedBytes == 0L`), условие `totalBytes > 0 && req.downloadedBytes < totalBytes` не срабатывало (например, при неизвестном `totalBytes <= 0`).
В результате `DownloadTask` закрывал поток, переименовывал 0-байтовый временный файл в целевое имя файла, удалял запись из базы данных очереди и вызывал `listener.onCompleted()` со статусом `COMPLETED`. На диск пользователя сохранялись битые 0-байтовые видеоролики и картинки.
**Исправление:** Добавлена строгая проверка `if (req.downloadedBytes == 0L)` перед завершением: временный файл удаляется, запрос сбрасывается, статус переводится в `Status.FAILED` и инициируется `listener.onError("Download failed: empty response body (0 bytes)")`.

### C95 — Сбой парсинга локальных файлов с URI-схемой `file://` в `UrlImage`. Высокая.

[core/src/main/java/com/client/xvideos/common/coil/UrlImage.kt:187-198](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/coil/UrlImage.kt#L187)

В компоненте `UrlImage` в блоке вычисления `dataSource` ветка `else` (для локальных файлов) передавала строку `url` напрямую в конструктор `File(url)`.
Если путь приходил в виде URI со схемой `file://` (например, `file:///storage/emulated/0/...`), на Android и Linux создавался объект `File` с некорректным путём, начинающимся с литерала `"file:"`. Попытка загрузить картинку через Coil приводила к `FileNotFoundException` и показу плейсхолдера ошибки.
**Исправление:** Префикс `file://` нормализуется и отсекается: `cleanUrl = if (url.startsWith("file://", ignoreCase = true)) url.substring(7) else url`.

### C96 — Падение с `SerializationException` и спам в логи при пустом ответе поиска в `parseJson`. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/search/parseJson.kt:10-16](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/search/parseJson.kt#L10)

При получении пустой или пробельной строки от сервера вызов `searchJson.decodeFromString(SearchResult.serializer(), json)` выбрасывал `SerializationException: Unexpected EOF` и печатал полный стектрейс ошибки в `Timber.e`.
**Исправление:** В функцию `parseJson` добавлен ранний возврат `if (json.isBlank()) return null`, исключающий лишние выбросы исключений и засорение журнала логов.

### C97 — Отсутствие валидации `item.id` в `SavedX_Favorites`. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Favorites.kt:31-66](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Favorites.kt#L31)

Методы добавления (`add`), удаления (`remove`) и проверки наличия (`contains`) не проверяли идентификатор видео на валидность (`item.id <= 0L`).
Элементы с некорректными/фиктивными ID могли попадать в дисковую базу данных `favoritesDb` и загрязнять коллекцию избранного.
**Исправление:**
- В `add` и `remove` добавлена проверка `if (item.id <= 0L) { SnackBar.error("Недопустимый ID видео"); return }`.
- В `contains(id: Long)` добавлена защита `id > 0L && favoriteIds.contains(id)`.

### C98 — Ошибка сериализации кэша приводила к сбою успешного сетевого запроса в `RedApi.cacheMediaResponse`. Высокая.

[feature-r/src/main/java/com/client/xvideos/r/network/api/RedApi.kt:386-388](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/network/api/RedApi.kt#L386)

В методе `cacheMediaResponse` обработка ответа сети выполнялась через `.onSuccess { cache.put(route.url, RJson.encodeToString(it)) }`.
По контракту `kotlin.Result.onSuccess`, если лямбда внутри `onSuccess` выбрасывает исключение (например, сбой сериализации `SerializationException`, ошибка записи на диск или закончившееся место), это исключение перевыбрасывается наружу. В результате абсолютно успешный сетевой запрос падал с ошибкой и не доходил до пользователя.
**Исправление:** Сериализация и запись в кэш обёрнуты в защищённый блок `runCatching { cache.put(route.url, RJson.encodeToString(it)) }.onFailure { Timber.w(...) }`.

## Статус

| Находка | Класс | Статус | Описание / Исправление |
| --- | --- | --- | --- |
| C94 | Core / KDownloader | Исправлен | Защита от 0-байтовых файлов при пустом ответе сервера в `DownloadTask` |
| C95 | Core / Coil | Исправлен | Отсечение схемы `file://` для корректного открытия локальных файлов в `UrlImage` |
| C96 | Feature-X / Search | Исправлен | Ранний возврат `null` на пустой JSON в `parseJson` без ложного `SerializationException` |
| C97 | Feature-X / Favorites | Исправлен | Валидация `id > 0L` в `SavedX_Favorites` (защита от сохранения фиктивных ID) |
| C98 | Feature-R / RedApi | Исправлен | Изоляция записи кэша через `runCatching`, защита сетевого ответа от сбоя кэша |
