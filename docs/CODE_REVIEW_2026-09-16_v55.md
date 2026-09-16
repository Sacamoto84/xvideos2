# Код-ревью xvideos — проход 76

> **Срез:** `9eb9f9b` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `9eb9f9b`. Изменено 3 исходных файла в модулях `core` и `app`.

Линзы:
- `UI` / Фиксация стабильности Compose-контракта для состояния троттлинга защиты пин-кодом ([AppLockThrottle.kt](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/applock/AppLockThrottle.kt)).
- `T` / Очистка неиспользуемых импортов делегатов состояния Compose в главном меню и экране бэкапов ([MenuScreen.kt](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/screenRoot/MenuScreen.kt), [BackupSelection.kt](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/screenSettings/backup/BackupSelection.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### UI91 — Отсутствие @Immutable на модели состояния троттлинга AppLockThrottle.State. Низкая.

[core/src/main/java/com/client/xvideos/common/applock/AppLockThrottle.kt:34-38](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/applock/AppLockThrottle.kt#L34)

Класс `AppLockThrottle.State` хранит количество неудачных попыток и метки времени блокировки, передаваясь в UI экрана блокировки (`AppLockScreen`). Без явной аннотации `@Immutable` Compose компилятор не мог оптимизировать перерисовку экрана при тиках обратного отсчёта.

**Исправление:**
- Добавлена аннотация `@Immutable` к `AppLockThrottle.State`.

---

### T41 — Неиспользуемые импорты Compose getValue / setValue в MenuScreen и BackupSelection. Низкая.

[app/src/main/java/com/client/xvideos/screenRoot/MenuScreen.kt:28-29](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/screenRoot/MenuScreen.kt#L28)
[app/src/main/java/com/client/xvideos/screenSettings/backup/BackupSelection.kt:7-8](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/screenSettings/backup/BackupSelection.kt#L7)

В файлах `MenuScreen.kt` и `BackupSelection.kt` сохранялись устаревшие неиспользуемые импорты `androidx.compose.runtime.getValue` и `androidx.compose.runtime.setValue`, оставшиеся после предыдущих рефакторингов.

**Исправление:**
- Неиспользуемые импорты удалены.
