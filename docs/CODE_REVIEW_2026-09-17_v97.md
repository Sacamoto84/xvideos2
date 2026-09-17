# Код-ревью: Итерация 118 (2026-09-17)

**Фокус ревью:** `:feature-l`, `:feature-r`, `:feature-x` (устранение гонок сброса флагов загрузки корутин, защита файловых и сетевых операций от пустых входных данных, Path Traversal guard, очистка неиспользуемого состояния и модульные тесты).

---

## 1. Контекст и цели
1. Устранить гонку сброса флага `isLoading` в блоках `finally` отменяемых корутин в [L_ScreenAlbumSearch.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumSearch/L_ScreenAlbumSearch.kt), [ScreenX_VideoPlayerSM.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt) и [ScreenX_VideoPlayerFullScreenSM.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt).
2. Защитить дисковый поиск элементов коллекций `lFindCollectionItemFolder` в [LCollectionFs.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/LCollectionFs.kt) от холостого обхода файловой системы при пустых или пробельных идентификаторах.
3. Добавить Path Traversal guard для параметра `coverFolderName` в `lResolveCollectionPreviewUrl` [LCollectionFs.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/LCollectionFs.kt).
4. Устранить рассинхрон передачи `nicheName` в `LazyRow123Host` в [ScreenNicheSM.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/niche/ScreenNicheSM.kt) и заблокировать ложное сохранение пустой ниши в [ScreenNiche.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/niche/ScreenNiche.kt).
5. Добавить защиту от холостых сетевых вызовов в [ItemNailsPagingSource.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/pagin/ItemNailsPagingSource.kt) и [ItemTopPagingSource.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/pagin/ItemTopPagingSource.kt).
6. Провести очистку устаревшего мертвого кода (`_list`, `loadNextPage`, `shareGifs`) в [ScreenRedProfileSM.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/profile/ScreenRedProfileSM.kt).
7. Разработать модульные тесты [LFindCollectionItemFolderTest.kt](file:///g:/xvideos2/feature-l/src/test/java/com/client/xvideos/l/featured/saved/LFindCollectionItemFolderTest.kt) и [ItemNailsPagingSourceTest.kt](file:///g:/xvideos2/feature-r/src/test/java/com/client/xvideos/r/common/pagin/ItemNailsPagingSourceTest.kt).

---

## 2. Выявленные замечания и исправления

### [C27] Гонка сброса флага `isLoading` в `ScreenLAlbumSearchSM`
- **Проблема:** При повторном вызове `ScreenLAlbumSearchSM.search()` старая задача отменялась, а новая запускалась и выставляла `isLoading.value = true`. Когда отменённая задача переходила в `finally { isLoading.value = false }`, она сбрасывала индикатор загрузки для всё ещё выполняющегося запроса.
- **Решение:** В [L_ScreenAlbumSearch.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumSearch/L_ScreenAlbumSearch.kt) сброс `isLoading.value = false` в `finally` выполняется только если задача совпадает с текущей активной (`searchJob === coroutineContext[Job]`). Добавлен явный сброс в `onDispose()`.
- **Статус:** Исправлено.

### [S61] Сканирование дискового корня по пустому списку в `lFindCollectionItemFolder`
- **Проблема:** Если в `lFindCollectionItemFolder(root: File, identifiers: List<String>)` передавался пустой список или список из пробелов, множество `normalizedIdentifiers` становилось пустым, после чего происходило полное чтение каталога `root.listFiles()` и парсинг всех файлов `metadata.json` на диске.
- **Решение:** В [LCollectionFs.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/LCollectionFs.kt) добавлен ранний выход:
  ```kotlin
  if (normalizedIdentifiers.isEmpty()) return null
  ```
- **Статус:** Исправлено.

### [S62] Path Traversal guard для `coverFolderName` в `lResolveCollectionPreviewUrl`
- **Проблема:** Чтение имени папки кастомной обложки из `collection.json` выполнялось без проверки безопасности пути, что теоретически допускало выход за пределы директории коллекции при повреждении конфигурации.
- **Решение:** В [LCollectionFs.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/featured/saved/LCollectionFs.kt) добавлена проверка `!isUnsafeItemName(it) && lIsInside(collectionFolder, it)`.
- **Статус:** Исправлено.

### [C28] Рассинхрон санитизации `nicheName` в `ScreenNicheSM`
- **Проблема:** Имя ниши санитизировалось в `init`, но в `LazyRow123Host` передавалось сырое несанитизированное значение `nicheName`, попадая в `feedKey` и пагинатор ленты.
- **Решение:** В [ScreenNicheSM.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/niche/ScreenNicheSM.kt) введено единое санитизированное свойство `val cleanNicheName = nicheName.trim()`, используемое как для хоста, так и для сетевых запросов.
- **Статус:** Исправлено.

### [UI10] Защита от сохранения ниши с пустым идентификатором в `ScreenNiche`
- **Проблема:** До завершения первичной загрузки метаданных `vm.niche.id` равен `""`. Клик по кнопке подписки приводил к попытке вставки пустого имени в `FileDB` и ошибке валидации `isUnsafeItemName`.
- **Решение:** В [ScreenNiche.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/niche/ScreenNiche.kt) в обработчик `onFollowClick` добавлен защитный guard:
  ```kotlin
  if (nicheInfo.id.isBlank()) return@remember
  ```
- **Статус:** Исправлено.

### [C29] Холостые сетевые вызовы в `ItemNailsPagingSource` и `ItemTopPagingSource`
- **Проблема:**
  - В `ItemNailsPagingSource` при пустом `nichesName` производился сетевой GET-запрос к эндпоинту `/v2/niches//gifs...`, возвращавший HTTP 404.
  - В `ItemTopPagingSource` проверка `searchText.isNotEmpty()` пропускала строки из одних пробелов в сетевой поиск RedGifs.
- **Решение:**
  - В [ItemNailsPagingSource.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/pagin/ItemNailsPagingSource.kt) добавлен возврат `LoadResult.Page(emptyList(), null, null)` при `cleanNichesName.isBlank()`.
  - В [ItemTopPagingSource.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/pagin/ItemTopPagingSource.kt) введено использование `val query = searchText.trim()` и проверка `query.isNotBlank()`.
- **Статус:** Исправлено.

### [D22] Очистка мертвого кода и привязка `isLoading` в `ScreenRedProfileSM`
- **Проблема:** В `ScreenRedProfileSM` оставался неиспользуемый код от предыдущих версий экрана (`loadNextPage`, `_list`, `shareGifs`, `maxCreatorGifs`), а `isLoading` никогда не переходил в `true`.
- **Решение:** В [ScreenRedProfileSM.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/profile/ScreenRedProfileSM.kt) удалены неиспользуемые члены, флаг `isLoading` корректно взводится на время запроса метаданных автора в `init` и сбрасывается в `finally`.
- **Статус:** Исправлено.

### [C30] Гонка сброса `isLoading` и отмена `loadJob` в плеерах раздела X
- **Проблема:** В [ScreenX_VideoPlayerSM.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt) и [ScreenX_VideoPlayerFullScreenSM.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt) при отмене `loadJob` блок `finally` безусловно сбрасывал `isLoading = false`, а при пустом URL производились холостые вызовы к кэшу и парсеру HTML.
- **Решение:**
  - Добавлена проверка `if (url.isBlank()) { isLoading = false; isError = true; return }`.
  - В `finally` проверка `if (loadJob === coroutineContext[Job]) isLoading = false`.
  - В `onDispose()` добавлена отмена `loadJob?.cancel()` и сброс флага.
- **Статус:** Исправлено.

### [T64] Модульные тесты для `lFindCollectionItemFolder` и `ItemNailsPagingSource`
- **Проблема:** Новые краевые проверки и алгоритмы поиска не имели изолированных тестов.
- **Решение:**
  - Создан [LFindCollectionItemFolderTest.kt](file:///g:/xvideos2/feature-l/src/test/java/com/client/xvideos/l/featured/saved/LFindCollectionItemFolderTest.kt), проверяющий отсечение пустых и пробельных идентификаторов, защиту от путей вне корня и поиск по локальным путям и URL.
  - Создан [ItemNailsPagingSourceTest.kt](file:///g:/xvideos2/feature-r/src/test/java/com/client/xvideos/r/common/pagin/ItemNailsPagingSourceTest.kt), проверяющий немедленный возврат пустой страницы при пустом или пробельном имени ниши без сетевых обращений.
- **Статус:** Исправлено.

---

## 3. Верификация
- Модульные тесты `:feature-l:testDebugUnitTest`, `:feature-r:testDebugUnitTest`, `:feature-x:testDebugUnitTest`: **Passed** (100%).
- Общепроектный набор тестов `testDebugUnitTest`: **Passed** (140 actionable tasks, 100% success).
- Статический анализ `detekt`: **0 issues**.
- Релизная компиляция `compileReleaseKotlin`: **BUILD SUCCESSFUL in 9s**.
