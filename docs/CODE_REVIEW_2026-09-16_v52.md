# Код-ревью xvideos — проход 73

> **Срез:** `9ae7c04` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `9ae7c04`. Изменено 3 исходных файла в модуле `core`.

Линзы:
- `UI` / Фиксация стабильности Compose-контрактов для сущностей базы данных коллекций и фоновых задач загрузок ([CollectionEntity.kt](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/collectionDB/model/CollectionEntity.kt), [DownloadWorkState.kt](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/download/work/DownloadWorkState.kt)).
- `UI` / Гарантия иммутабельности и предотвращение рекомпозиций подписчиков глобальной шины событий ([Event.kt](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/eventBus/Event.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### UI87 — Отсутствие @Immutable на CollectionEntity и моделях состояния загрузок WorkManager. Низкая.

[core/src/main/java/com/client/xvideos/common/collectionDB/model/CollectionEntity.kt:6-10](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/collectionDB/model/CollectionEntity.kt#L6)
[core/src/main/java/com/client/xvideos/common/download/work/DownloadWorkState.kt:6-23](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/download/work/DownloadWorkState.kt#L6)

Модели `CollectionEntity`, `DownloadWorkState` и перечисление `DownloadStatus` передаются в Composable-компоненты экранов сохранённых коллекций, диалогов и индикаторов загрузки. Из-за отсутствия аннотации `@Immutable` Compose компилятор относил их к нестабильным типам (в частности, дженерик `items: List<T>` в `CollectionEntity`), что мешало оптимизации smart skipping при обновлении списков коллекций и отслеживании прогресса загрузок.

**Исправление:**
- Добавлена аннотация `@Immutable` к `CollectionEntity<T>`.
- Добавлена аннотация `@Immutable` к `DownloadStatus` и `DownloadWorkState`.

---

### UI88 — Нестабильная иерархия событий sealed class Event в EventBus. Низкая.

[core/src/main/java/com/client/xvideos/common/eventBus/Event.kt:11-29](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/eventBus/Event.kt#L11)

Класс `Event` является корневым для всех событий приложения (сообщения снекбаров, позиция выхода из полноэкранного режима, прогресс передачи). Подписчики шины событий в Compose-дереве слушают поток `EventBus.events`. Пометка базового класса аннотацией `@Immutable` явно гарантирует компилятору Compose стабильность всех наследников sealed-иерархии.

**Исправление:**
- Добавлена аннотация `@Immutable` к `sealed class Event`.
