# Код-ревью xvideos — проход 86

> **Срез:** `4844535` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `4844535`. Затронуты модули `:feature-x`, `:core` и добавлены модульные тесты.

Линзы:
- `A` / Архитектурный переход полноэкранного режима видеоплеера X на единый экземпляр плеера без пересоздания экрана и EventBus ([ScreenX_VideoPlayer.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayer.kt), [ScreenX_VideoPlayerSM.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt), [ScreenX_VideoPlayerFullScreen.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreen.kt)).
- `C` / Сброс полноэкранного режима в портретную ориентацию при ошибках загрузки страницы видео в `loadVideo` ([ScreenX_VideoPlayerSM.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt)).
- `UI` / Корректное завершение жеста перемотки при отмене перетаскивания `onDragCancel` в `CustomSeekBar` ([CustomSeekBar.kt](../core/src/main/java/com/client/xvideos/common/videoplayer/ui/component/CustomSeekBar.kt)).
- `C` / Каноническая нормализация URL в `normalizeXUrl` с защитой от отсутствующих слэшей и дублирования протокола ([XSite.kt](../feature-x/src/main/java/com/client/xvideos/x/XSite.kt), [SavedX_Downloads.kt](../feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt), [ScreenFavorites.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/favorites/ScreenFavorites.kt), [ScreenTags.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/tags/ScreenTags.kt), [DashboardsPaginatedListScreen.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/DashboardsPaginatedListScreen.kt), [country.kt](../feature-x/src/main/java/com/client/xvideos/x/feature/country/country.kt)).
- `T` / Покрытие тестами нормализации ссылок и обработки сбоев воспроизведения ([XSiteTest.kt](../feature-x/src/test/java/com/client/xvideos/x/XSiteTest.kt), [ScreenX_VideoPlayerSMTest.kt](../feature-x/src/test/java/com/client/xvideos/x/ScreenX_VideoPlayerSMTest.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### A22 — Дублирование экземпляра плеера и повторная буферизация при переходе в полный экран X. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayer.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayer.kt)
[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt)
[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreen.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreen.kt)

Ранее кнопка перехода в ландшафтный режим открывала отдельный экран `ScreenX_VideoPlayerFullScreen`, что приводило к созданию второго физического экземпляра ExoPlayer через `MediaPlayerHost`, уничтожению первого экземпляра, повторной буферизации видеопотока с нуля и необходимости синхронизации позиции через шину `EventBus`.

**Исправление:**
- В `AndroidManifest.xml` для `MainActivity` добавлена обработка изменений конфигурации `android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize"`, предотвращающая пересоздание Activity и дерева Compose при смене ориентации.
- В `ScreenX_VideoPlayerSM` добавлено состояние `isFullScreen`, методы `toggleFullScreen()`, `enterFullScreen()`, `exitFullScreen()`.
- В `ScreenX_VideoPlayer` реализовано бесшовное переключение ориентации `ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE` / `SCREEN_ORIENTATION_PORTRAIT`, скрытие навигационной панели системы, перехват `BackHandler` для выхода из полноэкранного режима и автоскрытие контролов через 3.5 секунды.
- Единый экземпляр `MediaPlayerHost` сохраняется в памяти без остановки воспроизведения. Устаревший `ScreenX_VideoPlayerFullScreen` помечен `@Deprecated`.

---

### C136 — Зависание экрана в ландшафтной ориентации при ошибке загрузки видео в ScreenX_VideoPlayerSM. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt:157,171](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt#L157)

В `ScreenX_VideoPlayerSM.loadVideo()`, если при парсинге страницы отсутствовала ссылка на HLS/mp4 (`parsedData.third.isBlank()`) или возникало исключение сети/парсинга в блоке `catch (e: Exception)`, выставлялся флаг `isError = true`, но флаг `isFullScreen` не сбрасывался. Если пользователь нажимал «Повторить» или ошибка происходила во время удержания полноэкранного состояния, экран оставался заблокированным в ландшафтной ориентации, блокируя нормальное отображение диалога ошибки в портретном виде.

**Исправление:**
- Добавлен гарантированный сброс `isFullScreen = false` при установке `isError = true` в обеих ветках ошибок `loadVideo()`.

---

### UI110 — Залипание host.isSliding при отмене жеста перемотки в CustomSeekBar. Средняя.

[core/src/main/java/com/client/xvideos/common/videoplayer/ui/component/CustomSeekBar.kt:118](../core/src/main/java/com/client/xvideos/common/videoplayer/ui/component/CustomSeekBar.kt#L118)

В `CustomSeekBar` в обработчике `detectDragGestures` коллбэк отмены жеста `onDragCancel` сбрасывал только локальный флаг `isDragging = false`, но не вызывал `currentOnValueChangeFinished()`. Хосты, отслеживающие перетаскивание (например, `X_PlayerBottomBar`, выставляющий `host.isSliding = true`), при перехвате жеста системой (системные жесты «Назад», скролл родительского контейнера) навсегда оставались в состоянии `host.isSliding = true`, из-за чего ползунок перемотки переставал обновляться по текущему времени видео.

**Исправление:**
- В `onDragCancel` добавлен вызов `currentOnValueChangeFinished()`, завершающий цикл перетаскивания и сбрасывающий флаг `isSliding`.

---

### C137 — Риск повреждения URL при наивной конкатенации urlStart + item.href в модуле X. Низкая.

[feature-x/src/main/java/com/client/xvideos/x/XSite.kt](../feature-x/src/main/java/com/client/xvideos/x/XSite.kt)
[feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt:156](../feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt#L156)
[feature-x/src/main/java/com/client/xvideos/x/screens/favorites/ScreenFavorites.kt:89](../feature-x/src/main/java/com/client/xvideos/x/screens/favorites/ScreenFavorites.kt#L89)
[feature-x/src/main/java/com/client/xvideos/x/screens/tags/ScreenTags.kt:125](../feature-x/src/main/java/com/client/xvideos/x/screens/tags/ScreenTags.kt#L125)
[feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/DashboardsPaginatedListScreen.kt:214](../feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/DashboardsPaginatedListScreen.kt#L214)
[feature-x/src/main/java/com/client/xvideos/x/feature/country/country.kt:144](../feature-x/src/main/java/com/client/xvideos/x/feature/country/country.kt#L144)

В местах вызова плеера, загрузок и выбора страны URL формировался прямой конкатенацией `urlStart + item.href`. Если относительный путь не содержал ведущего слэша (например, `"video123"` вместо `"/video123"`), формировался невалидный URL `https://www.xv-ru.comvideo123`. Кроме того, если `href` уже содержал абсолютный URL (`https://...`), конкатенация приводила к дублированию хоста.

**Исправление:**
- Реализована функция `normalizeXUrl(href: String): String`, гарантирующая корректное добавление ведущего слэша и сохранение абсолютных `http/https` ссылок.
- Все места конкатенации в модуле `:feature-x` переведены на `normalizeXUrl`.
- В конструкторе `ScreenX_VideoPlayerSM` входящий URL также автоматически нормализуется через `normalizeXUrl`.

---

### T46 — Отсутствие тестов на нормализацию ссылок X и восстановление полноэкранного состояния при ошибках. Низкая.

[feature-x/src/test/java/com/client/xvideos/x/XSiteTest.kt](../feature-x/src/test/java/com/client/xvideos/x/XSiteTest.kt)
[feature-x/src/test/java/com/client/xvideos/x/ScreenX_VideoPlayerSMTest.kt](../feature-x/src/test/java/com/client/xvideos/x/ScreenX_VideoPlayerSMTest.kt)

Логика форматирования ссылок сайта X и сброса полноэкранного режима при сетевых/парсерных ошибках не была покрыта юнит-тестами.

**Исправление:**
- Создан тестовый класс `XSiteTest`, проверяющий пустые строки, сохранение абсолютных ссылок, добавление недостающего слэша и тримминг пробелов.
- В `ScreenX_VideoPlayerSMTest` добавлены тесты на нормализацию URL при создании модели и сброс `isFullScreen` при ошибках.

---

## Статус

| Находка | Класс | Статус | Детали |
| --- | --- | --- | --- |
| A22 | архитектура / UX | закрыт | Бесшовный single-instance плеер X в ландшафтном режиме без ребуферизации |
| C136 | корректность / состояние | закрыт | Сброс `isFullScreen = false` при ошибках в `loadVideo` |
| UI110 | UI / жесты | закрыт | Вызов `currentOnValueChangeFinished()` на `onDragCancel` в `CustomSeekBar` |
| C137 | корректность / сеть | закрыт | Безопасная нормализация `normalizeXUrl` для относительных и абсолютных ссылок X |
| T46 | тесты | закрыт | Модульные тесты в `XSiteTest` и `ScreenX_VideoPlayerSMTest` |

## Проверка

```
.\gradlew.bat app:detekt core:detekt feature-l:detekt feature-r:detekt feature-x:detekt
BUILD SUCCESSFUL in 5s

.\gradlew.bat testDebugUnitTest
BUILD SUCCESSFUL in 27s (140 actionable tasks)

.\gradlew.bat compileReleaseKotlin
BUILD SUCCESSFUL in 11s
```
