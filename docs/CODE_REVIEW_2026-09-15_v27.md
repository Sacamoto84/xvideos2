# Код-ревью xvideos — проход 48

> **Срез:** `9de0fea` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `9de0fea`. Изменено 5 файлов в модулях `app`, `core`, `feature-l`, `feature-x`.
Линзы: математическая корректность и форматирование UI (`UI`), защита криптографических операций и настроек от повреждений (`C`), нормализация состояний коллекций (`UI`), безопасность оверлеев длительности видео (`UI`). Граница P2P (`core/.../p2p/`) заморожена по решению владельца и не затрагивалась.

## Находки

### UI52 — Потеря знака минус при вычислении отрицательных дробей между 0 и -1 в `CalculatorState`. Высокая.

[app/src/main/java/com/client/xvideos/calculator/CalculatorState.kt:305-320](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/calculator/CalculatorState.kt#L305)

При получении отрицательного десятичного результата в диапазоне `(-1, 0)` (например, `1 - 1.5 = -0.5` или `0 - 0.25 = -0.25`), метод `formatNumber` разбивал `plain = "-0.5"` на `parts = ["-0", "5"]`. Вызов `parts[0].toLongOrNull()` возвращал `0L`, а `integerFormatter.format(0L)` возвращал `"0"`, полностью отсекая знак минус. В результате калькулятор выводил `"0.5"` вместо `"-0.5"`.
**Исправление:** Метод `formatNumber` обновлён: знак `isNegative` извлекается заранее, целая и дробная части форматируются от абсолютного значения, после чего знак минус гарантированно возвращается в результирующую строку. Покрыто юнит-тестами в [CalculatorStateTest.kt](file:///g:/xvideos2/app/src/test/java/com/client/xvideos/calculator/CalculatorStateTest.kt).

### C114 — Крах с `IllegalArgumentException` при повреждённых Base64-ключах замка в `AppLockRepository`. Средняя.

[core/src/main/java/com/client/xvideos/common/applock/AppLockRepository.kt:31-35, 156-160](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/applock/AppLockRepository.kt#L31)

В `fromBase64()` вызов `Base64.decode(this, Base64.NO_WRAP)` не был изолирован через `runCatching`. При повреждении файла настроек или обрыве записи строки хеша/соли `Base64.decode` выбрасывал необрабатываемое исключение `IllegalArgumentException: bad base-64`, приводя к аварийному завершению приложения при вызове `verifyPassword()`. Кроме того, `isPasswordSet()` не проверял валидность сохранённых строк Base64.
**Исправление:** `fromBase64()` переведён на `runCatching { Base64.decode(...) }.getOrNull()`, а `isPasswordSet()` валидирует успешность декодирования Base64 соли и хеша.

### UI53 — Сохранение ненормализованного и небезопасного имени в `SavedL_Collection.setCollection`. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt:173-178](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt#L173)

Метод `setCollection(collectionName)` напрямую сохранял переданный параметр в состояние `currentCollectionName = collectionName` без нормализации. Если строка содержала начальные/конечные пробелы или недопустимые символы, состояние загрязнялось, а последующий вызов `refresh()` завершался ошибкой «Недопустимое название коллекции» при каждом рендере экрана.
**Исправление:** Добавлена нормализация перед сохранением: `val safeName = CollectionName.normalizeOrNull(collectionName) ?: return` с показом сообщения об ошибке.

### UI54 — Обрезка цифр длительности и холостая аллокация Text в `DashboardsPaginatedListScreen`. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/DashboardsPaginatedListScreen.kt:220-246](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/DashboardsPaginatedListScreen.kt#L220)

В разметке ячейки дашборда для отображения длительности видео использовался слепой вызов `cell.duration.dropLast(1)`. Если длительность не заканчивалась точкой (например, `"12:34"`), отсекалась значащая последняя цифра (`"12:3"`). Кроме того, для пустых строк `""` всё равно аллоцировались два Compose `Text` элемента со смещением и шрифтами.
**Исправление:** Добавлена нормализация `val durationText = cell.duration.trim().removeSuffix(".")` с проверкой `if (durationText.isNotEmpty())`.

### UI55 — Некорректная обрезка и аллокация в `DurationOverlay` экрана `ScreenFavorites`. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/screens/favorites/ScreenFavorites.kt:315-339](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/favorites/ScreenFavorites.kt#L315)

Аналогично ячейкам дашборда, в `DurationOverlay` выполнялся безусловный `duration.dropLast(1)`, искажающий отображение времени, не оканчивающегося на точку, и создающий пустые `Text` элементы при пустом значении длительности.
**Исправление:** Заменено на `val text = duration.trim().removeSuffix(".")` с ранним выходом `if (text.isEmpty()) return`.

## Статус

| Находка | Класс | Статус | Описание / Исправление |
| --- | --- | --- | --- |
| UI52 | App / Calculator | Исправлен | Сохранение знака минус при форматировании отрицательных дробей `(-1, 0)` |
| C114 | Core / AppLock | Исправлен | Null-safe декодирование Base64 соли и хеша в `AppLockRepository` |
| UI53 | Feature-L / Collection | Исправлен | Нормализация имени коллекции в `SavedL_Collection.setCollection` |
| UI54 | Feature-X / Dashboards | Исправлен | Корректное удаление суффикса точки и проверка непустоты длительности видео |
| UI55 | Feature-X / Favorites | Исправлен | Защита от потери цифр и пустой композиции в `DurationOverlay` |
