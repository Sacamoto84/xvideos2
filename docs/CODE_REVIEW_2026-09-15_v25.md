# Код-ревью xvideos — проход 46

> **Срез:** `dc6cace` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `dc6cace68d6f5ad8df9521fae39d5b06497ec097`. Изменено 5 файлов в модулях `feature-x`, `app`, `core`.
Линзы: валидация входных данных парсера (`C`), форматирование UI калькулятора (`UI`), предотвращение холостых сетевых запросов (`C`), освобождение ресурсов WebView (`C`), безопасность сохранения медиа в галерею (`C`). Граница P2P (`core/.../p2p/`) заморожена по решению владельца и не затрагивалась.

## Находки

### C105 — Пропуск блоков с неположительными `videoId` в `parserListVideo`. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/parcer/parserListVideo.kt:35-36](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/parcer/parserListVideo.kt#L35)

Парсер блоков видео `parserListVideo` проверял только `toLongOrNull()`.
Если блок на странице содержал `data-id="0"` или отрицательное число (рекламный блок, заглушка плеера, трекер), карточка попадала в общий список видео, ломала операции добавления в избранное и засоряла базу данных.
**Исправление:** Добавлена проверка `if (videoId <= 0L) continue`.

### UI50 — Зависание разделителей тысяч при удалении цифр в `CalculatorState.onBackspace`. Средняя.

[app/src/main/java/com/client/xvideos/calculator/CalculatorState.kt:276-281](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/calculator/CalculatorState.kt#L276)

Метод `onBackspace` выполнял вызов `displayValue.dropLast(1)` по уже отформатированной строке.
При наличии пробелов-разделителей тысяч (например, `"-1 000"`) удаление последнего символа приводило к `"-1 00 "` с висячим пробелом в конце. Повторный ввод цифр или удаление приводили к некорректному отображению.
**Исправление:** Строка очищается от форматирующих пробелов перед удалением символа: `val clean = displayValue.removePrefix("-").replace(" ", "")`, после чего результат сохраняется корректно. Покрыто тестом в [CalculatorStateTest.kt](file:///g:/xvideos2/app/src/test/java/com/client/xvideos/calculator/CalculatorStateTest.kt).

### C106 — Холостой сетевой запрос при пустом URL в `readHtmlFromURLDirect`. Низкая.

[feature-x/src/main/java/com/client/xvideos/x/feature/net/readHtmlFromURLDirect.kt:49-50](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/net/readHtmlFromURLDirect.kt#L49)

При вызове `readHtmlFromURLDirect` с пустой или пробельной строкой клиент Ktor пытался выполнить HTTP GET запрос к пустому адресу, генерируя исключение и лог ошибки `Timber.e`.
**Исправление:** Добавлен ранний возврат `if (url.isBlank()) return ""` до вызова сети.

### C107 — Создание headless-WebView и удержание ресурсов при пустом URL в `readHtmlFromURLWebView`. Низкая.

[feature-x/src/main/java/com/client/xvideos/x/feature/net/readHtmlFromURLWebView.kt:36-37](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/net/readHtmlFromURLWebView.kt#L36)

При передаче пустого URL метод переключался на главный поток, создавал экземпляр `WebView` в памяти и ожидал загрузки вплоть до истечения таймаута (45 секунд).
**Исправление:** Добавлен немедленный возврат `if (url.isBlank()) return ""` до выделения WebView.

### C108 — Отсутствие проверки схемы URL в `GallerySaver.saveFromUrl`. Средняя.

[core/src/main/java/com/client/xvideos/common/gallery/GallerySaver.kt:94-98](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/gallery/GallerySaver.kt#L94)

В `GallerySaver.saveFromUrl` проверялось только имя файла. Передача невалидного URL или нестандартных схем приводила к созданию временной директории `gallery_tmp`, запуску корутины и постановке заведомо провальной задачи в `KDownloader`.
**Исправление:** Добавлена валидация `if (url.isBlank() || (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)))` с ранним показом ошибки.

## Статус

| Находка | Класс | Статус | Описание / Исправление |
| --- | --- | --- | --- |
| C105 | Feature-X / Parser | Исправлен | Фильтрация блоков с `videoId <= 0L` в `parserListVideo` |
| UI50 | App / Calculator | Исправлен | Очистка строки от разделителей при `onBackspace` в `CalculatorState` |
| C106 | Feature-X / Network | Исправлен | Ранний выход `if (url.isBlank()) return ""` в `readHtmlFromURLDirect` |
| C107 | Feature-X / WebView | Исправлен | Предотвращение холостого создания `WebView` при пустом URL в `readHtmlFromURLWebView` |
| C108 | Core / Gallery | Исправлен | Валидация схемы HTTP/HTTPS в `GallerySaver.saveFromUrl` |
