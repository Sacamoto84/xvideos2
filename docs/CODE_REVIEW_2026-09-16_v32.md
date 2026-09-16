# Код-ревью xvideos — проход 53

> **Срез:** `fd158b8` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `fd158b8`. Изменено 6 исходных файлов в модулях `feature-x`, `feature-r`, `feature-l`, удалён 1 устаревший файл мёртвого кода (`TikTokStyleVideoFeed.kt`), добавлены и обновлены unit-тесты в `feature-x` (`XParsersTest.kt`, `X_TagUrlSanitizationTest.kt`) и `feature-r` (`RedPooledVideoPlayerTest.kt`).

Линзы:
- `UI` / `C` / Стабильность Compose Pager: предотвращение краша `IllegalArgumentException` при инициализации пейджера в [ScreenTags.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/tags/ScreenTags.kt) и парсинге страниц в [parserScreenTags.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/parcer/parserScreenTags.kt).
- `S` / `Q` / Валидация и санитизация URL: устойчивая нормализация тегов с неразрывными пробелами (`\u00A0`) и схлопыванием дефисов в [ScreenTagsViewModel.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/tags/ScreenTagsViewModel.kt).
- `T` / `C` / Off-Main Safety: вынос всех файловых проверок (`exists()`, `length()`), отмен закачек (`kDownloader.cancel`) и поисков в кеше в [SavedX_Downloads.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt) и [DownloadRed.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt) на `Dispatchers.IO`.
- `UI` / Консистентность жизненного цикла плеера: защита от воспроизведения на неактивных страницах через `PlayerControls.play()` в [RedPooledVideoPlayer.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/video/RedPooledVideoPlayer.kt).
- `UI` / `Q` / Compose Stability и чистка кодовой базы: аннотация `@Stable` на [ScreenRedProfileSM.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/profile/ScreenRedProfileSM.kt), удаление неиспользуемых свойств плеера и закомментированного файла `TikTokStyleVideoFeed.kt`, удаление неиспользуемой зависимости в [ScreenRedFullScreenSM.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreenSM.kt), устранение искусственного `delay(100)` в [ScreenAlbumListSM.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumListSM.kt).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### UI65 / C128 — Предотвращение краша Jetpack Compose Pager при некорректном или нулевом числе страниц. Высокая.

[feature-x/src/main/java/com/client/xvideos/x/screens/tags/ScreenTags.kt:54](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/tags/ScreenTags.kt#L54)
[feature-x/src/main/java/com/client/xvideos/x/parcer/parserScreenTags.kt:22-25](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/parcer/parserScreenTags.kt#L22)

В Jetpack Compose Foundation `rememberPagerState` требует строго `pageCount > 0`. В `ScreenTags.kt` состояние создавалось как:
```kotlin
val pagerState = rememberPagerState(initialPage = 0) { vm.screen.lastPage }
```
Если на сервере блок постраничности содержал некорректный номер или значение `<= 0` (например, при пустой выборке или нестандартной верстке тегов), парсер возвращал `lastPage <= 0`, что приводило к немедленному падению приложения с `IllegalArgumentException: pageCount must be greater than 0`.
**Исправление:**
1. В `parserScreenTags.kt` значение пагинации ограничено снизу `.coerceAtLeast(1)`.
2. В `ScreenTags.kt` лямбда `pageCount` дополнительно защищена `vm.screen.lastPage.coerceAtLeast(1)`.
3. В `XParsersTest.kt` добавлен тест `при нулевом или некорректном last-page число страниц не падает ниже 1`.

---

### S35 / Q30 — Нормализация URL-тегов с поддержкой неразрывных пробелов и схлопыванием дефисов. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/screens/tags/ScreenTagsViewModel.kt:71-75, 100-108](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/tags/ScreenTagsViewModel.kt#L71)

В `ScreenTagsViewModel` санитизация тегов производилась ad-hoc выражением с `Regex("\\s+")`. Стандартный Java/Kotlin `\s` без флага юникода не сопоставляет неразрывные пробелы (`\u00A0`), часто встречающиеся в веб-верстке и заголовках. Кроме того, замена спецсимволов путей (`#`, `?`, `&`, `/`, `\`) на дефисы приводила к образованию серий дефисов (например, `tag---query`).
**Исправление:** Логика вынесена в отдельную внутреннюю функцию `sanitizeTagForUrl(tag: String)`. Она заменяет все пробелы (включая `\u00A0`) и спецсимволы на дефисы, схлопывает множественные дефисы через `replace(Regex("-+"), "-")` и обрезает краевые. В `X_TagUrlSanitizationTest.kt` добавлены тесты на неразрывные пробелы и схлопывание дефисов.

---

### T23 / C129 — Блокирующий дисковый ввод-вывод и синхронные отмены на UI-потоке. Высокая.

[feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt:156-188](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt#L156)
[feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt:88-135, 255-265](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/DownloadRed.kt#L88)

1. В `SavedX_Downloads.saveToGallery` и `DownloadRed.saveToGallery` проверка наличия локального файла `local.exists() && local.length() > 0L` выполнялась синхронно в вызывающем UI-потоке перед запуском корутины.
2. В `SavedX_Downloads.delete` и `DownloadRed.delete` вызов `kDownloader.cancel(id)` выполнялся на UI-потоке до перехода в корутину `Dispatchers.IO`. При отмене приостановленного запроса `DownloadDispatchers.cancel` синхронно удаляет временный файл на диске.
3. В `DownloadRed.ensureDownloaded` проверка `downloader.findVideoInDownload(id, name)` с дисковым I/O выполнялась в контексте `Dispatchers.Main`.
**Исправление:**
- В `saveToGallery` проверка `local.exists()` и `local.length()` перенесена внутрь `scope.launch(Dispatchers.IO)`.
- Вызовы `kDownloader.cancel` перемещены внутрь блока `Dispatchers.IO`.
- В `DownloadRed.ensureDownloaded` проверка вынесена в `withContext(Dispatchers.IO)` перед возвратом на Main для вызова `onReady()`.

---

### UI66 — Защита от нежелательного воспроизведения на соседних страницах в `RedPooledVideoPlayer`. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/ui/video/RedPooledVideoPlayer.kt:216-218](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/video/RedPooledVideoPlayer.kt#L216)

В `RedPooledVideoPlayer` объект `PlayerControls` в методе `play()` напрямую выставлял `exo.playWhenReady = true` без сверки с флагом `isCurrentPage`. При активной прокрутке ленты или обращении к контроллеру во время переходных состояний это могло приводить к принудительному старту воспроизведения плеера соседней страницы из пула.
**Исправление:** В метод `play()` добавлена проверка `if (isCurrentPage) { exo.playWhenReady = true }`. Вспомогательная функция `clampSeekPositionMs(positionMs, durationMs)` вынесена и покрыта модульными тестами в `RedPooledVideoPlayerTest.kt`.

---

### Q31 / UI67 — Оптимизация стабильности Compose, удаление мертвого кода и устранение искусственных задержек. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/ui/profile/ScreenRedProfileSM.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/profile/ScreenRedProfileSM.kt)
[feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreenSM.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreenSM.kt)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumListSM.kt:137-145](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumListSM.kt#L137)

1. Класс `ScreenRedProfileSM` аннотирован `@Stable`, что обеспечивает пропуск повторной рекомпозиции (smart skipping) для зависимых экранов.
2. Из `ScreenRedProfileSM` удалены неиспользуемые поля состояния видеоплеера (`currentTikTokPage`, `currentTikTokGifInfo`, `menuCenter`, `tictikStartIndex`, `play`, `mute`, `autoRotate`, `enableAB`, `timeA`, `timeB`, `currentPlayerControls`, `currentPlayerTime`, `currentPlayerDuration`), оставшиеся от давнего прототипа TikTok-ленты в профиле. Полностью закомментированный файл `TikTokStyleVideoFeed.kt` удален из репозитория.
3. Из `ScreenRedFullScreenSM` удалена неиспользуемая зависимость `connectivityObserver`.
4. В `ScreenAlbumListSM.loadInitialData` удален искусственный вызов `delay(100)` между `bigList.clear()` и установкой `bigList[0] = ...`, вызывавший микрозадержки и мерцание UI.

---

## Итог верификации

- **Unit-тесты:** `./gradlew testDebugUnitTest` — успешно (все тесты по всем 5 модулям `core`, `feature-l`, `feature-r`, `feature-x`, `app` пройдены без ошибок).
- **Detekt:** `./gradlew detekt --rerun-tasks` — 0 ошибок и предупреждений по всем 5 модулям.
- **Архитектурные ограничения:** Замороженный P2P (`core/.../p2p/`) не изменялся.
