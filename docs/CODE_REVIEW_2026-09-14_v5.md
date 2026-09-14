# Код-ревью xvideos — проход 26

> **Срез:** `master` · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода — `master` (коммит после прохода 25).
Линзы:
- **Синхронизация перемотки (seek) и отображения времени видеоплеера при паузе воспроизведения** (`CMPlayer2`, `MediaPlayerHost`, `StaticPlayer`);
- **Обработка сетевых сбоев и ошибок парсинга данных альбома Luscious, защита от сохранения фиктивных сущностей и UI-состояние ошибок с кнопкой повтора** (`AlbumInfo`, `ScreenAlbum`, `ScreenLAlbumSM`);
- **Предотвращение зависания экрана (вечный спиннер CircularProgressIndicator) при таймаутах WebView и сетевых сбоях ленты дашбордов** (`DashboardsPaginatedListScreen`, `readHtmlFromURLWebView`);
- **Дедупликация элементов в ленте подписок RedGifs и защита от падения Jetpack Compose LazyVerticalGrid с `Key was already used`** (`R_Saved_Subscriptions`, `ItemSubscriptionsPagingSource`);
- **Устойчивость фокуса Compose (`AppLockScreen`), сортировка сохранённых лайков по параметру `Order` и безопасный фоллбэк отсутствующих коллекций** (`ItemSavedLikesPagingSource`, `ItemCollectionPagingSource`);
- **Соблюдение архитектурного запрета на изменение P2P-транспорта** (`core/.../p2p/` не модифицировался).

Ключевой итог: **все 6 находок 26-го прохода устранены**. Репозиторий полностью собирается, detekt чист (0 замечаний), все unit-тесты успешно проходят (140 задач, 0 ошибок).

---

## Находки

### C53 — Рассинхронизация позиции и зависание индикаторов перемотки при seek на паузе в `CMPlayer2` и `MediaPlayerHost`. Высокая.

[CMPlayer2.kt:93-98](../core/src/main/java/com/client/xvideos/common/videoplayer/util/CMPlayer2.kt#L93-L98), [MediaPlayerHost.kt:133-148](../core/src/main/java/com/client/xvideos/common/videoplayer/host/MediaPlayerHost.kt#L133-L148), [StaticPlayer.kt:22-27](../core/src/main/java/com/client/xvideos/common/videoplayer/ui/StaticPlayer.kt#L22-L27)

В оптимизированном `CMPlayer2.kt` опрос позиции `callbacks.currentTime(position)` запускался через `LaunchedEffect(exoPlayer, config.isPause)`. Для снижения нагрузки на CPU во время паузы (`config.isPause == true`) цикл опроса прерывался (`break`). При этом в слушателе `createPlayerListener` коллбэк `currentTime` передавался пустым (`currentTime = {}`):
1. Когда пользователь перематывал видео на паузе через ползунок или кнопки перемотки, `exoPlayer.seekTo(...)` отрабатывал, но `callbacks.currentTime` не вызывался.
2. В `StaticPlayer.kt` очистка `playerHost.seekToTime = null` и обновление `playerHost.updateCurrentTime(it)` происходили исключительно внутри коллбэка `currentTime`.
3. В результате на паузе `currentTime` в `MediaPlayerHost` оставался на старом значении: текстовый таймер и ползунок оставались на прежней секунде до нажатия кнопки воспроизведения, а повторный переход на ту же позицию игнорировался Compose из-за не сброшенного флага `seekToTime`.

*Исправление:*
- В `CMPlayer2.kt` в `LaunchedEffect(exoPlayer, config.seekToTime)` добавлен немедленный вызов `callbacks.currentTime(it)`.
- В `MediaPlayerHost.kt` методы `seekTo(seconds: Float?)` и `seekTo(seconds: Int?)` обновляют `currentTime` сразу при вызове перемотки.
- Это гарантирует моментальное обновление UI таймеров/прогресс-баров и своевременный сброс `playerHost.seekToTime = null` в `StaticPlayer` даже при остановленном воспроизведении.

---

### C54 — Показ пустого экрана с "0 gifs / 0 pictures" без ошибки и сохранение фиктивного альбома с пустым id при сбое `AlbumInfo`. Высокая.

[AlbumInfo.kt:30-74](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumInfo.kt#L30-L74), [ScreenAlbum.kt:93-97, 172-285](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenAlbum.kt#L93-L97), [ScreenLAlbumSM.kt:70-76](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenLAlbumSM.kt#L70-L76)

В `AlbumInfo.kt` поле `albumInfo` инициализировалось фиктивным объектом с пустыми строками `AlbumDetails(id = "", title = "", ...)`. При сетевом сбое `repository.openURI` или ошибке парсинга `parseAlbumDetails` корутина просто логировала ошибку в Timber и завершала работу через `return@launch`:
1. В `ScreenAlbum.kt` проверка `if (parsed != null)` всегда возвращала `true`, поскольку объект `AlbumDetails` уже существовал.
2. Пользователю отображался пустой заголовок, пустая обложка, строка `"0 gifs / 0 pictures"` и дата `01.01.1970` без сообщения об ошибке сети и без возможности повторить запрос.
3. Кнопка «Сохранить альбом» была активна, и при нажатии `vm.saveAlbum()` сохраняла в базу `saved.albums` повреждённую запись с пустым идентификатором `id = ""`.

*Исправление:*
- Поле `AlbumInfo.albumInfo` переведено на nullable тип `MutableStateFlow<AlbumDetails?>(null)`. Добавлены реактивные состояния `loadError: MutableStateFlow<String?>` и `isLoading: MutableStateFlow<Boolean>`, а также публичный метод `retry()`.
- В `ScreenAlbum.kt` детальная информация и кнопки сохранения отображаются только при наличии валидных данных (`parsed != null && parsed.id.isNotBlank()`).
- При сетевой ошибке отображается блок сообщения об ошибке с кнопкой «Повторить» (`Button(onClick = { album.retry() })`).
- В `ScreenLAlbumSM.saveAlbum()` добавлена строгая проверка `if (details != null && details.id.isNotBlank())`, предотвращающая запись пустых сущностей в локальную базу данных.
- Добавлен юнит-тест `AlbumDetailsStateTest`, проверяющий валидацию ID и безопасные fallback-значения для обложки и URL скачивания.

---

### UI24 — Бесконечный спиннер `CircularProgressIndicator` при сбое сети или таймауте WebView в `DashboardsPaginatedListScreen`. Средняя.

[DashboardsPaginatedListScreen.kt:100-112](../feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/DashboardsPaginatedListScreen.kt#L100-L112), [readHtmlFromURLWebView.kt:36-44](../feature-x/src/main/java/com/client/xvideos/x/feature/net/readHtmlFromURLWebView.kt#L36-L44)

При загрузке страницы дашбордов XVideos функция `readHtmlFromURLWebView` по истечении таймаута (45 с) или ошибке возвращает пустую строку `""`. Функция `openNew` парсит её через `parserListVideo("")` и возвращает пустой список `emptyList()`:
1. Так как исключение не выбрасывалось, блок `catch (e: Exception)` не активировался, флаг `hasError` оставался `false`, а список `l` оставался пустым.
2. Условие `if (l.isEmpty())` переходило в ветку `else`, где бесконечно отображался `CircularProgressIndicator(40.dp)`. Пользователь не получал сообщения об ошибке сети и не имел возможности нажать кнопку «Повторить».

*Исправление:*
- В `DashboardsPaginatedListScreen` добавлена явная проверка полученного списка: если `items.isEmpty()`, выставляется `hasError = true` и выводится снекбар об ошибке загрузки.
- Экран корректно показывает плашку ошибки с кнопкой «Повторить», позволяя перезапустить загрузку без перезахода на экран.

---

### C55 — Потенциальное падение Compose `IllegalArgumentException: Key was already used` в ленте подписок RedGifs из-за отсутствия дедупликации ID. Средняя.

[R_Saved_Subscriptions.kt:100-121](../feature-r/src/main/java/com/client/xvideos/r/common/saved/R_Saved_Subscriptions.kt#L100-L121), [ItemSubscriptionsPagingSource.kt:14-23](../feature-r/src/main/java/com/client/xvideos/r/common/pagin/ItemSubscriptionsPagingSource.kt#L14-L23), [LazyRow123.kt:187](../feature-r/src/main/java/com/client/xvideos/r/ui/ui/lazyrow123/LazyRow123.kt#L187)

В методе `R_Saved_Subscriptions.refreshSubscription()` данные от всех выбранных авторов объединяются в общий список `res.addAll(read50LastItem(it.name))` без проверки на уникальность:
1. Если разные авторы делали репосты одних и тех же GIF, либо в выдаче API присутствовали повторяющиеся элементы, список содержал дубликаты `GifsInfo` с одинаковыми `id`.
2. В `LazyRow123.kt:187` ключ элемента задан как `val pagingItemKey = listGifs.itemKey { it.id }`.
3. При рендере сетки `LazyVerticalGrid` в Compose повторяющийся ключ приводил к мгновенному фатальному крэшу `java.lang.IllegalArgumentException: Key was already used`.

*Исправление:*
- В `R_Saved_Subscriptions.refreshSubscription()` добавлен вызов `.distinctBy { it.id }`.
- В `ItemSubscriptionsPagingSource` также добавлена защитная фильтрация `.distinctBy { it.id }`.
- Добавлен юнит-тест `SubscriptionsAndSavedLikesTest`, проверяющий дедупликацию списка подписок с сохранением уникальных постов.

---

### C56 — Игнорирование сортировки `order` в `ItemSavedLikesPagingSource` и падение с `NoSuchElementException` при открытии отсутствующей коллекции в `ItemCollectionPagingSource`. Средняя.

[ItemSavedLikesPagingSource.kt:14-25](../feature-r/src/main/java/com/client/xvideos/r/common/pagin/ItemSavedLikesPagingSource.kt#L14-L25), [ItemCollectionPagingSource.kt:16-24](../feature-r/src/main/java/com/client/xvideos/r/common/pagin/ItemCollectionPagingSource.kt#L16-L24)

1. В `ItemSavedLikesPagingSource` конструктор принимал параметр `val order: Order`, однако метод `load` возвращал список `savedRed.likes.list` в порядке добавления, полностью игнорируя выбранный пользователем порядок сортировки (Latest, Oldest, Top). Переключение сортировки в выпадающем меню не оказывало никакого эффекта.
2. В `ItemCollectionPagingSource` поиск элементов коллекции выполнялся через `collectionList.first { it.collection == collection }`. Если коллекция была удалена или ещё не синхронизировалась из хранилища, вызов выбрасывал `NoSuchElementException`, переводя пейджер в состояние `LoadResult.Error`.

*Исправление:*
- В `ItemSavedLikesPagingSource` реализована сортировка в зависимости от `order`: `Order.OLDEST` сортирует по возрастанию даты создания, `Order.TOP` (и варианты периодов топ) — по убыванию лайков, остальные — по убыванию даты создания (`latest`).
- В `ItemCollectionPagingSource` вызов заменён на безопасный `firstOrNull { it.collection == collection }?.items ?: emptyList()`, возвращающий пустой список без падений.
- Сценарии сортировки и безопасного поиска коллекций покрыты юнит-тестами в `SubscriptionsAndSavedLikesTest`.

---

### UI25 — Незащищённый вызов `focusRequester.requestFocus()` в `AppLockScreen` при неверном вводе пароля. Низкая.

[AppLockScreen.kt:181-186](../core/src/main/java/com/client/xvideos/common/applock/AppLockScreen.kt#L181-L186)

В `AppLockScreen.kt` в строках 137 и 148 вызовы `focusRequester.requestFocus()` были защищены через `runCatching` (находка UI23), однако в строке 183 при сбросе поля ввода после неверного пароля оставался прямой вызов `focusRequester.requestFocus()`:
1. Если в момент показа анимации ошибки или скрытия клавиатуры фокус-нода откреплялась от дерева Compose, вызов мог выбрасывать `IllegalStateException: FocusRequester is not initialized`.

*Исправление:*
- Вызов в строке 183 обёрнут в `runCatching { focusRequester.requestFocus() }`, полностью исключая сбои при любых сценариях рекомпозиции экрана блокировки.

---

## Статус

| Находка | Класс | Статус | Коммит |
| --- | --- | --- | --- |
| C53 | плеер / UI-синхронизация | закрыт | `master` |
| C54 | надёжность / данные альбома | закрыт | `master` |
| UI24 | стабильность / сетевые таймауты | закрыт | `master` |
| C55 | стабильность / Paging дедупликация | закрыт | `master` |
| C56 | корректность данных / Paging | закрыт | `master` |
| UI25 | стабильность / Compose фокус | закрыт | `master` |

---

## Проверено и в порядке — не трогать

1. **P2P-подсистема (`core/src/main/java/com/client/xvideos/common/p2p/`):** полностью заморожена решением владельца от 11.09.2026. В ходе 26-го прохода код P2P не модифицировался.
2. **Атомарность файловых операций в `SavedX_Downloads` и `SavedL_Collection`:** запись метаданных `.info` и сохранение коллекций выполняются атомарно, с проверкой уникальности и корректной очисткой.
3. **Плеер адаптивных потоков HLS:** распознавание потоков Media3 с CDN-параметрами и очистка фоновых ресурсов работают стабильно.

---

## Проверка качества

- **detekt:** 0 замечаний по всем 5 модулям (`app`, `core`, `feature-l`, `feature-r`, `feature-x`).
- **Юнит-тесты:** 140 задач успешно выполнены, 0 ошибок.
- **Новые тесты:**
  - `AlbumDetailsStateTest`: валидация идентификатора альбома при сохранении, безопасные фоллбэки миниатюр и URL скачивания при null-состоянии.
  - `SubscriptionsAndSavedLikesTest`: проверка дедупликации постов подписок, корректность применения сортировок `Order.OLDEST`, `Order.LATEST`, `Order.TOP` и безопасный `firstOrNull` фоллбэк коллекций.
- **P2P-транспорт:** директория `core/src/main/java/com/client/xvideos/common/p2p/` не модифицировалась (согласно решению о заморозке).
