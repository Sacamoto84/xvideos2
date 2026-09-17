# Код-ревью xvideos — проход 83

> **Срез:** `4844535` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `4844535`. Изменено 4 файла в модуле `:feature-l`.

Линзы:
- `UI` / Фиксация ключа `start` и вынос `persistentListOf` в селекторе типа альбома ([AlbumListFilterAlbumType.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumListFilterAlbumType.kt)).
- `UI` / Фиксация ключа `onStart` и вынос `persistentListOf` в селекторе типа контента ([AlbumListFilterContentType.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumListFilterContentType.kt)).
- `UI` / Вынос статического списка опций размера альбома на уровень файла ([AlbumListFilterSize.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumListFilterSize.kt)).
- `UI` / Вынос вычисления уникальных категорий сортировки альбомов на уровень файла ([AlbumFilterDisplay.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterDisplay.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### UI103 — Отсутствие ключа start в remember и пересоздание списка опций в AlbumListFilterAlbumType. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumListFilterAlbumType.kt:26](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumListFilterAlbumType.kt#L26)

В `AlbumListFilterAlbumType` начальный выбранный индекс запоминался без передачи ключа `start`. При внешнем сбросе фильтров или применении сохранённого пресета компонент сохранял старый выбранный индекс. Также список `options = listOf(...)` аллоцировался заново при каждой рекомпозиции.

**Исправление:**
- Добавлен ключ: `remember(start) { mutableIntStateOf(start) }`.
- Список опций вынесен в константу уровня файла `private val ALBUM_TYPE_OPTIONS = persistentListOf(...)`.

---

### UI104 — Отсутствие ключа onStart в remember и пересоздание списка опций в AlbumListFilterContentType. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumListFilterContentType.kt:28](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumListFilterContentType.kt#L28)

В селекторе `AlbumListFilterContentType` индекс выбранного типа контента запоминался в `remember` без ключа `onStart`. При загрузке пресета извне компонент не обновлял выбранный переключатель. Список названий вкладок аллоцировался на каждом цикле рекомпозиции.

**Исправление:**
- Добавлен ключ: `remember(onStart) { mutableIntStateOf(...) }`.
- Список опций вынесен в константу уровня файла `private val CONTENT_TYPE_OPTIONS = persistentListOf(...)`.

---

### UI105 — Аллокация списка диапазонов размера альбома при каждой рекомпозиции в AlbumListFilterSize. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumListFilterSize.kt:40](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumListFilterSize.kt#L40)

Список текстовых меток диапазонов `itemS = listOf("Any", "0..25", ...)` создавался непосредственно в теле Composable-функции `AlbumListFilterSize` на каждом кадре рекомпозиции.

**Исправление:**
- Список вынесен на верхний уровень файла в `private val SIZE_OPTIONS = persistentListOf(...)`.

---

### UI106 — Вычисление и сохранение уникальных категорий сортировки внутри Composable-функции в AlbumFilterDisplay. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterDisplay.kt:45](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/molecule/filter/atom/AlbumFilterDisplay.kt#L45)

В `AlbumFilterDisplay` список уникальных категорий `uniquePrimaryList` вычислялся через `remember { albumFilterDisplay.map { it.primary }.distinct() }`. Поскольку `albumFilterDisplay` является статическим списком, это вычисление и выделение слота `remember` были избыточными.

**Исправление:**
- Вычисление вынесено на уровень файла в константу `private val UNIQUE_PRIMARY_LIST = albumFilterDisplay.map { it.primary }.distinct()`.

---

## Статус

| Находка | Класс | Статус | Детали |
| --- | --- | --- | --- |
| UI103 | UI / Compose | закрыт | Ключ `start` в `remember` и `persistentListOf` опций типа альбома |
| UI104 | UI / Compose | закрыт | Ключ `onStart` в `remember` и `persistentListOf` опций типа контента |
| UI105 | UI / Compose | закрыт | Вынос `SIZE_OPTIONS` на верхний уровень в `AlbumListFilterSize` |
| UI106 | UI / Compose | закрыт | Вынос `UNIQUE_PRIMARY_LIST` на уровень файла в `AlbumFilterDisplay` |

## Проверка

```
.\gradlew.bat app:detekt core:detekt feature-l:detekt feature-r:detekt feature-x:detekt
BUILD SUCCESSFUL

.\gradlew.bat testDebugUnitTest
BUILD SUCCESSFUL in 11s (140 actionable tasks)
```
