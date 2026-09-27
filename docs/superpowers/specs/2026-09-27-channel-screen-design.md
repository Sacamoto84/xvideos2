# Дизайн экрана канала X (`ScreenX_Channel`)

**Дата:** 2026-09-27  
**Статус:** Согласовано  

---

## 1. Резюме требований (Understanding Summary)

- **Что создается**: Новый экран профиля канала `ScreenX_Channel` (Voyager Screen + ScreenModel) и маршрутизация кликов по чипу канала из плеера `ScreenX_VideoPlayer` / `ComposeTags`.
- **Зачем это нужно**: При клике на синий тег канала (`TagItem.Channel`) пользователь должен попадать на реальный профиль автора/студии со списком их видео, а не в выдачу поиска по тегам `/tags/{name}/0`.
- **Для кого**: Пользователи приложения X, желающие просматривать контент конкретного канала.
- **Ключевой UX**:
  - Полноценная шапка профиля: обложка-баннер, аватарка автора, имя, число подписчиков, общее число просмотров, сворачиваемый блок «Обо мне».
  - Переключатель сортировки: «Свежие» (`best`), «Новые» (`new`), «Топ» (`rating`).
  - Бесконечная лента видео (Infinite Scroll) в 2 колонки с карточками `UrlVideoImageAndLongClickX`.
- **Не-цели (Out of scope)**:
  - Авторизация и подписка на веб-сайте через аккаунт.
  - Раздел платных видео RED.

---

## 2. Допущения (Assumptions)

- **Сетевой протокол и производительность**:
  - Шапка парсится из легкого HTML-ответа страницы `/channels/{slug}` (или `/dart_oficial`) один раз при входе.
  - Видеолента загружается через прямой JSON API: `/channels/{slug}/videos/{sort}/{page}` (по 36 видео на порцию). Парсинг JSON исключает нагрузку DOM и многократно снижает трафик.
- **Отказоустойчивость**:
  - При ошибке сети отображается экран ошибки с кнопкой «Повторить» (Retry).
  - При скролле реализована защита от параллельных запросов пагинации.
- **Обратная совместимость**:
  - Чипы ключевых слов (`TagItem.Keyword`) и порнозвезд (`TagItem.Pornstar`) продолжают переходить в `ScreenTags(tag)` без поломки существующей логики.

---

## 3. Журнал решений (Decision Log)

| № | Решение | Альтернативы | Обоснование |
|---|---|---|---|
| **D1** | Полноценный экран канала (баннер, аватар, статы, описание, сортировка) | Минималистичный экран или чистая сетка | Полный брендинг канала и консистентность с веб-версией сайта. |
| **D2** | Бесконечная прокрутка (Infinite Scroll) | Постраничный Pager (1, 2, 3...) | Более естественный, плавный мобильный UX для каталогов видео. |
| **D3** | Гибридная загрузка: HTML для шапки + JSON API для видео | Парсинг видео из HTML | На сайте видео отдаются только через AJAX JSON. JSON быстрее в десятки раз и надежнее. |
| **D4** | Разделение колбэков в `ComposeTags` | Единый строковый `onClick: (String) -> Unit` | Позволяет безопасно передавать объект канала с его slug/href и предотвращает ложные переходы в теги. |

---

## 4. Финальный дизайн и Архитектура

### 4.1 Модели данных (`feature-x/.../model/ChannelModel.kt`)

```kotlin
@Immutable
data class ChannelHeaderModel(
    val slug: String = "",
    val name: String = "",
    val bannerUrl: String = "",
    val avatarUrl: String = "",
    val subscribers: String = "",
    val totalViews: String = "",
    val aboutMe: String = "",
    val videoCount: Int = 0,
)

enum class ChannelSortOrder(val apiKey: String, val title: String) {
    BEST("best", "Свежие"),
    NEW("new", "Новые"),
    RATING("rating", "Топ"),
}

@Immutable
data class ChannelUiState(
    val header: ChannelHeaderModel = ChannelHeaderModel(),
    val videos: List<ItemsX> = emptyList(),
    val currentSort: ChannelSortOrder = ChannelSortOrder.BEST,
    val isLoadingInitial: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isEndReached: Boolean = false,
    val error: String? = null,
)
```

### 4.2 Парсеры (`feature-x/.../parcer/`)

1. **`parserChannelHeader(html: String, fallbackSlug: String, fallbackName: String): ChannelHeaderModel`**
   - Извлекает баннер (`.banner-slider img`), аватар (`.profile-pic img`), подписчиков (`#pinfo-subscribers`), просмотры (`#pinfo-videos-views`), описание (`#header-about-me`).
2. **`parserChannelVideosJson(jsonString: String): List<ItemsX>`**
   - Разбирает массив `videos` из ответа `/channels/{slug}/videos/{sort}/{page}` и конструирует список `ItemsX` с нормализованными URL видео и постеров.

### 4.3 Управление состоянием (`ScreenX_ChannelSM.kt`)

- Voyager `ScreenModel` с инъекцией Assisted Factory (`channelSlug: String`, `initialModel: TagsMainUploaderPornstar?`).
- `loadInitial()`: фоновая загрузка шапки + 0-й страницы видео.
- `loadNextPage()`: инкремент `currentPage` и догрузка в `videos`.
- `changeSort(newSort)`: сброс списка и пагинации, загрузка с новым фильтром.

### 4.4 Пользовательский интерфейс (`ScreenX_Channel.kt`)

- `Scaffold` с фоном `#040404` и отступом под системный вырез (`getTopInsetDp()`).
- `LazyVerticalGrid(columns = GridCells.Fixed(2))`:
  - `ChannelHeader` (`GridItemSpan(2)`): баннер с градиентным фейдом, аватарка 64.dp, имя, бейджи подписчиков и просмотров, расширяемое описание.
  - `ChannelSortBar` (`GridItemSpan(2)`): чипы «Свежие», «Новые», «Топ».
  - Видеокарточки: `UrlVideoImageAndLongClickX` (стандартный компонент приложения).
  - Индикатор подгрузки в конце списка.

### 4.5 Интеграция в плеер (`ScreenX_VideoPlayer.kt`, `ComposeTags.kt`)

- `ComposeTags` принимает `onChannelClick: ((TagsMainUploaderPornstar) -> Unit)?`.
- В `ScreenX_VideoPlayer`:
  ```kotlin
  onChannelClick = { channel ->
      host.pause()
      val slug = channel.cleanHref.ifBlank { channel.name }
      navigator.push(ScreenX_Channel(slug = slug, initialModel = channel))
  }
  ```
