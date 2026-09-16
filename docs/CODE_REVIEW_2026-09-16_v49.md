# Код-ревью xvideos — проход 70

> **Срез:** `6f484e0` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `6f484e0`. Изменено 6 исходных файлов в модуле `feature-x`.

Линзы:
- `C` / Корректность потока управления и оптимизация разбора адресов превью в [parserVideoPreviewFromImageUrl.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/parcer/parserVideoPreviewFromImageUrl.kt) (ранний возврат при отсутствии сегмента `videos`, устранение мёртвого кода и избыточных вычислений hash/folders), модульные тесты в [XParsersTest.kt](file:///g:/xvideos2/feature-x/src/test/java/com/client/xvideos/x/parcer/XParsersTest.kt).
- `UI` / Инкапсуляция мутабельного состояния плеера в [ScreenX_VideoPlayerFullScreenSM.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt) (`passedString` с `private set`), предотвращение гонок мутаций избранного через `mutationJob` в [SavedX_Favorites.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Favorites.kt), фиксация стабильности Compose-контрактов (`@Stable`) для менеджеров хранилища в [SavedX.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX.kt), [SavedX_Favorites.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Favorites.kt) и [SavedX_Downloads.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### C56 — Избыточные вычисления и мёртвый код при разборе CDN URL в parserVideoPreviewFromImageUrl. Низкая.

[feature-x/src/main/java/com/client/xvideos/x/parcer/parserVideoPreviewFromImageUrl.kt:37-54](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/parcer/parserVideoPreviewFromImageUrl.kt#L37)

В функции `parserVideoPreviewFromImageUrl` поиск индекса сегмента `val videosIndex = parts.indexOf("videos")` происходил перед извлечением имени файла, вычислением хэша и формированием промежуточного списка папок `folders`. При этом проверка `if (videosIndex < 0) return null` находилась ниже всех этих операций, а условие в `val folders = if (videosIndex >= 0 ...)` дублировало проверку неотрицательности индекса. При невалидном URL без сегмента `videos` выполнялись бессмысленные операции со строками и регулярными выражениями.

**Исправление:**
- Проверка `if (videosIndex < 0) return null` вынесена наверх сразу после определения `videosIndex`.
- Условие формирования папок упрощено до `if (parts.size > videosIndex + 4)`.
- В `XParsersTest` добавлены тесты парсера на валидные legacy и modern CDN-адреса, а также на пустые/некорректные ссылки.

---

### T37 — Открытый setter свойства passedString в ScreenX_VideoPlayerFullScreenSM. Низкая.

[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt:56](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt#L56)

Свойство `passedString`, содержащее полученный HLS/MP4 URL видеопотока, имело публичный сеттер `var passedString: String by mutableStateOf("")`. Это нарушало инкапсуляцию ScreenModel и допускало непреднамеренную перезапись состояния экрана из UI-компонентов.

**Исправление:**
- Добавлен модификатор `private set`, аналогично другим свойствам экрана (`isLoading`, `isError`).

---

### UI84 — Нестабильные контракты SavedX-классов и риск гонок при частом переключении избранного. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX.kt:9](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX.kt#L9)
[feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Favorites.kt:15](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Favorites.kt#L15)
[feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt:40](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/feature/saved/SavedX_Downloads.kt#L40)

Классы `SavedX`, `SavedX_Favorites` и `SavedX_Downloads` передаются в Composable-функции экранов и дашбордов раздела X. Без аннотации `@Stable` Compose компилятор считает эти зависимости нестабильными, что приводит к избыточным рекомпозициям.
Кроме того, в `SavedX_Favorites` последовательные вызовы `add` и `remove` запускали асинхронные корутины без отслеживания и отмены предыдущих незавершённых операций записи в БД, что при быстром нажатии создавало гонку дисковых операций.

**Исправление:**
- Добавлена аннотация `@Stable` к `SavedX`, `SavedX_Favorites` и `SavedX_Downloads`.
- В `SavedX_Favorites` добавлено отслеживание `mutationJob` с отменой предыдущей корутины перед запуском новой операции `add` или `remove`.
