# Код-ревью xvideos — проход 108

> **Срез:** `6715bef` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `6715bef`. Затронут модуль `:feature-l`: компоненты пагинации и управления списка альбомов (`com.client.xvideos.l.ui.screens.screenAlbumList`). Обнаружен и устранён критический дефект в селекторе страниц `AlbumListPageSelector`: при навигации влево ограничение `coerceAtLeast(1)` блокировало возврат на первую страницу каталога (0-й индекс в пейджере), а переход вправо через `coerceAtMost(pageMax)` приводил к выходу за пределы массива допустимых страниц (`pageMax` вместо `pageMax - 1`) и отображению несуществующей страницы `Page 11 of 10`. Алгоритм вынесен в чистые функции `calculatePrevAlbumPage` и `calculateNextAlbumPage` и полностью покрыт модульными тестами. В `ScreenLAlbumListSM` добавлена защита от загрузки отрицательных страниц (`page < 0`).

Линзы:
- `UI` / Исправление блокировки первой страницы и выхода за границы диапазона ([AlbumListPageSelector.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/atom/AlbumListPageSelector.kt), [ScreenAlbumListSM.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumListSM.kt)).
- `T` / Тестирование граничных условий пагинации альбомов ([AlbumListPageSelectorTest.kt](../feature-l/src/test/java/com/client/xvideos/l/ui/screens/screenAlbumList/AlbumListPageSelectorTest.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### UI128 — Невозможность вернуться на первую страницу и оффсет +1 при перелистывании в AlbumListPageSelector. Высокая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/atom/AlbumListPageSelector.kt:73](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/atom/AlbumListPageSelector.kt#L73)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/atom/AlbumListPageSelector.kt:119](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/atom/AlbumListPageSelector.kt#L119)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumListSM.kt:162](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumListSM.kt#L162)

Пейджер альбомов `statePager` работает с 0-индексированными страницами (первая страница отображается пользователю как `Page 1 of N` при `page = 0`).
1. Кнопка «Назад» вызывала `onChange((page - 1).coerceAtLeast(1))`. Находясь на второй странице (`page = 1`), нажатие стрелки назад давало `(1 - 1).coerceAtLeast(1) = 1` — пользователь был навсегда заблокирован от перехода на первую страницу каталога кнопкой пагинации.
2. Кнопка «Вперёд» вызывала `onChange((page + 1).coerceAtMost(pageMax))`. На последней допустимой странице с индексом `pageMax - 1` (например, 9 для 10 страниц) нажатие вызывало переход на индекс `10`, приводя к ошибке `Page 11 of 10` и запросу пустых данных.

**Исправление:**
- Выделены чистые вспомогательные функции `calculatePrevAlbumPage(page: Int): Int = (page - 1).coerceAtLeast(0)` и `calculateNextAlbumPage(page: Int, pageMax: Int): Int = (page + 1).coerceAtMost((pageMax - 1).coerceAtLeast(0))`.
- Кнопки навигации переведены на эти функции.
- В `ScreenLAlbumListSM.loadAlbumList` добавлена ранняя проверка `if (page < 0) return`.

---

### T51 — Модульное тестирование расчёта переходов по страницам каталога альбомов. Средняя.

[feature-l/src/test/java/com/client/xvideos/l/ui/screens/screenAlbumList/AlbumListPageSelectorTest.kt](../feature-l/src/test/java/com/client/xvideos/l/ui/screens/screenAlbumList/AlbumListPageSelectorTest.kt)

Граничные переходы навигатора страниц списка альбомов ранее не тестировались.

**Исправление:**
- Создан тестовый класс `AlbumListPageSelectorTest` (3 теста), проверяющий корректный возврат на страницу 0, остановку на границах 0 и `pageMax - 1`, а также корректную работу при общем числе страниц 0 и 1.

---

## Сводка верификации

- `detekt`: 0 предупреждений и ошибок по всему проекту (`detekt --offline`).
- `testDebugUnitTest`: 100% тестов всех модулей зелёные (`testDebugUnitTest --offline`).
- `compileReleaseKotlin`: успешная компиляция release-версии всех модулей проекта.
