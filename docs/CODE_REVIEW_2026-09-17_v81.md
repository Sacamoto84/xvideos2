# Код-ревью xvideos — проход 102

> **Срез:** `063f8cf` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `063f8cf`. Затронут модуль `:feature-r`. Удалена неиспользуемая DI-зависимость `connectivityObserver` из `ScreenRedExplorerSearchSM` с одновременным удалением правила `UnusedPrivateProperty` из Detekt baseline. Устранён паразитный сетевой запрос при инициализации экрана поиска авторов (`searchText` теперь пустой по умолчанию, а не `"Ana"`). В `ScreenRedProfile` списки тегов обёрнуты в `remember` для предотвращения переаллокаций списков при каждом кадре рекомпозиции. Очищены отладочные маркеры (`!!!`) в логировании поисковых подсказок, профилей и ниш. Алгоритм фильтрации и сортировки ниш сделан доступным для тестов (`internal`) и покрыт набором модульных тестов `NichesFilterAndSortTest`.

Линзы:
- `Q` / Удаление мёртвого параметра конструктора `connectivityObserver` в `ScreenRedExplorerSearchSM` и очистка Detekt baseline ([SearchTab.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/search/SearchTab.kt), [baseline.xml](../config/detekt/baseline.xml)).
- `UI` / Устранение паразитной загрузки при старте поиска и мемоизация списков тегов в профиле автора ([SearchTab.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/search/SearchTab.kt), [ScreenRedProfile.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/profile/ScreenRedProfile.kt)).
- `L` / Удаление отладочных маркеров `!!!` и восклицаний в логах подсистем поиска и профилей ([SearchTab.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/search/SearchTab.kt), [ScreenRedProfileSM.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/profile/ScreenRedProfileSM.kt), [ScreenNicheSM.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/niche/ScreenNicheSM.kt), [ScreenNiche.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/niche/ScreenNiche.kt), [R_SearchNiches.kt](../feature-r/src/main/java/com/client/xvideos/r/common/search/R_SearchNiches.kt), [R_SearchExplorer.kt](../feature-r/src/main/java/com/client/xvideos/r/common/search/R_SearchExplorer.kt)).
- `T` / Покрытие модульными тестами всех вариантов сортировки и фильтрации каталога ниш ([NichesFilterAndSortTest.kt](../feature-r/src/test/java/com/client/xvideos/r/ui/explorer/tab/niches/NichesFilterAndSortTest.kt), [R_ScreenNichesTab.kt](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/niches/R_ScreenNichesTab.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### Q15 — Неиспользуемый параметр конструктора connectivityObserver и подавление в baseline. Низкая.

[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/search/SearchTab.kt:157](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/search/SearchTab.kt#L157)
[config/detekt/baseline.xml](../config/detekt/baseline.xml)

В конструкторе `ScreenRedExplorerSearchSM` был объявлен параметр `connectivityObserver: ConnectivityObserver`, который нигде в классе не использовался. Из-за этого в `config/detekt/baseline.xml` годами сохранялось подавление `UnusedPrivateProperty:SearchTab.kt$ScreenRedExplorerSearchSM$connectivityObserver`.

**Исправление:**
- Удалён неиспользуемый параметр и неиспользуемый импорт.
- Удалена запись подавления из `config/detekt/baseline.xml`.

---

### UI127 — Паразитный сетевой запрос при открытии поиска и переаллокация списков тегов. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/search/SearchTab.kt:161](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/search/SearchTab.kt#L161)
[feature-r/src/main/java/com/client/xvideos/r/ui/profile/ScreenRedProfile.kt:66](../feature-r/src/main/java/com/client/xvideos/r/ui/profile/ScreenRedProfile.kt#L66)

При переходе на вкладку поиска авторов `SearchTab` поле `searchText` инициализировалось тестовой константой `"Ana"`, из-за чего немедленно стартовал сетевой поиск авторов по этой строке, расходуя трафик и перезаписывая результаты. В `ScreenRedProfile.Content` вызовы `tags.toList()` и `tagsSelect.toList()` производились безусловно на каждом цикле рекомпозиции экрана.

**Исправление:**
- `searchText` в `ScreenRedExplorerSearchSM` инициализируется пустой строкой `""`. При пустой строке список очищается без сетевого обращения.
- В `ScreenRedProfile` списки обёрнуты в `remember(tags)` и `remember(tagsSelect)`.

---

### L35 — Отладочные маркеры и завышенные уровни логов. Низкая.

[feature-r/src/main/java/com/client/xvideos/r/ui/profile/ScreenRedProfileSM.kt:149, 169](../feature-r/src/main/java/com/client/xvideos/r/ui/profile/ScreenRedProfileSM.kt#L149)
[feature-r/src/main/java/com/client/xvideos/r/ui/niche/ScreenNiche.kt:91](../feature-r/src/main/java/com/client/xvideos/r/ui/niche/ScreenNiche.kt#L91)
[feature-r/src/main/java/com/client/xvideos/r/ui/niche/ScreenNicheSM.kt:69](../feature-r/src/main/java/com/client/xvideos/r/ui/niche/ScreenNicheSM.kt#L69)
[feature-r/src/main/java/com/client/xvideos/r/common/search/R_SearchNiches.kt:56, 73](../feature-r/src/main/java/com/client/xvideos/r/common/search/R_SearchNiches.kt#L56)
[feature-r/src/main/java/com/client/xvideos/r/common/search/R_SearchExplorer.kt:61, 68](../feature-r/src/main/java/com/client/xvideos/r/common/search/R_SearchExplorer.kt#L61)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/search/SearchTab.kt:187](../feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/search/SearchTab.kt#L187)

В логах экранов ниш, профилей и поисковых подсказок оставались префиксы `!!!` и `!!!!!!`, а обычный клик по кнопке отслеживания ниши логировался на уровне `Timber.i`.

**Исправление:**
- Убраны артефакты `!!!` из сообщений логгера.
- Лог `onFollowClick` переведён на уровень `Timber.d`.

---

### T45 — Модульное тестирование фильтрации и сортировки ниш. Средняя.

[feature-r/src/test/java/com/client/xvideos/r/ui/explorer/tab/niches/NichesFilterAndSortTest.kt](../feature-r/src/test/java/com/client/xvideos/r/ui/explorer/tab/niches/NichesFilterAndSortTest.kt)

Функция `filterAndSortNiches` содержала сложную логику фильтрации по подстроке и упорядочивания по 6 различным критериям (`Order.NICHES_*`), но была объявлена `private` и не тестировалась напрямую.

**Исправление:**
- Видимость изменена на `internal`.
- Написан тестовый класс `NichesFilterAndSortTest` (6 тестов), проверяющий сохранение списка при пустом запросе, регистронезависимый поиск и сортировку по подписчикам, постам и именам в прямом и обратном направлениях.

---

## Сводка верификации

- `detekt`: 0 предупреждений и ошибок в `:feature-r`.
- `testDebugUnitTest`: 100% юнит-тестов `:feature-r` зелёные, включая `NichesFilterAndSortTest`.
