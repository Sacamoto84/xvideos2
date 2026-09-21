# Дизайн: Модернизация экрана R_ScreenNiche и плавающие кнопки прокрутки

## 1. Сводка понимания (Understanding Summary)
- **Цель**: Привести экран ниши `R_ScreenNiche` (`feature-r`) к референсу экрана альбома `ScreenLAlbum` (`feature-l`), унифицировав цветовую схему и добавив плавающие кнопки быстрого скролла (FAB) «Вверх» и «Вниз» с плавной анимацией появления.
- **Целевая аудитория**: Пользователи раздела R, просматривающие ниши и медиа-ленты.
- **Ключевые ограничения**: Сохранение корректной работы Paging 3 и смещения на заголовочный контент (`contentBeforeList`) в `LazyRow123`.
- **Явные не-цели**: Изменение логики получения данных или структуры карточек элементов сетки.

## 2. Предположения (Assumptions)
- Исходный хардкод цветов `Color(0xFF0F0F0F)` и `Color(0xFF303030)` полностью заменяется на `Theme.background` (`0xFF262626`).
- Кнопка «Вверх» отображается при `firstVisibleItemIndex > 2`, клик скроллит к элементу 0 с виброоткликом.
- Кнопка «Вниз» отображается, если в списке > 4 элементов и нижний видимый элемент не достиг конца (`lastVisibleIndex < totalItems - 1`), клик скроллит к концу текущего списка.
- Устаревшая статичная кнопка `ButtonUpCircle` удаляется из `NicheBottomBar`.
- Неиспользуемый закомментированный тулбар и связанный с ним `exitAlwaysScrollBehavior` удаляются из `ScreenNiche.kt`.

## 3. Журнал решений (Decision Log)
1. **Расположение FAB**: Добавить кнопки в `LazyRow123` (по аналогии с `L_LazyRowPictureDetails`), чтобы поведение прокрутки было доступно на всех экранах R с этой сеткой.
2. **Удаление устаревшей кнопки**: Убрать `ButtonUpCircle` и параметр `onUpClick` из `NicheBottomBar`, чтобы избежать дублирования.
3. **Подход к интеграции**: Прямая интеграция в `LazyRow123` с параметром `showScrollButtons: Boolean = true` по умолчанию.

## 4. Архитектура и компоненты (Final Design)

### 4.1. Обновление цветов и очистка `ScreenNiche.kt`
- В `StatelessScreenNicheContent`:
  - `Scaffold`: `containerColor = Theme.background`.
  - Удаление `val exitAlwaysScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(...)` и модификатора `.nestedScroll(exitAlwaysScrollBehavior)`.
  - Удаление закомментированного кода `HorizontalFloatingToolbar`.
- В `ScreenNicheContent`:
  - Замена `Modifier.background(Color(0xFF303030))` на `Theme.background`.
  - Удаление `onUpClick`.
- В `NicheHeaderContent`:
  - Замена фона `Color(0xFF303030)` на `Theme.background`.
  - Удаление параметра `onUpClick`.
- В `NicheBottomBar.kt`:
  - Удаление компонента `ButtonUpCircle(44.dp, onUpClick)`.
  - Удаление параметра `onUpClick: () -> Unit`.

### 4.2. Интеграция плавающих кнопок в `LazyRow123.kt`
- В функцию `LazyRow123`:
  - Добавление параметра `showScrollButtons: Boolean = true`.
- Внутри корневого `Box` в `LazyRow123`:
  ```kotlin
  val showScrollToTop by remember(host.state) {
      derivedStateOf { host.state.firstVisibleItemIndex > 2 }
  }

  val showScrollToBottom by remember(host.state) {
      derivedStateOf {
          val layoutInfo = host.state.layoutInfo
          val totalItems = layoutInfo.totalItemsCount
          val visible = layoutInfo.visibleItemsInfo
          if (totalItems <= 4 || visible.isEmpty()) {
              false
          } else {
              val lastVisibleIndex = visible.maxOf { it.index }
              lastVisibleIndex < totalItems - 1
          }
      }
  }

  if (showScrollButtons) {
      Column(
          modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(16.dp),
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
          AnimatedVisibility(
              visible = showScrollToTop,
              enter = fadeIn() + scaleIn(),
              exit = fadeOut() + scaleOut()
          ) {
              FloatingActionButton(
                  onClick = {
                      haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                      scope.launch { host.state.scrollToItem(0) }
                  }
              ) {
                  Icon(
                      imageVector = Icons.Default.KeyboardArrowUp,
                      contentDescription = "Scroll to top"
                  )
              }
          }

          AnimatedVisibility(
              visible = showScrollToBottom,
              enter = fadeIn() + scaleIn(),
              exit = fadeOut() + scaleOut()
          ) {
              FloatingActionButton(
                  onClick = {
                      haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                      scope.launch {
                          val total = host.state.layoutInfo.totalItemsCount
                          if (total > 0) {
                              host.state.scrollToItem(total - 1)
                          }
                      }
                  }
              ) {
                  Icon(
                      imageVector = Icons.Default.KeyboardArrowDown,
                      contentDescription = "Scroll to bottom"
                  )
              }
          }
      }
  }
  ```

## 5. Проверка и валидация
- Сборка и компиляция модуля `feature-r`.
- Проверка предпросмотра превью `ScreenNicheContentPreview` и `NicheBottomBarPreview`.
- Проверка отображения экрана ниши: единый фон без швов, появление кнопок при скролле и корректное перемещение в начало и конец списка.
