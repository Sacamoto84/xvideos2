# Код-ревью xvideos — проход 101

> **Срез:** `063f8cf` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `063f8cf`. Затронут модуль `:core`. Проведено ужесточение санитайзинга путей файловой системы и имён пользовательских коллекций: запрещены непечатаемые управляющие символы ASCII (`\u0000`..`\u001F`, включая `\n`, `\r`, `\t`) в `SafePath.normalizeRelativePath`, `SafePath.isUnsafeItemName` и `CollectionName.normalizeOrNull`. В семействе функций форматирования чисел `toPrettyCount.kt` предотвращено переполнение знака при обработке граничного значения `Long.MIN_VALUE`. Все изменения покрыты модульными тестами.

Линзы:
- `S` / Предотвращение инъекций управляющих символов в имена файлов и пути каталогов ([SafePath.kt](../core/src/main/java/com/client/xvideos/common/io/SafePath.kt), [CollectionName.kt](../core/src/main/java/com/client/xvideos/common/collectionDB/CollectionName.kt)).
- `C` / Защита от переполнения дополнительного кода (`Long.MIN_VALUE`) при вычислении абсолютных значений для форматирования счётчиков ([toPrettyCount.kt](../core/src/main/java/com/client/xvideos/common/util/toPrettyCount.kt)).
- `T` / Добавление тестов на отсечение управляющих символов и экстремальных граничных чисел ([SafePathTest.kt](../core/src/test/java/com/client/xvideos/common/io/SafePathTest.kt), [CollectionNameTest.kt](../core/src/test/java/com/client/xvideos/common/collectionDB/CollectionNameTest.kt), [ToPrettyCountTest.kt](../core/src/test/java/com/client/xvideos/common/util/ToPrettyCountTest.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### S49 — Пропуск непечатаемых управляющих символов ASCII при валидации путей и имён папок. Высокая.

[core/src/main/java/com/client/xvideos/common/io/SafePath.kt:18](../core/src/main/java/com/client/xvideos/common/io/SafePath.kt#L18)
[core/src/main/java/com/client/xvideos/common/collectionDB/CollectionName.kt:29](../core/src/main/java/com/client/xvideos/common/collectionDB/CollectionName.kt#L29)

`SafePath.isUnsafeItemName` и `SafePath.normalizeRelativePath` проверяли лишь фиксированный набор символов (`/`, `\`, `:`, `*`, `?`, `"`, `<`, `>`, `|`, `\0`), пропуская управляющие символы ASCII из диапазона 0x01..0x1F (`\n`, `\r`, `\t` и др.). Аналогично, `CollectionName.normalizeOrNull` выполнял `raw.trim()`, который срезал управляющие символы по краям, но не отвергал входные строки целиком, если в них присутствовали скрытые или разделительные байты. В Unix/Android-файловых системах имена файлов с переводами строк могут приводить к разрушению логов, протоколов резервного копирования и метаданных.

**Исправление:**
- В `SafePath.normalizeRelativePath` сегменты с `segment.any { it < ' ' }` теперь вызывают отсечение пути (`null`).
- В `SafePath.isUnsafeItemName` добавлено условие `name.any { it < ' ' }`.
- В `CollectionName.normalizeOrNull` добавлена строгая предварительная проверка: `if (raw.any { it in FORBIDDEN_CHARS || it < ' ' }) return null`.

---

### C21 — Переполнение 2's complement при abs(Long.MIN_VALUE) в toPrettyCount. Средняя.

[core/src/main/java/com/client/xvideos/common/util/toPrettyCount.kt:11](../core/src/main/java/com/client/xvideos/common/util/toPrettyCount.kt#L11)

В функциях `toPrettyCount`, `toPrettyCount2`, `toPrettyCount3` и `toPrettyCountInt` отрицательные значения преобразовывались через стандартную функцию `abs(this)`. Однако в 64-битном знаковом целом числе `Long` отрицательный диапазон асимметричен: `abs(Long.MIN_VALUE)` переполняется обратно в `Long.MIN_VALUE` (`-9223372036854775808L`), из-за чего форматирование выдавало отрицательное число с суффиксом величины вместо корректного абсолютного значения.

**Исправление:**
- Использован безопасный паттерн: `val absNumber = if (this == Long.MIN_VALUE) Long.MAX_VALUE else abs(this)`.

---

### T44 — Модульное тестирование безопасности путей и граничных значений счётчиков. Средняя.

[core/src/test/java/com/client/xvideos/common/io/SafePathTest.kt](../core/src/test/java/com/client/xvideos/common/io/SafePathTest.kt)
[core/src/test/java/com/client/xvideos/common/collectionDB/CollectionNameTest.kt](../core/src/test/java/com/client/xvideos/common/collectionDB/CollectionNameTest.kt)
[core/src/test/java/com/client/xvideos/common/util/ToPrettyCountTest.kt](../core/src/test/java/com/client/xvideos/common/util/ToPrettyCountTest.kt)

Ранее отсутствовали тесты на проверку управляющих байтов в `SafePathTest` и `CollectionNameTest`, а также экстремального граничного значения `Long.MIN_VALUE` в `ToPrettyCountTest`.

**Исправление:**
- В `SafePathTest` добавлены проверки отклонения путей и имён с `\n`, `\r`, `\t` и `\u001F`.
- В `CollectionNameTest` добавлен тест `имя с управляющими символами отвергается`.
- В `ToPrettyCountTest` добавлен тест `toPrettyCount handles extreme bounds without 2s complement overflow` для всех вариаций функции.

---

## Сводка верификации

- `detekt`: 0 предупреждений и ошибок в `:core`.
- `testDebugUnitTest`: 100% тестов `:core` зелёные (219 тестов).
