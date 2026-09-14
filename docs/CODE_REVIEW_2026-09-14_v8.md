# Код-ревью xvideos — проход 29

> **Срез:** `master` · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода — `master` (коммит `8591847` после прохода 28).
Линзы:
- **Целостность данных и устранение дублирования кода загрузчика** (`Downloader.kt`, `DownloaderRecoveryDeduplicationTest.kt`);
- **Корректность расчёта прогресса загрузки и предотвращение NPE** (`AlbumPicsDetails.kt`, `AlbumList.kt`, `AlbumPicsDetailsProgressTest.kt`);
- **Состояние экранов пагинации и обработка сетевых ошибок при смене фильтров** (`DashboardsPaginatedListScreen.kt`);
- **Жизненный цикл Compose-эффектов и виброотклика (UX/Haptic)** (`ExpandMenuVideo.kt`);
- **Архитектура кэширования и ротация просроченных/повреждённых записей базы файлов** (`FolderTable.kt`, `FolderTableTtlOrphanTest.kt`);
- **Конкурентность воспроизведения и управление паузой ExoPlayer при навигации** (`ScreenX_VideoPlayer.kt`);
- **Соблюдение архитектурного запрета на изменение P2P-транспорта** (`core/.../p2p/` не модифицировался).

Ключевой итог: **все 6 находок 29-го прохода устранены**. Репозиторий полностью собирается, detekt чист (0 замечаний), все unit-тесты успешно проходят (140 задач, 0 ошибок).

---

## Находки

### C62 — Потеря метаданных `.info` при фоновом скачивании, вызов `onComplete` вне Main-потока и дублирование логики в `Downloader`. Высокая.

[Downloader.kt:129-303](../feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt#L129-L303), [DownloaderRecoveryDeduplicationTest.kt:1-46](../feature-r/src/test/java/com/client/xvideos/r/common/downloader/DownloaderRecoveryDeduplicationTest.kt#L1-L46)

В модуле `feature-r` в классе `Downloader.kt`:
1. Метод `downloadMissingFiles` после завершения скачивания видеофайла вызывал только `onComplete()` и не сохранял JSON-файл метаданных `${item.id}.info` на диске (в отличие от `downloadRedName`). Список скачанных видео в приложении (`DownloadRed.downloadList`) строится исключительно по `.info`-файлам на диске. В результате видео, скачанные через `downloadMissingFiles`, оставались невидимыми во вкладке «Загрузки» и не могли быть экспортированы в P2P/бэкапы.
2. Колбэк `onComplete()` вызывался непосредственно из фонового потока завершения `kDownloader`, хотя вызывающий код (UI-компоненты) ожидает выполнение на `Dispatchers.Main` для отображения снекбаров, тостов или навигации.
3. Методы `downloadMissingFiles` и `downloadMissingFilesForRecovery` содержали ~90 строк почти побайтово скопированного кода (проверки путей, создание папок, постановка превью и видео в очередь).
4. Присутствовала неиспользуемая аннотация `@OptIn(DelicateCoroutinesApi::class)`.

*Исправление:*
- При успешном завершении скачивания видео в `downloadMissingFiles` выполняется атомарная запись `${item.id}.info` на диске, если файл ещё не существовал.
- Вызов `onComplete()` гарантированно перенаправлен на `Dispatchers.Main` (включая ветку `queuedVideo == 0`).
- Метод `downloadMissingFilesForRecovery` переписан в виде делегирования вызова `downloadMissingFiles(item, onComplete, onEvent, showSnackBarErrors = false)`, устранив дублирование кода.
- Удалены неиспользуемая аннотация `@OptIn(DelicateCoroutinesApi::class)` и неиспользуемый импорт.
- Добавлен юнит-тест `DownloaderRecoveryDeduplicationTest`.

---

### C63 — Регрессионный откат прогресса, ложный 100%-статус загрузки в `AlbumPicsDetails` и риск NPE в `AlbumList`. Высокая.

[AlbumPicsDetails.kt:208-270, 290-298](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumPicsDetails.kt#L208-L270), [AlbumList.kt:100-104](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumList.kt#L100-L104), [AlbumPicsDetailsProgressTest.kt:1-45](../feature-l/src/test/java/com/client/xvideos/l/net/AlbumPicsDetailsProgressTest.kt#L1-L45)

1. В `AlbumPicsDetails.appendPage` расчёт прогресса выполнялся как `percentLoad = page.page.toFloat() / pages`. Значение `page.page` — это номер поступившей страницы, а не количество загруженных страниц. Если страницы завершались не по порядку (или при ретрае упавших страниц), значение `percentLoad` скакало назад (например, с 0.6 до 0.2). Кроме того, успешная загрузка последней страницы (например, стр. 10 из 10) немедленно выставляла `percentLoad = 1.0f`, даже если промежуточные страницы завершились ошибкой. Из-за этого `bundleSnapshotOrNull()` ошибочно считал альбом полностью готовым и отдавал неполный снапшот.
2. В `contentUrls()` и `retryFailedPages()` при выходе выставлялось `percentLoad = 1f` даже при наличии незагруженных страниц в `failedPages`.
3. В `AlbumList.kt` при обработке ошибки в `getAlbumListAggregationsImpl` вызывался `Result.failure(result.exceptionOrNull()!!)`. Форсированное разыменование `!!` создавало риск падения с `NullPointerException`, если сбой произошёл без заполнения исключения.

*Исправление:*
- В `AlbumPicsDetails.appendPage` расчёт прогресса переведён на подсчёт реально загруженных страниц:
  `val successfulPages = loadedPages.count { it.value.isNotEmpty() }`
  `percentLoad = (successfulPages.toFloat() / pages.coerceAtLeast(1)).coerceIn(0f, 1f)`.
- В `contentUrls` и `retryFailedPages` значение `percentLoad = 1f` выставляется только если `failedPages.isEmpty()`.
- В `AlbumList.kt` оператор `!!` заменён на безопасный fallback: `result.exceptionOrNull() ?: IllegalStateException("Failed to load album aggregations")`.
- Добавлен юнит-тест `AlbumPicsDetailsProgressTest`.

---

### UI27 (C64) — Скрытие ошибки и блокировка повторной загрузки при смене страны или сбое пагинации в `DashboardsPaginatedListScreen`. Средняя.

[DashboardsPaginatedListScreen.kt:85-165](../feature-x/src/main/java/com/client/xvideos/x/screens/dashboards/DashboardsPaginatedListScreen.kt#L85-L165)

В `DashboardsPaginatedListScreen`:
1. Список элементов объявлялся как `val l = remember { mutableStateListOf<ItemsX>() }` без привязки к `pageIndex`. При переходе между страницами или переключении страны (`CountryState.userSelectionEpoch`) список удерживал элементы старой страницы до завершения нового сетевого запроса.
2. При возникновении сетевой ошибки (`hasError = true`) экран проверял `if (l.isEmpty())`. Поскольку в `l` оставались элементы предыдущей страницы/страны, условие не выполнялось, и рисовался обычный `DashboardsPaginatedListContent`. В результате пользователь оставался на старых данных без какого-либо визуального индикатора ошибки и без кнопки «Повторить» (`Button("Повторить")` была недоступна).

*Исправление:*
- Состояние списка привязано к странице: `val l = remember(pageIndex) { mutableStateListOf<ItemsX>() }`.
- В ветке `else` (когда элементы в списке уже есть, но произошла ошибка перезагрузки, например, при смене страны) добавлен компактный верхний оверлей-баннер: «Не удалось обновить видео» с кнопкой «Повторить», увеличивающей `retryTrigger`.

---

### UI28 (T33) — Паразитная виброотдача при композиции каждого элемента ленты и при закрытии контекстного меню в `ExpandMenuVideo`. Низкая.

[ExpandMenuVideo.kt:58-65](../feature-r/src/main/java/com/client/xvideos/r/ui/expand_menu_video/ExpandMenuVideo.kt#L58-L65)

В `ExpandMenuVideo.kt` виброотклик был реализован через:
```kotlin
LaunchedEffect(expanded) {
    haptic.invoke()
}
```
В Jetpack Compose `LaunchedEffect` выполняется сразу при первой композиции, когда `expanded == false`.
В результате:
1. При прокрутке ленты видеороликов каждая появляющаяся карточка вызывала физическую вибрацию устройства на главном потоке.
2. При закрытии меню (`expanded` переходит из `true` в `false`) виброотдача срабатывала повторно.

*Исправление:*
- Внутрь эффекта добавлена проверка `if (expanded) { haptic.invoke() }`. Вибрация срабатывает строго при открытии меню.

---

### A10 — Вечные файлы-зомби без метки времени в TTL-кэшах `FolderTable.deleteOlderThan`. Средняя.

[FolderTable.kt:73-88](../core/src/main/java/com/client/xvideos/common/fileDB/folder/FolderTable.kt#L73-L88), [FolderTableTtlOrphanTest.kt:1-50](../core/src/test/java/com/client/xvideos/common/fileDB/folder/FolderTableTtlOrphanTest.kt#L1-L50)

В `FolderTable.deleteOlderThan(timeMs)` производился обход подпапок таблицы и чтение поля `timeCreate`:
```kotlin
val timeCreate = File(rowDir, fieldFileName(FIELD_TIME_CREATE))
    .takeIf { it.exists() }
    ?.readText(Charsets.UTF_8)
    ?.toLongOrNull()

if (timeCreate != null && timeCreate < timeMs) {
    rowDir.deleteRecursively()
}
```
Если запись в кэше была повреждена или создана без поля `timeCreate`, `timeCreate` становился `null`.
При чтении через `FileStringCacheTable.get(key)` такая запись интерпретируется как имеющая время `0L` (бесконечно старая / просроченная). Однако фоновая процедура `deleteExpired()` вызывала `deleteOlderThan`, которая полностью игнорировала записи с `timeCreate == null`.
В результате повреждённые и устаревшие записи без валидного таймстемпа никогда не удалялись с диска и накапливались как неудаляемый мусор.

*Исправление:*
- Условие удаление скорректировано на `if (timeCreate == null || timeCreate < timeMs)`.
- Добавлен юнит-тест `FolderTableTtlOrphanTest`, проверяющий удаление записей без `timeCreate`.

---

### T34 — Одновременное воспроизведение в двух экземплярах ExoPlayer и фоновая утечка при навигации в `ScreenX_VideoPlayer`. Средняя.

[ScreenX_VideoPlayer.kt:80-115](../feature-x/src/main/java/com/client/xvideos/x/screens/videoPlayer/ScreenX_VideoPlayer.kt#L80-L115)

В плеере `ScreenX_VideoPlayer.kt`:
1. При переходе в полноэкранный режим кнопка вызывала `vm.openFullScreen(navigator, (host.currentTime * 1000).toLong())` без предварительной остановки воспроизведения в `host`. Полноэкранный плеер `ScreenX_VideoPlayerFullScreen` создаёт собственный `ExoPlayer`. Пока оригинальный экран находился в стеке навигатора Voyager, оба плеера декодировали один и тот же HLS-поток одновременно, нагружая CPU/GPU и расходуя мобильный трафик.
2. При клике на тег (`vm.openTag(it, navigator)`) плеер также продолжал воспроизведение на заднем плане.
3. При возврате из полноэкранного режима через `EventBus.X_FullScreenExitPosition` позиция выставлялась (`host.seekTo`), но воспроизведение автоматически не возобновлялось.

*Исправление:*
- Перед вызовом `vm.openFullScreen` и `vm.openTag` вызывается `host.pause()`.
- При получении позиции из полноэкранного режима после перемотки вызывается `host.play()` для бесшовного продолжения просмотра.

---

## Статус

| Находка | Класс | Статус | Коммит |
| --- | --- | --- | --- |
| C62 | корректность / данные | закрыт | рабочее дерево |
| C63 | корректность / расчёт | закрыт | рабочее дерево |
| UI27 (C64) | UI / состояние | закрыт | рабочее дерево |
| UI28 (T33) | UI / UX / Haptic | закрыт | рабочее дерево |
| A10 | архитектура / хранилище | закрыт | рабочее дерево |
| T34 | конкурентность / плеер | закрыт | рабочее дерево |

---

## Проверка

```bash
./gradlew detekt
# BUILD SUCCESSFUL in 3s (0 issues across all modules)

./gradlew testDebugUnitTest
# BUILD SUCCESSFUL in 11s (140 actionable tasks, 100% unit tests passed)
```

Автоматические тесты:
- `FolderTableTtlOrphanTest` — проверка вычищения записей без таймстемпа и сохранения свежих записей при TTL-ротации `FolderTable`;
- `AlbumPicsDetailsProgressTest` — проверка корректности прогресса и формирования снапшота при кэшировании/восстановлении альбомов;
- `DownloaderRecoveryDeduplicationTest` — проверка валидации путей и безопасной работы дедуплицированного загрузчика;
- Все 140 тестов проекта успешно проходят.

## Что осталось открытым

- P2P-тракт (`core/.../p2p/`) остаётся замороженным по решению владельца от 11.09.2026.
