# Код-ревью xvideos — проход 103

> **Срез:** `063f8cf` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `063f8cf`. Затронут модуль `:feature-l`. В полноэкранном просмотрщике `L_FullScreenImage` обеспечено гарантированное закрытие экрана (`navigator.pop()`) даже при выбросе исключений внутри пользовательского колбэка `onClose`. Очищены многочисленные отладочные маркеры (`!!!`, `iiii`, `eee`) и понижены уровни рутинного логирования попадания/сохранения в дисковый кэш бандлов альбомов (`Timber.i` -> `Timber.d`). Функция вычисления смещения ленты миниатюр `resolveScrollIndex` сделана доступной для тестов (`internal`) и покрыта модульными тестами граничных условий в `LFullScreenPayloadTest`.

Линзы:
- `S` / Гарантированное закрытие экрана полноэкранного просмотра при исключениях в обратном вызове `onClose` ([L_FullScreenImage.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/L_FullScreenImage.kt)).
- `L` / Удаление отладочных маркеров `!!!` и понижение уровня логов рутинных операций кэша ([L_FullScreenImage.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/L_FullScreenImage.kt), [LFullScreenPage.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/LFullScreenPage.kt), [LFullScreenVideo.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/LFullScreenVideo.kt), [L_LazyRowPictureDetails.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/element/lazyRowPictureDetails/L_LazyRowPictureDetails.kt), [ScreenAlbum.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenAlbum.kt), [L_ScreenAlbumSearch.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumSearch/L_ScreenAlbumSearch.kt), [AlbumInfo.kt](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumInfo.kt), [LSavedLikeMetadata.kt](../feature-l/src/main/java/com/client/xvideos/l/featured/saved/LSavedLikeMetadata.kt)).
- `T` / Покрытие модульными тестами алгоритма центрирования миниатюр `resolveScrollIndex` ([LFullScreenPayloadTest.kt](../feature-l/src/test/java/com/client/xvideos/l/LFullScreenPayloadTest.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### S50 — Зависание экрана полноэкранного просмотра при ошибке в onClose. Средняя.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/L_FullScreenImage.kt:159](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/L_FullScreenImage.kt#L159)

В блоке `LaunchedEffect(isClosing)` при обработке закрытия экрана вызов `onClose(...)` выполнялся напрямую перед `navigator.pop()`. Если лямбда `onClose` (передаваемая вызывающим экраном для синхронизации позиции прокрутки ленты) выбрасывала непредвиденное исключение, выполнение эффекта прерывалось до вызова `navigator.pop()`, в результате чего экран полноэкранного просмотра зависал в заблокированном состоянии и не мог быть закрыт стандартной кнопкой «Назад».

**Исправление:**
- Вызов `onClose` обёрнут в `runCatching { ... }`, обеспечивая безусловный переход к `navigator.pop()`.

---

### L36 — Отладочные префиксы и избыточный уровень INFO для локального кэша. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/LFullScreenPage.kt:142](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/LFullScreenPage.kt#L142)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/LFullScreenVideo.kt:84](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/LFullScreenVideo.kt#L84)
[feature-l/src/main/java/com/client/xvideos/l/ui/element/lazyRowPictureDetails/L_LazyRowPictureDetails.kt:405](../feature-l/src/main/java/com/client/xvideos/l/ui/element/lazyRowPictureDetails/L_LazyRowPictureDetails.kt#L405)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenAlbum.kt:118](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenAlbum/ScreenAlbum.kt#L118)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumSearch/L_ScreenAlbumSearch.kt:275](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/albumSearch/L_ScreenAlbumSearch.kt#L275)
[feature-l/src/main/java/com/client/xvideos/l/net/AlbumInfo.kt:90, 99, 108, 144, 162, 164](../feature-l/src/main/java/com/client/xvideos/l/net/AlbumInfo.kt#L90)
[feature-l/src/main/java/com/client/xvideos/l/featured/saved/LSavedLikeMetadata.kt:60](../feature-l/src/main/java/com/client/xvideos/l/featured/saved/LSavedLikeMetadata.kt#L60)

В логах ошибок загрузки видео и картинок полного экрана, результатов поиска и метаданных присутствовали маркеры `!!!`, `iiii`, `eee`. Кроме того, рутинные попадания и сохранения в дисковый кэш бандлов альбомов логировались на уровне `Timber.i`, создавая шум в logcat при обычном перелистывании альбомов.

**Исправление:**
- Удалены отладочные маркеры из сообщений логгера.
- Рутинные события кэша бандлов переведены с `Timber.i` на `Timber.d`.

---

### T46 — Модульное тестирование расчёта индекса прокрутки ленты превью. Средняя.

[feature-l/src/test/java/com/client/xvideos/l/LFullScreenPayloadTest.kt](../feature-l/src/test/java/com/client/xvideos/l/LFullScreenPayloadTest.kt)
[feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/L_FullScreenImage.kt:358](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/L_FullScreenImage.kt#L358)

Функция `resolveScrollIndex` отвечает за расчёт позиции автоматической центровки нижней ленты миниатюр при смене слайда в `L_FullScreenImage`. Ранее она была `private` и не тестировалась на граничных значениях (начальные индексы, конец списка, пустой список).

**Исправление:**
- Видимость изменена на `internal`.
- В `LFullScreenPayloadTest` добавлен тест `resolveScrollIndex сдвигает позицию на 2 назад и удерживает в границах списка`, проверяющий корректность центрирования, удержание в пределах `[0, maxIndex]` и устойчивость к пустым коллекциям.

---

## Сводка верификации

- `detekt`: 0 предупреждений и ошибок в `:feature-l`.
- `testDebugUnitTest`: 100% юнит-тестов `:feature-l` зелёные (включая обновлённый `LFullScreenPayloadTest`).
