# Код-ревью xvideos — проход 82

> **Срез:** `4844535` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `4844535`. Изменено 4 файла в модулях `:feature-l` и `:app`.

Линзы:
- `UI` / Фиксация ключа `filter` при инициализации имени пресета в диалоге сохранения ([AlbumFilterSaveDialog.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterSaveDialog.kt)).
- `UI` / Фиксация ключей `payloadKey, item` при вычислении отфильтрованного списка фото в полноэкранном режиме ([L_FullScreenImage.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/L_FullScreenImage.kt)).
- `UI` / Фиксация ключа `initialUrl` в диалоге настройки пользовательского DoH URL ([NetworkSettingsSection.kt](../app/src/main/java/com/client/xvideos/screenSettings/section/NetworkSettingsSection.kt)).
- `UI` / Оптимизация композиции `L_ScreenExplorer`: вынос статических коллекций иконок и тегов на уровень файла и зачистка неиспользуемого импорта ([L_ScreenExplorer.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/L_ScreenExplorer.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### UI99 — Отсутствие ключа filter в remember для presetName в AlbumFilterSaveDialog. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterSaveDialog.kt:49](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterSaveDialog.kt#L49)

В диалоге сохранения пресета фильтров начальное имя вычислялось через `remember { mutableStateOf(AlbumFilterPresetManager.generateDefaultName(filter)) }` без указания `filter` в качестве ключа. При повторном открытии диалога с другим фильтром или изменении фильтра поле имени сохраняло старое сгенерированное значение.

**Исправление:**
- Добавлен ключ зависимости: `remember(filter) { mutableStateOf(...) }`.

---

### UI100 — Отсутствие ключей payloadKey и item в remember для filteredPic. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/L_FullScreenImage.kt:133](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/L_FullScreenImage.kt#L133)

В экране полноэкранного просмотра фотографий списка альбома `filteredPic` извлекался через `remember { LFullScreenPayload.get(payloadKey).ifEmpty { listOf(item) } }`. Отсутствие ключей не позволяло обновить список при изменении полезной нагрузки или переходе между экранами.

**Исправление:**
- Добавлены ключи: `remember(payloadKey, item) { ... }`.

---

### UI101 — Отсутствие ключа initialUrl в remember для tempUrl в CustomDohUrlDialog. Низкая.

[app/src/main/java/com/client/xvideos/screenSettings/section/NetworkSettingsSection.kt:237](../app/src/main/java/com/client/xvideos/screenSettings/section/NetworkSettingsSection.kt#L237)

В диалоге ввода кастомного DoH URL временное состояние текстового поля создавалось через `remember { mutableStateOf(initialUrl) }`. При изменении `initialUrl` извне текстовое поле отображало устаревший URL.

**Исправление:**
- Добавлен ключ зависимости: `remember(initialUrl) { mutableStateOf(initialUrl) }`.

---

### UI102 — Размещение статических коллекций persistentListOf внутри Composable-функции в L_ScreenExplorer. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/L_ScreenExplorer.kt:85-101](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/L_ScreenExplorer.kt#L85)

Константные списки иконок и тегов панели навигации объявлялись внутри `Content()` с вызовом `remember { persistentListOf(...) }`. Это выделяло лишние слоты в слоте памяти Compose и требовало импорта `remember`.

**Исправление:**
- Списки вынесены на уровень файла в `private val EXPLORER_ICONS` и `private val EXPLORER_TAGS`.
- Удалён неиспользуемый импорт `androidx.compose.runtime.remember`.

---

## Статус

| Находка | Класс | Статус | Детали |
| --- | --- | --- | --- |
| UI99 | UI / Compose | закрыт | Ключ `filter` в `remember(filter)` для `presetName` |
| UI100 | UI / Compose | закрыт | Ключи `payloadKey, item` в `remember` для `filteredPic` |
| UI101 | UI / Compose | закрыт | Ключ `initialUrl` в `remember(initialUrl)` для `tempUrl` |
| UI102 | UI / Compose | закрыт | Вынос константных списков на уровень файла в `L_ScreenExplorer` |

## Проверка

```
.\gradlew.bat app:detekt core:detekt feature-l:detekt feature-r:detekt feature-x:detekt
BUILD SUCCESSFUL

.\gradlew.bat testDebugUnitTest
BUILD SUCCESSFUL in 17s (140 actionable tasks)
```
