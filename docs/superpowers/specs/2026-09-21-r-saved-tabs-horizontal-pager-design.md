# Дизайн: Горизонтальный пейджер для вкладок раздела R «Сохраненное» (R_ScreenSavedTab)

## 1. Сводка понимания (Understanding Summary)
- **Цель**: Заменить статичный селектор `when (vm.screenType)` на `HorizontalPager` в экране `R_ScreenSavedTab` ([`ScreenSaved.kt`](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/saved/ScreenSaved.kt)), предоставив пользователям возможность переключаться между вкладками свайпом влево/вправо.
- **Целевая аудитория**: Пользователи раздела R, работающие с сохраненным контентом (лайки, создатели, ниши, загрузки, коллекции, подписки).
- **Ключевые ограничения**:
  - Плавная анимация прокрутки при тапе на иконку в нижней панели (`animateScrollToPage`).
  - Сохранение переключения колонок сетки при повторном клике на активный таб 0 (Лайки) и 4 (Коллекции).
  - Отключение горизонтального свайпа пейджера (`userScrollEnabled = false`), пока открыта конкретная коллекция на вкладке 4, во избежание конфликта жестов.
  - Нажатие кнопки «Назад» при `currentPage != 0` возвращает на главную вкладку (0 — Лайки).
- **Явные не-цели**: Изменение внутренней разметки или бизнес-логики самих экранов вкладок.

## 2. Предположения (Assumptions)
- 6 страниц пейджера соответствуют порядку вкладок в `TabRow`:
  - 0: `R_Screen_Saved_LikesTab`
  - 1: `R_Screen_CreatorsTab`
  - 2: `SavedNichesTab`
  - 3: `R_Screen_Saved_DownloadTab`
  - 4: `R_Screen_CollectionTab`
  - 5: `R_Screen_Saved_SubscriptionsTab`
- Значение `beyondViewportPageCount = 0` обеспечивает оптимальное использование памяти и ресурсов сети, подгружая контент страниц по мере свайпа.
- `RNavigationState.savedTab` продолжает хранить текущий индекс выбранной вкладки для сохранения состояния между экранами.

## 3. Журнал решений (Decision Log)
1. **Переход по клику на таб**: Использовать `animateScrollToPage(page)` при тапе по иконкам `TabRow`.
2. **Изоляция открытой коллекции**: Блокировать свайп пейджера (`userScrollEnabled = false`), пока открыта конкретная коллекция (`selectedCollection != null`).
3. **Архитектурный подход**: Подход 1 — идиоматичный `HorizontalPager` с `rememberPagerState` и синхронизацией с `RNavigationState`.

## 4. Архитектура и компоненты (Final Design)

### 4.1. Обновление `R_SavedTabSM`
- Внедрить `SavedRed` в конструктор `R_SavedTabSM`:
  ```kotlin
  @Stable
  class R_SavedTabSM @Inject constructor(
      private val navigationState: RNavigationState,
      val savedRed: SavedRed
  ) : ScreenModel {
      var screenType: Int
          get() = navigationState.savedTab
          set(value) {
              navigationState.savedTab = value
          }
  }
  ```

### 4.2. Обновление `R_ScreenSavedTab.Content()` в `ScreenSaved.kt`
- Создание `pagerState` и `scope`:
  ```kotlin
  val scope = rememberCoroutineScope()
  val pagerState = rememberPagerState(
      initialPage = vm.screenType.coerceIn(0, 5),
      pageCount = { 6 }
  )
  ```
- Синхронизация текущей страницы при свайпе:
  ```kotlin
  LaunchedEffect(pagerState.currentPage) {
      vm.screenType = pagerState.currentPage
  }
  ```
- Проверка открытой коллекции:
  ```kotlin
  val selectedCollection by vm.savedRed.collections.selectedCollection.collectAsStateWithLifecycle()
  val isInsideCollection = pagerState.currentPage == 4 && selectedCollection != null
  ```
- Обработчик клика в нижней панели:
  ```kotlin
  val onTabChange: (Int) -> Unit = remember(pagerState, scope) {
      { tab ->
          if (tab == pagerState.currentPage) {
              when (tab) {
                  0 -> ColumnSelect_AddRColumn(Settings.r_likesTab_column_current_count)
                  4 -> ColumnSelect_AddRColumn(Settings.r_collectionTab_column_current_count)
              }
          } else {
              scope.launch { pagerState.animateScrollToPage(tab) }
          }
      }
  }
  ```
- Обработка кнопки «Назад»:
  ```kotlin
  BackHandler(enabled = pagerState.currentPage != 0) {
      scope.launch { pagerState.animateScrollToPage(0) }
  }
  ```
- Разметка `HorizontalPager`:
  ```kotlin
  HorizontalPager(
      state = pagerState,
      userScrollEnabled = !isInsideCollection,
      beyondViewportPageCount = 0,
      modifier = Modifier.fillMaxSize()
  ) { page ->
      when (page) {
          0 -> R_Screen_Saved_LikesTab.Content()
          1 -> R_Screen_CreatorsTab.Content()
          2 -> SavedNichesTab.Content()
          3 -> R_Screen_Saved_DownloadTab.Content()
          4 -> R_Screen_CollectionTab.Content()
          5 -> R_Screen_Saved_SubscriptionsTab.Content()
      }
  }
  ```

## 5. Проверка и валидация
- Сборка `:feature-r:compileDebugKotlin`.
- Проверка плавного свайпа между всеми 6 экранами вкладок.
- Проверка анимации и индикатора в `TabRow`.
- Проверка переключения числа колонок для табов 0 и 4.
- Проверка отключения свайпа внутри открытой коллекции и возврата назад на вкладку 0.
