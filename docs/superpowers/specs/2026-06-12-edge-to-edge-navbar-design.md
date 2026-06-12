# Edge-to-edge: корректный кнопочный навигационный бар (API 26–37)

Дата: 2026-06-12
Статус: дизайн утверждён, ожидает реализации

## Проблема

Приложение работало edge-to-edge только де-факто на Android 15+/16 с жестовой
навигацией: системный бар там всегда прозрачный, и сквозь него был виден
`windowBackground` (#212121). На устройствах с кнопочной навигацией
(в т.ч. Android 10) нижний бар отображается **белым со светлым фоном**
и не управляется цветом — потому что:

1. Тема — `Theme.Material.Light.NoActionBar.Fullscreen` (светлая), а
   `android:navigationBarColor` в теме не задан вообще → система рисует
   дефолт светлой темы (белый бар, тёмные кнопки).
2. На API 29–34 поверх прозрачного бара система добавляет contrast-scrim
   (`enforceNavigationBarContrast`) — полупрозрачную белёсую подложку.
3. На API 35+ `navigationBarColor` игнорируется полностью — бар всегда
   прозрачный, красить его обязано приложение.

Сопутствующий баг: при выходе из полноэкранного плеера код вызывает
`setDecorFitsSystemWindows(true)` + `show(systemBars())`
(`ScreenX_VideoPlayerFullScreen.kt:111`, `util.android.kt:58`) — это
выключает edge-to-edge и показывает статус-бар, который должен быть
скрыт всегда.

## Требования (утверждены)

- Кнопочный навигационный бар: всегда виден на обычных экранах, цвет
  **#212121**, светлые (белые) кнопки.
- Контент отступает от бара **только** на кнопочной навигации; на жестовой
  навигации контент остаётся под полоской-pill, как сейчас.
- Полноэкранный видеоплеер: immersive — бар скрыт полностью, появляется
  свайпом и сам прячется (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`).
- Статус-бар скрыт везде и всегда (текущее поведение сохраняется).
- Поддержка: API 26–37, жесты + кнопки, устройства с челкой (cutout).

## Решение (вариант B: enableEdgeToEdge + insets в Compose)

### 1. Activity + тема

`MainActivity.onCreate` — заменить ручную настройку окна на:

```kotlin
enableEdgeToEdge(
    statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    navigationBarStyle = SystemBarStyle.dark(0xFF212121.toInt())
)
```

`enableEdgeToEdge` сам по API:
- 26–28: ставит `window.navigationBarColor = #212121`;
- 29–34: ставит цвет и выключает `isNavigationBarContrastEnforced`;
- 35+: цвет не применяет (система игнорирует), бар прозрачный;
- везде: `isAppearanceLightNavigationBars = false` (светлые кнопки),
  `setDecorFitsSystemWindows(false)`.

Оставить без изменений: `layoutInDisplayCutoutMode = SHORT_EDGES`,
`systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`,
однократный `hide(WindowInsetsCompat.Type.statusBars())` при старте.
Ручной вызов `setDecorFitsSystemWindows(false)` из onCreate убрать
(дублирует enableEdgeToEdge).

Темы:
- `values/themes.xml`, `values-v31/themes.xml`: убрать
  `android:windowFullscreen=true` (legacy-флаг, конфликтует с insets API
  на Android 10). `windowBackground` не трогать.
- Новый `values-v27/themes.xml`: полная копия стиля `Theme.Xvideos` +
  `android:windowLightNavigationBar=false` — первый кадр до применения
  `enableEdgeToEdge` не мигает тёмными кнопками на светлом баре.
  Тот же item добавить и в `values-v31/themes.xml`: квалификатор v31
  перекрывает v27 целиком (resource merging — по ресурсу, не по item).

### 2. Корень Compose

В `setContent` корневой контейнер:

```kotlin
Box(Modifier.fillMaxSize().background(Color(0xFF212121))) {
    // существующий контент
    Content(
        Modifier.windowInsetsPadding(
            WindowInsets.tappableElement.only(WindowInsetsSides.Bottom)
        )
    )
}
```

Механика `tappableElement` снизу:
- кнопочная навигация → inset = высота бара → контент отступает, под
  прозрачным баром (API 35+) видна полоса #212121 из Box;
- жестовая навигация → inset = 0 → контент под pill, как сейчас.

Детект режима навигации не нужен.

### 3. Полноэкранный плеер (2 точки)

`ScreenX_VideoPlayerFullScreen.kt` и `common/videoplayer/util/util.android.kt`:

- Вход в fullscreen: `hide(WindowInsetsCompat.Type.navigationBars())` —
  только навигация; статус-бар уже скрыт глобально.
- Выход: `show(WindowInsetsCompat.Type.navigationBars())` + повторный
  `hide(WindowInsetsCompat.Type.statusBars())` (страховка: если где-то
  был show(systemBars), статус-бар не должен остаться видимым).
- **Запрещено**: `setDecorFitsSystemWindows(true)` при выходе —
  edge-to-edge постоянный, окно не перенастраивается. Оба текущих вызова
  удалить.
- Переключения cutout-режима в `util.android.kt`: логика «когда» не
  меняется, но reset() восстанавливает `SHORT_EDGES` (глобальная
  конфигурация MainActivity), а не `DEFAULT` — иначе после выхода из
  плеера у выреза появляется чёрная полоса.

### 4. Граничные случаи

| Среда | Поведение |
|---|---|
| API 26–28, кнопки | бар #212121 через `navigationBarColor`, кнопки белые |
| API 29–34, кнопки (Android 10) | цвет + отключён contrast-scrim → ровный #212121 |
| API 35+, кнопки | бар прозрачный, полосу #212121 рисует Box (п.2) |
| Жесты, любой API | inset tappableElement = 0, контент под pill |
| Челка | существующие SHORT_EDGES + `displayCutoutPadding` не меняются |
| Поворот в плеере | бары скрыты, transient по свайпу |

### 5. Проверка

Ручная матрица, на каждом устройстве/режиме:

1. Обычный экран: бар #212121, кнопки светлые, нижний контент не перекрыт.
2. Вход/выход плеера: бар скрылся → вернулся; статус-бар не появился;
   edge-to-edge не сломался (контент по-прежнему за вырезом/полоской).
3. Поворот внутри плеера.

Устройства: Android 10 кнопки; Android 16 жесты; Android 16 кнопки
(переключить режим в настройках); устройство с челкой.

## Вне скоупа

- Динамический цвет бара под цвет панели экрана (отклонено — всегда #212121).
- Показ статус-бара.
- Изменение поведения жестовой полоски.

## Затрагиваемые файлы

- `app/src/main/java/com/client/xvideos/MainActivity.kt` (onCreate, корень setContent, EdgeToEdgeFix)
- `app/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreen.kt`
- `app/src/main/java/com/client/xvideos/common/videoplayer/util/util.android.kt`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values-v31/themes.xml`
- `app/src/main/res/values-v27/themes.xml` (новый)
