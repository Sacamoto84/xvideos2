# Код-ревью xvideos — проход 107

> **Срез:** `6715bef` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `6715bef`. Затронут модуль `:core`: общие утилиты форматирования строковых данных (`com.client.xvideos.common.util`). Исправлены граничные случаи форматирования времени, объёма данных и скорости (`toMinSec`, `formatBytes`, `formatSpeed`, `toTwoDecimalPlacesWithColon`): отрицательные и NaN-значения больше не приводят к артефактам вида `"00:-5"` или `"NaN"`, а расчет степеней 1024 в вычислениях байт переведен на явные `Long`-литералы во избежание переполнения `Int`. Добавлен перегруженный метод `Float.toMinSec()`. Добавлено комплексное тестовое покрытие.

Линзы:
- `C` / Устранение артефактов форматирования и защита от переполнений в утилитах ([toMinSec.kt](../core/src/main/java/com/client/xvideos/common/util/toMinSec.kt), [formatBytes.kt](../core/src/main/java/com/client/xvideos/common/util/formatBytes.kt), [formatSpeed.kt](../core/src/main/java/com/client/xvideos/common/util/formatSpeed.kt), [toTwoDecimalPlacesWithColon.kt](../core/src/main/java/com/client/xvideos/common/util/toTwoDecimalPlacesWithColon.kt)).
- `T` / Тестирование граничных и экстремальных значений утилит форматирования ([FormattingUtilsTest.kt](../core/src/test/java/com/client/xvideos/common/util/FormattingUtilsTest.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### C22 — Искажение вывода отрицательных чисел и NaN в утилитах toMinSec и toTwoDecimalPlacesWithColon. Низкая.

[core/src/main/java/com/client/xvideos/common/util/toMinSec.kt:9](../core/src/main/java/com/client/xvideos/common/util/toMinSec.kt#L9)
[core/src/main/java/com/client/xvideos/common/util/formatBytes.kt:8](../core/src/main/java/com/client/xvideos/common/util/formatBytes.kt#L8)
[core/src/main/java/com/client/xvideos/common/util/formatSpeed.kt:7](../core/src/main/java/com/client/xvideos/common/util/formatSpeed.kt#L7)
[core/src/main/java/com/client/xvideos/common/util/toTwoDecimalPlacesWithColon.kt:18](../core/src/main/java/com/client/xvideos/common/util/toTwoDecimalPlacesWithColon.kt#L18)

В функции `Double.toMinSec()` при передаче отрицательного значения времени (что может происходить при инициализации или перемотке видеоплеера) формировалась строка вида `00:-5`. При `NaN` или `Infinity` функция `Float.toTwoDecimalPlacesWithColon()` возвращала `"NaN"` или `"Infinity"`, отображавшиеся на месте таймера в UI. В `formatBytes` и `formatSpeed` границы `1024 * 1024 * 1024` вычислялись через `Int`-умножение.

**Исправление:**
- В `Double.toMinSec()` добавлена проверка `if (this.isNaN() || this <= 0.0) return "00:00"`, а также добавлена вспомогательная функция `Float.toMinSec()`.
- В `toTwoDecimalPlacesWithColon()` добавлена проверка `if (this.isNaN() || this.isInfinite()) return "0:00"`.
- В `formatBytes` и `formatSpeed` применены типизированные `Long`-литералы (`1024L * 1024L * 1024L`) и единообразная обработка неположительных величин (`bytes <= 0L`).

---

### T50 — Модульное тестирование вспомогательных функций форматирования core. Низкая.

[core/src/test/java/com/client/xvideos/common/util/FormattingUtilsTest.kt](../core/src/test/java/com/client/xvideos/common/util/FormattingUtilsTest.kt)

Утилиты форматирования `toMinSec`, `formatBytes`, `formatSpeed` и `toTwoDecimalPlacesWithColon` не имели прямого модульного покрытия на нормальные и граничные входные данные.

**Исправление:**
- Добавлен тестовый класс `FormattingUtilsTest`, включающий 5 тестовых сценариев: штатное форматирование секунд в минуты, обработка отрицательных чисел и NaN, форматирование байт (B, KB, MB, GB), форматирование сетевой скорости и перевод `Float` в строковый формат времени с двоеточием.

---

## Сводка верификации

- `detekt`: 0 предупреждений и ошибок по всему проекту (`detekt --offline`).
- `testDebugUnitTest`: 100% тестов всех модулей зелёные (`testDebugUnitTest --offline`).
- `compileReleaseKotlin`: успешная компиляция release-версии всех модулей проекта.
