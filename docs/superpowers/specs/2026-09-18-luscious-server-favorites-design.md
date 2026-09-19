# Luscious Server Favorites & Subscriptions (L Savable Tab) Design

## 1. Understanding Summary
- **Что создаётся**: Две новые страницы-заготовки в разделе `Savable` (`L_SavedTab`) модуля `feature-l`:
  1. Экран подписанных альбомов пользователя с сервера Luscious.
  2. Экран лайкнутых картинок пользователя с сервера Luscious.
- **Интеграция в навигацию**: Расширение нижнего бара `TabRow` экрана `L_SavedTab` с 3 до 5 вкладок:
  - `0`: Локальные лайки (`Icons.Outlined.Save`)
  - `1`: Локальные альбомы (`Icons.Outlined.Folder`)
  - `2`: Коллекции (`Icons.Outlined.Apps`)
  - `3`: Подписки на альбомы с сервера (`Icons.Outlined.Subscriptions`)
  - `4`: Серверные лайки картинок (`Icons.Outlined.FavoriteBorder`)
- **UI и компоненты**: Полноценный Compose UI в едином стиле L-модуля:
  - Альбомы: `LazyVerticalGrid` (2 колонки) + карточки `AlbumListItem` с переходом в `ScreenLAlbum`.
  - Картинки: сетка изображений через `L_LazyRowPictureDetails` с тегом `"l_server_likes"`.
- **Архитектурный каркас**:
  - Voyager `Screen` и Hilt `ScreenModel` для каждого экрана.
  - Выделенный репозиторий-контракт `LusciousServerFavoritesRepository` с Hilt заглушкой `LusciousServerFavoritesRepositoryStub`, готовой к наполнению GraphQL-запросами.
  - Поддержка пагинации (`page`, `hasMore`, `loadNextPage`) и Pull-to-refresh.
- **Ограничения и non-goals**:
  - Фактические GraphQL-запросы будут добавлены пользователем позже; сейчас реализуется чистый контракт и UI-каркас.
  - Существующие локальные вкладки (0, 1, 2) сохраняют прежнее поведение.

## 2. Assumptions & Non-Functional Requirements
- **Модели**: Переиспользуются существующие доменные модели `AlbumDetails` и `PicsDetails`.
- **Авторизация**: Запросы на бэкенд будут требовать сессию `LSession`, экраны готовы к состояниям «пусто / ошибка / загрузка».
- **Производительность**: Использование Compose LazyGrid с переиспользованием Coil ImageLoader, без лишней нагрузки на память.
- **Навигация**: Сохранение состояния активной вкладки через `LNavigationState`.

## 3. Decision Log
| Решение | Выбранный вариант | Рассмотренные альтернативы | Обоснование |
|---|---|---|---|
| **Расположение в навигации** | 5 вкладок в нижнем `TabRow` раздела `L_SavedTab` | Верхний сегмент / объединённая вкладка | Консистентность с существующей структурой раздела Savable |
| **Уровень детализации UI** | Полноценный UI (сетка альбомов и сетка картинок) | Текстовые заглушки без верстки | Позволяет протестировать адаптивность, вырезы экрана и скролл |
| **Иконки табов** | `Subscriptions` (альбомы), `FavoriteBorder` (лайки) | `Cloud`, `Favorite` | Чёткая семантика подписок и облачного избранного |
| **Пагинация и свайп** | Заложить контракт пагинации и pull-to-refresh | Однократный запрос без пагинации | Списки на сервере Luscious пагинируются, важно иметь готовую структуру |
| **Архитектурный подход** | Отдельные экраны, ScreenModel и контракт репозитория | Заглушки внутри ScreenModel или в `ScreenSaved.kt` | Чистая архитектура, лёгкое подключение реальных GraphQL-запросов |

## 4. Final Design

### 4.1. Data Layer Contracts (`feature-l/src/main/java/com/client/xvideos/l/repository/`)
- `LusciousServerFavoritesRepository.kt`:
  ```kotlin
  interface LusciousServerFavoritesRepository {
      suspend fun getSubscribedAlbums(page: Int): Result<List<AlbumDetails>>
      suspend fun getServerLikedPictures(page: Int): Result<List<PicsDetails>>
  }
  ```
- `LusciousServerFavoritesRepositoryImpl.kt`:
  ```kotlin
  @Singleton
  class LusciousServerFavoritesRepositoryImpl @Inject constructor(...) : LusciousServerFavoritesRepository {
      ...
  }
  ```
- Регистрация в DI (`LusciousModule.kt` или отдельный модуль).

### 4.2. UI Components & ScreenModels
- **Подписанные альбомы**:
  - Пакет: `com.client.xvideos.l.ui.screens.explorer.tab.saved.subscribedAlbums`
  - `L_ScreenSubscribedAlbumsTab.kt`: Voyager `Screen`, отображающий `LazyVerticalGrid(columns = GridCells.Fixed(2))` с элементами `AlbumListItem` и переходом в `ScreenLAlbum`.
  - `ScreenLSubscribedAlbumsSM.kt`: Hilt `ScreenModel` с управлением пагинацией и состояниями `isLoading`, `isRefreshing`, `hasMore`, `items`.
- **Серверные лайки**:
  - Пакет: `com.client.xvideos.l.ui.screens.explorer.tab.saved.serverLikes`
  - `L_ScreenServerLikesTab.kt`: Voyager `Screen`, использующий `L_LazyRowPictureDetails` с тегом `"l_server_likes"`.
  - `ScreenLServerLikesSM.kt`: Hilt `ScreenModel` со списком `PicsDetails` и связкой с хостом `LazyRowPictureDetailsHost`.

### 4.3. Navigation Integration (`ScreenSaved.kt`)
- Расширение `SAVED_TAB_ICONS`:
  - `Icons.Outlined.Subscriptions` (индекс 3)
  - `Icons.Outlined.FavoriteBorder` (индекс 4)
- Ветка `when (screenType)`:
  - `3 -> L_ScreenSubscribedAlbumsTab.Content()`
  - `4 -> L_ScreenServerLikesTab.Content()`
