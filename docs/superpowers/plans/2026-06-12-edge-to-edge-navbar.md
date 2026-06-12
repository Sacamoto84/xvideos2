# Edge-to-edge Button Nav Bar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Кнопочный навигационный бар всегда #212121 со светлыми кнопками на API 26–37; контент отступает от бара только на кнопочной навигации; плеер immersive; статус-бар скрыт всегда.

**Architecture:** `enableEdgeToEdge(navigationBarStyle = SystemBarStyle.dark(#212121))` в Activity красит бар и снимает contrast-scrim на API 26–34; на API 35+ полосу под прозрачным баром рисует корневой Compose-`Box` с фоном #212121, контент отступает по `WindowInsets.tappableElement` (равен высоте бара на кнопках, 0 на жестах). Выход из плеера больше не вызывает `setDecorFitsSystemWindows(true)` — edge-to-edge постоянный.

**Tech Stack:** Kotlin, Jetpack Compose, androidx.activity 1.13.0 (`enableEdgeToEdge`), androidx.core (`WindowCompat`/`WindowInsetsControllerCompat`).

**Spec:** `docs/superpowers/specs/2026-06-12-edge-to-edge-navbar-design.md`

**Testing note:** Изменения — конфигурация окна/системных баров. Автотестов на window-флаги в проекте нет, юнит-тесты к ним неприменимы. Верификация каждой задачи = успешная компиляция; финальная верификация = ручная матрица (Task 7). Запуск gradle — из корня репо, Windows: `.\gradlew.bat`.

---

### Task 1: Темы — убрать legacy fullscreen, задать цвет/вид navbar для первого кадра

**Files:**
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `app/src/main/res/values-v31/themes.xml`
- Create: `app/src/main/res/values-v27/themes.xml`

Зачем: родитель `...NoActionBar.Fullscreen` сам включает `windowFullscreen` — недостаточно удалить item, надо сменить родителя на `...NoActionBar`. `windowLightNavigationBar=false` + `navigationBarColor=#212121` в теме закрывают первый кадр до срабатывания `enableEdgeToEdge`. Квалификатор v31 перекрывает v27 **целиком** (merge по ресурсу, не по item), поэтому item'ы дублируются в v27 и v31. `windowLightNavigationBar` доступен с API 27 → в базовой `values` его нет.

- [ ] **Step 1: Заменить содержимое `app/src/main/res/values/themes.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>

    <style name="Theme.Xvideos" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:windowNoTitle">true</item>

        <!-- Статус-бар прозрачный (скрывается кодом в MainActivity) -->
        <item name="android:statusBarColor">@android:color/transparent</item>

        <!-- Кнопочный навигационный бар: тёмный до применения enableEdgeToEdge -->
        <item name="android:navigationBarColor">#212121</item>

        <!-- Чёрный фон -->
        <item name="android:windowBackground">#000000</item>

    </style>

    <style name="Theme.MyApp.Splash" parent="Theme.Material3.DayNight.NoActionBar">
        <!-- Фон сплэша -->
        <item name="android:windowBackground">@color/black</item>
        <!-- Лого/иконка -->

    </style>


</resources>
```

- [ ] **Step 2: Создать `app/src/main/res/values-v27/themes.xml`**

Полная копия стиля + `windowLightNavigationBar=false` (светлые кнопки). Splash-стиль в v27 не дублируем — его v31-версия уже существует, а на 27–30 хватает базовой.

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>

    <style name="Theme.Xvideos" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:windowNoTitle">true</item>

        <!-- Статус-бар прозрачный (скрывается кодом в MainActivity) -->
        <item name="android:statusBarColor">@android:color/transparent</item>

        <!-- Кнопочный навигационный бар: тёмный фон, светлые кнопки -->
        <item name="android:navigationBarColor">#212121</item>
        <item name="android:windowLightNavigationBar">false</item>

        <!-- Чёрный фон -->
        <item name="android:windowBackground">#000000</item>

    </style>

</resources>
```

- [ ] **Step 3: Заменить содержимое `app/src/main/res/values-v31/themes.xml`**

Splash-стиль (windowSplashScreen*) сохраняется без изменений.

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>

    <style name="Theme.Xvideos" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:windowNoTitle">true</item>

        <!-- Статус-бар прозрачный (скрывается кодом в MainActivity) -->
        <item name="android:statusBarColor">@android:color/transparent</item>

        <!-- Кнопочный навигационный бар: тёмный фон, светлые кнопки -->
        <item name="android:navigationBarColor">#212121</item>
        <item name="android:windowLightNavigationBar">false</item>

        <!-- Фон окна: виден сквозь прозрачный бар на API 35+ -->
        <item name="android:windowBackground">#212121</item>

    </style>

    <style name="Theme.MyApp.Splash" parent="Theme.Material3.DayNight.NoActionBar">
        <!-- Фон сплэша -->
        <item name="android:windowBackground">@color/black</item>
        <!-- Лого/иконка -->
        <item name="android:windowSplashScreenBackground">#252525</item>
        <item name="android:windowSplashScreenAnimatedIcon">@drawable/anim_data5</item>
        <item name="android:windowSplashScreenAnimationDuration">0</item>
    </style>

</resources>
```

- [ ] **Step 4: Проверить сборку ресурсов**

Run: `.\gradlew.bat :app:processDebugResources`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values/themes.xml app/src/main/res/values-v27/themes.xml app/src/main/res/values-v31/themes.xml
git commit -m "feat(theme): dark button nav bar, drop legacy windowFullscreen"
```

---

### Task 2: MainActivity.onCreate — enableEdgeToEdge + скрытие статус-бара

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/MainActivity.kt:134-152` (onCreate, начало)

- [ ] **Step 1: Добавить импорты в `MainActivity.kt`**

К существующим импортам добавить:

```kotlin
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
```

(`androidx.core.view.WindowCompat`, `WindowInsetsCompat`, `WindowInsetsControllerCompat` уже импортированы.)

- [ ] **Step 2: Заменить начало onCreate**

Сначала Read файл (точные байты). Текущий код (строки 135–152):

```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {

        //enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
        super.onCreate(savedInstanceState)

        val window = this.window

        val windowInsetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        windowInsetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        window?.let { WindowCompat.setDecorFitsSystemWindows(window, false) }
        window?.attributes = window.attributes?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        //windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
```

Заменить на:

```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {

        // Edge-to-edge: на 26-28 красит navigationBarColor, на 29-34 дополнительно
        // снимает contrast-scrim, на 35+ цвет задаёт приложение (корневой Box в setContent).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(0xFF212121.toInt()),
        )
        super.onCreate(savedInstanceState)

        val window = this.window

        val windowInsetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        windowInsetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Статус-бар скрыт всегда (раньше это делал windowFullscreen из темы)
        windowInsetsController?.hide(WindowInsetsCompat.Type.statusBars())

        window?.attributes = window.attributes?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
```

Удалено: ручной `setDecorFitsSystemWindows(false)` (его делает `enableEdgeToEdge`), оба закомментированных вызова.
`SystemBarStyle.dark(...)` использует `android.graphics.Color` — полное имя обязательно, в файле уже импортирован Compose `Color`.

- [ ] **Step 3: Проверить компиляцию**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/client/xvideos/MainActivity.kt
git commit -m "feat(window): enableEdgeToEdge with dark nav bar, hide status bar via insets"
```

---

### Task 3: Корень Compose — подложка #212121 + отступ по tappableElement

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/MainActivity.kt:190-226` (setContent)

- [ ] **Step 1: Добавить импорты в `MainActivity.kt`**

```kotlin
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.windowInsetsPadding
```

(Часть может уже присутствовать — добавлять только отсутствующие.)

- [ ] **Step 2: Обернуть Surface в Box-подложку и заменить паддинг**

Сначала Read файл. Текущий код (строки 190–201):

```kotlin
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .background(Color.Black)
                        .semantics { testTagsAsResourceId = true }
                    //.windowInsetsPadding(WindowInsets.ime)
                    //.consumeWindowInsets(WindowInsets.ime)
                    //.displayCutoutPadding()
                    //.systemBarsPadding())
                )
                {
```

Заменить на:

```kotlin
                // Подложка: на API 35+ системный бар прозрачный, полосу #212121 под
                // кнопками рисует это приложение. tappableElement снизу = высота
                // кнопочного бара, 0 на жестовой навигации.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF212121))
                ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.tappableElement.only(WindowInsetsSides.Bottom))
                        .background(Color.Black)
                        .semantics { testTagsAsResourceId = true }
                )
                {
```

И после закрывающей скобки этого `Surface` (строка `                }` на текущей строке 226, перед `}` блока `XvideosTheme`) добавить ещё одну закрывающую `}` для нового Box:

```kotlin
                }
                } // Box-подложка
```

Убрано: `.systemBarsPadding()` (статус-бар скрыт → верхний inset 0; нижний паддинг теперь по tappableElement — на жестах контент остаётся под полоской, на кнопках отступает). Закомментированные модификаторы удалены.

- [ ] **Step 3: Проверить компиляцию**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/client/xvideos/MainActivity.kt
git commit -m "feat(ui): nav bar backdrop + tappableElement bottom padding at compose root"
```

---

### Task 4: Плеер ScreenX_VideoPlayerFullScreen — не ломать edge-to-edge на выходе

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreen.kt:97-116`

- [ ] **Step 1: Заменить DisposableEffect входа/выхода в fullscreen**

Сначала Read файл. Текущий код (строки 97–116):

```kotlin
        DisposableEffect(Unit) {
            val activity = context.findActivityOrNull()
            val window = activity?.window
            val prevOrientation = activity?.requestedOrientation
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, false)
                WindowCompat.getInsetsController(it, it.decorView)
                    .hide(WindowInsetsCompat.Type.systemBars())
            }
            onDispose {
                activity?.requestedOrientation =
                    prevOrientation ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                window?.let {
                    WindowCompat.setDecorFitsSystemWindows(it, true)
                    WindowCompat.getInsetsController(it, it.decorView)
                        .show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
```

Заменить на:

```kotlin
        DisposableEffect(Unit) {
            val activity = context.findActivityOrNull()
            val window = activity?.window
            val prevOrientation = activity?.requestedOrientation
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            // Edge-to-edge включён глобально (MainActivity) и не перенастраивается.
            // Статус-бар скрыт глобально — прячем/возвращаем только навигацию.
            window?.let {
                WindowCompat.getInsetsController(it, it.decorView)
                    .hide(WindowInsetsCompat.Type.navigationBars())
            }
            onDispose {
                activity?.requestedOrientation =
                    prevOrientation ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                window?.let {
                    val controller = WindowCompat.getInsetsController(it, it.decorView)
                    controller.show(WindowInsetsCompat.Type.navigationBars())
                    // Страховка: статус-бар обязан остаться скрытым
                    controller.hide(WindowInsetsCompat.Type.statusBars())
                }
            }
        }
```

- [ ] **Step 2: Проверить компиляцию**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/client/xvideos/x/screens/videoplayerFullScreen/ScreenX_VideoPlayerFullScreen.kt
git commit -m "fix(player): keep edge-to-edge on fullscreen exit, toggle nav bar only"
```

---

### Task 5: util.android.kt LandscapeOrientation — то же самое

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/common/videoplayer/util/util.android.kt:56-86`

- [ ] **Step 1: Заменить reset() и LaunchedEffect**

Сначала Read файл. Текущий код (строки 56–86):

```kotlin
    fun reset() {
        if (enableFullEdgeToEdge) {
            window?.let {  WindowCompat.setDecorFitsSystemWindows(window, true) }
            window?.attributes = window.attributes?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                }
            }
        }
        windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    LaunchedEffect(isLandscape) {
        if (isLandscape) {
            if (enableFullEdgeToEdge) {
                window?.let {  WindowCompat.setDecorFitsSystemWindows(window, false) }
                window?.attributes = window.attributes?.apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
            }
            windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            reset()
        }
    }
```

Заменить на:

```kotlin
    fun reset() {
        // Возврат к глобальной конфигурации MainActivity: SHORT_EDGES, edge-to-edge
        // не выключаем (setDecorFitsSystemWindows(true) ломал прозрачные бары).
        if (enableFullEdgeToEdge) {
            window?.attributes = window.attributes?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        }
        windowInsetsController?.show(WindowInsetsCompat.Type.navigationBars())
        // Страховка: статус-бар обязан остаться скрытым
        windowInsetsController?.hide(WindowInsetsCompat.Type.statusBars())
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    LaunchedEffect(isLandscape) {
        if (isLandscape) {
            if (enableFullEdgeToEdge) {
                window?.attributes = window.attributes?.apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
            }
            windowInsetsController?.hide(WindowInsetsCompat.Type.navigationBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            reset()
        }
    }
```

Отличия от спеки (осознанные): reset() восстанавливает `SHORT_EDGES`, а не `DEFAULT` — `DEFAULT` противоречил глобальной настройке MainActivity и давал чёрную полосу у выреза после выхода из плеера. Логика "когда переключать" не изменена.

- [ ] **Step 2: Проверить компиляцию**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/client/xvideos/common/videoplayer/util/util.android.kt
git commit -m "fix(player): LandscapeOrientation keeps edge-to-edge and hidden status bar on reset"
```

---

### Task 6: Удалить мёртвый код EdgeToEdgeFix

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/MainActivity.kt` (функция `EdgeToEdgeFix`, ~строки 442-463, и закомментированный вызов `//EdgeToEdgeFix()` в setContent)

- [ ] **Step 1: Удалить функцию и закомментированный вызов**

Read файл, найти:
1. Composable-функцию `fun EdgeToEdgeFix()` с её KDoc-комментарием (`/** ... Принудительно обновляет insets...*/`) — удалить целиком.
2. Строку `//EdgeToEdgeFix()` внутри `setContent` — удалить.

Функция мертва (единственный вызов закомментирован), её логика теперь в onCreate.

- [ ] **Step 2: Удалить осиротевшие импорты**

После удаления проверить, не остались ли неиспользуемыми `androidx.compose.runtime.LaunchedEffect` и прочие — удалять только реально неиспользуемые (компилятор/IDE покажет warnings; `WindowCompat`, `WindowInsetsCompat`, `WindowInsetsControllerCompat` используются в onCreate — остаются).

- [ ] **Step 3: Проверить компиляцию**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/client/xvideos/MainActivity.kt
git commit -m "chore: remove dead EdgeToEdgeFix composable"
```

---

### Task 7: Финальная сборка и ручная матрица

- [ ] **Step 1: Полная сборка**

Run: `.\gradlew.bat :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Ручная проверка (выполняет пользователь на устройствах)**

Матрица — устройства: Android 10 кнопки; Android 16 жесты; Android 16 кнопки (режим переключить в настройках системы); устройство с челкой. На каждом:

1. Обычный экран: navbar #212121, кнопки светлые, нижние элементы UI не перекрыты кнопками; на жестах контент под полоской (без лишнего отступа).
2. Статус-бар не виден нигде.
3. Вход в полноэкранный плеер: navbar скрылся; свайп от края показывает его временно.
4. Выход из плеера: navbar вернулся #212121, статус-бар НЕ появился, контент не съехал (edge-to-edge жив, вырез обработан).
5. Поворот внутри плеера (если используется `LandscapeOrientation`): то же поведение.

- [ ] **Step 3: Итоговый коммит (если были правки по результатам)**

По результатам матрицы фиксы — отдельными коммитами.
