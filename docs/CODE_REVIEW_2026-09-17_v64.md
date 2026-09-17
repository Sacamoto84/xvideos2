# Код-ревью xvideos — проход 85

> **Срез:** `4844535` + рабочее дерево · **Статус:** закрыт полностью · **Индекс:** [все документы](README.md)

База прохода `4844535`. Изменено 5 файлов в модулях `:feature-x`, `:core` и `:feature-l`.

Линзы:
- `A` / Инкапсуляция и осмысленное именование `playerConfig` вместо публично мутабельного `a: MutableState<HTML5PlayerConfig?>` ([ScreenX_VideoPlayerSM.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt), [ScreenX_VideoPlayerFullScreenSM.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt)).
- `UI` / Фиксация ключа `host` в `remember(host)` для `sliderValue` нижней панели плеера X ([X_PlayerBottomBar.kt](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/atom/X_PlayerBottomBar.kt)).
- `UI` / Защита от устаревших замыканий коллбэков `onValueChange` и `onValueChangeFinished` в корутинах жестов `CustomSeekBar` через `rememberUpdatedState` ([CustomSeekBar.kt](../core/src/main/java/com/client/xvideos/common/videoplayer/ui/component/CustomSeekBar.kt)).
- `UI` / Миграция с устаревшего компонента `com.composeunstyled.Text` на стандартный `androidx.compose.material3.Text` ([CollectionsGrid.kt](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/CollectionsGrid.kt)).

Граница P2P (`core/.../p2p/`) заморожена по решению владельца от 11.09.2026 и не затрагивалась.

---

## Находки

### A21 — Публичное изменяемое свойство с однобуквенным именем a в ScreenX_VideoPlayerSM и ScreenX_VideoPlayerFullScreenSM. Средняя.

[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt:76](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/ScreenX_VideoPlayerSM.kt#L76)
[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt:65](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreenSM.kt#L65)

В обеих моделях видеоплеера X конфигурация плеера объявлялась как `val a: MutableState<HTML5PlayerConfig?> = mutableStateOf(HTML5PlayerConfig())`. Это создавало ненужный фиктивный экземпляр `HTML5PlayerConfig()` при старте, нарушало инкапсуляцию (позволяло внешнему коду перезаписывать состояние) и использовало неинформативное имя `a`.

**Исправление:**
- Поле переименовано в `playerConfig` и переведено на идиоматичный синтаксис делегата с приватным мутатором: `var playerConfig: HTML5PlayerConfig? by mutableStateOf(null); private set`.
- Удалены неиспользуемые импорты `MutableState`.

---

### UI107 — Отсутствие ключа host в remember для sliderValue в X_PlayerBottomBar. Низкая.

[feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/atom/X_PlayerBottomBar.kt:48](../feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/atom/X_PlayerBottomBar.kt#L48)

В `X_PlayerBottomBar` состояние `sliderValue` запоминалось без привязки к `host`: `remember { mutableFloatStateOf(0f) }`. При смене хоста или повторном использовании бара для другого видео плеер мог удерживать прогресс от предыдущего медиафайла.

**Исправление:**
- Добавлен ключ: `remember(host) { mutableFloatStateOf(0f) }`.

---

### UI108 — Захват устаревших лямбд коллбэков в pointerInput жестов CustomSeekBar. Низкая.

[core/src/main/java/com/client/xvideos/common/videoplayer/ui/component/CustomSeekBar.kt:88-115](../core/src/main/java/com/client/xvideos/common/videoplayer/ui/component/CustomSeekBar.kt#L88)

В `CustomSeekBar` блоки `detectTapGestures` и `detectDragGestures` вызывались внутри `pointerInput(maxProgress)` и напрямую захватывали `onValueChange` и `onValueChangeFinished`. Если вызывающий передавал новые экземпляры лямбд без изменения `maxProgress`, жесты продолжали вызывать устаревшие замыкания.

**Исправление:**
- Коллбэки обёрнуты в `rememberUpdatedState`: `currentOnValueChange` и `currentOnValueChangeFinished`.

---

### UI109 — Использование устаревшего com.composeunstyled.Text в сетке коллекций CollectionsGrid. Низкая.

[feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/CollectionsGrid.kt:37](../feature-l/src/main/java/com/client/xvideos/l/ui/screens/explorer/tab/saved/collection/CollectionsGrid.kt#L37)

В `CollectionsGrid.kt` заголовок и счетчик элементов карточки коллекции использовали `com.composeunstyled.Text`, помеченный в сторонней библиотеке как устаревший.

**Исправление:**
- Импорт заменен на стандартный `androidx.compose.material3.Text`.

---

## Статус

| Находка | Класс | Статус | Детали |
| --- | --- | --- | --- |
| A21 | архитектура / API | закрыт | `playerConfig` с `private set` вместо `a: MutableState` в SM плееров X |
| UI107 | UI / Compose | закрыт | Ключ `host` в `remember(host)` для `sliderValue` в `X_PlayerBottomBar` |
| UI108 | UI / Compose | закрыт | `rememberUpdatedState` для коллбэков скролла в `CustomSeekBar` |
| UI109 | UI / Compose | закрыт | Замена `com.composeunstyled.Text` на `material3.Text` в `CollectionsGrid` |

## Проверка

```
.\gradlew.bat app:detekt core:detekt feature-l:detekt feature-r:detekt feature-x:detekt
BUILD SUCCESSFUL

.\gradlew.bat testDebugUnitTest
BUILD SUCCESSFUL in 29s (140 actionable tasks)

.\gradlew.bat compileReleaseKotlin
BUILD SUCCESSFUL in 11s
```
