# Код-ревью xvideos — проход 57

> **Срез:** `73edff1` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `73edff1`. Изменено 5 исходных файлов в модулях `feature-r` и `feature-l`, расширены unit-тесты в `feature-r` (`DownloadedVideoKeysTest.kt`).

Линзы:
- `S` / `T` / Целостность скачанных медиафайлов: валидация размера файла перед созданием файла метаданных `.info` и отсечение 0-байтовых испорченных видео в [Downloader.kt](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt).
- `M` / Архитектурная модульность и сложность кода: декомпозиция метода `downloadMissingFiles` с выносом `enqueuePreview` и `enqueueVideo`, устраняющая цикломатическую перегруженность (`CyclomaticComplexMethod`).
- `Q` / `UI` / Оптимизация Compose Stability: добавление `@Stable` в ScreenModel-классы экранов гифок и блокировок R, а также альбомов L.

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### S38 / T28 — Создание метаданных .info и ложное завершение загрузки при 0-байтовых видео в Red Downloader. Высокая.

[feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt:108-124, 175-182, 280-305](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/common/downloader/Downloader.kt#L108)

При обрывах соединения, HTTP-ошибках без тела или сбоях файловой системы KDownloader мог сохранять пустой (0 байт) файл `.mp4`. В обоих методах загрузки (`download` и `downloadMissingFiles`):
1. В колбэке `onCompleted` создавался валидный файл метаданных `.info` (через `writeTextAtomically(AppJson.encodeToString(item))`).
2. Загрузка рапортовалась как успешная (`SnackBar.success`, `onComplete()`, `percent.value = -2f`).
3. При последующих запусках система считала видео уже скачанным, что приводило к невозможности повторно загрузить видеофайл и ошибкам воспроизведения.

**Исправление:**
- В `onCompleted` для обоих сценариев загрузки добавлена проверка `!videoFile.exists() || videoFile.length() == 0L`.
- Если файл пуст или отсутствует: битый файл удаляется, прогресс сбрасывается в ошибку (`percent.value = -3f`), пользователю выводится сообщение об ошибке, а файл `.info` не создаётся.
- В `DownloadedVideoKeysTest.kt` добавлен модульный тест фильтрации 0-байтовых файлов.
- Для предотвращения превышения цикломатической сложности метод `downloadMissingFiles` декомпозирован на приватные функции `enqueuePreview` и `enqueueVideo`.

---

### Q33 / UI73 — Стабильность Compose State для экранов R и L. Средняя.

[feature-r/src/main/java/com/client/xvideos/r/ui/manager_block/ScreenRedManageBlockSM.kt:16](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/manager_block/ScreenRedManageBlockSM.kt#L16)
[feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/gifs/R_ScreenGifsTab.kt:219](file:///g:/xvideos2/feature-r/src/main/java/com/client/xvideos/r/ui/explorer/tab/gifs/R_ScreenGifsTab.kt#L219)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenLAlbumSM.kt:42](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenLAlbumSM.kt#L42)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumListSM.kt:33](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbumList/ScreenAlbumListSM.kt#L33)

ScreenModel-классы `ScreenRedManageBlockSM`, `ScreenRedExplorerGifsSM`, `ScreenLAlbumSM` и `ScreenLAlbumListSM` передаются непосредственно в Composable-функции верхнего уровня. Из-за отсутствия явной аннотации стабильности компилятор Compose рассматривал данные типы как нестабильные параметры, отключая smart skipping при рекомпозициях соответствующих экранов и дочерних компонентов.

**Исправление:**
Все 4 класса аннотированы `@Stable`.

---

## Итог верификации

- **Unit-тесты:** `./gradlew testDebugUnitTest` — успешно (все тесты по всем 5 модулям выполнены без ошибок).
- **Detekt:** `./gradlew detekt --rerun-tasks` — 0 ошибок и предупреждений по всем 5 модулям.
- **Архитектурные ограничения:** Замороженный P2P (`core/.../p2p/`) не изменялся.
