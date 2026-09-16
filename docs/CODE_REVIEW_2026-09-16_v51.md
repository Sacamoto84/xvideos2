# Код-ревью xvideos — проход 72

> **Срез:** `15fd5cb` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `15fd5cb`. Изменено 5 исходных файлов в модуле `feature-l`.

Линзы:
- `C` / Устранение расхождений в фильтрации и воспроизведении анимированных элементов в альбомах Luscious ([ScreenAlbum.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenAlbum.kt), [PicsDetailsMedia.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/model/PicsDetailsMedia.kt), [AlbumPicsDetails.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/net/AlbumPicsDetails.kt), [AlbumInfoFilterButton.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/atom/AlbumInfoFilterButton.kt)).
- `T` / Надёжный расчёт общего числа страниц альбомов L при отсутствии поля `total_pages` от API ([AlbumPicsDetails.kt](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/net/AlbumPicsDetails.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### C29 — Занижение числа элементов при фильтре «только анимированные» и потеря gif/видео в альбомах L. Высокая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenAlbum.kt:133](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenAlbum.kt#L133)
[feature-l/src/main/java/com/client/xvideos/l/model/PicsDetailsMedia.kt:14-25](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/model/PicsDetailsMedia.kt#L14)
[feature-l/src/main/java/com/client/xvideos/l/net/AlbumPicsDetails.kt:115-130](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/net/AlbumPicsDetails.kt#L115)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/atom/AlbumInfoFilterButton.kt:40-47](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/atom/AlbumInfoFilterButton.kt#L40)

При включении фильтра «показать только анимированные» в альбомах Luscious отображалось меньше элементов, чем фактически содержит альбом:
1. Фильтрация в `ScreenAlbum.kt` опиралась исключительно на примитивный флаг `it.is_animated`. В API Luscious часть анимированных медиа приходит с `is_animated = false`, но имеет заполненный `url_to_video` или расширение `.gif` в оригинальном URL.
2. Метод `normalizePictureUrls` в `AlbumPicsDetails.kt` безусловно перезаписывал `url_to_original` статической ссылкой `thumbnailUrl` (JPEG), уничтожая оригинальный gif-URL и не обновляя флаг `is_animated`.
3. В `PicsDetailsMedia.kt` метод `lAnimationVideoUrl()` имел жесткую проверку `if (!is_animated) return null`, блокируя воспроизведение видеопотока, даже если `url_to_video` присутствовал.
4. В `AlbumInfoFilterButton.kt` кнопка скрывалась по `number_of_animated_pictures == 0`, даже если в альбоме фактически имелись анимированные элементы.

**Исправление:**
- Добавлена функция-расширение `Picture.isAnimatedMedia()`, проверяющая `is_animated`, непустой `url_to_video` и gif/видео расширения в URL.
- В `PicsDetailsMedia.kt` обновлены `lAnimationVideoUrl()` и `lDownloadUrl()` для поддержки всех анимированных картинок.
- В `AlbumPicsDetails.kt` в `normalizePictureUrls` сохраняется оригинальный URL, если он указывает на анимацию, и флаг `is_animated` синхронизируется с `isAnimatedMedia()`.
- В `AlbumInfoFilterButton.kt` добавлен параметр `hasAnimatedItems`, гарантирующий доступность фильтра при наличии анимаций в загруженных элементах.
- Добавлен юнит-тест в `LPureFunctionsTest.kt`.

---

### T39 — Занижение пагинации альбомов Luscious при неполных метаданных API. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/net/AlbumPicsDetails.kt:92-95](file:///g:/xvideos2/feature-l/src/main/java/com/client/xvideos/l/net/AlbumPicsDetails.kt#L92)

В парсере `AlbumPicsDetails.parsePage` при отсутствии или нулевом значении поля `total_pages` в объекте `info` количество страниц откатывалось к `1`, даже если `total_items` и `items_per_page` содержали корректные значения (например, 50 элементов при 20 на страницу означают 3 страницы). Это приводило к преждевременной остановке пагинации и недозагрузке последующих анимированных картинок.

**Исправление:**
- В `parsePage` вычисление страниц расширено: `val calculatedPages = if (itemsPerPage > 0 && totalItems > 0) ceil(totalItems.toDouble() / itemsPerPage).toInt() else 1`, и общее число страниц определяется как `max(explicitPages, calculatedPages)`.
