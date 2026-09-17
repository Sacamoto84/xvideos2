# Код-ревью xvideos — проход 81

> **Срез:** `4844535` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `4844535`. Изменено 6 файлов в модулях `:feature-l` и `:feature-r`.

Линзы:
- `T` / Инкапсуляция мутабельного реактивного состояния `AlbumInfo` в закрытые `MutableStateFlow` с предоставлением публичных read-only `StateFlow` ([AlbumInfo.kt](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumInfo.kt)).
- `C` / Устранение состояния гонки публикации метаданных при восстановлении альбома из локального кэша `restoreBundleIfFresh`: наполнение `albumPicsDetails` гарантированно завершается до переключения `_albumInfo` ([AlbumInfo.kt](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumInfo.kt)).
- `UI` / Фиксация ключа `host.state` в `remember(host.state) { derivedStateOf { ... } }` для кнопок быстрого скролла ([L_LazyRowPictureDetails.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/element/lazyRowPictureDetails/L_LazyRowPictureDetails.kt)).
- `UI` / Повышение стабильности Compose-дерева через аннотацию `@Immutable` для сетевых моделей поиска, ниш Red и сохранённых пресетов фильтров Luscious ([SearchCreatorsResponse.kt](../feature-r/src/main/java/com/client/xvideos/r/model/search/SearchCreatorsResponse.kt), [SearchItemNichesResponse.kt](../feature-r/src/main/java/com/client/xvideos/r/model/search/SearchItemNichesResponse.kt), [SearchItemTagsResponce.kt](../feature-r/src/main/java/com/client/xvideos/r/model/search/SearchItemTagsResponce.kt), [NichesResponse.kt](../feature-r/src/main/java/com/client/xvideos/r/model/NichesResponse.kt), [SavedAlbumFilter.kt](../feature-l/src/main/java/com/client/xvideos/l/model/SavedAlbumFilter.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### T44 — Публичные изменяемые MutableStateFlow в AlbumInfo. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/net/AlbumInfo.kt:33-44](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumInfo.kt#L33)

В классе `AlbumInfo` поля `albumInfo`, `loadError`, `isLoading` и `isRefreshing` были объявлены как публичные `MutableStateFlow`. Это нарушало инкапсуляцию, позволяя внешним потребителям и Composable-компонентам напрямую мутировать внутреннее состояние сетевой загрузки альбома в обход методов `loadAlbum`, `retry` и `refresh`.

**Исправление:**
- Поля переведены в приватные `_albumInfo`, `_loadError`, `_isLoading`, `_isRefreshing` с предоставлением публичных неизменяемых `StateFlow` через `.asStateFlow()`.

---

### C134 — Состояние гонки при восстановлении кэша альбома в AlbumInfo.restoreBundleIfFresh. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/net/AlbumInfo.kt:139](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumInfo.kt#L139)

При попадании в локальный дисковый кэш бандла альбома метод `restoreBundleIfFresh` выставлял `_albumInfo.value = bundle.album` до вызова `albumPicsDetails.restoreFromBundleCache(...)`. В результате подписчики реактивного потока `albumInfo` сразу получали ненулевые метаданные альбома, в то время как список страниц `albumPicsDetails.pics` ещё оставался пустым (что приводило к рассинхронизации в UI и падению тестов на асинхронном ожидании).

**Исправление:**
- Вызов `albumPicsDetails.restoreFromBundleCache` переставлен перед публикацией `_albumInfo.value = bundle.album`, обеспечивая атомарную доступность полного снимка данных для подписчиков.

---

### UI97 — Отсутствие ключа зависимостей в remember { derivedStateOf } для кнопок скролла. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/ui/element/lazyRowPictureDetails/L_LazyRowPictureDetails.kt:126-129](../feature-l/src/main/java/com/client/xvideos/l/ui/element/lazyRowPictureDetails/L_LazyRowPictureDetails.kt#L126)

Флаги показа кнопок быстрой прокрутки `showScrollToTop` и `showScrollToBottom` вычислялись через `remember { derivedStateOf { host.state... } }` без передачи ключа. При изменении ссылки на состояние сетки `host.state` (или смене хоста) `derivedStateOf` продолжал бы отслеживать устаревший экземпляр состояния.

**Исправление:**
- Добавлен ключ зависимости: `remember(host.state) { derivedStateOf { ... } }`.

---

### UI98 — Модели поиска, ниш Red и пресетов фильтров Luscious без маркера стабильности Compose. Низкая.

[feature-r/src/main/java/com/client/xvideos/r/model/search/SearchCreatorsResponse.kt:6-30](../feature-r/src/main/java/com/client/xvideos/r/model/search/SearchCreatorsResponse.kt#L6)
[feature-r/src/main/java/com/client/xvideos/r/model/search/SearchItemNichesResponse.kt:6-35](../feature-r/src/main/java/com/client/xvideos/r/model/search/SearchItemNichesResponse.kt#L6)
[feature-r/src/main/java/com/client/xvideos/r/model/search/SearchItemTagsResponce.kt:9](../feature-r/src/main/java/com/client/xvideos/r/model/search/SearchItemTagsResponce.kt#L9)
[feature-r/src/main/java/com/client/xvideos/r/model/NichesResponse.kt:6-48](../feature-r/src/main/java/com/client/xvideos/r/model/NichesResponse.kt#L6)
[feature-l/src/main/java/com/client/xvideos/l/model/SavedAlbumFilter.kt:6](../feature-l/src/main/java/com/client/xvideos/l/model/SavedAlbumFilter.kt#L6)

Модели данных `SearchCreatorsResponse`, `SearchItemCreatorsResponse`, `SearchNichesShortResponse`, `SearchItemNichesResponse`, `SearchItemTagsResponse`, `NichesResponse`, `Niche`, `Preview` и `SavedAlbumFilter` активно используются в списках и UI-компонентах поиска и фильтрации. Без явной аннотации `@Immutable` компилятор Compose мог считать их нестабильными при наличии параметров коллекций (`List`), провоцируя избыточные рекомпозиции элементов списков.

**Исправление:**
- На все перечисленные классы навешена аннотация `androidx.compose.runtime.Immutable`.

---

## Статус

| Находка | Класс | Статус | Детали |
| --- | --- | --- | --- |
| T44 | многопоточность | закрыт | Инкапсуляция изменяемых Flow в `AlbumInfo` |
| C134 | корректность | закрыт | Устранение гонки публикации в `AlbumInfo.restoreBundleIfFresh` |
| UI97 | UI / Compose | закрыт | Ключ `host.state` в `remember(host.state)` для скролл-кнопок |
| UI98 | UI / Compose | закрыт | `@Immutable` для моделей поиска R, ниш R и сохранённых фильтров L |

## Проверка

```
.\gradlew.bat app:detekt core:detekt feature-l:detekt feature-r:detekt feature-x:detekt
BUILD SUCCESSFUL

.\gradlew.bat testDebugUnitTest
BUILD SUCCESSFUL in 9s (140 actionable tasks)

.\gradlew.bat compileReleaseKotlin
BUILD SUCCESSFUL in 13s
```
