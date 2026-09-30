# План реализации: Рефакторинг экранов Compose (Atomic UI) — Фаза 1: `:feature-x`

> **Спецификация:** [`docs/superpowers/specs/2026-09-30-compose-atomic-design-spec.md`](../specs/2026-09-30-compose-atomic-design-spec.md)  
> **Стандарт:** [`GEMINI.md`](file:///g:/xvideos2/GEMINI.md) (Архитектура экранов Compose)

---

## Цель фазы
Привести все экраны модуля `:feature-x` в строгое соответствие с правилом **«1 компонент = 1 файл с превью в `atom/` или `molecule/`»** с сохранением 100% работоспособности и прохождения тестов.

---

## Шаги реализации в модуле `:feature-x`

### Шаг 1: Экран каналов и моделей (`screens/channel/`)
- [ ] **1.1. Вынос молекул из `ScreenX_Channel.kt`**:
  - `molecule/ChannelCollapsingLayout.kt` — кастомный макет схлопывания шапки + `@Preview`.
  - `molecule/ChannelVideoItem.kt` — карточка видео канала + `@Preview`.
  - Очистить `ScreenX_Channel.kt`, оставив только класс экрана Voyager и `ChannelScreenContent`.
- [ ] **1.2. Декомпозиция шапки `ChannelHeader.kt`**:
  - `molecule/ChannelHeaderBanner.kt` — баннер с градиентом и кнопкой «Назад» + `@Preview`.
  - `molecule/ChannelHeaderProfileRow.kt` — строка с аватаром, именем и кнопкой подписки + `@Preview`.
  - `molecule/ChannelHeaderStatsRow.kt` — плашка статистики (видео, просмотры, подписчики) + `@Preview`.
  - `molecule/ChannelHeaderInfoCard.kt` — разворачиваемая карточка «О себе», рангов и возраста + `@Preview`.
  - `molecule/ChannelRankingsSection.kt` — секция категорий рейтингов + `@Preview`.
  - `atom/CollaboratorChip.kt` — чип соавтора/студии + `@Preview`.
  - `atom/CountryFlagTag.kt` — бейдж флага страны + `@Preview`.
  - `atom/ChannelRankItemView.kt` — строка отдельного ранга + `@Preview`.
  - `atom/ChannelRankGroupView.kt` — группа рангов по регионам + `@Preview`.
- [ ] **1.3. Декомпозиция панели фильтрации `ChannelModelFilterBar.kt`**:
  - `atom/ChannelModelTriggerButton.kt` + `@Preview`.
  - `atom/ChannelModelDropdownItem.kt` + `@Preview`.
  - `molecule/ChannelModelDropdownMenu.kt` + `@Preview`.
- [ ] **1.4. Проверка и тесты**:
  - `./gradlew :feature-x:detekt :feature-x:testDebugUnitTest`.

---

### Шаг 2: Экран поиска (`screens/search/`)
- [ ] **2.1. Вынос молекул и атомов из `ScreenXSearchTab.kt`**:
  - `molecule/SearchTopBar.kt` — строка ввода с кнопками очистки, поиска и возврата + `@Preview`.
  - `molecule/SearchHistoryView.kt` — список недавних запросов + `@Preview`.
  - `atom/SearchHistoryItem.kt` — элемент истории поиска + `@Preview`.
  - `molecule/SearchSuggestionsView.kt` — контейнер живых подсказок + `@Preview`.
  - `atom/KeywordSuggestionItem.kt` — подсказка фразы + `@Preview`.
  - `atom/ModelSuggestionItem.kt` — карточка найденной модели + `@Preview`.
  - `atom/ChannelSuggestionItem.kt` — карточка найденного канала + `@Preview`.
  - `atom/SectionHeader.kt` — заголовок секции подсказок + `@Preview`.
  - `molecule/SearchResultsView.kt` — отображение выдачи видео + `@Preview`.
  - Очистить `ScreenXSearchTab.kt` до связующего `X_SearchContent`.
- [ ] **2.2. Проверка и тесты**:
  - `./gradlew :feature-x:detekt :feature-x:testDebugUnitTest`.

---

### Шаг 3: Экран каталога актрис (`screens/actresses/`)
- [ ] **3.1. Декомпозиция `ScreenX_ActressesIndex.kt` и `ActressesFilterBar.kt`**:
  - `molecule/ActressesIndexTopBar.kt` + `@Preview`.
  - `molecule/ActressesIndexGrid.kt` + `@Preview`.
  - `atom/DropdownOptionRow.kt` + `@Preview`.
  - `atom/DropdownOptionsSearchField.kt` + `@Preview`.
  - Очистить `ScreenX_ActressesIndex.kt`.
- [ ] **3.2. Проверка и тесты**:
  - `./gradlew :feature-x:detekt :feature-x:testDebugUnitTest`.

---

### Шаг 4: Экран видеоплеера (`screens/videoplayer/`)
- [ ] **4.1. Декомпозиция `ScreenX_VideoPlayer.kt` и вспомогательных файлов**:
  - Выделение элементов управления оверлея, таймлайна и плашек в `atom/` и `molecule/`.
  - Приведение `ComposeTags.kt`, `X_PlayerBottomBar.kt` к принципу «1 компонент = 1 файл».
- [ ] **4.2. Проверка и тесты**:
  - `./gradlew :feature-x:detekt :feature-x:testDebugUnitTest`.

---

### Шаг 5: Дашборды, подписки, история и избранное
- [ ] **5.1. Рефакторинг дашбордов (`screens/dashboards/`)**:
  - Декомпозиция `ScreenXDashBoards.kt` и `DashboardsPaginatedListScreen.kt`.
- [ ] **5.2. Рефакторинг подписок (`screens/subscriptions/`)**:
  - `ScreenXSubscriptionsTab.kt` и `DialogXSubscriptionDelete.kt`.
- [ ] **5.3. Рефакторинг истории и избранного**:
  - `ScreenXHistory.kt`, `ScreenFavorites.kt`.
- [ ] **5.4. Финальный прогон Фазы 1**:
  - `./gradlew :feature-x:detekt :feature-x:testDebugUnitTest :app:testDebugUnitTest`.

---

## Правила проверки (Hard Gates)
1. Ни в одном файле `Screen...kt` не должно оставаться приватных `@Composable` функций.
2. Имя каждого `.kt` файла в `atom/` и `molecule/` строго равно имени `@Composable` функции.
3. В каждом файле есть `@Preview private fun ...Preview()`.
4. `./gradlew :feature-x:detekt` даёт 0 замечаний.
5. 100% прохождение всех unit-тестов.
