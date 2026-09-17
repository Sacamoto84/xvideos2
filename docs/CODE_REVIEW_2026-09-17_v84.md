# Код-ревью xvideos — проход 105

> **Срез:** `6715bef` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `6715bef`. Затронут модуль `:feature-r`: компоненты контекстного меню видео (`expand_menu_video`). Устранена орфографическая ошибка в названии функции и параметров (`DropdownMenuItem_Subscribtion` -> `DropdownMenuItem_Subscription`, `isSubscribted` -> `isSubscribed`), добавлена строгая проверка на пустоту имени автора (`item == null || item.userName.isBlank()`) перед обращением к `redApi.readCreator(...)` в пунктах `Subscribe` и `Follow`, что предотвращает отправку некорректных сетевых HTTP-запросов и паразитные ошибки UI. Корректность разрешения состояний подписки и отслеживания покрыта модульными тестами.

Линзы:
- `L` / Защита от невалидных сетевых запросов и исправление опечатки в API меню ([DropdownMenuItem_Subscription.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/expand_menu_video/DropdownMenuItem_Subscription.kt), [DropdownMenuItem_Follow.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/expand_menu_video/DropdownMenuItem_Follow.kt), [ExpandMenuVideo.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/expand_menu_video/ExpandMenuVideo.kt)).
- `T` / Модульное тестирование состояний и граничных условий пунктов меню ([ExpandMenuActionItemLogicTest.kt](../feature-r/src/test/java/com/client/xvideos/r/ui/expand_menu_video/ExpandMenuActionItemLogicTest.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### L37 — Невалидные сетевые вызовы при пустом авторе и опечатки в элементах контекстного меню видео. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/ui/expand_menu_video/DropdownMenuItem_Subscription.kt:19](../feature-r/src/main/java/com/client/xvideos/r/ui/expand_menu_video/DropdownMenuItem_Subscription.kt#L19)
[feature-r/src/main/java/com/client/xvideos/r/ui/expand_menu_video/DropdownMenuItem_Follow.kt:24](../feature-r/src/main/java/com/client/xvideos/r/ui/expand_menu_video/DropdownMenuItem_Follow.kt#L24)
[feature-r/src/main/java/com/client/xvideos/r/ui/expand_menu_video/ExpandMenuVideo.kt:97](../feature-r/src/main/java/com/client/xvideos/r/ui/expand_menu_video/ExpandMenuVideo.kt#L97)

В компонентах `DropdownMenuItem_Subscription` и `DropdownMenuItem_Follow` проверка на валидность имени создателя контента перед обращением к сети ограничивалась проверкой `if (item == null)`. Если видео не имело привязанного автора (`userName.isBlank()`), нажатие на пункт подписки приводило к сетевому вызову `redApi.readCreator("")`, заведомо завершавшемуся 404/Bad Request и показом ошибочного снэкбара пользователю. Кроме того, функция и параметр содержали опечатку `Subscribtion` / `isSubscribted`.

**Исправление:**
- Имя функции и параметров приведено к стандарту: `DropdownMenuItem_Subscription`, `isSubscribed`.
- Введена ранняя проверка `if (item == null || item.userName.isBlank()) { onDismiss.invoke(); return@... }`.
- Вычисление флагов `isSubscribed` и `isFollowed` нормализовано с учётом фильтрации пустых строк (`userName.takeIf { it.isNotBlank() }`).
- Обновлена точка вызова в `ExpandMenuVideo.kt`.

---

### T48 — Модульные тесты вычисления статусов подписки и подписки на авторов в контекстном меню. Низкая.

[feature-r/src/test/java/com/client/xvideos/r/ui/expand_menu_video/ExpandMenuActionItemLogicTest.kt](../feature-r/src/test/java/com/client/xvideos/r/ui/expand_menu_video/ExpandMenuActionItemLogicTest.kt)

Логика сопоставления имени автора в `GifsInfo` со списком авторов в базе подписок и отслеживания ранее не тестировалась изолированно на граничные условия (пустые строки, пробельные строки, `null`).

**Исправление:**
- Создан тестовый класс `ExpandMenuActionItemLogicTest`, проверяющий положительное сопоставление имени автора с записями `UserInfo`, корректное игнорирование пустых/пробельных имён и безопасную обработку `null`.

---

## Сводка верификации

- `detekt`: 0 предупреждений и ошибок по всему проекту (`detekt --offline`).
- `testDebugUnitTest`: 100% тестов всех модулей зелёные (`testDebugUnitTest --offline`).
- `compileReleaseKotlin`: успешная компиляция release-версии всех модулей проекта.
