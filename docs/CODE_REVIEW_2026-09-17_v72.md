# Код-ревью xvideos — проход 93

> **Срез:** `94e4c62` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `94e4c62`. Затронут модуль `:feature-l`. Проведена комплексная ревизия экранов раздела Luscious (альбомы, топы, коллекции, сохранённые лайки), устранены избыточные контейнеры `Scaffold` и сопутствующие подавления `@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")`, декомпозированы длинные функции (`LongMethod`) с выносом модальных диалогов и оверлеев в независимые компоненты, очищен устаревший `detekt baseline`.

Линзы:
- `UI` / Устранение избыточных `Scaffold`, удаление подавлений `@SuppressLint`, вынос вычислений `screenWidth` из списков, добавление стабильных ключей `key` в `items`, декомпозиция разметки ([L_ScreenAlbumTopHits.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumTopHits/L_ScreenAlbumTopHits.kt), [L_ScreenSavedLikesTab.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/likes/L_ScreenSavedLikesTab.kt), [L_Screen_CollectionTab.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/L_Screen_CollectionTab.kt), [ScreenCollectionName.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/ScreenCollectionName.kt), [L_ScreenExplorer.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/L_ScreenExplorer.kt), [ScreenAlbumList.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumList.kt)).
- `D` / Удаление закомментированного мёртвого кода и устаревших аннотаций `@OptIn(DelicateCoroutinesApi::class)` ([L_ScreenAlbumTopHits.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumTopHits/L_ScreenAlbumTopHits.kt), [ScreenCollectionName.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/ScreenCollectionName.kt)).
- `L` / Перевод логов жизненного цикла ScreenModel из информационного уровня `Timber.i` в отладочный `Timber.d` ([L_ScreenAlbumTopHits.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumTopHits/L_ScreenAlbumTopHits.kt)).
- `Q` / Декомпозиция сложных функций и удаление устаревших исключений `LongMethod` из конфигурации Detekt ([config/detekt/baseline.xml](../config/detekt/baseline.xml)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### UI122 — Избыточные Scaffold-контейнеры и подавления UnusedMaterial3ScaffoldPaddingParameter. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumTopHits/L_ScreenAlbumTopHits.kt:73](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumTopHits/L_ScreenAlbumTopHits.kt#L73)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/likes/L_ScreenSavedLikesTab.kt:43](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/likes/L_ScreenSavedLikesTab.kt#L43)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/L_Screen_CollectionTab.kt:98](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/L_Screen_CollectionTab.kt#L98)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/ScreenCollectionName.kt:82](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/ScreenCollectionName.kt#L82)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/L_ScreenExplorer.kt:49](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/L_ScreenExplorer.kt#L49)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumList.kt:155](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumList.kt#L155)

На экранах топов альбомов, сохранённых лайков и коллекций использовался компонент `Scaffold` исключительно как фоновый контейнер без верхних или нижних панелей (`topBar`, `bottomBar`). Это приводило к необходимости подавления предупреждений компилятора `@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")`. В `ScreenAlbumList` и `ScreenCollectionName` подавления маскировали неиспользуемые отступы.

**Исправление:**
- В `L_ScreenAlbumTopHits` и `L_ScreenSavedLikesTab` пустой `Scaffold` заменён на легковесный `Box(modifier = Modifier.fillMaxSize().background(Theme.background))`.
- Удалены аннотации `@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")` и неиспользуемые импорты `Scaffold` и `SuppressLint`.
- В `ScreenAlbumList` отступы `Scaffold` корректно проброшены в `Modifier.padding(bottom = padding.calculateBottomPadding())`, аннотация `@SuppressLint` удалена.
- Заменено сравнение `title != ""` на идиоматичное `title.isNotEmpty()`.

---

### D15 — Закомментированный отладочный код и устаревшие аннотации. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumTopHits/L_ScreenAlbumTopHits.kt:70, 107](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumTopHits/L_ScreenAlbumTopHits.kt#L70)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/ScreenCollectionName.kt:53, 81](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/ScreenCollectionName.kt#L53)

В `L_ScreenAlbumTopHits` присутствовали закомментированные фрагменты кода (`val haptic = ...`, `dialogExpanded`, `selectIndexDrawer`), оставшиеся от старых реализаций. В `ScreenCollectionName` использовалась устаревшая аннотация `@OptIn(DelicateCoroutinesApi::class)`, которая более не требовалась коду экрана.

**Исправление:**
- Полностью удалён закомментированный мёртвый код.
- Удалены аннотации `@OptIn(DelicateCoroutinesApi::class)` и соответствующие импорты.
- В `ScreenLAlbumTopHitsSM` лог переведён с `Timber.i` на `Timber.d`.
- В `L_ScreenAlbumTopHits` переименована затененая переменная в цикле тегов (`album`), вычисление `screenWidth` вынесено из цикла, добавлен стабильный `key` для элементов списков.

---

### Q12 — Архитектурная декомпозиция LongMethod экранов коллекций и списков альбомов. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/L_Screen_CollectionTab.kt:180-260](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/L_Screen_CollectionTab.kt#L180)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumList.kt:237-340](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumList.kt#L237)

Функции `Content()` на экранах `L_Screen_CollectionTab` и `ScreenAlbumListContent` превышали лимит длины методов detekt (`LongMethod`: >120 строк) и содержали громоздкую монолитную вёрстку с встроенными диалогами действий, переименования, подтверждения удаления и оверлеями фильтрации.

**Исправление:**
- В `L_Screen_CollectionTab.kt` модальные диалоги выделены в отдельные приватные Composable-функции:
  - `CollectionActionDialog`: меню выбора действия над коллекцией;
  - `CollectionRenameDialog`: диалог переименования коллекции с валидацией;
  - `CollectionDeleteDialog`: диалог подтверждения удаления.
- В `ScreenAlbumList.kt` логика отображения сетки страниц и оверлея фильтров декомпозирована в:
  - `AlbumListPageGrid`: отображение сетки альбомов с кастомным скроллбаром и заголовком;
  - `AlbumListFilterOverlay`: анимированный оверлей фильтрации с корректным наложением поверх контента.
- Из `config/detekt/baseline.xml` удалены устаревшие подавления `LongMethod`, теперь код полностью соответствует строгим правилам Detekt.

---

## Сводка верификации

- `detekt`: 0 предупреждений и ошибок во всех модулях проекта (`:app`, `:core`, `:feature-l`, `:feature-r`, `:feature-x`).
- `testDebugUnitTest`: 140 задач выполнено, 100% юнит-тестов пройдено успешно.
- `compileReleaseKotlin`: 61 задача выполнена, чистая сборка релизных Kotlin-артефактов без ошибок.
