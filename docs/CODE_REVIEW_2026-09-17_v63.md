# Код-ревью xvideos — проход 84

> **Срез:** `4844535` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `4844535`. Изменено 1 файл в модуле `:feature-l`, добавлен 1 новый файл тестов.

Линзы:
- `C` / Захват консистентного атомарного снимка через `updateAndGet` перед асинхронным сохранением в SharedPreferences ([AlbumFilterPresetManager.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterPresetManager.kt)).
- `T` / Изоляция параллельных мутаций и устранение рассинхронизации между памятью и диском при частом добавлении/удалении пресетов ([AlbumFilterPresetManager.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterPresetManager.kt)).
- `T` / Добавление модульных тестов на форматирование сводок и генерацию имён пресетов фильтров ([AlbumFilterPresetManagerTest.kt](../feature-l/src/test/java/com/client/xvideos/l/model/AlbumFilterPresetManagerTest.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### C135 — Чтение устаревшего состояния Flow при асинхронном сохранении пресетов в AlbumFilterPresetManager. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterPresetManager.kt:57-75](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterPresetManager.kt#L57)

В методах `savePreset` и `deletePreset` сначала выполнялся `_presets.update { ... }`, а затем в отдельной фоновой корутине на `IO` вызывался `persist(prefs, _presets.value)`. Если несколько операций добавления или удаления пресетов происходили параллельно, корутины могли прочитать нестабильный срез `_presets.value`, перезаписав диск неактуальным состоянием.

**Исправление:**
- Переход на `_presets.updateAndGet { ... }` с передачей вычисленного неизменяемого снимка напрямую в корутину сохранения: `persist(prefs, snapshot)`.
- Добавлен вспомогательный метод `resetForTesting(...)` с аннотацией `@VisibleForTesting`.

---

### T45 — Отсутствие тестов на логику формирования имён и сводок фильтров. Низкая.

[feature-l/src/test/java/com/client/xvideos/l/model/AlbumFilterPresetManagerTest.kt](../feature-l/src/test/java/com/client/xvideos/l/model/AlbumFilterPresetManagerTest.kt)

В проекте отсутствовали автоматизированные тесты для проверки формирования читаемых названий фильтров (`generateDefaultName`) и текстовой сводки (`formatFilterSummary`) при различных комбинациях поисковых параметров, типов альбомов, категорий контента и тегов.

**Исправление:**
- Написан тестовый класс `AlbumFilterPresetManagerTest`, проверяющий дефолтные имена, форматирование сводок для сложных фильтров и сброс состояния менеджера пресетов.

---

## Статус

| Находка | Класс | Статус | Детали |
| --- | --- | --- | --- |
| C135 | корректность | закрыт | Атомарный снимок `updateAndGet` при сериализации пресетов |
| T45 | многопоточность / тесты | закрыт | Комплексные тесты `AlbumFilterPresetManagerTest` |

## Проверка

```
.\gradlew.bat feature-l:detekt
BUILD SUCCESSFUL

.\gradlew.bat testDebugUnitTest
BUILD SUCCESSFUL in 5s (140 actionable tasks)
```
