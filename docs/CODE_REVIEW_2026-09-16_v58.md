# Код-ревью xvideos — проход 79

> **Срез:** `ec34601` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `ec34601`. Изменено 6 исходных файлов в модуле `feature-r`.

Линзы:
- `UI` / Фиксация стабильности Compose-контрактов для моделей тегов и поисковых подсказок ([TagInfo.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/model/tag/TagInfo.kt), [TagSuggestion.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/model/tag/TagSuggestion.kt), [TagsResponse.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/model/tag/TagsResponse.kt)).
- `UI` / Глубокая Compose-иммутабельность моделей сетевых ответов RedGifs ([TopCreatorsResponse.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/model/TopCreatorsResponse.kt), [MediaResponse.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/model/MediaResponse.kt), [CreatorResponse.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/model/CreatorResponse.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### UI94 — Отсутствие @Immutable на моделях тегов и поисковых подсказок R. Низкая.

[feature-r/src/main/java/com/client/xvideos/r/model/tag/TagInfo.kt:7](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/model/tag/TagInfo.kt#L7)
[feature-r/src/main/java/com/client/xvideos/r/model/tag/TagSuggestion.kt:7](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/model/tag/TagSuggestion.kt#L7)
[feature-r/src/main/java/com/client/xvideos/r/model/tag/TagsResponse.kt:7](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/model/tag/TagsResponse.kt#L7)

Классы `TagInfo`, `TagSuggestion` и `TagsResponse` используются при отображении облака тегов, списков подсказок в строке поиска и карточек категорий. Без явной аннотации `@Immutable` Compose компилятор не мог оптимизировать перерисовку элементов выпадающих списков.

**Исправление:**
- Добавлена аннотация `@Immutable` к `TagInfo`, `TagSuggestion` и `TagsResponse`.

---

### UI95 — Отсутствие @Immutable на моделях сетевых ответов TopCreatorsResponse, MediaResponse и CreatorResponse. Низкая.

[feature-r/src/main/java/com/client/xvideos/r/model/TopCreatorsResponse.kt:7-13](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/model/TopCreatorsResponse.kt#L7)
[feature-r/src/main/java/com/client/xvideos/r/model/MediaResponse.kt:7](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/model/MediaResponse.kt#L7)
[feature-r/src/main/java/com/client/xvideos/r/model/CreatorResponse.kt:7](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/model/CreatorResponse.kt#L7)

Модели `TopCreatorsResponse`, `TopCreator`, `MediaResponse` и `CreatorResponse` возвращаются API-клиентом RedGifs и передаются в Paging-источники и Composable-экраны авторов/исследователя. Из-за наличия коллекций `List<GifsInfo>`, `List<UserInfo>` и `List<NichesInfo>` Compose компилятор по умолчанию помечал параметры как нестабильные.

**Исправление:**
- Добавлена аннотация `@Immutable` к `TopCreatorsResponse`, `TopCreator`, `MediaResponse` и `CreatorResponse`.
