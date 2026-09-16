# Код-ревью xvideos — проход 74

> **Срез:** `7411484` + рабочее дерево · **Статус:** закрыт · **Индекс:** [все документы](README.md)

База прохода `7411484`. Изменено 2 исходных файла в модуле `feature-x`.

Линзы:
- `T` / Инкапсуляция внутреннего состояния плеера и гигиена свойств ScreenModel ([ScreenX_VideoPlayerSM.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt), [ScreenX_VideoPlayer.kt](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayer.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### T40 — Отсутствие private set на полях плеера в ScreenX_VideoPlayerSM и прямая мутация состояния из UI. Низкая.

[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt:66-82](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt#L66)
[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayer.kt:99](file:///g:/xvideos2/feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayer.kt#L99)

В `ScreenX_VideoPlayerSM` поля состояния `passedHLS`, `tags` и `positionFromFullscreen` были объявлены без спецификатора `private set`. Это нарушало инкапсуляцию модели экрана и позволяло внешним компонентам бесконтрольно переопределять URL видеопотока или модель тегов. В `ScreenX_VideoPlayer.kt` сброс позиции воспроизведения после возврата из полноэкранного режима производился прямой мутацией свойства `vm.positionFromFullscreen = -1L`.

**Исправление:**
- Полям `passedHLS`, `tags` и `positionFromFullscreen` добавлен спецификатор `private set`.
- В `ScreenX_VideoPlayerSM` добавлен метод `consumePositionFromFullscreen()`, инкапсулирующий сброс позиции.
- В `ScreenX_VideoPlayer.kt` прямой вызов заменён на `vm.consumePositionFromFullscreen()`.
