# Код-ревью xvideos — проход 62

> **Срез:** `20e51fc` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `20e51fc`. Изменено 4 исходных файла в модуле `feature-x`.

Линзы:
- `IO` / Вынос дисковых операций `cacheUrlStringRam` на пул `Dispatchers.IO` в плеерах [ScreenX_VideoPlayerSM.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt) и [ScreenX_VideoPlayerFullScreenSM.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt).
- `UI` / Фиксация Compose stability: аннотация `@Stable` для [ScreenTagsViewModel.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/tags/ScreenTagsViewModel.kt) и `@Immutable` для [TagsModel.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/model/TagsModel.kt).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### IO44 — Блокирующие дисковые операции с cacheUrlStringRam на главном потоке плееров X. Высокая.

[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt:108-160](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt#L108)
[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt:79-125](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt#L79)

В `ScreenX_VideoPlayerSM` и `ScreenX_VideoPlayerFullScreenSM` взаимодействие с таблицей кэша `db.cacheUrlStringRam` (`get`, `put`, `delete`) вызывалось непосредственно внутри `screenModelScope.launch` (`Dispatchers.Main.immediate`), а также в методе `onPlaybackError()`. Поскольку `cacheUrlStringRam` реализован поверх `FolderTable` и читает/пишет файлы на диск (HTML страниц видео), синхронный I/O приводил к микрофризам UI-потока при открытии видео и обработке сбоев сети.

**Исправление:**
- Все вызовы `db.cacheUrlStringRam.get(url)`, `put(url, s)` и `delete(url)` обёрнуты в `withContext(Dispatchers.IO)`.
- Вызов `readHtmlFromURLDirect(url)` при промахе кэша также вынесен на `Dispatchers.IO`.
- В `onPlaybackError()` запуск корутины переведён на `screenModelScope.launch(Dispatchers.IO)`.

---

## UI76 — Отсутствие @Stable и @Immutable в компонентах тегов и экрана тегов X. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/screens/tags/ScreenTagsViewModel.kt:29](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/tags/ScreenTagsViewModel.kt#L29)
[feature-x/src/main/java/com/client/xvideos/x/model/TagsModel.kt:14-30](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/model/TagsModel.kt#L14)

`ScreenTagsViewModel` передаётся как ScreenModel в экран тегов Voyager, однако не был помечен `@Stable`, из-за чего компилятор Compose расценивал класс как нестабильный. Также модели `TagsModel` и `TagsMainUploaderPornstar` содержат коллекции (`List`), что без явной аннотации `@Immutable` приводило к пропуску оптимизации скиппинга рекомпозиции при обновлении родительских контейнеров.

**Исправление:**
- Класс `ScreenTagsViewModel` помечен аннотацией `@Stable`.
- Модели данных `TagsMainUploaderPornstar` и `TagsModel` помечены `@Immutable`.

---

## Итог верификации

- **Unit-тесты:** `./gradlew :feature-x:testDebugUnitTest` — успешно.
- **Detekt:** `./gradlew :feature-x:detekt` — 0 ошибок и предупреждений.
- **Архитектурные ограничения:** Замороженный P2P (`core/.../p2p/`) не изменялся.
