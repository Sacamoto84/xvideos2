# Код-ревью xvideos — проход 77

> **Срез:** `fcfb2a7` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `fcfb2a7`. Изменено 5 исходных файлов в модуле `feature-l`.

Линзы:
- `UI` / Глубокая Compose-стабильность моделей деталей альбомов и медиа-контента ([AlbumDetails.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/model/AlbumDetails.kt), [PicsDetails.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/model/PicsDetails.kt)).
- `UI` / Фиксация стабильности Compose-контрактов для фильтров каталога и жанров ([AlbumListFilter.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/model/AlbumListFilter.kt), [FilterGenre.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/model/FilterGenre.kt), [Display.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/model/Display.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### UI92 — Отсутствие @Immutable на моделях AlbumDetails, Content и вложенных Thumbnails. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/model/AlbumDetails.kt:14-48](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/model/AlbumDetails.kt#L14)
[feature-l/src/main/java/com/client/xvideos/l/model/PicsDetails.kt:86-91](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/model/PicsDetails.kt#L86)

Классы `AlbumDetails` и `Content` активно передаются в Composable-экраны сохранённых альбомов и диалоги управления. При этом в классе `PicsDetails` модель списка `Thumbnails` не имела аннотации `@Immutable`, что нарушало рекурсивную стабильность графа объектов при рендеринге списков фотографий.

**Исправление:**
- Добавлена аннотация `@Immutable` к `AlbumDetails` и `Content`.
- Добавлена аннотация `@Immutable` к `Thumbnails`.

---

### UI93 — Отсутствие @Immutable на моделях фильтров каталога L (AlbumListFilter, FilterGenre, OnlyContent, DataAlbumFilterDisplay). Низкая.

[feature-l/src/main/java/com/client/xvideos/l/model/AlbumListFilter.kt:15-30](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/model/AlbumListFilter.kt#L15)
[feature-l/src/main/java/com/client/xvideos/l/model/FilterGenre.kt:19-71](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/model/FilterGenre.kt#L19)
[feature-l/src/main/java/com/client/xvideos/l/model/Display.kt:5](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/model/Display.kt#L5)

Модели фильтрации `AlbumListFilter`, `FilterGenre`, `OnlyContent` и элементы отображения пресетов `DataAlbumFilterDisplay` используются в диалогах и плашках фильтрации списков альбомов. Без явного указания `@Immutable` компилятор Compose считал параметры нестабильными из-за наличия коллекций `List<FilterGenre>` и строковых списков.

**Исправление:**
- Добавлены аннотации `@Immutable` к `AlbumListFilter`, `FilterGenre`, `OnlyContent` и `DataAlbumFilterDisplay`.
