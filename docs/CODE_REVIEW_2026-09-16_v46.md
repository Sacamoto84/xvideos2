# Код-ревью xvideos — проход 67

> **Срез:** `b199cc2` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `b199cc2`. Изменено 13 исходных файлов в модуле `feature-r`.

Линзы:
- `D` / Устранение блокирующего дискового ввода-вывода (канонизация путей `requireInside`) из основного потока в [DownloadRed.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt).
- `UI` / Фиксация стабильности Compose-контрактов (`@Stable`, `@Immutable`) для моделей данных и 6 ScreenModel экранов в [Downloader.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt), [DownloadRed.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt), [R_Saved_Subscriptions.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Subscriptions.kt), [R_Saved_NichesCaches.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_NichesCaches.kt), [SavedCollectionName.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/collection/SavedCollectionName.kt), [R_Screen_CreatorsTab.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_CreatorsTab.kt), [R_Screen_Saved_DownloadTab.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_DownloadTab.kt), [R_Screen_Saved_LikesTab.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_LikesTab.kt), [R_Screen_Saved_SubscriptionsTab.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_SubscriptionsTab.kt), [SavedNichesTab.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/savedNiche/SavedNichesTab.kt).
- `T` / Устранение `var`-мутабельности коллекций и `MutableStateFlow` в [Downloader.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt), [R_Saved_Collection.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Collection.kt), [R_Saved_Likes.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Likes.kt), [R_Saved_Creator.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Creator.kt), [R_Saved_Subscriptions.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Subscriptions.kt).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### D9 — Синхронная проверка путей (requireInside) на вызывающем/UI-потоке в saveToGallery. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt:120-137](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt#L120)

Метод `saveToGallery(item: GifsInfo)` выполнял вызовы `requireInside(baseDir, userDir)` и `requireInside(userDir, local)` синхронно до запуска корутины. `requireInside` вызывает `canonicalFile`, разрешающий симлинки через файловую систему. При нажатии кнопки «Сохранить в галерею» в интерфейсе это приводило к дисковому вводу-выводу на главном потоке.

**Исправление:**
- Создание `File` объектов и проверка `requireInside` перенесены внутрь `scope.launch(Dispatchers.IO)`.

---

### UI81 — Отсутствие @Stable на Voyager ScreenModel и моделях состояния в feature-r. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/collection/SavedCollectionName.kt:108](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/collection/SavedCollectionName.kt#L108)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_CreatorsTab.kt:347](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_CreatorsTab.kt#L347)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_DownloadTab.kt:279](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_DownloadTab.kt#L279)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_LikesTab.kt:105](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_LikesTab.kt#L105)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_SubscriptionsTab.kt:263](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/R_Screen_Saved_SubscriptionsTab.kt#L263)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/savedNiche/SavedNichesTab.kt:177](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/tab/savedNiche/SavedNichesTab.kt#L177)

Ряд `ScreenModel` моделей в табах раздела «Сохранённое» R не были аннотированы `@Stable`. Из-за этого Compose-компилятор считал их нестабильными типами и отключал smart skipping при рекомпозиции.
Кроме того, классы `SelectedCreator`, `R_Saved_NichesCaches`, `ItemsRedDownload`, `RedDownloadEnqueueReport` и `RedDownloadRecoveryReport` не имели аннотаций `@Stable`/`@Immutable`.

**Исправление:**
- Добавлен `@Stable` ко всем 6 классам `ScreenModel` (`ScreenRedCollectionNameSM`, `ScreenSavedCreatorSM`, `ScreenSavedDownloadSM`, `ScreenSavedLikesSM`, `ScreenSavedSubscriptionsSM`, `ScreenSavedNichesSM`).
- Добавлен `@Stable` к `SelectedCreator` и `R_Saved_NichesCaches`.
- Добавлен `@Immutable` к `ItemsRedDownload`, `RedDownloadEnqueueReport` и `RedDownloadRecoveryReport`.

---

### T34 — Нестабильные var-ссылки на списки и потоки состояния в data-слое R. Низкая.

[feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt:48](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt#L48)
[feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Collection.kt:36](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Collection.kt#L36)
[feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Likes.kt:24](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Likes.kt#L24)
[feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Creator.kt:21](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Creator.kt#L21)
[feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Subscriptions.kt:32](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Subscriptions.kt#L32)

Поля `percent`, `selectedCollection`, `list` и `listCreators` были объявлены как `var`, хотя переприсваивание инстансов не требовалось и могло приводить к потере подписчиков.

**Исправление:**
- Все указанные ссылки переведены в неизменяемые `val`.

---

## Итог верификации

- **Unit-тесты:** `./gradlew :feature-r:testDebugUnitTest` — успешно (все 83 теста пройдены).
- **Detekt:** `./gradlew :feature-r:detekt` — 0 ошибок и предупреждений.
- **Архитектурные ограничения:** Замороженный P2P (`core/.../p2p/`) не изменялся.
