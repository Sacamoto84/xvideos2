# Код-ревью xvideos — проход 56

> **Срез:** `c56bcc5` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `c56bcc5`. Изменено 4 исходных файла в модулях `core`, `feature-l`, `feature-x`, расширены unit-тесты в `feature-l` (`LPureFunctionsTest.kt`).

Линзы:
- `UI` / `C` / Корректность навигации и позиционирования в StaggeredGrid: учёт смещения заголовков (`itemBefore` и индикатора начальной загрузки) при возврате позиции из полноэкранного режима в [L_LazyRowPictureDetails.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/element/lazyRowPictureDetails/L_LazyRowPictureDetails.kt).
- `T` / Асинхронная отмена загрузок: вынос блокирующего удаления временных файлов при отмене приостановленных запросов в [DownloadDispatchers.kt](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadDispatchers.kt) на фоновый `dbScope`.
- `Q` / `UI` / Оптимизация Compose Stability: аннотирование `@Stable` моделей экранов плеера X в [ScreenX_VideoPlayerSM.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt) и [ScreenX_VideoPlayerFullScreenSM.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt) для активации smart skipping.

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### UI71 / C133 — Смещение позиции скролла при выходе из полноэкранного просмотра альбома L. Высокая.

[feature-l/src/main/java/com/client/xvideos/l/ui/element/lazyRowPictureDetails/L_LazyRowPictureDetails.kt:180-188](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/element/lazyRowPictureDetails/L_LazyRowPictureDetails.kt#L180)

В сетке `LazyVerticalStaggeredGrid` списка картинок альбома перед основными элементами всегда размещается заголовок (`itemBefore`), а также опциональный лоадер (`InitialPictureItemsLoading()`). При закрытии полноэкранного просмотра `L_FullScreenImage` возвращал индекс картинки `position` в исходном списке `filteredPic`, и вызывался `host.state.scrollToItem(position)` без учета заголовочных слотов.
1. При закрытии первой картинки (`position = 0`) скролл производился на заголовок (индекс 0), а не на саму картинку.
2. Для всех остальных картинок происходил сдвиг на 1 или 2 элемента назад.
3. Отсутствовала проверка валидности диапазона при возможном изменении списка во время нахождения в полноэкранном режиме.

**Исправление:**
- Вынесена функция `calculateGridScrollIndex(position: Int, itemCount: Int, showInitialLoading: Boolean): Int?`.
- Применяется корректное смещение `headerOffset = if (showInitialLoading) 2 else 1`. Если индекс выходит за границы `0 until itemCount`, скролл отклоняется (`null`).
- В `LPureFunctionsTest.kt` добавлены модульные тесты для обоих режимов и граничных условий.

---

### T27 — Синхронное удаление временных файлов на вызывающем потоке при отмене KDownloader. Средняя.

[core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadDispatchers.kt:75-82](file:///g:/xvideos2/core/src/main/java/com/client/xvideos/common/kdownloader/internal/DownloadDispatchers.kt#L75)

В методе `DownloadDispatchers.cancel(req)` при статусе запроса `Status.PAUSED` выполнялось синхронное удаление временного файла `File(tempPath).delete()` непосредственно в вызывающем потоке (который часто является UI/Main-потоком). В случае больших файлов или задержек файловой системы это приводило к микродропам кадров или риску ANR.

**Исправление:**
Операция удаления файла `tempFile.delete()` перенесена в существующий фоновый контекст `dbScope.launch`, выполняющий очистку базы данных `dbHelper.remove(req.downloadId)`.

---

### Q32 / UI72 — Стабильность Compose State для видеоплееров раздела X. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt:48](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt#L48)
[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt:38](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt#L38)

Классы `ScreenX_VideoPlayerSM` и `ScreenX_VideoPlayerFullScreenSM` содержат реактивные состояния Compose (`passedHLS`, `isError`, `isLoading`, `tags`, `positionFromFullscreen`), передаваемые в Composable-функции экранов. Отсутствие явной аннотации `@Stable` препятствовало компилятору Compose оптимизировать пропуск повторных рекомпозиций (smart skipping) при обновлении несвязанных свойств родительского скоупа.

**Исправление:**
Оба класса аннотированы `@Stable`.

---

## Итог верификации

- **Unit-тесты:** `./gradlew testDebugUnitTest` — успешно (все тесты по всем 5 модулям выполнены без ошибок).
- **Detekt:** `./gradlew detekt --rerun-tasks` — 0 ошибок и предупреждений по всем 5 модулям.
- **Архитектурные ограничения:** Замороженный P2P (`core/.../p2p/`) не изменялся.
