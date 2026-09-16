# Код-ревью xvideos — проход 68

> **Срез:** `38ae988` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `38ae988`. Изменено 8 исходных файлов в модуле `feature-l`.

Линзы:
- `T` / Устранение гонок параллельных файловых мутаций (отмена in-flight задач через `mutationJob`) в [SavedL_Likes.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Likes.kt) и [SavedL_Albums.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Albums.kt).
- `D` / Вынос сетевых запросов и парсинга JSON на пул `Dispatchers.IO` в [AlbumTopHits.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/net/AlbumTopHits.kt).
- `UI` / Фиксация стабильности Compose-контрактов (`@Stable`, `@Immutable`) и устранение `var`-мутабельности для моделей данных и классов ScreenModel в [SavedL_Likes.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Likes.kt), [SavedL_Albums.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Albums.kt), [AlbumTopHits.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/net/AlbumTopHits.kt), [ScreenLAlbumLandingTag.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/albumLandingTag/ScreenLAlbumLandingTag.kt), [L_ScreenAlbumSearch.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumSearch/L_ScreenAlbumSearch.kt), [L_ScreenAlbumTopHits.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumTopHits/L_ScreenAlbumTopHits.kt), [SavedL.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL.kt), [AlbumPicsDetails.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/net/AlbumPicsDetails.kt), [AlbumInfo.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/net/AlbumInfo.kt) и [LDownloadRecovery.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/LDownloadRecovery.kt).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### T35 — Гонки параллельных файловых мутаций в SavedL_Likes и SavedL_Albums. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Likes.kt:40-85](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Likes.kt#L40)
[feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Albums.kt:24-85](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Albums.kt#L24)

В `SavedL_Likes` операции `add` и `remove`, а в `SavedL_Albums` операции `add`, `addAndPicsDetails` и `remove` запускали длительные дисковые операции (`scope.launch(Dispatchers.IO)`) без отслеживания `Job`. При быстром повторном нажатии на кнопки добавления/удаления параллельные задачи выполнялись одновременно, что могло приводить к повреждению файловой структуры и наложению результатов `refresh()`.

**Исправление:**
- Добавлено поле `private var mutationJob: Job? = null` в `SavedL_Likes` и `SavedL_Albums`.
- Перед запуском операций мутации выполняется отмена предыдущей активной задачи `mutationJob?.cancel()`.

---

### D10 — Неявный диспетчер корутины сетевого запроса в AlbumTopHitsImpl. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/net/AlbumTopHits.kt:27](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/net/AlbumTopHits.kt#L27)

`AlbumTopHitsImpl` запускал `scope.launch { ... }` без явного указания `Dispatchers.IO`. Если переданный скоуп имел диспетчер `Main` (или `Default`), выполнение сетевого запроса `repository.openURI` и разбор JSON происходили на вызывающем потоке. Кроме того, поле `items` было объявлено как `var`, что допускало случайное переприсваивание ссылки и поломку Compose Snapshot State.

**Исправление:**
- Запуск корутины переведён на `scope.launch(Dispatchers.IO)`.
- Поле `items` изменено с `var` на неизменяемое `val`.

---

### UI82 — Отсутствие @Stable/@Immutable на моделях и ScreenModel в feature-l. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/albumLandingTag/ScreenLAlbumLandingTag.kt:407](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/albumLandingTag/ScreenLAlbumLandingTag.kt#L407)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumSearch/L_ScreenAlbumSearch.kt:228](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumSearch/L_ScreenAlbumSearch.kt#L228)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumTopHits/L_ScreenAlbumTopHits.kt:232](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumTopHits/L_ScreenAlbumTopHits.kt#L232)

Модели экранов `ScreenLAlbumLandingTagSM`, `ScreenLAlbumSearchSM` и `ScreenLAlbumTopHitsSM`, а также классы состояния `SavedL`, `SavedL_Albums`, `SavedL_Likes`, `AlbumPicsDetails`, `AlbumInfo` и `AlbumTopHitsImpl` не были аннотированы `@Stable`. Модели `LDownloadRecoveryReport`, `LAlbumPageLoadIssue` и `LAlbumPicsBundleSnapshot` не имели аннотации `@Immutable`. Поля `albumTopHits` в `ScreenLAlbumLandingTagSM` и `ScreenLAlbumTopHitsSM` были объявлены как `var`.

**Исправление:**
- Добавлен `@Stable` к `ScreenLAlbumLandingTagSM`, `ScreenLAlbumSearchSM`, `ScreenLAlbumTopHitsSM`, `SavedL`, `SavedL_Albums`, `SavedL_Likes`, `AlbumPicsDetails`, `AlbumInfo`, `AlbumTopHitsImpl`.
- Добавлен `@Immutable` к `LDownloadRecoveryReport`, `LAlbumPageLoadIssue`, `LAlbumPicsBundleSnapshot`.
- Поля `albumTopHits` переведены в `val`.

---

## Итог верификации

- **Unit-тесты:** `./gradlew :feature-l:testDebugUnitTest` — успешно.
- **Архитектурные ограничения:** Замороженный P2P (`core/.../p2p/`) не изменялся.
