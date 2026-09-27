# План реализации экрана канала X (`ScreenX_Channel`)

**Цель:** Создать отдельный экран канала со стильной шапкой (баннер, аватарка, подписчики, просмотры, блок «Обо мне»), сортировкой («Свежие», «Новые», «Топ») и бесконечной лентой видеороликов через JSON API, а также связать его с кликом по каналу в плеере (`TagItem.Channel`).

---

## Шаг 1: Модели данных (`feature-x`)
- [ ] Создать [`ChannelModel.kt`](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/model/ChannelModel.kt):
  - `ChannelHeaderModel`: метаданные профиля канала (slug, name, bannerUrl, avatarUrl, subscribers, totalViews, aboutMe, videoCount).
  - `ChannelSortOrder`: enum с вариантами `BEST("best", "Свежие")`, `NEW("new", "Новые")`, `RATING("rating", "Топ")`.
  - `ChannelUiState`: полное UI-состояние экрана канала.

## Шаг 2: Парсеры и тесты (`feature-x`)
- [ ] Создать [`parserChannel.kt`](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/parcer/parserChannel.kt):
  - `parserChannelHeader(html: String, fallbackSlug: String, fallbackName: String): ChannelHeaderModel` — разбор HTML шапки.
  - `parserChannelVideosJson(jsonString: String): List<ItemsX>` — разбор JSON массива видео.
- [ ] Написать Unit-тесты в [`ParserChannelTest.kt`](file:///g:/xvideos2/feature-x/src/test/java/com/client/xvideos/x/parcer/ParserChannelTest.kt).

## Шаг 3: Управление состоянием (ScreenModel)
- [ ] Создать [`ScreenX_ChannelSM.kt`](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/channel/ScreenX_ChannelSM.kt):
  - Voyager `ScreenModel` с инъекцией Assisted Factory (`channelSlug`, `initialModel`).
  - Методы: `loadInitial()`, `loadNextPage()`, `changeSort()`, `retry()`.
  - Hilt-модуль привязки фабрики.

## Шаг 4: UI Компоненты экрана канала
- [ ] Создать атомы:
  - [`ChannelHeader.kt`](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/channel/atom/ChannelHeader.kt) — обложка, аватарка, статистика, раскрывающееся описание.
  - [`ChannelSortBar.kt`](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/channel/atom/ChannelSortBar.kt) — горизонтальные чипы сортировки.
- [ ] Создать экран [`ScreenX_Channel.kt`](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/channel/ScreenX_Channel.kt):
  - `LazyVerticalGrid(columns = GridCells.Fixed(2))`.
  - Карточки роликов `UrlVideoImageAndLongClickX`.
  - Обработка скролла, индикатор загрузки, Scaffold, системные insets.

## Шаг 5: Интеграция с плеером (`ComposeTags`)
- [ ] В [`ComposeTags.kt`](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/atom/ComposeTags.kt) добавить поддержку `onChannelClick: ((TagsMainUploaderPornstar) -> Unit)?`.
- [ ] В [`ScreenX_VideoPlayer.kt`](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayer.kt) обработать клик: пауза видео + переход `navigator.push(ScreenX_Channel(slug, model))`.

## Шаг 6: Проверка и верификация
- [ ] Запуск Unit-тестов модуля `feature-x`.
- [ ] Проверка компиляции проекта.
