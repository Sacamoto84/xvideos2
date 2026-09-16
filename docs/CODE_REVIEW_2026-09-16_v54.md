# Код-ревью xvideos — проход 75

> **Срез:** `e4740db` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `e4740db`. Изменено 3 исходных файла в модуле `feature-r`.

Линзы:
- `UI` / Глубокая Compose-иммутабельность сетевых моделей R для медиа-ссылок, категорий и профилей пользователей ([URL1.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/model/URL1.kt), [NichesInfo.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/model/NichesInfo.kt), [UserInfo.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/model/UserInfo.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### UI89 — Отсутствие @Immutable на модели медиа-ссылок URL1 в GifsInfo. Низкая.

[feature-r/src/main/java/com/client/xvideos/r/model/URL1.kt:7-15](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/model/URL1.kt#L7)

Модель `GifsInfo` помечена как `@Immutable`, однако её вложенный объект `URL1` (содержащий URL постеров, SD/HD видеопотоков и веб-страниц) не имел явной аннотации `@Immutable`. В Compose компиляторе стабильность структуры данных оценивается рекурсивно по всем полям: отсутствие явной стабильности у вложенного типа снижало стабильность всего графа медиа-объекта при передаче в карточки ленты и элементы плеерных списков.

**Исправление:**
- Добавлена аннотация `@Immutable` к классу `URL1`.

---

### UI90 — Отсутствие @Immutable на моделях ниш (NichesInfo, NicheResponse) и пользователя (UserInfo). Низкая.

[feature-r/src/main/java/com/client/xvideos/r/model/NichesInfo.kt:7-49](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/model/NichesInfo.kt#L7)
[feature-r/src/main/java/com/client/xvideos/r/model/UserInfo.kt:51-66](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/model/UserInfo.kt#L51)

Классы `NicheResponse`, `NichesInfo` и `UserInfo` передаются в Composable-функции экрана исследователя (вкладки категорий), результатов поиска и шапки профиля пользователя. Без аннотации `@Immutable` компилятор Compose не мог гарантировать smart skipping при прокрутке и обновлении связанных списков.

**Исправление:**
- Добавлена аннотация `@Immutable` к `NicheResponse` и `NichesInfo`.
- Добавлена аннотация `@Immutable` к `UserInfo`.
