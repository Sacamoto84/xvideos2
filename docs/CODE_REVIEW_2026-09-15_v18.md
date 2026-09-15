# Код-ревью xvideos — проход 39

> **Срез:** `a47bbf1` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `a47bbf12e2e6739b9afba200becf86ed227e1b96`. Изменено 6 файлов в модулях `core`, `feature-r`, `feature-l` и `feature-x`.
Линзы: защита файловой целостности (`C`), устойчивость сетевого слоя и предотвращение отравления кэша (`C`), производительность парсинга потоков HLS (`C`). Граница P2P (`core/.../p2p/`) заморожена по решению владельца и не затрагивалась.

## Находки

### C84 — `useCaseShareFile` передаёт системному Intent повреждённые 0-байтовые файлы. Высокая.

[core/src/main/java/com/client/xvideos/common/share/useCaseShareFile.kt:20](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/share/useCaseShareFile.kt#L20), [UseCaseShareFileTest.kt:26](file:///g:/xvideos2/core/src/test/java/com/client/xvideos/common/share/UseCaseShareFileTest.kt#L26)

Функция `useCaseShareFile` проверяла только существование файла `if (!file.exists())`. Если на диске находился повреждённый пустой файл размером 0 байт (остаток прерванной загрузки), системный диалог `ACTION_SEND` открывался и передавал пустышку внешним приложениям (мессенджеры, плееры), где происходил сбой.
**Исправление:**
- Проверка дополнена: `if (!file.exists() || file.length() == 0L)`.
- Добавлено предупреждение в лог и показ `SnackBar.error("Файл повреждён или отсутствует")`.
- Добавлен юнит-тест `useCaseShareFile возвращает false и не падает если файл пустой 0 байт` в `UseCaseShareFileTest`.

### C85 — Передача 0-байтовых файлов в системный шеринг из `useCaseShareGifs`. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/common/share/useCaseShareGifs.kt:35](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/share/useCaseShareGifs.kt#L35)

Метод `useCaseShareGifs` вызывал `useCaseShareFile` по условию `if (file.exists())`. При наличии 0-байтового файла инициировался ошибочный процесс отправки.
**Исправление:** Проверка изменена на `if (file.exists() && file.length() > 0L)` с выводом информативного сообщения пользователю при отсутствии или повреждении файла.

### C86 — `LCollectionFs` и `LSavedLikeMetadata` считают 0-байтовые файлы валидными обложками и элементами коллекций. Высокая.

[feature-l/src/main/java/com/client/xvideos/l/featured/saved/LCollectionFs.kt:134, 148, 170](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/LCollectionFs.kt#L134), [feature-l/src/main/java/com/client/xvideos/l/featured/saved/LSavedLikeMetadata.kt:66, 71, 78](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/LSavedLikeMetadata.kt#L66)

1. В `lResolveCollectionItemsCount` функция считала `File(it, L_METADATA_FILE_NAME).exists()` без проверки размера. При наличии 0-байтового `metadata.json` элемент учитывался в общем счётчике коллекции, но отсеивался при десериализации, вызывая рассинхронизацию отображаемого числа элементов и реального списка.
2. В `lResolveItemPreviewUrl` и `lResolveCollectionPreviewUrl` fallback-кандидаты проверялись только на `exists()`, из-за чего битый 0-байтовый файл выбирался в качестве обложки коллекции.
3. В `LSavedLikeMetadata.toPicsDetails` `mediaFile`, `savedPreviews` и `oldPreviewFile` извлекались через `.takeIf { it.exists() }`, передавая пустые файлы в модель `PicsDetails`.
**Исправление:** Все проверки файлов дополнены условием `it.length() > 0L`.

### C87 — Отравление RAM-кэша ошибками HTTP 404/403 в `readHtmlFromURLDirect` и `getSearchResults`. Высокая.

[feature-x/src/main/java/com/client/xvideos/x/feature/net/readHtmlFromURLDirect.kt:53](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/net/readHtmlFromURLDirect.kt#L53), [feature-x/src/main/java/com/client/xvideos/x/search/getSearchResults.kt:54](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/search/getSearchResults.kt#L54)

По умолчанию в Ktor вызов `client.get(url).bodyAsText()` не выбрасывает исключение при статус-кодах 404, 403 или 500, возвращая HTML-текст страницы ошибки. В `ScreenX_VideoPlayerSM` при вызове `readHtmlFromURLDirect` тело страницы ошибки 404 попадало в `db.cacheUrlStringRam.put(url, content)`: страница ошибки навсегда оседала в оперативной памяти, и любые повторные попытки открыть ролик парсили тело ошибки и падали.
**Исправление:** Добавлена проверка `response.status.isSuccess()`. При неуспешном HTTP-статусе методы логируют предупреждение и возвращают `""` / `null`, предотвращая попадание страниц ошибок в кэш.

### C88 — Утечка клиентов OkHttp, отсутствие DoH/таймаутов и некорректное разрешение URL в `M3U8Helper`. Средняя.

[core/src/main/java/com/client/xvideos/common/videoplayer/util/M3U8Helper.kt:24-105](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/videoplayer/util/M3U8Helper.kt#L24)

1. `M3U8Helper.fetchM3U8Data` создавал и закрывал новый экземпляр `HttpClient(OkHttp)` на каждый запрос манифеста m3u8. Клиент не имел настроек DNS-over-HTTPS (`AppDns`) и таймаутов (`HttpTimeout`), что приводило к сбоям у провайдеров с блокировкой DNS и зависанию соединений.
2. Ручная склейка `$baseUri/${urlLine.trim()}` приводила к невалидным URL (`https://cdn/dir//vod/file.m3u8`), если путь в манифесте начинался со слэша `/`.
3. В циклах обхода строк манифеста происходила повторная компиляция десятков объектов `Regex`.
**Исправление:**
- Подключён общий `HttpClient` с `AppDns` и `HttpTimeout`.
- Применён стандартный резолвер `URI(baseUrl).resolve(trimmed).toString()`, корректно обрабатывающий относительные и абсолютные пути (RFC 3986).
- Все регулярные выражения вынесены в предкомпилированные константы.
- Написаны изолированные юнит-тесты в `M3U8HelperTest`.

## Статус

| Находка | Класс | Статус | Описание / Исправление |
| --- | --- | --- | --- |
| C84 | Core / Share | Исправлен | `useCaseShareFile` проверяет `length() > 0L`, добавлен тест |
| C85 | Feature-R / Share | Исправлен | `useCaseShareGifs` защищён от 0-байтовых файлов |
| C86 | Feature-L / Storage | Исправлен | Фильтрация 0-байтовых файлов в `LCollectionFs` и `LSavedLikeMetadata` |
| C87 | Feature-X / Network | Исправлен | Проверка `response.status.isSuccess()` исключает отравление RAM-кэша |
| C88 | Core / Video Player | Исправлен | Оптимизация `M3U8Helper`: DoH, таймауты, URI.resolve, предкомпиляция Regex |
