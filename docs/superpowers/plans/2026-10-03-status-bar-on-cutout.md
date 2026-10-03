# Статус-бар на устройствах с вырезом — план реализации

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** На устройствах с вырезом камеры показывать системный статус-бар с полупрозрачной подложкой на всех экранах, кроме полноэкранных (просмотр картинок L, лента R, развёрнутый плеер X).

**Architecture:** Правило видимости — чистая функция в `core`. Полноэкранные экраны подают заявку «скрыть» через эффект со счётчиком. Один Composable в корне `app` применяет правило к окну и рисует подложку. Разбросанные `hide(statusBars())` удаляются.

**Tech Stack:** Kotlin, Jetpack Compose (material3 1.5 alpha), `WindowInsetsControllerCompat`, JUnit4.

Спецификация: [2026-10-03-status-bar-on-cutout-design.md](../specs/2026-10-03-status-bar-on-cutout-design.md)

Коммиты в плане не расписаны: владелец коммитит по своей команде. Перед коммитом — `gradlew.bat testDebugUnitTest detekt` (Задача 7).

> **Отклонение при выполнении (2026-10-03).** `object StatusBarPolicy` из задач
> 1, 2, 4, 5 не прошёл `GlobalStateTest` (новая изменяемая глобальная точка).
> Сделано иначе: класс `StatusBarRequests` + `LocalStatusBarRequests`
> (`core/.../common/ui/statusbar/StatusBarRequests.kt`, тест
> `StatusBarRequestsTest.kt`); экземпляр создаёт `MainActivity` через `remember`
> и раздаёт `CompositionLocalProvider`; `StatusBarHost` получает `hideRequests`
> параметром. Код в задачах ниже — исходный вариант плана; действующий — в
> спецификации (разделы 3.2–3.3) и в самих файлах. Все задачи выполнены.

---

## Файлы

| Файл | Действие | Ответственность |
|---|---|---|
| `core/src/main/java/com/client/xvideos/common/ui/statusbar/StatusBarPolicy.kt` | создать | правило `shouldShowStatusBar` и счётчик заявок |
| `core/src/main/java/com/client/xvideos/common/ui/statusbar/HideStatusBarEffect.kt` | создать | эффект-заявка «скрыть бар» |
| `core/src/test/java/com/client/xvideos/common/ui/statusbar/StatusBarPolicyTest.kt` | создать | тест правила и счётчика |
| `core/src/main/java/com/client/xvideos/common/util/getTopInsetDp.kt` | изменить | отступ = max(вырез, статус-бар) |
| `app/src/main/java/com/client/xvideos/screenRoot/molecule/StatusBarHost.kt` | создать | применяет правило, рисует подложку |
| `app/src/main/java/com/client/xvideos/MainActivity.kt` | изменить | подключает `StatusBarHost`, убирает безусловное скрытие |
| `feature-l/.../screenFullScreen/L_FullScreenImage.kt` | изменить | заявка «скрыть» |
| `feature-r/.../fullscreen/ScreenRedFullScreen.kt` | изменить | заявка «скрыть» |
| `feature-x/.../videoplayer/atom/OrientationAndSystemBarsEffect.kt` | изменить | заявка «скрыть» при развороте, без прямых `hide(statusBars())` |
| `core/.../common/videoplayer/util/util.android.kt` | изменить | убрать `hide(statusBars())` из `LandscapeOrientation` |
| `core/.../common/ui/atom/CutoutPlate.kt` + 11 файлов L | удалить / откатить | убрать градиент |

---

### Задача 1: Правило и счётчик заявок (`core`)

**Files:**
- Create: `core/src/main/java/com/client/xvideos/common/ui/statusbar/StatusBarPolicy.kt`
- Test: `core/src/test/java/com/client/xvideos/common/ui/statusbar/StatusBarPolicyTest.kt`

- [ ] **Шаг 1: написать падающий тест**

```kotlin
package com.client.xvideos.common.ui.statusbar

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StatusBarPolicyTest {

    @Before
    @After
    fun reset() {
        StatusBarPolicy.resetForTesting()
    }

    @Test
    fun `вырез есть и заявок нет - бар виден`() {
        assertTrue(shouldShowStatusBar(hasTopCutout = true, hideRequests = 0))
    }

    @Test
    fun `выреза нет - бар скрыт при любом числе заявок`() {
        assertFalse(shouldShowStatusBar(hasTopCutout = false, hideRequests = 0))
        assertFalse(shouldShowStatusBar(hasTopCutout = false, hideRequests = 1))
    }

    @Test
    fun `есть заявка - бар скрыт даже с вырезом`() {
        assertFalse(shouldShowStatusBar(hasTopCutout = true, hideRequests = 1))
        assertFalse(shouldShowStatusBar(hasTopCutout = true, hideRequests = 2))
    }

    @Test
    fun `счётчик держит бар скрытым пока жива хоть одна заявка`() {
        StatusBarPolicy.acquireHide()
        StatusBarPolicy.acquireHide()
        StatusBarPolicy.releaseHide()
        assertEquals(1, StatusBarPolicy.hideRequests)
        StatusBarPolicy.releaseHide()
        assertEquals(0, StatusBarPolicy.hideRequests)
    }

    @Test
    fun `лишний release не уводит счётчик в минус`() {
        StatusBarPolicy.releaseHide()
        assertEquals(0, StatusBarPolicy.hideRequests)
    }
}
```

- [ ] **Шаг 2: убедиться, что тест не компилируется**

Run: `gradlew.bat :core:testDebugUnitTest --tests "*StatusBarPolicyTest"`
Expected: FAIL, `Unresolved reference: StatusBarPolicy`.

- [ ] **Шаг 3: реализация**

```kotlin
package com.client.xvideos.common.ui.statusbar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * Виден ли системный статус-бар.
 *
 * @param hasTopCutout У устройства есть вырез камеры сверху: бар занимает его полосу.
 * @param hideRequests Сколько экранов в композиции попросили скрыть бар.
 */
fun shouldShowStatusBar(hasTopCutout: Boolean, hideRequests: Int): Boolean =
    hasTopCutout && hideRequests == 0

/**
 * Заявки полноэкранных экранов на скрытие статус-бара.
 *
 * Счётчик, а не флаг: при переходе между двумя полноэкранными экранами новый
 * входит в композицию раньше, чем старый её покидает.
 */
object StatusBarPolicy {

    /** Число живых заявок. Наблюдаемое: корень пересчитывает видимость бара. */
    var hideRequests: Int by mutableIntStateOf(0)
        private set

    fun acquireHide() {
        hideRequests += 1
    }

    fun releaseHide() {
        hideRequests = (hideRequests - 1).coerceAtLeast(0)
    }

    fun resetForTesting() {
        hideRequests = 0
    }
}
```

- [ ] **Шаг 4: тест проходит**

Run: `gradlew.bat :core:testDebugUnitTest --tests "*StatusBarPolicyTest"`
Expected: PASS, 5 тестов.

---

### Задача 2: Эффект-заявка (`core`)

**Files:**
- Create: `core/src/main/java/com/client/xvideos/common/ui/statusbar/HideStatusBarEffect.kt`

- [ ] **Шаг 1: создать файл**

```kotlin
package com.client.xvideos.common.ui.statusbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * Заявка полноэкранного экрана: пока он в композиции, статус-бар скрыт.
 */
@Composable
fun HideStatusBarEffect() {
    DisposableEffect(Unit) {
        StatusBarPolicy.acquireHide()
        onDispose { StatusBarPolicy.releaseHide() }
    }
}
```

---

### Задача 3: Отступ сверху учитывает статус-бар (`core`)

**Files:**
- Modify: `core/src/main/java/com/client/xvideos/common/util/getTopInsetDp.kt`

- [ ] **Шаг 1: заменить KDoc и тело `getTopInsetDp()`**

```kotlin
/**
 * Верхний отступ экрана: большее из выреза камеры (displayCutout) и статус-бара.
 *
 * Статус-бар виден только на устройствах с вырезом и не на полноэкранных экранах
 * (см. `StatusBarPolicy`); когда он скрыт, его инсет равен 0 и остаётся вырез.
 * На устройствах без выреза камеры (например, Samsung S7) возвращает 0.dp.
 *
 * ```
 * val topInset = getTopInsetDp()
 *
 * Box(
 *     modifier = Modifier
 *         .fillMaxSize()
 *         .padding(top = topInset)
 * )
 * ```
 */
@Composable
fun getTopInsetDp(): Dp {
    val density = LocalDensity.current
    return with(density) {
        maxOf(
            WindowInsets.displayCutout.getTop(this),
            WindowInsets.statusBars.getTop(this),
        ).toDp()
    }
}
```

`getStatusBarInsetDp()` ниже в файле не менять.

---

### Задача 4: Хозяин статус-бара в корне (`app`)

**Files:**
- Create: `app/src/main/java/com/client/xvideos/screenRoot/molecule/StatusBarHost.kt`

- [ ] **Шаг 1: создать файл**

```kotlin
package com.client.xvideos.screenRoot.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.statusbar.StatusBarPolicy
import com.client.xvideos.common.ui.statusbar.shouldShowStatusBar

/** Непрозрачность подложки: контент под баром просвечивает, значки читаются. */
private const val STATUS_BAR_SCRIM_ALPHA = 0.6f

/**
 * Единственный хозяин системного статус-бара.
 *
 * Считает, виден ли бар (вырез сверху есть и ни один экран не просил его скрыть),
 * сообщает это окну через [onVisibleChange] и рисует под видимым баром
 * полупрозрачную подложку цвета фона.
 */
@Composable
fun StatusBarHost(
    onVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasTopCutout = WindowInsets.displayCutout.getTop(LocalDensity.current) > 0
    val visible = shouldShowStatusBar(hasTopCutout, StatusBarPolicy.hideRequests)

    LaunchedEffect(visible) { onVisibleChange(visible) }

    if (visible) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(Theme.background.copy(alpha = STATUS_BAR_SCRIM_ALPHA))
        )
    }
}

@Preview
@Composable
private fun StatusBarHostPreview() {
    StatusBarHost(onVisibleChange = {})
}
```

---

### Задача 5: `MainActivity` — подключить хозяина, убрать безусловное скрытие

**Files:**
- Modify: `app/src/main/java/com/client/xvideos/MainActivity.kt`

- [ ] **Шаг 1: поле и метод применения**

Рядом с `private var isAppMinimized by mutableStateOf(false)` добавить:

```kotlin
    /** Последнее решение `StatusBarHost`: переприменяется в `onResume`. */
    private var statusBarVisible = false

    private fun applyStatusBarVisibility(visible: Boolean) {
        statusBarVisible = visible
        val currentWindow = window ?: return
        val controller = WindowCompat.getInsetsController(currentWindow, currentWindow.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (visible) {
            controller.show(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.statusBars())
        }
    }

    /**
     * Есть ли вырез сверху — до первой композиции, чтобы бар не мигал при запуске.
     */
    @Suppress("DEPRECATION")
    private fun hasTopCutoutAtStart(): Boolean {
        val cutout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.cutout
        } else {
            windowManager.defaultDisplay.cutout
        }
        return (cutout?.safeInsetTop ?: 0) > 0
    }
```

- [ ] **Шаг 2: `onResume` — переприменить решение вместо скрытия**

Было:

```kotlin
        window?.let {
            val controller = WindowCompat.getInsetsController(it, it.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.statusBars())
        }
```

Стало:

```kotlin
        applyStatusBarVisibility(statusBarVisible)
```

- [ ] **Шаг 3: `configureWindow` — начальное состояние по вырезу**

Было:

```kotlin
        val windowInsetsController = WindowCompat.getInsetsController(currentWindow, currentWindow.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
```

Стало:

```kotlin
        applyStatusBarVisibility(hasTopCutoutAtStart())
```

- [ ] **Шаг 4: подключить `StatusBarHost` в `setContent`**

Было:

```kotlin
                                ScreenRoot.Content()
                                P2pBackgroundOverlay()
```

Стало:

```kotlin
                                ScreenRoot.Content()
                                P2pBackgroundOverlay()
                                StatusBarHost(onVisibleChange = ::applyStatusBarVisibility)
```

Импорт: `import com.client.xvideos.screenRoot.molecule.StatusBarHost`.

- [ ] **Шаг 5: компиляция**

Run: `gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

---

### Задача 6: Заявки полноэкранных экранов и уборка скрытий

**Files:**
- Modify: `feature-l/src/main/java/com/client/xvideos/l/ui/screens/screenFullScreen/L_FullScreenImage.kt`
- Modify: `feature-r/src/main/java/com/client/xvideos/r/ui/fullscreen/ScreenRedFullScreen.kt`
- Modify: `feature-x/src/main/java/com/client/xvideos/x/screens/videoplayer/atom/OrientationAndSystemBarsEffect.kt`
- Modify: `core/src/main/java/com/client/xvideos/common/videoplayer/util/util.android.kt`

- [ ] **Шаг 1: L — первая строка `L_FullScreenImage.Content()`**

```kotlin
    override fun Content() {
        HideStatusBarEffect()
```

Импорт: `import com.client.xvideos.common.ui.statusbar.HideStatusBarEffect`.

- [ ] **Шаг 2: R — первая строка `ScreenRedFullScreen.Content()`**

```kotlin
    override fun Content() {
        HideStatusBarEffect()
        val navigator = LocalNavigator.currentOrThrow
```

Импорт тот же.

- [ ] **Шаг 3: X — `OrientationAndSystemBarsEffect`**

В начало функции, до `val context`:

```kotlin
    // Развёрнутый плеер — полноэкранный экран: статус-бар скрыт, пока он развёрнут.
    if (isFullScreen) {
        HideStatusBarEffect()
    }
```

Удалить все четыре строки `controller.hide(WindowInsetsCompat.Type.statusBars())`.
Строки с `navigationBars()` и ориентацией не трогать. KDoc функции:

```kotlin
/**
 * Управляет ориентацией экрана (альбомная/портретная) и нижней панелью навигации.
 * В полноэкранном режиме прячет панель навигации и подаёт заявку на скрытие
 * статус-бара; при выходе или закрытии экрана возвращает стандартные настройки.
 */
```

- [ ] **Шаг 4: `LandscapeOrientation` — убрать страховку**

Удалить из `reset()`:

```kotlin
        // Страховка: статус-бар обязан остаться скрытым
        windowInsetsController?.hide(WindowInsetsCompat.Type.statusBars())
```

- [ ] **Шаг 5: в коде не осталось прямых скрытий статус-бара**

Run: `grep -rn "statusBars())" --include=*.kt app/src/main core/src/main feature-x/src/main feature-r/src/main feature-l/src/main`
Expected: только две строки в `MainActivity.applyStatusBarVisibility`.

---

### Задача 7: Откат градиента и общая проверка

**Files:**
- Delete: `core/src/main/java/com/client/xvideos/common/ui/atom/CutoutPlate.kt`
- Revert: 11 файлов L с `CutoutPlate` / `cutoutPlate`

- [ ] **Шаг 1: убедиться, что в 11 файлах только правки плашки**

Run: `git diff -U0 -- feature-l | grep "^[+-]" | grep -vi "cutout\|getTopInsetDp"`
Expected: только перестановка `LikesFilterSegmentedRow` в `Column`, строки-распорки и импорты `Column` / `fillMaxWidth` / `Spacer`, плюс правки `ScreenAlbumList.kt` (полоса загрузки — их не откатывать).

- [ ] **Шаг 2: откатить 11 файлов и удалить атом**

```bash
B=feature-l/src/main/java/com/client/xvideos/l/ui/screens
git checkout -- \
  $B/albumLandingTag/molecule/LandingTagTopBar.kt \
  $B/explorer/tab/albumSearch/molecule/AlbumSearchInputField.kt \
  $B/explorer/tab/saved/albums/L_ScreenSavedAlbumsTab.kt \
  $B/explorer/tab/saved/collection/LCollectionsTopBar.kt \
  $B/explorer/tab/saved/collection/molecule/LCollectionDetailTopBar.kt \
  $B/explorer/tab/saved/likes/molecule/LikesFilterSegmentedRow.kt \
  $B/explorer/tab/saved/serverLikes/L_ScreenServerLikesTab.kt \
  $B/explorer/tab/saved/subscribedAlbums/molecule/SubscribedAlbumsGrid.kt \
  $B/screenAlbum/molecule/ScreenLAlbumBody.kt \
  $B/screenAlbumList/molecule/AlbumListPageGrid.kt \
  $B/screenAlbumList/molecule/filter/atom/AlbumListFilterHeader.kt
rm core/src/main/java/com/client/xvideos/common/ui/atom/CutoutPlate.kt
```

- [ ] **Шаг 3: все тесты и detekt**

Run: `gradlew.bat testDebugUnitTest detekt`
Expected: BUILD SUCCESSFUL.

- [ ] **Шаг 4: проверка на устройстве** — сценарии из раздела 5 спецификации.
