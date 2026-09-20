# Архитектурный дизайн навигации и изоляции данных коллекций (L Collections)

## 1. Understanding Summary

- **Что создаётся:** Новая архитектура навигации и изоляции данных для сохранённых коллекций в разделе L (`feature-l`), устраняющая костыльный вложенный `Navigator` и общее разделяемое состояние картинок.
- **Зачем создаётся:** Устранить давний баг «промигивания» элементов ранее открытой коллекции при входе в другую коллекцию из-за переиспользования общего `listUrl` в синглтоне `SavedL_Collection`, а также перейти к легковесной и надежной модели навигации.
- **Для кого:** Для пользователей приложения, взаимодействующих со своими коллекциями в разделе L.
- **Ключевые требования и ограничения:**
  - Просмотр коллекции остаётся внутри рабочей области таба (нижние панели навигации уровня 0 и уровня 1 остаются видимыми).
  - Навигация между сеткой коллекций и содержимым коллекции реализуется через декларативный `AnimatedContent` (без динамического создания/уничтожения инстансов `Navigator`).
  - Данные открытых коллекций изолируются и кэшируются в памяти на время сессии (мгновенный повторный вход без дисковых задержек).
  - Сохраняется вся сопутствующая функциональность: поиск по коллекции, удаление дубликатов, P2P-шеринг, переименование, удаление, установка обложек, добавление элементов.
- **Явные не-цели (Explicit Non-Goals):**
  - Изменение дисковой структуры хранения файлов или метаданных (`AppPath.l_collection`).
  - Модификация сетевого протокола или логики P2P-передачи коллекций.
  - Изменение внешнего вида карточек в сетке коллекций.

---

## 2. Assumptions & Non-Functional Requirements

- **Производительность:**
  - Плавные 60/120 fps анимации перехода (горизонтальный слайд + fade) без зависаний.
  - Первичное чтение с диска выполняется строго асинхронно на пуле `Dispatchers.IO`.
  - Открытие закэшированной коллекции происходит за 0 мс (мгновенный отклик).
- **Изоляция данных и надежность:**
  - Начальное состояние вновь открываемой коллекции — нейтральный лоадер (`null`), но ни в коем случае не чужие данные из другой коллекции.
  - Точечная инвалидация кэша сессии при любых мутациях (добавление, удаление файлов, дедупликация, переименование).
- **Иерархия жестов и системной кнопки «Назад» (BackHandler):**
  - Приоритет 1: закрыть диалог дубликатов (если открыт).
  - Приоритет 2: сбросить строку поиска (если не пустая).
  - Приоритет 3: выйти из коллекции к сетке коллекций (`currentCollectionName = null`).
  - Приоритет 4: переключение табов родительского экрана.

---

## 3. Decision Log

| № | Решение | Рассмотренные альтернативы | Обоснование выбора |
|---|---|---|---|
| **1** | Отображение внутри области таба (нижние бары остаются видимыми) | Полноэкранный push на весь экран (как альбомы `ScreenLAlbum`) | Выбор пользователя: быстрый доступ к нижнему таб-бару при навигации по коллекциям |
| **2** | Декларативная Compose-навигация через `AnimatedContent` | Вложенный постоянный `Navigator` Voyager | Исключение оверхеда, устранение конфликтов навигаторов (`LocalNavigator` vs `LocalMainNavigator`) и чистый жизненный цикл |
| **3** | Сессионный кэш содержимого коллекций в памяти | Холодная загрузка с диска каждый раз | Мгновенный отклик при повторном переходе, отсутствие мерцания и лишних дисковых операций |
| **4** | UDF-архитектура потока данных в `SavedL_Collection` | Разделяемый `listUrl` или привязка к ScreenModelStore | Потокобезопасность (`ConcurrentHashMap`), полная изоляция данных коллекций, легкая тестируемость |

---

## 4. Final Design

### 4.1. Data Layer: Кэширование и изоляция в `SavedL_Collection.kt`

1. **Сессионный кэш:**
   ```kotlin
   private val collectionCache = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.flow.MutableStateFlow<List<PicsDetails>?>>()
   ```
2. **Получение данных коллекции (`getCollectionItems`):**
   ```kotlin
   fun getCollectionItems(collectionName: String): StateFlow<List<PicsDetails>?> {
       val safeName = CollectionName.normalizeOrNull(collectionName) ?: return MutableStateFlow(emptyList())
       return collectionCache.getOrPut(safeName) {
           MutableStateFlow<List<PicsDetails>?>(null).also { flow ->
               scope.launch(Dispatchers.IO) {
                   val items = try {
                       lReadCollectionItems(File(AppPath.l_collection, safeName))
                   } catch (e: Exception) {
                       emptyList()
                   }
                   flow.value = items
               }
           }
       }
   }
   ```
3. **Мутации данных:**
   - `addAll` / `remove` / `removeAll`: после завершения файловых операций на IO актуализируют `collectionCache[safeName]?.value`.
   - `removeDuplicateItems`: после очистки дубликатов перезапрашивает и обновляет `collectionCache[safeName]`.
   - `renameCollection`: переносит кэш `collectionCache.remove(safeOldName)?.let { collectionCache[trimmedNewName] = it }`.
   - `deleteCollection`: удаляет запись из `collectionCache.remove(safeName)`.

### 4.2. UI & Navigation Layer: `L_Screen_CollectionTab.kt`

1. **Устранение вложенного `Navigator`:**
   Вместо конструкции:
   ```kotlin
   if (selectedCollection == null) {
       L_SavedCollectionTabContent(...)
   } else {
       Navigator(ScreenCollectionName(selectedCollection))
   }
   ```
   используется декларативный `AnimatedContent`:
   ```kotlin
   AnimatedContent(
       targetState = selectedCollection,
       transitionSpec = {
           if (targetState != null) {
               (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it / 3 } + fadeOut())
           } else {
               (slideInHorizontally { -it / 3 } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
           }
       },
       label = "LCollectionNavigation"
   ) { collectionName ->
       if (collectionName == null) {
           L_SavedCollectionTabContent(
               collectionList = savedL.collection.collectionList,
               sortOrder = savedL.collection.sortOrder,
               gridState = vm.gridState,
               onSortOrderClick = { savedL.collection.applySortOrder(it) },
               onCollectionClick = { savedL.collection.currentCollectionName = it },
               onCollectionLongClick = { itemPendingAction = it },
               onCreateNewCollectionClick = { savedL.collection.visibleDialogCreateNew = true }
           )
       } else {
           L_CollectionNameContent(
               collectionName = collectionName,
               savedL = savedL,
               onExitCollection = { savedL.collection.currentCollectionName = null }
           )
       }
   }
   ```

2. **Обновление `L_CollectionNameContent` (`ScreenCollectionName.kt`):**
   - Компонент принимает `savedL` и `collectionName`.
   - Создаёт локальный экземпляр `LazyRowPictureDetailsHost(collectionName)`.
   - Подписывается на `savedL.collection.getCollectionItems(collectionName)`.
   - Обновляет `host.replaceFilteredPictures(...)` только данными запрошенной коллекции.
   - Реализует 3-уровневый `BackHandler`:
     1. Дубликаты
     2. Поисковый запрос
     3. `onExitCollection()` (сброс `currentCollectionName = null`).

---

## 5. Verification Plan

1. **Unit-тесты (`SavedL_CollectionTest`):**
   - Проверка изоляции: чтение коллекции B не возвращает элементы коллекции A.
   - Проверка кэширования: повторный запрос возвращает сохранённый `StateFlow` без повторного дискового чтения.
   - Проверка мутаций: добавление, удаление и переименование корректно обновляют сессионный кэш.
2. **Интеграционная верификация:**
   - `./gradlew testDebugUnitTest` — 100% прохождение тестов.
   - `./gradlew detekt` — 0 предупреждений и ошибок.
