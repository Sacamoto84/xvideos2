# Код-ревью xvideos — проход 27

> **Срез:** `master` · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода — `master` (коммит после прохода 26).
Линзы:
- **Жизненный цикл файловых дескрипторов и синхронизация БД KDownloader** (`DownloadTask`, `Utils`, `KDownloaderQueueTest`);
- **Корректность формирования URL тегов XVideos, обработка пустых ответов сети и UX экрана тегов** (`ScreenTagsViewModel`, `TagsPaginatedListScreen`, `X_TagUrlSanitizationTest`);
- **Изоляция слоёв архитектуры (data layer / UI) в репозитории Luscious** (`Repository.kt`);
- **Соблюдение архитектурного запрета на изменение P2P-транспорта** (`core/.../p2p/` не модифицировался).

Ключевой итог: **все 3 находки 27-го прохода устранены**. Репозиторий полностью собирается, detekt чист (0 замечаний), все unit-тесты успешно проходят (140 задач, 0 ошибок).

---

## Находки

### C57 — Удержание открытого дескриптора файла `RandomAccessFile` при вызове `renameFileName` и утечка записей загрузок в SQLite DB KDownloader. Высокая.

[DownloadTask.kt:250-289](../core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadTask.kt#L250-L289), [Utils.kt:31-44](../core/src/main/java/com/client/xvideos/common/kdownloader/utils/Utils.kt#L31-L44), [KDownloaderQueueTest.kt:70-87](../core/src/test/java/com/client/xvideos/common/kdownloader/KDownloaderQueueTest.kt#L70-L87)

В конвейере загрузки файлов `DownloadTask.kt` дескриптор `outputStream` (`FileDownloadRandomAccessFile` в режиме `"rw"`) закрывался только в финальном блоке `finally` корутины. Вызов `renameFileName(tempPath, path)` по окончании скачивания происходил внутри блока `try`, когда поток оставался открытым:
1. На Windows и в файловых системах с обязательными блокировками дескрипторов `oldFile.renameTo(newFile)` завершался сбоем (`false`).
2. Функция `renameFileName` пыталась выполнить аварийное копирование `oldFile.copyTo(newFile, overwrite = true)` с последующим `oldFile.delete()`. Но вызов `oldFile.delete()` также завершался ошибкой `IOException("Deletion Failed")` из-за занятого дескриптора процесса.
3. В результате полностью и успешно скачанный файл объявлялся сбойным (`Status.FAILED`), вызывался `listener.onError()`, а временный файл оставался заблокированным.
4. Кроме того, метод `removeNoMoreNeededModelFromDatabase()` вызывался только при перезапуске/смене ETag, но никогда не вызывался при успешном завершении скачивания (`onCompleted`) или при отмене (`CANCELLED`), из-за чего в локальной таблице `downloads` накапливались неудаляемые строки.
5. При отмене загрузки `deleteTempFile()` вызывался до закрытия дескриптора, что также блокировало удаление `.temp` файла на диске.

*Исправление:*
- В `DownloadTask.kt` потоки `outputStream`, `inputStream` и `httpClient` синхронизируются, закрываются и обнуляются через `closeAllSafely` **до** вызова `renameFileName` и до вызовов `deleteTempFile()`.
- Вызов `removeNoMoreNeededModelFromDatabase()` добавлен во все точки успешного завершения скачивания и отмены загрузки (`CANCELLED`).
- В `closeAllSafely` добавлено явное обнуление ссылок на `httpClient` и `inputStream` в секциях `finally`.
- В `KDownloaderQueueTest` добавлен тест, проверяющий корректное переименование после закрытия потока записи.

---

### C58 — Недопустимые пробелы в URL тегов XVideos в `ScreenTagsViewModel` и зависание экрана без возможности повтора в `TagsPaginatedListScreen`. Средняя.

[ScreenTagsViewModel.kt:66-78](../feature-x/src/main/java/com/client/xvideos/x/screens/tags/ScreenTagsViewModel.kt#L66-L78), [TagsPaginatedListScreen.kt:46-80](../feature-x/src/main/java/com/client/xvideos/x/screens/tags/atom/TagsPaginatedListScreen.kt#L46-L80), [X_TagUrlSanitizationTest.kt:1-46](../feature-x/src/test/java/com/client/xvideos/x/X_TagUrlSanitizationTest.kt#L1-L46)

Парсер тегов `parserItemVideoTags.kt` извлекает текстовые метки тегов через `it.text()` (например, `"big tits"` или `"hard core"`). При клике на тег в плеере открывался экран `ScreenTags`, передававший строку в `ScreenTagsViewModel`:
1. В `ScreenTagsViewModel.loadPage` запрос формировался напрямую как `$urlStart/tags/$tag/$index`. Пробелы в URL запрещены стандартом HTTP и движком OkHttp. Сетевой вызов падал с `IllegalArgumentException`, `readHtmlFromURLDirect` перехватывал исключение и возвращал пустую строку `""`.
2. Функция `loadPage` возвращала `parserScreenTags("")` с пустым списком элементов, не выбрасывая ошибку наружу.
3. Экран `TagsPaginatedListScreen` видел не-null пустой список `loaded.isEmpty()` и отображал пустой чёрный экран без какого-либо текста ошибки или заглушки.
4. При реальных сбоях сети `failed = true` отображал статический текст `"Страница не загрузилась"` без кнопки «Повторить» (в отличие от `DashboardsPaginatedListScreen`).

*Исправление:*
- В `ScreenTagsViewModel.loadPage` добавлена нормализация тега: `val formattedTag = tag.trim().replace(Regex("\\s+"), "-")` согласно схеме адресации XVideos (`/tags/big-tits/0`).
- При получении пустого HTML метод `loadPage` выбрасывает `IOException`, переводя экран пагинации в состояние ошибки.
- При загрузке нулевой страницы обновляется состояние заголовков и числа страниц `screen`, что восстанавливает метаданные даже в случае первоначального сбоя сети при инициализации.
- В `TagsPaginatedListScreen` добавлен `retryTrigger` с кнопкой «Повторить» и отображение плейсхолдера `"Видео не найдены"` при пустом списке роликов.
- Добавлен юнит-тест `X_TagUrlSanitizationTest`, проверяющий нормализацию тегов и валидность сформированных URI.

---

### A8 — Нарушение изоляции слоёв архитектуры: прямой вызов UI `SnackBar.error` из слоя данных в `Repository.kt`. Низкая.

[Repository.kt:205-215](../feature-l/src/main/java/com/client/xvideos/l/repository/Repository.kt#L205-L215)

В `feature-l/.../Repository.kt` внутри метода `openURI` в секции обработки исключений `catch (e: Exception)` находился прямой вызов `SnackBar.error("Ошибка запроса к Luscious")`:
1. Это нарушало архитектурное правило разделения ответственности G2: слой данных (Data Layer / Repository) не должен иметь зависимостей на UI-компоненты и контекст отображения.
2. При сбое серии параллельных постраничных запросов альбома это приводило к лавинообразному показу всплывающих снекбаров в UI.
3. Вызывающие слои (`AlbumPicsDetails`, `AlbumInfo`, `ScreenAlbum`) уже получают `Result.failure(e)` и самостоятельно отображают ошибки в соответствующих UI-компонентах с кнопками повтора.

*Исправление:*
- Из `Repository.kt` удалён вызов `SnackBar.error(...)`.
- Удалён неиспользуемый импорт `com.client.xvideos.common.snackbar.SnackBar`.

---

## Статус

| Находка | Класс | Статус | Коммит |
| --- | --- | --- | --- |
| C57 | корректность | закрыт | рабочее дерево |
| C58 | корректность | закрыт | рабочее дерево |
| A8 | архитектура | закрыт | рабочее дерево |

---

## Проверка

```bash
./gradlew detekt
# BUILD SUCCESSFUL in 1s (0 issues across all 5 modules)

./gradlew testDebugUnitTest
# BUILD SUCCESSFUL in 27s (140 actionable tasks, 100% unit tests passed)
```

Автоматические тесты:
- `KDownloaderQueueTest` — проверка работы очереди, дедупликации и переименования файлов KDownloader;
- `X_TagUrlSanitizationTest` — проверка нормализации строк тегов с пробелами, дефисами и валидации формируемых URI;
- Все тесты модулей `:app`, `:core`, `:feature-l`, `:feature-r`, `:feature-x` зелёные.

## Что осталось открытым

- P2P-тракт (`core/.../p2p/`) остаётся замороженным по решению владельца от 11.09.2026.
