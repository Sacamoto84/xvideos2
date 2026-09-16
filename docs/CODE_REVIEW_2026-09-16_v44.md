# Код-ревью xvideos — проход 65

> **Срез:** `4b07c18` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `4b07c18`. Изменено 4 исходных файла в модулях `core` и `app`.

Линзы:
- `C` / Корректность и отказоустойчивость файловых операций в [CollectionDB.kt](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt) (fallback при переименовании коллекций через `copyRecursively` + `deleteRecursively`) и модульные тесты в [CollectionDBTest.kt](file:///g:/xvideos2/core/src/test/java/com/client/xvideos/common/collectionDB/CollectionDBTest.kt).
- `UI` / Фиксация стабильности Compose-контрактов (`@Stable`) для состояния калькулятора в [CalculatorState.kt](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/calculator/CalculatorState.kt) и ScreenModel экрана настроек в [AppSettingsScreen.kt](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### C53 — Отсутствие fallback при неудаче java.io.File.renameTo в CollectionDB.renameCollection. Высокая.

[core/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt:145-165](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/collectionDB/CollectionDB.kt#L145)

Метод `CollectionDB.renameCollection(oldName, newName)` вызывал `oldDir.renameTo(newDir)` и при возврате `false` немедленно выбрасывал `IOException("Failed to rename collection directory")`. В Android и Linux метод `File.renameTo` часто терпит неудачу при перемещении каталогов между точками монтирования (scoped storage / emulation / SD-карта) либо при кратковременном удержании открытых дескрипторов файлов процессами индексации.

Аналогичная проблема ранее была решена в `SavedL_Collection` через надёжный fallback: если атомарный `renameTo` завершился с `false`, выполняется рекурсивное копирование `oldDir.copyRecursively(newDir, overwrite = false)` с последующей очисткой исходной папки `oldDir.deleteRecursively()`.

**Исправление:**
- В `CollectionDB.renameCollection` добавлен двухэтапный fallback:
  ```kotlin
  val renamed = oldDir.renameTo(newDir) || (
      oldDir.copyRecursively(newDir, overwrite = false) && oldDir.deleteRecursively()
  )
  if (!renamed) {
      throw IOException("Failed to rename collection directory: ${oldDir.absolutePath} -> ${newDir.absolutePath}")
  }
  ```
- В `CollectionDBTest.kt` добавлены модульные тесты: успешное переименование, обработка несуществующей папки, конфликт с уже существующей целевой коллекцией, сохранение файлов внутри переименованной директории.

---

### UI79 — Отсутствие @Stable на CalculatorState и AppSettingsSM в модуле app. Средняя.

[app/src/main/java/com/client/xvideos/calculator/CalculatorState.kt:25](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/calculator/CalculatorState.kt#L25)
[app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt:94](file:///g:/xvideos2/app/src/main/java/com/client/xvideos/screenSettings/AppSettingsScreen.kt#L94)

Класс `CalculatorState` содержит изменяемые поля (`var displayValue by mutableStateOf(...)` и др.), однако не был аннотирован `@Stable`. Из-за этого Compose-компилятор считал объект нестабильным параметром при передаче в Composable-функции экрана калькулятора, вызывая избыточные рекомпозиции всей сетки кнопок.
Аналогично, `AppSettingsSM` в `AppSettingsScreen.kt` не имел аннотации `@Stable`.

**Исправление:**
- Добавлена аннотация `@Stable` к `class CalculatorState`.
- Добавлена аннотация `@Stable` к `class AppSettingsSM`.

---

## Итог верификации

- **Unit-тесты:** `./gradlew :core:testDebugUnitTest :app:testDebugUnitTest` — успешно.
- **Detekt:** `./gradlew :core:detekt :app:detekt` — 0 ошибок и предупреждений.
- **Архитектурные ограничения:** Замороженный P2P (`core/.../p2p/`) не изменялся.
