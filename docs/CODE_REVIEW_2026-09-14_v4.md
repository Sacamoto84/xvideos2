# Код-ревью xvideos — проход 25

> **Срез:** `master` · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода — `master` (коммит после прохода 24).
Линзы:
- **Надёжность воспроизведения адаптивного HLS-видео в Media3 и корректное определение типов медиа с CDN query-параметрами** (`rememberExoPlayerWithLifecycle`, `MediaPlayerHost`, `ExoplayerHelper`);
- **Безопасность взаимодействия с системным хранилищем MediaStore и устойчивость к сбоям `ContentResolver`** (`GallerySaver`);
- **Производительность и исключение избыточных O(N) аллокаций в фоновых источниках пагинации Paging3 и фильтрации лент** (`BlockRed`, `ItemTopPagingSource`, `ItemProfilePagingSource`, `ItemNailsPagingSource`);
- **Устойчивость жизненного цикла Compose UI к запросам фокуса и программной клавиатуры** (`AppLockScreen`);
- **Соблюдение архитектурного запрета на изменение P2P-транспорта** (`core/.../p2p/` не модифицировался).

Ключевой итог: **все 4 находки 25-го прохода устранены**. Репозиторий полностью собирается, detekt чист (0 замечаний), все unit-тесты успешно проходят (145 задач, 0 ошибок).

---

## Находки

### C51 — Ложное определение HLS-потоков с query-параметрами и фрагментами в `rememberExoPlayerWithLifecycle` и `MediaPlayerHost`. Высокая.

[ExoplayerHelper.kt:66-83](../core/src/main/java/com/client/xvideos/common/videoplayer/util/ExoplayerHelper.kt#L66-L83), [rememberExoPlayerWithLifecycle.kt:92-108](../core/src/main/java/com/client/xvideos/common/videoplayer/rememberExoPlayerWithLifecycle.kt#L92-L108), [MediaPlayerHost.kt:251](../core/src/main/java/com/client/xvideos/common/videoplayer/host/MediaPlayerHost.kt#L251)

В глобальных компонентах плеера для распознавания HLS (.m3u8) использовалась наивная проверка `url.endsWith(".m3u8", ignoreCase = true)`:
1. Когда URL адаптивного потока содержал query-параметры (токены авторизации CDN, временные метки сессии `?token=...&expires=...`, что является стандартом для защищённых CDN) или URL-фрагменты, проверка возвращала `false`.
2. В `rememberExoPlayerWithLifecycle` это приводило к тому, что HLS-поток передавался в `createProgressiveMediaSource` вместо `createHlsMediaSource`. Экзоплеер пытался парсить плейлист через экстракторы MP4/MKV, приводя к падению `UnrecognizedInputFormatException` и невозможности воспроизведения видео.
3. В `MediaPlayerHost` проверка `videoUrl.endsWith(".m3u8")` отсекала вызов парсера `m3u8Helper.fetchM3U8Data`, из-за чего для таких потоков полностью очищались и отключались списки выбора качества видео, аудиодорожек и субтитров.
4. При этом в `FeedPlayerState.kt:156-158` разработчики ранее уже фиксировали аналогичную проблему: *"Проверка endsWith(\".m3u8\") ломалась бы на подписанном url с ?token=… и на редиректе, то есть ровно там, где краш и вернулся бы"*, однако в базовых компонентах видеоплеера оставался устаревший код.

*Исправление:*
- В `ExoplayerHelper.kt` реализована общая функция `isHlsUrl(url: String?): Boolean`, корректно отсекающая параметры запроса и якоря (`substringBefore('?').substringBefore('#')`), проверяющая расширение `.m3u8` и валидирующая тип потока через Media3 `Util.inferContentType`.
- В `rememberExoPlayerWithLifecycle` проверка заменена на `isHlsUrl(url)`, а также добавлен ранний возврат при пустом URL (`if (url.isBlank()) { exoPlayer.stop(); exoPlayer.clearMediaItems(); return@LaunchedEffect }`), исключающий ложные сообщения `MediaPlayerError.PlaybackError` при начальной инициализации экранов.
- В `MediaPlayerHost` проверка `videoUrl.endsWith(".m3u8")` заменена на `isHlsUrl(videoUrl)`.
- Написан изолированный юнит-тест `HlsUrlDetectionTest`, проверяющий работу функции со стандартными URL, подписанными CDN URL с токенами, фрагментами, различными регистрами символов и неподдерживаемыми форматами.

---

### C52 — Необработанный сбой `ContentResolver.query()` при проверке существования файла в `GallerySaver.exists`. Средняя.

[GallerySaver.kt:171-180](../core/src/main/java/com/client/xvideos/common/gallery/GallerySaver.kt#L171-L180)

В модуле сохранения медиа в общую галерею (`GallerySaver.kt`) метод `exists(context, fileName)` напрямую обращался к `context.contentResolver.query(...)` без перехвата исключений:
1. На специализированных сборках Android, устройствах с ограниченным доступом к Scoped Storage или при сбоях системного процесса MediaStore вызов `query` может выбрасывать `SecurityException`, `SQLiteException` или `IllegalStateException`.
2. В методе `saveFromUrl` вызов `exists` выполнялся на верхнем уровне корутины вне блока `try-catch`. Выброшенное исключение приводило к неотловленному крэшу корутины в `scope`.
3. В методе `saveFromFile` сбой приводил к ложному показу пользователю снекбара об ошибке сохранения файла.

*Исправление:*
- Тело функции `exists()` обёрнуто в `runCatching { ... }.getOrDefault(false)`. При любом сбое взаимодействия с MediaStore проверка безопасно возвращает `false`, не прерывая выполнение корутины и давая механизму сохранения шанс создать запись.

---

### UI22 — Избыточное создание множеств ID заблокированных элементов (`map { it.id }.toSet()`) при пагинации лент и фильтрации. Низкая.

[BlockRed.kt:30-49](../feature-r/src/main/java/com/client/xvideos/r/common/block/BlockRed.kt#L30-L49), [ItemTopPagingSource.kt:60](../feature-r/src/main/java/com/client/xvideos/r/common/pagin/ItemTopPagingSource.kt#L60), [ItemProfilePagingSource.kt:46](../feature-r/src/main/java/com/client/xvideos/r/common/pagin/ItemProfilePagingSource.kt#L46), [ItemNailsPagingSource.kt:46](../feature-r/src/main/java/com/client/xvideos/r/common/pagin/ItemNailsPagingSource.kt#L46)

При загрузке каждой страницы ленты в `ItemTopPagingSource`, `ItemProfilePagingSource` и `ItemNailsPagingSource`, а также при вызове `BlockRed.refreshListAndBlock`, выполнялась трансформация `val blockedSet = block.blockList.value.map { it.id }.toSet()`:
1. Это приводило к постоянным аллокациям промежуточных списков строк и хэш-сетов в памяти при каждом скролле страниц.
2. В `BlockRed` отсутствовала предвычисленная коллекция идентификаторов для быстрого поиска за O(1).

*Исправление:*
- В `BlockRed` добавлено предвычисленное реактивное поле `val blockedIds: StateFlow<Set<String>>`, обновляемое при чтении списка блокировок в `HashSet` единым проходом.
- Добавлен вспомогательный метод `isBlocked(id: String): Boolean = id in _blockedIds.value`.
- В `ItemTopPagingSource`, `ItemProfilePagingSource`, `ItemNailsPagingSource` и `refreshListAndBlock` фильтрация переведена на прямое использование `blockedIds.value` за O(1) без повторных аллокаций множеств.
- Метод `BlockRed.refresh()` теперь возвращает `Job` для возможности синхронизации в тестах и фоновых процессах.
- Добавлен комплексный юнит-тест `BlockRedFilterTest`, проверяющий реактивное обновление `blockedIds`, блокировку, фильтрацию списка и последующую разблокировку.

---

### UI23 — Защита от `IllegalStateException` при запросе фокуса в `AppLockScreen`. Низкая.

[AppLockScreen.kt:134-152](../core/src/main/java/com/client/xvideos/common/applock/AppLockScreen.kt#L134-L152)

В экране блокировки приложения `AppLockScreen` запросы фокуса `focusRequester.requestFocus()` в `LaunchedEffect` и `DisposableEffect` (при возвращении из фона `ON_RESUME`) вызывались напрямую:
1. Если по какой-либо причине (задержка лейаута, переключение экрана, анимация перехода) узел поля ввода ещё не успел прикрепиться к дереву композиции, `FocusRequester` выбрасывает `IllegalStateException: FocusRequester is not initialized`.
2. Это могло приводить к крэшу при открытии приложения на медленных устройствах.

*Исправление:*
- Вызовы `focusRequester.requestFocus()` защищены через `runCatching { focusRequester.requestFocus() }`, гарантируя отсутствие падений при рассинхронизации состояния лейаута.

---

## Статус

| Находка | Класс | Статус | Коммит |
| --- | --- | --- | --- |
| C51 | надёжность / Media3 | закрыт | `master` |
| C52 | стабильность / MediaStore | закрыт | `master` |
| UI22 | производительность / аллокации | закрыт | `master` |
| UI23 | стабильность / Compose | закрыт | `master` |

---

## Проверка качества

- **detekt:** 0 замечаний по всем 5 модулям (`app`, `core`, `feature-l`, `feature-r`, `feature-x`).
- **Юнит-тесты:** 145 задач успешно выполнены, 0 ошибок.
- **Новые тесты:**
  - `HlsUrlDetectionTest`: 7 тестовых сценариев (стандартные URL, токены CDN, фрагменты, регистры, mp4/webm/mkv, null/blank).
  - `BlockRedFilterTest`: сквозное тестирование жизненного цикла блокировки, предвычисленного `blockedIds` и фильтрации.
- **P2P-транспорт:** директория `core/src/main/java/com/client/xvideos/common/p2p/` не модифицировалась (согласно решению о заморозке).
