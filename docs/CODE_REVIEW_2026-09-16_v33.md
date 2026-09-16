# Код-ревью xvideos — проход 54

> **Срез:** `fe45dfe` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `fe45dfe`. Изменено 3 исходных файла в модулях `feature-r`, `feature-l`, `feature-x`, добавлены и расширены unit-тесты в `feature-r` (`RedPooledVideoPlayerTest.kt`) и `feature-x` (`X_BottomNavigationButtonsTest.kt`).

Линзы:
- `UI` / `C` / Корректность жестов и перемотки в Compose: устранение нежелательной перемотки на 1 кадр назад при нулевом драге (тапе) в нижней трети экрана и изоляция жестов от неактивных соседних страниц пула в [RedPooledVideoPlayer.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/video/RedPooledVideoPlayer.kt).
- `T` / `S` / Асинхронный ввод-вывод и валидация файловых операций: вынос дисковых проверок и создания каталога коллекции в [SavedL_Collection.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt) на `Dispatchers.IO` с валидацией результата `mkdirs()`.
- `UI` / Геометрия Compose Scroll: исправление расчёта смещения элемента пагинации для центрирования на экране в [ScreenDashBoardsBottomNavigationButtons.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/common/bottomKeyboard/ScreenDashBoardsBottomNavigationButtons.kt).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### UI68 / C130 — Устранение ложной перемотки назад при касании и изоляция жестов от неактивных страниц в `RedPooledVideoPlayer`. Высокая.

[feature-r/src/main/java/com/client/xvideos/r/ui/video/RedPooledVideoPlayer.kt:235-248](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/video/RedPooledVideoPlayer.kt#L235)

В `RedPooledVideoPlayer` обработчик горизонтального драга в нижней трети экрана вычислял смещение перемотки следующим образом:
```kotlin
val stepMs = if (seekDragAmount.absoluteValue > 400) 1000L else (1000 / 30f).toLong()
val deltaMs = if (seekDragAmount > 0) stepMs else -stepMs
exo.seekTo(exo.clampSeekPositionMs(exo.currentPosition + deltaMs))
```
1. Если пользователь просто нажимал в нижнюю треть экрана без перемещения пальца (или если итоговое смещение жеста равнялось нулю), условие `seekDragAmount > 0` возвращало `false`, приводя к `deltaMs = -stepMs` (-33 мс). В результате каждый тап или минимальное касание в нижней зоне приводили к отматыванию видео назад на 1 кадр.
2. Модификатор `Modifier.pointerInput(player)` не отслеживал `isCurrentPage`. Соседняя подогретая страница в `VerticalPager` (с `beyondViewportPageCount = 1`) могла перехватывать жесты скролла/драга и производить перемотку на скрытом плеере из пула.

**Исправление:**
- Вычисление дельты вынесено в чистую функцию `calculateDragDeltaMs(seekDragAmount: Float): Long`. При нулевом значении или нечисловом `Float.NaN` возвращается `0L` без вызова перемотки плеера.
- В `pointerInput(player, isCurrentPage)` добавлена проверка `if (!isCurrentPage) return@pointerInput`.
- В `RedPooledVideoPlayerTest.kt` добавлены тесты для `calculateDragDeltaMs` (нулевой драг, субпиксели, переходы за порог 400px).

---

### T24 / S36 — Перенос создания коллекции на `Dispatchers.IO` и валидация создания каталога. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt:85-100](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/SavedL_Collection.kt#L85)

В отличие от `deleteCollection`, метод `createCollection` в `SavedL_Collection` выполнял `collectionRoot.exists()` и `collectionRoot.mkdirs()` синхронно в вызывающем Main-потоке. Кроме того, метод игнорировал возвращаемое значение `mkdirs()`: в случае нехватки свободного места на носителе или сбоя прав доступа пользователю всё равно отображалось уведомление об успешном создании коллекции.

**Исправление:**
Файловые проверки и вызов `mkdirs()` перенесены в корутину `scope.launch(Dispatchers.IO)`. Проверяется `collectionRoot.mkdirs() || collectionRoot.isDirectory`. При ошибке создания отображается `SnackBar.error`, при успехе — `SnackBar.success` и вызов `refreshCollectionList()` с переключением на `Dispatchers.Main`.

---

### UI69 — Исправление центрирования кнопки выбранной страницы в нижней панели навигации X. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/screens/common/bottomKeyboard/ScreenDashBoardsBottomNavigationButtons.kt:178-189](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/common/bottomKeyboard/ScreenDashBoardsBottomNavigationButtons.kt#L178)

В функции `calculateCenterOffset` расчет смещения содержал ошибку:
```kotlin
return (viewportWidth + itemWidth) / 2 // Центр экрана минус половина ширины элемента
```
При `state.animateScrollToItem(index = indexToScroll, scrollOffset = -offset)` отрицательный `scrollOffset` сдвигает начало элемента вправо на `offset`. Чтобы центр элемента (`itemStart + itemWidth / 2`) совпал с центром вьюпорта (`viewportWidth / 2`), смещение начала должно быть строго `(viewportWidth - itemWidth) / 2`. Значение с плюсом смещало активную кнопку правее центра ровно на одну ширину элемента.

**Исправление:**
Формула исправлена на `((viewportWidth - itemWidth) / 2).coerceAtLeast(0)`. Логика вынесена в тестируемую функцию с перегрузкой `calculateCenterOffset(viewportWidth, itemWidth)` и покрыта тестами в `X_BottomNavigationButtonsTest.kt`.

---

## Итог верификации

- **Unit-тесты:** `./gradlew testDebugUnitTest` — успешно (все тесты по всем 5 модулям выполнены без ошибок).
- **Detekt:** `./gradlew detekt --rerun-tasks` — 0 ошибок и предупреждений по всем 5 модулям.
- **Архитектурные ограничения:** Замороженный P2P (`core/.../p2p/`) не изменялся.
