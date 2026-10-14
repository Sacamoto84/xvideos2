# Раскладка на раскрытых складных телефонах — план реализации

> **Для исполнителя:** обязательный навык — superpowers:subagent-driven-development
> (рекомендуется) или superpowers:executing-plans; задачи выполняются по
> порядку, шаги отмечаются чекбоксами (`- [ ]`).

**Цель:** на внутреннем экране складного телефона сетки получают больше
колонок, одноколоночный контент не шире 640dp по центру, клавиатура
калькулятора помещается в окно; на телефоне разметка не меняется.

**Архитектура:** чистые правила (класс ширины окна, число колонок, отступы,
ширина клавиатуры) — функции без Compose с JVM-тестами. Тонкий слой
`@Composable` читает ширину окна из `LocalWindowInfo` и отдаёт готовые
значения. `Modifier.adaptiveContentWidth()` ограничивает ширину только тогда,
когда родитель шире лимита, а иначе ничего не меняет. Сторож в `app/arch`
запрещает возврат к жёстким колонкам и к ширине экрана.

**Стек:** Kotlin, Jetpack Compose (UI 1.13.0-alpha01 по факту в APK,
`LocalWindowInfo.containerDpSize`), Voyager, JUnit 4.

**Спека:** [docs/superpowers/specs/2026-10-14-foldable-adaptive-layout-design.md](../specs/2026-10-14-foldable-adaptive-layout-design.md)

## Глобальные ограничения

- Классы ширины: Compact < 600dp, Medium 600–839dp, Expanded ≥ 840dp.
- Жёсткие сетки: 2 / 3 / 4 колонки. Пользовательский выбор: + 0 / + 1 / + 2.
  Горизонтальные ряды L: 3 / 4 / 5 карточек.
- Лимит одноколоночного контента — 640dp (`CONTENT_MAX_WIDTH_DP`), только
  когда родитель шире.
- На Compact разметка не меняется ни на пиксель: хелперы на Compact
  возвращают прежние значения или ничего не делают.
- Ширина окна — `LocalWindowInfo.current.containerDpSize.width`, округлённая
  вниз до целого dp (так она совпадает с прежним `screenWidthDp`).
- Названия сайтов не пишутся нигде: только X / L / R.
- Молекулы и атомы не принимают ScreenModel; у каждого файла в `atom/` и
  `molecule/` остаётся минимальное `@Preview`.
- Перед каждым коммитом — гейт целиком (Приложение Б), в отчёт — фактический
  результат.
- Коммиты — в `master`, сообщения на русском (Conventional Commits),
  последняя строка `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
  Перед первым коммитом серии спросить владельца; пуш делает владелец.
- Коммитить только свои пути (`git add -- <пути>`, затем
  `git commit -F <scratchpad>\msg.txt -- <пути>`; файл сообщения — в каталоге
  scratchpad сессии, не в репозитории), никогда `-a`: у владельца в рабочей копии
  лежат свои правки (`.idea/…`, `gradle/libs.versions.toml`,
  `GifsTabBottomBar.kt`).
- P2P (отправка и приём) и «Поделиться» на устройстве не проверяются.

## Отступления от спеки

1. **Сторож не запрещает `screenHeightDp`** (спека, 5.2). Высота экрана в
   проекте нужна только как доля для максимальной высоты (пять диалогов
   фильтра L и панель фильтра альбомов). От ширины окна она не зависит, а
   замена на высоту окна изменила бы панель на телефонах с Android 10–14, где
   `screenHeightDp` не включает системные панели. Запрещены ширина экрана и
   ориентация.
2. **Панель фильтра альбомов L сохраняет `screenHeightDp`** (спека, 3.3 а) —
   по той же причине. Ограничивается только её ширина.
3. **Без изменений остаются выпадающий список стран X (`ComposeCountry`) и
   подсказки поиска R (`SuggestionList`).** Оба не на всю ширину окна: первый —
   всплывающее меню 312dp, второй — внутри поля поиска. Отступ от ширины окна
   был бы для них неверным. Спека (3.3, последний абзац) разрешает плану
   уточнять набор файлов.
4. **Сторож дополнительно запрещает `GridItemSpan(<число>)`.** У актрис X
   строки состояния заданы как `GridItemSpan(2)` и при 3–4 колонках заняли бы
   часть строки.
5. **Форма экрана замка (`AppLockScreen`) ограничивается 420dp** — раздел 3.4
   спеки называется «Калькулятор и экран замка», а поля замка сейчас
   растянуты на всю ширину.
6. **`keypadWidthDp` и его тест — в `:app`, а не в `:core`** (спека, 5.1):
   калькулятор живёт в `:app`, правило относится только к нему. Сигнатура —
   с классом ширины и возвратом `null` на Compact: так на телефоне клавиатура
   не получает ограничитель, который из-за округления dp мог сдвинуть её на
   пиксель.
7. **Ширина окна — из `LocalWindowInfo.containerDpSize`**, не из
   `currentWindowAdaptiveInfo()` (спека, 3.1 оставляла выбор плану): API есть в
   Compose UI 1.13 и задокументирован ровно для точек перелома, `material3-adaptive`
   для этого не нужен.

## Что проверить на ревью (Review Focus)

1. **Телефон не задет.** Любой хелпер на Compact обязан вернуть прежнее
   значение: ширину окна — как `screenWidthDp`, `adaptiveListPadding(base)` —
   тот же `base`, `adaptiveContentWidth()` — пропуск без изменений, ширину
   клавиатуры — `null`. Тесты: задача 1 (`plusSides` с нулём возвращает тот же
   объект, отступ ≤ 640 равен 0), задача 10 (`keypadWidthDp` на Compact —
   `null`), задача 11 (сверка снимков с эталоном на телефоне и внешнем экране).
2. **Раскрытие посреди прокрутки.** Число колонок меняется, а позиция
   прокрутки держится по индексу элемента, не по ряду. Проверка: задача 11,
   сценарий 4 (X — избранное, R — лента, L — лайки).
3. **Устаревшие колонки в `derivedStateOf`.** `rememberActiveItemRange` у L
   получает число колонок параметром — оно обязано быть ключом `remember`.
   Проверка: задача 5, шаг на эмуляторе (после раскрытия догружаются фото за
   прежним буфером).
4. **Имя автора в карточке R** зависит от выбора пользователя, а не от
   эффективного числа колонок: карточка при выборе 2 на Expanded размером как
   на телефоне. Проверка: задача 5, шаг на эмуляторе.
5. **Фон шапок и панелей на всю ширину.** Ограничитель стоит в цепочке после
   фона; если фон рисует вложенный элемент, ограничитель ставится внутри него.
   Проверка: задачи 8 и 9, шаги на эмуляторе (по краям нет полос другого
   цвета).

---

## Структура файлов

Новые файлы (`core/src/main/java/com/client/xvideos/common/ui/adaptive/`):

| файл | ответственность |
|---|---|
| `WidthClass.kt` | `enum WidthClass`, границы, `widthClassOf` |
| `AdaptiveRules.kt` | `gridColumns`, `userGridColumns`, `rowVisibleItems`, `CONTENT_MAX_WIDTH_DP`, `contentSidePaddingDp` |
| `SideInsetPadding.kt` | `PaddingValues.plusSides(side)` |
| `AdaptiveContentWidth.kt` | `Modifier.adaptiveContentWidth(maxWidth)`, `centeredOffsetPx` |
| `WindowWidth.kt` | `@Composable`: `currentWindowWidthDp`, `currentWidthClass`, `adaptiveGridColumns`, `adaptiveUserGridColumns`, `rowItemWidth`, `adaptiveListPadding` |

Тесты: `core/src/test/java/com/client/xvideos/common/ui/adaptive/AdaptiveRulesTest.kt`,
`app/src/test/java/com/client/xvideos/arch/AdaptiveLayoutTest.kt`,
`app/src/test/java/com/client/xvideos/calculator/KeypadWidthTest.kt`.

Прочие новые: `app/src/main/java/com/client/xvideos/calculator/KeypadWidth.kt`.

Изменяемые файлы перечислены в задачах. Пути в задачах — от
`<модуль>/src/main/java/com/client/xvideos/`.

---

### Задача 1. Основа в `:core`

**Файлы:**
- Создать: `core/…/common/ui/adaptive/WidthClass.kt`, `AdaptiveRules.kt`,
  `SideInsetPadding.kt`, `AdaptiveContentWidth.kt`, `WindowWidth.kt`
- Тест: `core/src/test/java/com/client/xvideos/common/ui/adaptive/AdaptiveRulesTest.kt`

**Интерфейсы:**
- Потребляет: ничего.
- Производит (пакет `com.client.xvideos.common.ui.adaptive`):
  - `enum class WidthClass { Compact, Medium, Expanded }`
  - `fun widthClassOf(widthDp: Int): WidthClass`
  - `fun gridColumns(widthClass: WidthClass): Int`
  - `fun userGridColumns(selected: Int, widthClass: WidthClass): Int`
  - `fun rowVisibleItems(widthClass: WidthClass): Int`
  - `const val CONTENT_MAX_WIDTH_DP: Int = 640`
  - `fun contentSidePaddingDp(containerWidthDp: Int): Int`
  - `fun PaddingValues.plusSides(side: Dp): PaddingValues`
  - `fun centeredOffsetPx(availablePx: Int, contentPx: Int): Int`
  - `fun Modifier.adaptiveContentWidth(maxWidth: Dp = CONTENT_MAX_WIDTH_DP.dp): Modifier`
  - `@Composable fun currentWindowWidthDp(): Int`
  - `@Composable fun currentWidthClass(): WidthClass`
  - `@Composable fun adaptiveGridColumns(): Int`
  - `@Composable fun adaptiveUserGridColumns(selected: Int): Int`
  - `@Composable fun rowItemWidth(gap: Dp = 8.dp): Dp`
  - `@Composable fun adaptiveListPadding(base: PaddingValues = PaddingValues(0.dp)): PaddingValues`

- [ ] **Шаг 1. Написать падающий тест**

`core/src/test/java/com/client/xvideos/common/ui/adaptive/AdaptiveRulesTest.kt`:

```kotlin
package com.client.xvideos.common.ui.adaptive

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AdaptiveRulesTest {

    @Test
    fun `границы классов ширины - 600 и 840dp`() {
        assertEquals(WidthClass.Compact, widthClassOf(0))
        assertEquals(WidthClass.Compact, widthClassOf(599))
        assertEquals(WidthClass.Medium, widthClassOf(600))
        assertEquals(WidthClass.Medium, widthClassOf(839))
        assertEquals(WidthClass.Expanded, widthClassOf(840))
        assertEquals(WidthClass.Expanded, widthClassOf(883))
    }

    @Test
    fun `жёсткие сетки - 2, 3 и 4 колонки`() {
        assertEquals(2, gridColumns(WidthClass.Compact))
        assertEquals(3, gridColumns(WidthClass.Medium))
        assertEquals(4, gridColumns(WidthClass.Expanded))
    }

    @Test
    fun `пользовательский выбор плюс запас широкого окна`() {
        // R выбирает 2..4, L — из разрешённых в настройках, крайние 1 и 5.
        listOf(1, 2, 3, 4, 5).forEach { selected ->
            assertEquals(selected, userGridColumns(selected, WidthClass.Compact))
            assertEquals(selected + 1, userGridColumns(selected, WidthClass.Medium))
            assertEquals(selected + 2, userGridColumns(selected, WidthClass.Expanded))
        }
    }

    @Test
    fun `видимых карточек горизонтального ряда - 3, 4 и 5`() {
        assertEquals(3, rowVisibleItems(WidthClass.Compact))
        assertEquals(4, rowVisibleItems(WidthClass.Medium))
        assertEquals(5, rowVisibleItems(WidthClass.Expanded))
    }

    @Test
    fun `боковой отступ появляется только шире лимита`() {
        assertEquals(0, contentSidePaddingDp(411))
        assertEquals(0, contentSidePaddingDp(640))
        assertEquals(0, contentSidePaddingDp(641))
        assertEquals(30, contentSidePaddingDp(700))
        assertEquals(106, contentSidePaddingDp(852))
    }

    @Test
    fun `смещение по центру не бывает отрицательным`() {
        assertEquals(200, centeredOffsetPx(availablePx = 1000, contentPx = 600))
        assertEquals(0, centeredOffsetPx(availablePx = 600, contentPx = 600))
        assertEquals(0, centeredOffsetPx(availablePx = 500, contentPx = 600))
    }

    @Test
    fun `нулевой боковой отступ возвращает тот же PaddingValues`() {
        val base = PaddingValues(top = 8.dp)
        assertSame(base, base.plusSides(0.dp))
    }

    @Test
    fun `боковой отступ прибавляется слева и справа, верх и низ прежние`() {
        val padded = PaddingValues(start = 4.dp, top = 8.dp, end = 6.dp, bottom = 10.dp).plusSides(30.dp)
        assertEquals(34.dp, padded.calculateLeftPadding(LayoutDirection.Ltr))
        assertEquals(36.dp, padded.calculateRightPadding(LayoutDirection.Ltr))
        assertEquals(8.dp, padded.calculateTopPadding())
        assertEquals(10.dp, padded.calculateBottomPadding())
    }
}
```

- [ ] **Шаг 2. Запустить и убедиться, что не компилируется**

PowerShell:
`cmd /c ".\gradlew.bat :core:testDebugUnitTest --tests *AdaptiveRulesTest* > build\t1.log 2>&1"; "exit=$LASTEXITCODE"`

Ожидается: `exit=1`, в `build\t1.log` — `Unresolved reference 'widthClassOf'`.

- [ ] **Шаг 3. Правила и класс ширины**

`core/…/common/ui/adaptive/WidthClass.kt`:

```kotlin
package com.client.xvideos.common.ui.adaptive

/**
 * Класс ширины окна. Границы — стандартные для Material: 600 и 840dp.
 *
 * Compact — телефон и внешний экран складного; Medium и Expanded — внутренний
 * экран складного в портрете и в альбомной ориентации. Вся адаптация
 * раскладки включается только начиная с [Medium].
 */
enum class WidthClass { Compact, Medium, Expanded }

const val MEDIUM_MIN_WIDTH_DP = 600
const val EXPANDED_MIN_WIDTH_DP = 840

fun widthClassOf(widthDp: Int): WidthClass = when {
    widthDp >= EXPANDED_MIN_WIDTH_DP -> WidthClass.Expanded
    widthDp >= MEDIUM_MIN_WIDTH_DP -> WidthClass.Medium
    else -> WidthClass.Compact
}
```

`core/…/common/ui/adaptive/AdaptiveRules.kt`:

```kotlin
package com.client.xvideos.common.ui.adaptive

/** Колонки сетки, у которой на телефоне их две. */
fun gridColumns(widthClass: WidthClass): Int = when (widthClass) {
    WidthClass.Compact -> 2
    WidthClass.Medium -> 3
    WidthClass.Expanded -> 4
}

/**
 * Колонки сетки, где число выбирает пользователь: выбор плюс запас широкого
 * окна. Выбор хранится как есть — прибавка только при отрисовке.
 */
fun userGridColumns(selected: Int, widthClass: WidthClass): Int = selected + when (widthClass) {
    WidthClass.Compact -> 0
    WidthClass.Medium -> 1
    WidthClass.Expanded -> 2
}

/** Сколько карточек горизонтального ряда видно целиком. */
fun rowVisibleItems(widthClass: WidthClass): Int = when (widthClass) {
    WidthClass.Compact -> 3
    WidthClass.Medium -> 4
    WidthClass.Expanded -> 5
}

/** Наибольшая ширина одноколоночного контента: списков, шапок, рядов панелей. */
const val CONTENT_MAX_WIDTH_DP = 640

/** Отступ списка с каждой стороны: 0, пока контейнер не шире [CONTENT_MAX_WIDTH_DP]. */
fun contentSidePaddingDp(containerWidthDp: Int): Int =
    ((containerWidthDp - CONTENT_MAX_WIDTH_DP) / 2).coerceAtLeast(0)
```

`core/…/common/ui/adaptive/SideInsetPadding.kt`:

```kotlin
package com.client.xvideos.common.ui.adaptive

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Тот же отступ, но с прибавкой [side] слева и справа.
 *
 * Нулевая прибавка возвращает сам [this] — на телефоне список получает ровно
 * прежний `contentPadding`.
 */
fun PaddingValues.plusSides(side: Dp): PaddingValues =
    if (side <= 0.dp) this else SideInsetPadding(this, side)

private class SideInsetPadding(
    private val base: PaddingValues,
    private val side: Dp,
) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        base.calculateLeftPadding(layoutDirection) + side

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        base.calculateRightPadding(layoutDirection) + side

    override fun calculateTopPadding(): Dp = base.calculateTopPadding()

    override fun calculateBottomPadding(): Dp = base.calculateBottomPadding()

    override fun equals(other: Any?): Boolean =
        other is SideInsetPadding && other.base == base && other.side == side

    override fun hashCode(): Int = 31 * base.hashCode() + side.hashCode()
}
```

`core/…/common/ui/adaptive/AdaptiveContentWidth.kt`:

```kotlin
package com.client.xvideos.common.ui.adaptive

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Содержимое не шире [maxWidth] и по центру — только если родитель шире.
 *
 * Пока родитель не шире лимита (телефон, внешний экран складного), модификатор
 * ничего не меняет. Когда шире — узел занимает всю ширину родителя, а
 * содержимое меряется не шире [maxWidth] и ставится по центру. Поэтому фон,
 * заданный в цепочке **до** этого модификатора, остаётся на всю ширину.
 */
fun Modifier.adaptiveContentWidth(maxWidth: Dp = CONTENT_MAX_WIDTH_DP.dp): Modifier =
    layout { measurable, constraints ->
        val capPx = maxWidth.roundToPx()
        if (!constraints.hasBoundedWidth || constraints.maxWidth <= capPx) {
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        } else {
            val placeable = measurable.measure(
                constraints.copy(minWidth = minOf(constraints.minWidth, capPx), maxWidth = capPx)
            )
            layout(constraints.maxWidth, placeable.height) {
                placeable.place(centeredOffsetPx(constraints.maxWidth, placeable.width), 0)
            }
        }
    }

/** Отступ слева, при котором [contentPx] стоит по центру [availablePx]. */
fun centeredOffsetPx(availablePx: Int, contentPx: Int): Int =
    ((availablePx - contentPx) / 2).coerceAtLeast(0)
```

`core/…/common/ui/adaptive/WindowWidth.kt`:

```kotlin
package com.client.xvideos.common.ui.adaptive

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified

/**
 * Ширина окна в целых dp, округлённая вниз — как `Configuration.screenWidthDp`,
 * поэтому на телефоне в портрете значения совпадают.
 *
 * Окно, а не экран: в разделённом экране раскрытого складного окно уже экрана.
 * Без окна (редкие превью) — ширина из конфигурации, как было до адаптации.
 */
@Composable
fun currentWindowWidthDp(): Int {
    val width = LocalWindowInfo.current.containerDpSize.width
    return if (width.isSpecified) width.value.toInt() else LocalConfiguration.current.screenWidthDp
}

@Composable
fun currentWidthClass(): WidthClass = widthClassOf(currentWindowWidthDp())

/** Колонки сетки, у которой на телефоне их две. */
@Composable
fun adaptiveGridColumns(): Int = gridColumns(currentWidthClass())

/** Колонки сетки с выбором пользователя: [selected] плюс запас широкого окна. */
@Composable
fun adaptiveUserGridColumns(selected: Int): Int = userGridColumns(selected, currentWidthClass())

/** Ширина карточки горизонтального ряда: (окно − [gap]) / видимых карточек. */
@Composable
fun rowItemWidth(gap: Dp = 8.dp): Dp {
    val widthDp = currentWindowWidthDp()
    return (widthDp.dp - gap) / rowVisibleItems(widthClassOf(widthDp))
}

/**
 * `contentPadding` списка на всю ширину окна: [base] плюс боковой отступ,
 * который держит строки не шире [CONTENT_MAX_WIDTH_DP]. Список при этом
 * свайпается по всей ширине. На телефоне возвращает сам [base].
 *
 * Только для списков на всю ширину окна: отступ считается от окна, а не от
 * родителя.
 */
@Composable
fun adaptiveListPadding(base: PaddingValues = PaddingValues(0.dp)): PaddingValues =
    base.plusSides(contentSidePaddingDp(currentWindowWidthDp()).dp)
```

- [ ] **Шаг 4. Тест проходит**

`cmd /c ".\gradlew.bat :core:testDebugUnitTest --tests *AdaptiveRulesTest* > build\t1.log 2>&1"; "exit=$LASTEXITCODE"`

Ожидается: `exit=0`; в `core/build/test-results/testDebugUnitTest/TEST-com.client.xvideos.common.ui.adaptive.AdaptiveRulesTest.xml` — `tests="8" failures="0" errors="0"`.

- [ ] **Шаг 5. Гейт и коммит**

Гейт — Приложение Б. Затем:

```bash
git add -- core/src/main/java/com/client/xvideos/common/ui/adaptive core/src/test/java/com/client/xvideos/common/ui/adaptive
git commit -F "<scratchpad>/msg.txt" -- core/src/main/java/com/client/xvideos/common/ui/adaptive core/src/test/java/com/client/xvideos/common/ui/adaptive
```

Остальные задачи коммитят так же: `git add -- <пути задачи>`, затем
`git commit -F "<scratchpad>/msg.txt" -- <пути задачи>`.

Сообщение:

```
feat(core): класс ширины окна и правила адаптивной раскладки

- WidthClass: Compact < 600dp, Medium до 840dp, Expanded от 840dp.
- Колонки жёстких сеток 2 / 3 / 4, пользовательский выбор + 0 / 1 / 2,
  горизонтальные ряды 3 / 4 / 5 карточек.
- Лимит одноколоночного контента 640dp: Modifier.adaptiveContentWidth()
  и adaptiveListPadding(); на окне не шире лимита они ничего не меняют.
- Ширина окна из LocalWindowInfo, округлённая вниз, как screenWidthDp.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
```

---

### Задача 2. Сторож адаптивной раскладки

**Файлы:**
- Изменить: `app/src/test/java/com/client/xvideos/arch/ProjectSources.kt`
  (добавить `kotlinSourcesWithoutComments()`)
- Изменить: `app/src/test/java/com/client/xvideos/arch/AutofillExclusionTest.kt`
  (брать исходники из `ProjectSources`)
- Создать: `app/src/test/java/com/client/xvideos/arch/AdaptiveLayoutTest.kt`

**Интерфейсы:**
- Потребляет: путь пакета `common/ui/adaptive/` из задачи 1 (исключение).
- Производит: `ProjectSources.kotlinSourcesWithoutComments(): Sequence<Pair<String, String>>`;
  `AdaptiveLayoutTest.BASELINE` — каждая следующая задача удаляет из него
  свои строки.

- [ ] **Шаг 1. Перенести чтение исходников в `ProjectSources`**

В `ProjectSources.kt` внутрь `object ProjectSources` после
`fun File.invariantPath()` добавить:

```kotlin
    /**
     * Исходники всех модулей: пары «путь от `com/client/xvideos` — код без комментариев».
     *
     * Комментарии описывают прежний код и сами запрещённые вызовы — считать их
     * нельзя. Блочные убираются целиком, а не по первому символу строки: вызов,
     * закомментированный внутри такого блока, иначе сошёл бы за настоящий.
     */
    fun kotlinSourcesWithoutComments(): Sequence<Pair<String, String>> =
        roots().flatMap { root ->
            root.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .map { file -> file.relativeTo(root).invariantPath() to file.codeWithoutComments() }
        }

    private fun File.codeWithoutComments(): String =
        readText()
            .replace(BLOCK_COMMENT, "")
            .lineSequence()
            .filterNot { it.trimStart().startsWith("//") }
            .joinToString("\n")

    // Блочный комментарий, начатый с начала строки. Внутри строки его
    // открывающая пара символов встречается и в коде — в MIME-типах со
    // звёздочкой, — и вырезать от неё до ближайшего закрытия значило бы
    // выбросить настоящий код вместе с вызовами.
    private val BLOCK_COMMENT = Regex(
        """^[ \t]*/\*.*?\*/""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE),
    )
```

В `AutofillExclusionTest.kt` заменить функцию `sources()` и
`codeWithoutComments()` (вместе с их KDoc) на:

```kotlin
    /** Пары «путь от `com/client/xvideos` — код без комментариев». */
    private fun sources(): Sequence<Pair<String, String>> = ProjectSources.kotlinSourcesWithoutComments()
```

и удалить из `companion object` блок `BLOCK_COMMENT` вместе с комментарием
над ним. Удалить ставшие лишними импорты
`com.client.xvideos.arch.ProjectSources.invariantPath` и `java.io.File`.

- [ ] **Шаг 2. Сторож автозаполнения по-прежнему зелёный**

`cmd /c ".\gradlew.bat :app:testDebugUnitTest --tests *AutofillExclusionTest* > build\t2.log 2>&1"; "exit=$LASTEXITCODE"`

Ожидается: `exit=0`, 4 теста, 0 падений.

- [ ] **Шаг 3. Написать сторож с BASELINE текущих нарушений**

`app/src/test/java/com/client/xvideos/arch/AdaptiveLayoutTest.kt`:

```kotlin
package com.client.xvideos.arch

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Сторож адаптивной раскладки.
 *
 * На раскрытом складном телефоне окно 700–880dp в ширину, и сетка с жёсткими
 * двумя колонками растягивает карточки вдвое. Колонки, ширина карточек ряда и
 * ширина контента берутся из `common/ui/adaptive/`; тест не даёт новой сетке
 * или ряду снова завязаться на число или на размер экрана.
 *
 * Высота экрана (`screenHeightDp`) не запрещена: доля высоты в диалогах и в
 * панели фильтра от ширины окна не зависит.
 *
 * Тест падает в обе стороны: и на новом нарушении, и на записи [BASELINE],
 * которая больше не нарушается.
 */
class AdaptiveLayoutTest {

    @Test
    fun `сетки и ряды берут колонки и ширину из common-ui-adaptive`() {
        val found = ProjectSources.kotlinSourcesWithoutComments()
            .filterNot { (path, _) -> path.startsWith(ADAPTIVE_PACKAGE) }
            .flatMap { (path, code) ->
                RULES.asSequence()
                    .filter { (_, pattern) -> pattern.containsMatchIn(code) }
                    .map { (name, _) -> "$path: $name" }
            }
            .toSortedSet()

        assertEquals(
            "Сетка или ряд снова завязаны на жёсткое число колонок или на размер экрана. " +
                "Колонки — adaptiveGridColumns() / adaptiveUserGridColumns(), ширина — " +
                "currentWindowWidthDp() / rowItemWidth(), полная строка — GridItemSpan(maxLineSpan). " +
                "Запись, которая больше не нарушается, уберите из BASELINE.",
            BASELINE,
            found,
        )
    }

    private companion object {
        const val ADAPTIVE_PACKAGE = "common/ui/adaptive/"

        val RULES = listOf(
            "колонки числом" to Regex("""\b(?:Staggered)?GridCells\.Fixed\(\s*\d"""),
            "строка сетки числом" to Regex("""\bGridItemSpan\(\s*\d"""),
            "ширина экрана" to Regex("""\bscreenWidthDp\b"""),
            "ориентация экрана" to Regex("""\bConfiguration\.ORIENTATION_(?:LANDSCAPE|PORTRAIT)\b"""),
        )

        /** Ещё не исправленные места. Каждая задача плана убирает свои строки. */
        val BASELINE = sortedSetOf(
            "common/collectionDB/ui/CollectionsGrid.kt: колонки числом",
            "l/ui/screens/albumLandingTag/ScreenLAlbumLandingTag.kt: ширина экрана",
            "l/ui/screens/explorer/tab/albumSearch/L_ScreenAlbumSearch.kt: ширина экрана",
            "l/ui/screens/explorer/tab/albumTopHits/L_ScreenAlbumTopHits.kt: ширина экрана",
            "l/ui/screens/explorer/tab/saved/albums/L_ScreenSavedAlbumsTab.kt: колонки числом",
            "l/ui/screens/explorer/tab/saved/collection/CollectionsGrid.kt: колонки числом",
            "l/ui/screens/explorer/tab/saved/subscribedAlbums/molecule/SubscribedAlbumsGrid.kt: колонки числом",
            "l/ui/screens/screenAlbumList/molecule/AlbumListPageGrid.kt: колонки числом",
            "x/screens/actresses/molecule/ActressesIndexGrid.kt: колонки числом",
            "x/screens/actresses/molecule/ActressesIndexGrid.kt: строка сетки числом",
            "x/screens/channel/molecule/ChannelVideosPager.kt: колонки числом",
            "x/screens/dashboards/molecule/DashboardsPaginatedListContent.kt: колонки числом",
            "x/screens/favorites/molecule/FavoritesContent.kt: колонки числом",
            "x/screens/history/molecule/HistoryGrid.kt: колонки числом",
            "x/screens/tags/molecule/TagsPaginatedListScreen.kt: ориентация экрана",
        )
    }
}
```

- [ ] **Шаг 4. Сторож зелёный на текущем коде**

`cmd /c ".\gradlew.bat :app:testDebugUnitTest --tests *AdaptiveLayoutTest* > build\t2.log 2>&1"; "exit=$LASTEXITCODE"`

Ожидается: `exit=0`. Если тест красный, то сообщение `assertEquals` показывает
фактический набор. Сверить его с поиском (Grep по
`GridCells\.Fixed\(\s*\d|GridItemSpan\(\s*\d|screenWidthDp|Configuration\.ORIENTATION_`
в `*/src/main`) и привести `BASELINE` к фактическому: это уже существующие
нарушения, а не новые.

- [ ] **Шаг 5. Сторож ловит новое нарушение**

Две временные правки:
- в `core/…/common/ui/TabRow.kt` (его нет в BASELINE) дописать строку-комментарий
  `// GridCells.Fixed(3)`;
- в `feature-x/…/x/screens/favorites/molecule/FavoritesContent.kt`, строка
  `item(key = "header", … span = { GridItemSpan(maxLineSpan) })`, заменить
  `GridItemSpan(maxLineSpan)` на `GridItemSpan(1)` (код должен компилироваться:
  тест собирает модули).

Прогнать тест из шага 4: ожидается красный, в фактическом наборе новая
запись `x/screens/favorites/molecule/FavoritesContent.kt: строка сетки числом`
и **нет** записи про `common/ui/TabRow.kt`. Откатить обе правки
(`git checkout -- <оба файла>`), прогнать ещё раз — зелёный.

- [ ] **Шаг 6. Гейт и коммит**

Гейт — Приложение Б. Коммит путей
`app/src/test/java/com/client/xvideos/arch/ProjectSources.kt`,
`app/src/test/java/com/client/xvideos/arch/AutofillExclusionTest.kt`,
`app/src/test/java/com/client/xvideos/arch/AdaptiveLayoutTest.kt`:

```
test(arch): сторож жёстких колонок и размеров экрана в сетках

AdaptiveLayoutTest запрещает GridCells.Fixed(<число>),
GridItemSpan(<число>), screenWidthDp и Configuration.ORIENTATION_ вне
common/ui/adaptive/. Текущие места — в BASELINE, их убирают задачи
адаптации под складные телефоны.

Чтение исходников без комментариев переехало из AutofillExclusionTest в
ProjectSources, чтобы оба сторожа считали код одинаково.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
```

---

### Задача 3. Сетки X

**Файлы** (`feature-x/src/main/java/com/client/xvideos/x/screens/`):
- Изменить: `favorites/molecule/FavoritesContent.kt:102`
- Изменить: `history/molecule/HistoryGrid.kt:34`
- Изменить: `channel/molecule/ChannelVideosPager.kt:138`
- Изменить: `dashboards/molecule/DashboardsPaginatedListContent.kt:39`
- Изменить: `actresses/molecule/ActressesIndexGrid.kt:46,52,63,86,113,129,135`
- Изменить: `tags/molecule/TagsPaginatedListScreen.kt:3,25,81-82`
- Тест: `app/src/test/java/com/client/xvideos/arch/AdaptiveLayoutTest.kt` (BASELINE)

**Интерфейсы:**
- Потребляет: `adaptiveGridColumns(): Int` (задача 1).

- [ ] **Шаг 0. Эталон телефона до правок UI**

Это первая задача, которая меняет UI, поэтому до правок снимается эталон
(Приложение А, «Эталон»): телефонный эмулятор `emulator-5554` и внешний экран
`Pixel_10_Pro_Fold`. Снимки и дампы — в подкаталог `baseline\` каталога
scratchpad своей сессии; в репозиторий они не идут.

- [ ] **Шаг 1. Убрать записи X из BASELINE — сторож краснеет**

Удалить из `AdaptiveLayoutTest.BASELINE` семь строк, начинающихся с `x/`.
Прогнать сторож (команда — задача 2, шаг 4). Ожидается: красный, в
фактическом наборе — эти семь записей.

- [ ] **Шаг 2. Колонки сеток**

В `FavoritesContent.kt`, `HistoryGrid.kt`, `ChannelVideosPager.kt`,
`DashboardsPaginatedListContent.kt`, `ActressesIndexGrid.kt` заменить

```kotlin
columns = GridCells.Fixed(2),
```

на

```kotlin
columns = GridCells.Fixed(adaptiveGridColumns()),
```

и добавить импорт `import com.client.xvideos.common.ui.adaptive.adaptiveGridColumns`.

- [ ] **Шаг 3. Полные строки у актрис**

В `ActressesIndexGrid.kt` все шесть `item(span = { GridItemSpan(2) }) {`
заменить на `item(span = { GridItemSpan(maxLineSpan) }) {`.
Иначе при 3–4 колонках строка загрузки, ошибки и подгрузки займёт две
ячейки, а не всю строку.

- [ ] **Шаг 4. Ряды тега по ширине окна**

В `TagsPaginatedListScreen.kt` заменить

```kotlin
    val orientation = LocalConfiguration.current.orientation
    val itemsPerRow = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 2
```

на

```kotlin
    val itemsPerRow = adaptiveGridColumns()
```

Удалить импорты `android.content.res.Configuration` и
`androidx.compose.ui.platform.LocalConfiguration`, добавить
`import com.client.xvideos.common.ui.adaptive.adaptiveGridColumns`.
`remember(loaded, itemsPerRow) { loaded.chunked(itemsPerRow) }` остаётся:
`itemsPerRow` — ключ, при раскрытии ряды пересобираются.

- [ ] **Шаг 5. Сторож зелёный, тесты X зелёные**

`cmd /c ".\gradlew.bat :app:testDebugUnitTest --tests *AdaptiveLayoutTest* :feature-x:testDebugUnitTest --continue > build\t3.log 2>&1"; "exit=$LASTEXITCODE"`

Ожидается: `exit=0`. `ScreenX_ChannelSMPageTest` известен как
нестабильный: при его падении — перезапуск (Приложение Б).

- [ ] **Шаг 6. Эмулятор**

Приложение А, «Сборка и установка», затем на внутреннем экране
`Pixel_10_Pro_Fold`:
- X → избранное, история, дашборды, канал, актрисы, страница тега:
  4 колонки в портрете и в альбомной;
- актрисы: индикатор первой загрузки по центру во всю строку;
- внешний экран (сложить): 2 колонки, как в эталоне.

- [ ] **Шаг 7. Гейт и коммит**

Гейт — Приложение Б. Коммит путей задачи (шесть файлов X и
`AdaptiveLayoutTest.kt`):

```
feat(x): сетки X получают колонки по ширине окна

Избранное, история, канал, дашборды, актрисы и страница тега: 2 колонки
на телефоне, 3 — на окне от 600dp, 4 — от 840dp (раскрытый складной).
Полные строки сетки актрис — GridItemSpan(maxLineSpan) вместо двух
ячеек. Ряды страницы тега считаются от ширины окна, а не от ориентации.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
```

---

### Задача 4. Сетки L и общие коллекции

**Файлы:**
- Изменить: `feature-l/…/l/ui/screens/screenAlbumList/molecule/AlbumListPageGrid.kt:55`
- Изменить: `feature-l/…/l/ui/screens/explorer/tab/saved/albums/L_ScreenSavedAlbumsTab.kt:113`
- Изменить: `feature-l/…/l/ui/screens/explorer/tab/saved/collection/CollectionsGrid.kt:64`
- Изменить: `feature-l/…/l/ui/screens/explorer/tab/saved/subscribedAlbums/molecule/SubscribedAlbumsGrid.kt:37`
- Изменить: `core/…/common/collectionDB/ui/CollectionsGrid.kt:95`
- Тест: `AdaptiveLayoutTest.kt` (BASELINE)

**Интерфейсы:**
- Потребляет: `adaptiveGridColumns(): Int` (задача 1).

- [ ] **Шаг 1. Убрать пять записей из BASELINE — сторож краснеет**

Удалить строки `common/collectionDB/ui/CollectionsGrid.kt: колонки числом`
и четыре строки `l/…: колонки числом` (сохранённые альбомы, коллекции,
подписки, список альбомов). Прогнать сторож: красный, ровно эти пять записей.

- [ ] **Шаг 2. Колонки**

`AlbumListPageGrid.kt:55`:

```kotlin
        LazyVerticalGrid(state = stateGrid, modifier = Modifier.fillMaxSize(), columns = GridCells.Fixed(2)) {
```

→

```kotlin
        LazyVerticalGrid(
            state = stateGrid,
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Fixed(adaptiveGridColumns())
        ) {
```

`L_ScreenSavedAlbumsTab.kt:113`, `SubscribedAlbumsGrid.kt:37`:
`columns = GridCells.Fixed(2),` → `columns = GridCells.Fixed(adaptiveGridColumns()),`.

`l/…/collection/CollectionsGrid.kt:64` и `core/…/CollectionsGrid.kt:95`:
`columns = GridCells.Fixed(2)` → `columns = GridCells.Fixed(adaptiveGridColumns())`.

Во все пять файлов — импорт
`import com.client.xvideos.common.ui.adaptive.adaptiveGridColumns`.

- [ ] **Шаг 3. Сторож и тесты модулей**

`cmd /c ".\gradlew.bat :app:testDebugUnitTest --tests *AdaptiveLayoutTest* :feature-l:testDebugUnitTest :core:testDebugUnitTest --continue > build\t4.log 2>&1"; "exit=$LASTEXITCODE"`

Ожидается: `exit=0` (`KDownloaderQueueTest` в `:core` нестабилен —
Приложение Б).

- [ ] **Шаг 4. Эмулятор**

Внутренний экран: L → список альбомов, сохранённые альбомы, коллекции,
подписки; R → сохранённое → коллекции (общий `CollectionsGrid` из core):
4 колонки. Внешний экран — 2.

- [ ] **Шаг 5. Гейт и коммит**

```
feat(l): сетки альбомов и коллекций по ширине окна

Список альбомов, сохранённые альбомы, коллекции и подписки L, а также
общая сетка коллекций из core: 2 колонки на телефоне, 3 — от 600dp,
4 — от 840dp.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
```

---

### Задача 5. Сетки с выбором пользователя (R и L)

**Файлы:**
- Изменить: `feature-r/…/r/ui/ui/lazyrow123/LazyRow123.kt:106-110,255`
- Изменить: `feature-l/…/l/ui/element/lazyRowPictureDetails/L_LazyRowPictureDetails.kt:104,113,237-255`

**Интерфейсы:**
- Потребляет: `adaptiveUserGridColumns(selected: Int): Int` (задача 1).
- Не меняет: `LazyRow123Host.columns`, `LazyRowPictureDetailsHost.columns`,
  настройки выбора колонок, кнопки выбора (показывают выбор пользователя).

Правило задачи: **эффективное** число идёт в `GridCells.Fixed(...)`,
`StaggeredGridCells.Fixed(...)`, полосу прокрутки и буферы предзагрузки;
**выбор** (`host.columns`) остаётся в карточке R (`LazyRow123GridItem`
прячет имя автора при выборе больше 2 — это зависит от размера карточки, а
он на широком окне при том же выборе почти телефонный).

- [ ] **Шаг 1. R: эффективные колонки сетки и полосы прокрутки**

В `LazyRow123.kt`, функция `LazyRow123`, перед `val scrollPercent = …`
добавить

```kotlin
    // Выбор пользователя плюс запас широкого окна; сам выбор в host не меняется.
    val gridColumns = adaptiveUserGridColumns(host.columns)
```

и в вызове `rememberVisibleRangePercentIgnoringFirstNForGrid` заменить
`numberOfColumns = host.columns` на `numberOfColumns = gridColumns`.
(`LaunchedEffect` внутри этой функции перезапускается по `numberOfColumns`,
ключ уже есть.)

В функции `LazyRow123Content` в вызове `LazyRow123ContentStateless(` заменить
`columns = host.columns,` на `columns = adaptiveUserGridColumns(host.columns),`.

Импорт: `import com.client.xvideos.common.ui.adaptive.adaptiveUserGridColumns`.
`LazyRow123GridItem.kt` не трогать.

- [ ] **Шаг 2. L: эффективные колонки сетки и буферов**

В `L_LazyRowPictureDetails.kt`, функция `L_LazyRowPictureDetails`, перед
`val activeItemRange by rememberActiveItemRange(host, showInitialLoading)`
добавить

```kotlin
    // Выбор пользователя плюс запас широкого окна; сам выбор в host не меняется.
    val gridColumns = adaptiveUserGridColumns(host.columns)
```

заменить вызов на `rememberActiveItemRange(host, showInitialLoading, gridColumns)`
и `columns = StaggeredGridCells.Fixed(host.columns),` на
`columns = StaggeredGridCells.Fixed(gridColumns),`.

`rememberActiveItemRange` заменить целиком на:

```kotlin
@Composable
private fun rememberActiveItemRange(
    host: LazyRowPictureDetailsHost,
    showInitialLoading: Boolean,
    columns: Int,
): State<IntRange> {
    // columns — ключ: derivedStateOf читает его как обычный параметр, без ключа
    // после раскрытия складного буферы остались бы от прежнего числа колонок.
    return remember(host, showInitialLoading, columns) {
        derivedStateOf {
            val visible = host.state.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) {
                0..15
            } else {
                val headerOffset = if (showInitialLoading) 2 else 1
                val first = (visible.minOf { it.index } - headerOffset).coerceAtLeast(0)
                val last = (visible.maxOf { it.index } - headerOffset).coerceAtLeast(0)
                val bufferBefore = columns
                val bufferAfter = columns * 2
                ((first - bufferBefore).coerceAtLeast(0))..(last + bufferAfter)
            }
        }
    }
}
```

Импорт: `import com.client.xvideos.common.ui.adaptive.adaptiveUserGridColumns`.

- [ ] **Шаг 3. Сборка и тесты R и L**

`cmd /c ".\gradlew.bat :feature-r:testDebugUnitTest :feature-l:testDebugUnitTest --continue > build\t5.log 2>&1"; "exit=$LASTEXITCODE"`

Ожидается: `exit=0`. Существующие тесты `calculateGridScrollIndex` и
`normalizeColumns` не меняются.

- [ ] **Шаг 4. Эмулятор (Review Focus 3 и 4)**

Внутренний экран, Expanded:
- R → лента при выборе 2: 4 колонки, **имя автора видно** (как при 2
  колонках на телефоне); при выборе 4 — 6 колонок, имени нет; кнопка выбора
  показывает 2 и 4, а не 4 и 6.
- R → ниша и лайки: то же правило.
- L → лайки при выборе 2: 4 колонки. Прокрутить вниз, сложить и раскрыть:
  фото ниже прежнего буфера догружаются, пустых ячеек нет.
- L → альбом (`ScreenLAlbumBody`): по умолчанию 3 → 5 колонок.
- Полноэкранный просмотр R из ленты и возврат: лента на той же гифке.
- Внешний экран: колонки равны выбору.

- [ ] **Шаг 5. Гейт и коммит**

```
feat(ui): сетки с выбором колонок получают запас на широком окне

Ленты R и фото L (лайки, лайки с сервера, коллекции, альбом): выбор
пользователя на телефоне, +1 колонка от 600dp, +2 от 840dp. Выбор в
настройках и в host не меняется; карточка R решает про имя автора по
выбору, а не по эффективному числу колонок. Буферы предзагрузки фото L
считаются от эффективного числа и перезапускаются при его смене.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
```

---

### Задача 6. Горизонтальные ряды L

**Файлы** (`feature-l/src/main/java/com/client/xvideos/l/ui/screens/`):
- Изменить: `explorer/tab/albumTopHits/L_ScreenAlbumTopHits.kt:14,45-46`
- Изменить: `explorer/tab/albumSearch/L_ScreenAlbumSearch.kt:22,59,82,97,171,192`
- Изменить: `explorer/tab/albumSearch/molecule/AlbumSearchSectionBlock.kt:32,38,103`
- Изменить: `albumLandingTag/ScreenLAlbumLandingTag.kt:19,50,75,90,125,147`
- Изменить: `albumLandingTag/molecule/LandingTagSectionItem.kt:31,37,98`
- Тест: `AdaptiveLayoutTest.kt` (BASELINE)

**Интерфейсы:**
- Потребляет: `rowItemWidth(gap: Dp = 8.dp): Dp` (задача 1).
- Меняет: параметр `screenWidth: Dp` → `itemWidth: Dp` у
  `L_ScreenAlbumSearchContent`, `AlbumSearchSectionBlock`,
  `ScreenLAlbumLandingTagContent` (корневая функция экрана тега) и
  `LandingTagSectionItem` — как уже сделано у топа (`itemWidth`).

- [ ] **Шаг 1. Убрать три записи «ширина экрана» из BASELINE — сторож краснеет**

- [ ] **Шаг 2. Топ**

`L_ScreenAlbumTopHits.kt`:

```kotlin
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val itemWidth = remember(screenWidth) { (screenWidth - 8.dp) / 3 }
```

→

```kotlin
        val itemWidth = rowItemWidth()
```

Удалить импорт `LocalConfiguration`, добавить
`import com.client.xvideos.common.ui.adaptive.rowItemWidth`.

- [ ] **Шаг 3. Поиск**

`L_ScreenAlbumSearch.kt`: строку
`val screenWidth = LocalConfiguration.current.screenWidthDp.dp` заменить на
`val itemWidth = rowItemWidth()`; аргумент `screenWidth = screenWidth,` при
вызове `L_ScreenAlbumSearchContent` → `itemWidth = itemWidth,`; параметр
`screenWidth: Dp,` → `itemWidth: Dp,`; в вызове `AlbumSearchSectionBlock(`
`screenWidth = screenWidth,` → `itemWidth = itemWidth,`; в превью
`screenWidth = 360.dp,` → `itemWidth = 117.dp,` (как прежде (360 − 8) / 3).
Импорты: убрать `LocalConfiguration`, добавить `rowItemWidth`.

`AlbumSearchSectionBlock.kt`: параметр `screenWidth: Dp,` → `itemWidth: Dp,`;
удалить строку `val itemWidth = remember(screenWidth) { (screenWidth - 8.dp) / 3 }`;
в превью `screenWidth = 360.dp,` → `itemWidth = 117.dp,`. Если `remember`
больше не используется — убрать его импорт.

- [ ] **Шаг 4. Страница тега**

`ScreenLAlbumLandingTag.kt` и `LandingTagSectionItem.kt` — те же замены, что
в шаге 3: `val screenWidth = LocalConfiguration.current.screenWidthDp.dp` →
`val itemWidth = rowItemWidth()`; все `screenWidth = screenWidth,` →
`itemWidth = itemWidth,`; параметры `screenWidth: Dp,` → `itemWidth: Dp,`;
в `LandingTagSectionItem` удалить
`val itemWidth = remember(screenWidth) { (screenWidth - 8.dp) / 3 }`;
превью `screenWidth = 360.dp,` → `itemWidth = 117.dp,`. Импорты — как в шаге 3.

- [ ] **Шаг 5. Сторож и тесты L**

`cmd /c ".\gradlew.bat :app:testDebugUnitTest --tests *AdaptiveLayoutTest* :feature-l:testDebugUnitTest --continue > build\t6.log 2>&1"; "exit=$LASTEXITCODE"`

Ожидается: `exit=0`.

- [ ] **Шаг 6. Эмулятор**

Внутренний экран, Expanded: L → топ, поиск (с запросом), страница тега —
в ряду 5 карточек целиком; Medium (Приложение А, «Medium») — 4. Внешний
экран — 3, ширина карточки как в эталоне.

- [ ] **Шаг 7. Гейт и коммит**

```
feat(l): горизонтальные ряды L по ширине окна

Топ, поиск и страница тега: 3 карточки в ряду на телефоне, 4 — от 600dp,
5 — от 840dp. Ширина карточки считается от окна (rowItemWidth), а не от
screenWidthDp; молекулы рядов получают готовую itemWidth, как у топа.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
```

---

### Задача 7. Списки и одноколоночные экраны

**Файлы:**
- app: `screenSettings/AppSettingsScreen.kt:255-258`, `screenRoot/MenuScreen.kt:85-88`
- core: `common/p2p/ui/ScreenP2pSend.kt:176-180`, `common/p2p/ui/ScreenP2pReceive.kt:102`
- X: `x/screens/saved/molecule/X_SavedList.kt:48-51`,
  `x/screens/subscriptions/molecule/X_SubscriptionsList.kt:38`,
  `x/screens/search/molecule/SearchHistoryView.kt:53`,
  `x/screens/search/molecule/SearchSuggestionsView.kt:56`
- R: `r/ui/explorer/tab/niches/R_ScreenNichesTab.kt:223`,
  `r/ui/explorer/tab/saved/tab/R_Screen_CreatorsTab.kt:136-139`,
  `r/ui/explorer/tab/saved/tab/R_Screen_Saved_DownloadTab.kt:159`,
  `r/ui/explorer/tab/saved/tab/savedNiche/SavedNichesTab.kt:132-135`,
  `r/ui/explorer/tab/search/molecule/SearchCreatorsList.kt:44`,
  `r/ui/manager_block/molecule/BlockedUsersList.kt:25-28`
- L: `l/ui/screens/screenAlbumList/molecule/AlbumListFilterOverlay.kt:58-60`

**Интерфейсы:**
- Потребляет: `adaptiveListPadding(base)`, `Modifier.adaptiveContentWidth()`,
  `CONTENT_MAX_WIDTH_DP` (задача 1).

Правило задачи: ленивые списки на всю ширину окна получают
`contentPadding = adaptiveListPadding(<прежний contentPadding>)`, чтобы
свайп работал по всей ширине. Нелениные колонки получают
`.adaptiveContentWidth()` в цепочке после фона и внешних отступов.

- [ ] **Шаг 1. app**

`AppSettingsScreen.kt`, модификатор `AppSettingsScreenBody`:

```kotlin
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
                    .verticalScroll(scrollState),
```

→

```kotlin
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
                    .verticalScroll(scrollState)
                    .adaptiveContentWidth(),
```

(прокрутка остаётся на всю ширину, страницы настроек — не шире 640dp).

`MenuScreen.kt`, `Column` в `MenuScreenContent`:

```kotlin
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF353535))
                .padding(paddingValues),
```

→ добавить `.adaptiveContentWidth()` последней строкой цепочки (фон — на
всё окно, кнопки разделов — не шире 640dp).

- [ ] **Шаг 2. core, P2P (только разметка)**

`ScreenP2pSend.kt`: в цепочке `Column` после `.padding(padding)` вставить
`.adaptiveContentWidth()` (перед `.padding(16.dp)`).
`ScreenP2pReceive.kt`: `Modifier.fillMaxSize().padding(padding).padding(24.dp)` →
`Modifier.fillMaxSize().padding(padding).adaptiveContentWidth().padding(24.dp)`.

- [ ] **Шаг 3. X**

- `X_SavedList.kt`: в `LazyColumn(` после `modifier = Modifier.fillMaxSize()`
  дописать запятую и `contentPadding = adaptiveListPadding()`.
- `X_SubscriptionsList.kt`:
  `contentPadding = PaddingValues(top = topCutout, bottom = 24.dp)` →
  `contentPadding = adaptiveListPadding(PaddingValues(top = topCutout, bottom = 24.dp))`.
- `SearchHistoryView.kt`: `contentPadding = PaddingValues(vertical = 8.dp)` →
  `contentPadding = adaptiveListPadding(PaddingValues(vertical = 8.dp))`.
- `SearchSuggestionsView.kt`: `contentPadding = PaddingValues(bottom = 16.dp)` →
  `contentPadding = adaptiveListPadding(PaddingValues(bottom = 16.dp))`.

- [ ] **Шаг 4. R**

- `R_ScreenNichesTab.kt`: `contentPadding = PaddingValues(top = getTopInsetDp())` →
  `contentPadding = adaptiveListPadding(PaddingValues(top = getTopInsetDp()))`.
- `R_Screen_CreatorsTab.kt`, `SavedNichesTab.kt`, `BlockedUsersList.kt`: в
  `LazyColumn(` после строки `modifier = …fillMaxSize()` дописать запятую и
  `contentPadding = adaptiveListPadding()`.
- `R_Screen_Saved_DownloadTab.kt`: `contentPadding = PaddingValues(4.dp)` →
  `contentPadding = adaptiveListPadding(PaddingValues(4.dp))`.
- `SearchCreatorsList.kt`: `contentPadding = PaddingValues(vertical = 4.dp)` →
  `contentPadding = adaptiveListPadding(PaddingValues(vertical = 4.dp))`.

- [ ] **Шаг 5. L, панель фильтра альбомов**

`AlbumListFilterOverlay.kt`:

```kotlin
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .heightIn(max = screenHeight * 0.9f)
```

→

```kotlin
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .widthIn(max = CONTENT_MAX_WIDTH_DP.dp)
                    .heightIn(max = screenHeight * 0.9f)
```

(`screenHeight` остаётся — «Отступления от спеки», п. 2). Импорты
`androidx.compose.foundation.layout.widthIn` и
`com.client.xvideos.common.ui.adaptive.CONTENT_MAX_WIDTH_DP`.

Во всех файлах шагов 1–5 — импорты
`com.client.xvideos.common.ui.adaptive.adaptiveListPadding` и/или
`com.client.xvideos.common.ui.adaptive.adaptiveContentWidth`, по
использованию.

- [ ] **Шаг 6. Тесты модулей**

`cmd /c ".\gradlew.bat testDebugUnitTest --continue > build\t7.log 2>&1"; "exit=$LASTEXITCODE"`

Ожидается: `exit=0` (нестабильные — Приложение Б).

- [ ] **Шаг 7. Эмулятор**

Внутренний экран, Expanded: настройки (главная и пара вложенных страниц),
меню разделов, X — сохранённые, подписки, поиск (история и подсказки), R —
ниши, авторы, загрузки, сохранённые ниши, поиск авторов, заблокированные;
L — панель фильтра альбомов. Строки не шире 640dp по центру, а список
прокручивается свайпом у самого края экрана. Внешний экран — как в эталоне.
P2P-экраны на устройстве не проверяются.

- [ ] **Шаг 8. Гейт и коммит**

```
feat(ui): списки и одноколоночные экраны не шире 640dp на широком окне

Настройки, меню разделов, P2P, списки X (сохранённые, подписки, поиск),
списки R (ниши, авторы, загрузки, сохранённые ниши, поиск авторов,
заблокированные) и панель фильтра альбомов L. У ленивых списков —
боковой contentPadding, свайп остаётся на всю ширину. На окне не шире
640dp ничего не меняется.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
```

---

### Задача 8. Шапки и верхние панели

**Файлы и правки** (во всех — импорт
`com.client.xvideos.common.ui.adaptive.adaptiveContentWidth`):

| файл | правка |
|---|---|
| `feature-x/…/x/screens/channel/molecule/ChannelHeader.kt:53-57` | во вложенном `Column` под баннером: `Modifier.fillMaxWidth().padding(horizontal = 16.dp)` → `Modifier.adaptiveContentWidth().fillMaxWidth().padding(horizontal = 16.dp)`; баннер остаётся на всю ширину |
| `feature-x/…/x/screens/search/molecule/SearchTopBar.kt:55` | после `.background(Color(0xFF222222))` вставить `.adaptiveContentWidth()` |
| `feature-x/…/x/screens/history/molecule/HistoryTopBar.kt:56-58` | во вложенном `Row`: после `.background(Theme.L.grey6)` вставить `.adaptiveContentWidth()` |
| `feature-x/…/x/screens/actresses/molecule/ActressesIndexTopBar.kt:48-49` | после `.padding(top = topCutout)` корневого `Column` вставить `.adaptiveContentWidth()` |
| `feature-r/…/r/ui/profile/molecule/RedProfileCreaterInfo.kt:42` | `modifier.padding(horizontal = 4.dp).fillMaxWidth()` → `modifier.adaptiveContentWidth().padding(horizontal = 4.dp).fillMaxWidth()` |
| `feature-r/…/r/ui/niche/molecule/NicheHeaderContent.kt:55` | после `.background(Theme.background)` вставить `.adaptiveContentWidth()` |
| `feature-r/…/r/ui/explorer/tab/search/molecule/SearchTopBar.kt:41` | после `.windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))` вставить `.adaptiveContentWidth()` |
| `feature-r/…/r/ui/explorer/tab/saved/tab/collection/molecule/CollectionNameTopBar.kt:28` | после `.fillMaxWidth()` вставить `.adaptiveContentWidth()` |
| `feature-r/…/r/ui/manager_block/atom/ManageBlockTopBar.kt:33` | после `.windowInsetsPadding(…)` вставить `.adaptiveContentWidth()` |
| `feature-l/…/l/ui/screens/screenAlbum/molecule/ScreenLAlbumDetailsHeader.kt:55` | `Column {` → `Column(modifier = Modifier.adaptiveContentWidth()) {` |
| `feature-l/…/l/ui/screens/explorer/tab/saved/collection/LCollectionsTopBar.kt:65` | `modifier = columnModifier` → `modifier = columnModifier.adaptiveContentWidth()` |
| `feature-l/…/l/ui/screens/explorer/tab/saved/collection/molecule/LCollectionDetailTopBar.kt:42` | после `.background(Theme.background)` вставить `.adaptiveContentWidth()` |
| `feature-l/…/l/ui/screens/albumLandingTag/molecule/LandingTagTopBar.kt:41` | после `.background(Theme.background)` вставить `.adaptiveContentWidth()` |

**Интерфейсы:**
- Потребляет: `Modifier.adaptiveContentWidth()` (задача 1).

- [ ] **Шаг 1. Внести правки из таблицы**

Правило: ограничитель ставится **после** модификатора, рисующего фон, и
после отступа под вырез; горизонтальные внутренние отступы — после
ограничителя. Если при проверке у шапки оказался вложенный элемент со своим
фоном на всю ширину, ограничитель переносится внутрь него, как в
`HistoryTopBar`.

- [ ] **Шаг 2. Тесты модулей**

`cmd /c ".\gradlew.bat :feature-x:testDebugUnitTest :feature-r:testDebugUnitTest :feature-l:testDebugUnitTest --continue > build\t8.log 2>&1"; "exit=$LASTEXITCODE"`

Ожидается: `exit=0`.

- [ ] **Шаг 3. Эмулятор (Review Focus 5)**

Внутренний экран, Expanded: X — канал (баннер на всю ширину, профиль и
статистика по центру), поиск, история (обычный режим и режим выбора),
актрисы; R — профиль автора, ниша (включая ряд похожих ниш), поиск,
коллекция, заблокированные; L — альбом, коллекции, коллекция изнутри,
страница тега. По краям шапок нет полос другого цвета, кнопки «назад» и
действия — в пределах 640dp. Внешний экран — как в эталоне.

- [ ] **Шаг 4. Гейт и коммит**

```
feat(ui): шапки и верхние панели не шире 640dp на широком окне

Канал, поиск, история и актрисы X; профиль, ниша, поиск, коллекция и
блокировки R; альбом, коллекции и страница тега L. Фон и баннеры — на
всю ширину окна, текст, поля и кнопки — по центру в пределах 640dp.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
```

---

### Задача 9. Нижние панели

**Перед задачей:** в `feature-r/…/r/ui/explorer/tab/gifs/molecule/GifsTabBottomBar.kt`
у владельца незакоммиченная правка (убрана кнопка «вверх»). Спросить
владельца, закоммитит ли он её сам. Пока правка не закоммичена, файл
**не трогать** и пропустить его строку в шаге 2. Чужие изменения в коммит
не попадают.

**Файлы:**
- core: `common/ui/TabRow.kt:72-74` — покрывает панели проводника R и L,
  сохранённого R и L и вкладки дашбордов X
- X: `x/screens/common/bottomKeyboard/BottomListDashBoardNavigationButtons2.kt:85-87`
  (страницы дашбордов-поиска и канала), `x/screens/dashboards/bottomBar/DashboardControlsRow.kt:33`
- R: `r/ui/explorer/tab/niches/molecule/NichesBottomBar.kt:55-57`,
  `r/ui/niche/NicheBottomBar.kt:43-45`,
  `r/ui/explorer/tab/gifs/molecule/GifsTabBottomBar.kt:51-53` (условно, см. выше)
- L: `l/ui/screens/screenAlbumList/bottomBar/AlbumListBottomBar.kt:41-44`

**Интерфейсы:**
- Потребляет: `Modifier.adaptiveContentWidth()` (задача 1).

- [ ] **Шаг 1. Общий `TabRow`**

```kotlin
    SecondaryTabRow(
        modifier = Modifier.height(48.dp),
        selectedTabIndex = state,
        containerColor = containerColor,
```

→

```kotlin
    // Фон — на всю ширину, вкладки — не шире 640dp по центру. Цвет рисует
    // внешний background, а не сам ряд: при прозрачном ряде цвет не ложится дважды.
    SecondaryTabRow(
        modifier = Modifier
            .background(containerColor)
            .adaptiveContentWidth()
            .height(48.dp),
        selectedTabIndex = state,
        containerColor = Color.Transparent,
```

Импорт `com.client.xvideos.common.ui.adaptive.adaptiveContentWidth`
(`background` и `Color` уже импортированы).

- [ ] **Шаг 2. Свои ряды панелей**

| файл | было | стало |
|---|---|---|
| `BottomListDashBoardNavigationButtons2.kt` | `modifier = modifier`<br>`.height(48.dp)`<br>`.fillMaxWidth(),` | `modifier = modifier`<br>`.height(48.dp)`<br>`.fillMaxWidth()`<br>`.adaptiveContentWidth(),` |
| `DashboardControlsRow.kt` | `modifier = modifier.fillMaxWidth(),` | `modifier = modifier.fillMaxWidth().adaptiveContentWidth(),` |
| `NichesBottomBar.kt` | `.fillMaxWidth()`<br>`.padding(vertical = 2.dp),` | `.fillMaxWidth()`<br>`.adaptiveContentWidth()`<br>`.padding(vertical = 2.dp),` |
| `NicheBottomBar.kt` | `.fillMaxWidth()`<br>`.height(48.dp),` | `.fillMaxWidth()`<br>`.adaptiveContentWidth()`<br>`.height(48.dp),` |
| `AlbumListBottomBar.kt` | `.fillMaxWidth()`<br>`.height(48.dp)`<br>`.background(Theme.tabLevel1),` | `.fillMaxWidth()`<br>`.height(48.dp)`<br>`.background(Theme.tabLevel1)`<br>`.adaptiveContentWidth(),` |
| `GifsTabBottomBar.kt` (условно) | `modifier = Modifier`<br>`.padding(top = 1.dp, start = 1.dp)`<br>`.background(Theme.tabLevel1),` | `modifier = Modifier`<br>`.adaptiveContentWidth()`<br>`.padding(top = 1.dp, start = 1.dp)`<br>`.background(Theme.tabLevel1),` |

Во все — импорт `com.client.xvideos.common.ui.adaptive.adaptiveContentWidth`.

- [ ] **Шаг 3. Тесты модулей**

`cmd /c ".\gradlew.bat testDebugUnitTest --continue > build\t9.log 2>&1"; "exit=$LASTEXITCODE"`

Ожидается: `exit=0`. `X_BottomNavigationButtonsTest` — должен остаться
зелёным: панель X меняет только модификатор.

- [ ] **Шаг 4. Эмулятор (Review Focus 5)**

Внутренний экран, Expanded: нижние панели X (дашборды: вкладки, страницы,
поиск по страницам; канал), L (проводник, сохранённое, список альбомов),
R (проводник, лента, ниши, сохранённое, ниша). Фон панели — во всю ширину
без полос, вкладки и кнопки — в пределах 640dp по центру, индикатор
выбранной вкладки под своей вкладкой. Внешний экран — как в эталоне.

- [ ] **Шаг 5. Гейт и коммит**

```
feat(ui): ряды нижних панелей не шире 640dp на широком окне

Общий TabRow: фон на всю ширину, вкладки по центру в пределах 640dp —
это покрывает проводники и сохранённое R и L и вкладки дашбордов X.
Свои ряды панелей: страницы дашбордов и канала X, ниши и ниша R, список
альбомов L. На окне не шире 640dp ничего не меняется.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
```

---

### Задача 10. Калькулятор и экран замка

**Файлы:**
- Создать: `app/src/main/java/com/client/xvideos/calculator/KeypadWidth.kt`
- Тест: `app/src/test/java/com/client/xvideos/calculator/KeypadWidthTest.kt`
- Изменить: `app/src/main/java/com/client/xvideos/calculator/CalculatorScreen.kt:68-92`
- Изменить: `core/src/main/java/com/client/xvideos/common/applock/AppLockScreen.kt:233-236`

**Интерфейсы:**
- Потребляет: `WidthClass`, `currentWidthClass()`, `Modifier.adaptiveContentWidth(maxWidth)` (задача 1).
- Производит: `fun keypadWidthDp(widthClass: WidthClass, availableWidthDp: Int, availableHeightDp: Int): Int?`.

- [ ] **Шаг 1. Написать падающий тест**

`app/src/test/java/com/client/xvideos/calculator/KeypadWidthTest.kt`:

```kotlin
package com.client.xvideos.calculator

import com.client.xvideos.common.ui.adaptive.WidthClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeypadWidthTest {

    @Test
    fun `на телефоне ограничения нет`() {
        assertNull(keypadWidthDp(WidthClass.Compact, availableWidthDp = 379, availableHeightDp = 800))
    }

    @Test
    fun `широкое окно с запасом по высоте - 420dp`() {
        assertEquals(420, keypadWidthDp(WidthClass.Expanded, availableWidthDp = 820, availableHeightDp = 828))
        assertEquals(420, keypadWidthDp(WidthClass.Medium, availableWidthDp = 668, availableHeightDp = 816))
    }

    @Test
    fun `широкое низкое окно - ширина, при которой пять рядов влезают по высоте`() {
        val height = 676
        val width = requireNotNull(keypadWidthDp(WidthClass.Expanded, availableWidthDp = 808, availableHeightDp = height))
        assertEquals(400, width)
        val button = (width - 3 * KEYPAD_SPACING_DP) / 4
        assertTrue(5 * (button + KEYPAD_SPACING_DP) <= height - DISPLAY_RESERVE_DP)
    }

    @Test
    fun `не шире доступной ширины и не отрицательная`() {
        assertEquals(380, keypadWidthDp(WidthClass.Medium, availableWidthDp = 380, availableHeightDp = 900))
        assertEquals(0, keypadWidthDp(WidthClass.Medium, availableWidthDp = 600, availableHeightDp = 100))
    }
}
```

- [ ] **Шаг 2. Убедиться, что не компилируется**

`cmd /c ".\gradlew.bat :app:testDebugUnitTest --tests *KeypadWidthTest* > build\t10.log 2>&1"; "exit=$LASTEXITCODE"`

Ожидается: `exit=1`, `Unresolved reference 'keypadWidthDp'`.

- [ ] **Шаг 3. Правило ширины клавиатуры**

`app/src/main/java/com/client/xvideos/calculator/KeypadWidth.kt`:

```kotlin
package com.client.xvideos.calculator

import com.client.xvideos.common.ui.adaptive.WidthClass

/** Расстояние между кнопками и под каждым рядом — как в KeypadRow. */
internal const val KEYPAD_SPACING_DP = 12

/** Высота, которую клавиатура оставляет дисплею калькулятора. */
internal const val DISPLAY_RESERVE_DP = 160

/** Наибольшая ширина клавиатуры на широком окне. */
internal const val KEYPAD_MAX_WIDTH_DP = 420

private const val KEYPAD_COLUMNS = 4
private const val KEYPAD_ROWS = 5

/**
 * Ширина клавиатуры калькулятора или `null`, если ограничивать не надо.
 *
 * Кнопки квадратные и делят ширину на четыре, поэтому высота клавиатуры растёт
 * вместе с шириной: на раскрытом складном в альбомной ориентации клавиатура во
 * всю ширину не влезла бы по высоте. На телефоне — `null`, разметка прежняя.
 * На широком окне — не больше [KEYPAD_MAX_WIDTH_DP], не больше доступной
 * ширины и не больше ширины, при которой пять рядов помещаются в высоту,
 * оставляя дисплею [DISPLAY_RESERVE_DP].
 */
fun keypadWidthDp(widthClass: WidthClass, availableWidthDp: Int, availableHeightDp: Int): Int? {
    if (widthClass == WidthClass.Compact) return null
    val keypadHeight = (availableHeightDp - DISPLAY_RESERVE_DP).coerceAtLeast(0)
    // Ряд = кнопка + отступ под ним; кнопка = (ширина − 3 отступа) / 4.
    val button = keypadHeight / KEYPAD_ROWS - KEYPAD_SPACING_DP
    val byHeight = (button * KEYPAD_COLUMNS + (KEYPAD_COLUMNS - 1) * KEYPAD_SPACING_DP).coerceAtLeast(0)
    return minOf(availableWidthDp, KEYPAD_MAX_WIDTH_DP, byHeight)
}
```

- [ ] **Шаг 4. Тест проходит**

Команда шага 2. Ожидается: `exit=0`, 4 теста.

- [ ] **Шаг 5. Калькулятор**

В `CalculatorScreen.kt` корневой `Column(…) { CalculatorDisplay(…); CalculatorKeypad(…) }`
заменить на:

```kotlin
    val widthClass = currentWidthClass()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .background(Color.Black)
            .displayCutoutPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        val keypadWidth = keypadWidthDp(widthClass, maxWidth.value.toInt(), maxHeight.value.toInt())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (keypadWidth != null) Modifier.adaptiveContentWidth(keypadWidth.dp) else Modifier),
            verticalArrangement = Arrangement.Bottom
        ) {
            CalculatorDisplay(
                displayValue = state.displayValue,
                expressionHistory = state.expressionHistory,
                modifier = Modifier.weight(1f)
            )
            CalculatorKeypad(
                state = state,
                scope = scope,
                haptic = haptic,
                onUnlock = onUnlock,
                onUnlockFailed = onUnlockFailed
            )
        }
    }
```

Импорты: `androidx.compose.foundation.layout.BoxWithConstraints`,
`com.client.xvideos.common.ui.adaptive.adaptiveContentWidth`,
`com.client.xvideos.common.ui.adaptive.currentWidthClass`. На телефоне
`keypadWidth == null`, и `Column` получает ровно прежнюю геометрию.

- [ ] **Шаг 6. Форма замка**

`AppLockScreen.kt`, внутренний `Column`:

```kotlin
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp)
                    .verticalScroll(rememberScrollState()),
```

→ добавить последней строкой `.adaptiveContentWidth(maxWidth = 420.dp)`.
Импорт `com.client.xvideos.common.ui.adaptive.adaptiveContentWidth`.

- [ ] **Шаг 7. Тесты app и core**

`cmd /c ".\gradlew.bat :app:testDebugUnitTest :core:testDebugUnitTest --continue > build\t10.log 2>&1"; "exit=$LASTEXITCODE"`

Ожидается: `exit=0`, `CalculatorStateTest` зелёный.

- [ ] **Шаг 8. Эмулятор**

Включить маскировку под калькулятор и код доступа в настройках debug-сборки
(значения — тестовые, не настоящий код владельца; в чат и в файлы проекта их
не писать). Свернуть и вернуться — показывается калькулятор.
- Внутренний экран, портрет и альбомная: вся клавиатура видна, кнопка «=»
  целиком, дисплей сверху; ширина 420dp или меньше по высоте.
- Medium в альбомной (Приложение А): клавиатура уже 420dp и целиком в окне.
- Без маскировки — экран кода: поле и кнопки не шире 420dp по центру.
- Внешний экран: как в эталоне.
Экран замка закрыт `FLAG_SECURE`, поэтому снимок экрана там чёрный.
Проверка — по `uiautomator dump` (bounds) и на глаз в окне эмулятора.

- [ ] **Шаг 9. Гейт и коммит**

```
feat(app): клавиатура калькулятора и форма замка влезают в широкое окно

Квадратные кнопки калькулятора на раскрытом складном в альбомной
ориентации не помещались по высоте. Клавиатура там не шире 420dp и не
шире того, что даёт высота окна; дисплей — в той же ширине. Форма кода
доступа — не шире 420dp. На телефоне разметка прежняя.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
```

---

### Задача 11. Проверка целиком и README

**Файлы:**
- Изменить: `README.md` (число тестов и дата в разделе «Проверки»)

- [ ] **Шаг 1. Сторож без исключений**

Убедиться, что `AdaptiveLayoutTest.BASELINE` пустой
(`sortedSetOf<String>()`); если в нём что-то осталось — вернуться к
задаче, которая это место не закрыла.

- [ ] **Шаг 2. Гейт целиком**

Приложение Б. В отчёт — `exit`, суммы `tests`/`failures`/`errors` по
модулям из JUnit XML и ошибки lint из `app/build/reports/lint-results-release.xml`.

- [ ] **Шаг 3. Сверка телефона с эталоном (Review Focus 1)**

Приложение А, «Сверка»: те же экраны, что в эталоне задачи 3, на
`emulator-5554` и на внешнем экране `Pixel_10_Pro_Fold`. Списки `bounds`
из дампов совпадают для неизменной части экрана (шапки, панели, первые
карточки), снимки — на глаз. Расхождение — дефект, который нужно чинить до
коммита README.

- [ ] **Шаг 4. Сценарии внутреннего экрана**

1. Expanded, портрет и альбомная: X / L / R, вкладки проводников,
   настройки, меню, калькулятор — по чек-листам задач 3–10.
2. Medium (`wm density`, Приложение А): сетки 3 колонки, пользовательские —
   выбор + 1, ряды L по 4, списки с отступом ~30dp, калькулятор в альбомной.
3. Разделённый экран (две задачи рядом на внутреннем экране): окно
   приложения уже 600dp — раскладка как на телефоне.
4. **Раскрытие посреди прокрутки (Review Focus 2):** X — избранное,
   R — лента, L — лайки; прокрутить на несколько экранов, сложить, раскрыть,
   повернуть. Видна та же область списка (по индексу элемента), вкладка
   прежняя, открытый экран и стек навигации на месте.

- [ ] **Шаг 5. README**

В `README.md`, раздел «Проверки», строку
`- **1531 юнит-тест** в 272 файлах (на 13.10.2026), включая сторожей архитектуры`
обновить по суммам из JUnit XML после шага 2 (число тестов, число файлов
тестов, дата).

- [ ] **Шаг 6. Коммит**

```
docs(readme): число тестов после адаптации под складные телефоны

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
```

---

## Приложение А. Эмулятор

`adb` — `/c/zip/bin/adb` (Git Bash) или `C:\zip\bin\adb.exe` (PowerShell).
Логи приложения на эмуляторе, не на телефоне владельца.

**Запуск.** PowerShell, в фоне:
`& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd Pixel_10_Pro_Fold`.
Телефонный — `Medium_Phone` (обычно уже `emulator-5554`). Серийник
складного — из `adb devices` (например, `emulator-5556`); дальше `-s <serial>`.

**Сборка и установка.**
`cmd /c ".\gradlew.bat :app:assembleDebug > build\asm.log 2>&1"`, затем
`adb -s <serial> install -r app\build\outputs\apk\debug\app-debug.apk` и
`adb -s <serial> shell monkey -p com.client.xvideos 1`.

**Сложить / раскрыть.** `adb -s <serial> emu fold` / `adb -s <serial> emu unfold`.
Если консоль эмулятора их не знает — переключатель Fold/Unfold на боковой
панели эмулятора.

**Поворот.** `adb -s <serial> shell settings put system accelerometer_rotation 0`,
затем `… put system user_rotation 1` (альбомная) или `0` (портрет).

**Medium.** На раскрытом: `adb -s <serial> shell wm density 475` — окно ~700dp
в ширину. После проверки обязательно `adb -s <serial> shell wm density reset`.

**Снимок и дамп.**
`adb -s <serial> exec-out screencap -p > <scratchpad>\<имя>.png`;
`adb -s <serial> shell uiautomator dump /sdcard/u.xml` и
`adb -s <serial> pull /sdcard/u.xml <scratchpad>\<имя>.xml`.

**Эталон** (задача 3, шаг 0, до правок UI): на `emulator-5554` и на
сложенном `Pixel_10_Pro_Fold` — меню разделов, настройки, X (дашборды,
избранное, канал, актрисы, страница тега, поиск), L (список альбомов, топ,
лайки, альбом, коллекции), R (лента, ниши, сохранённое, профиль),
калькулятор. Для каждого экрана — снимок и дамп с одинаковым именем.

**Сверка** (задача 11): те же экраны после правок. Геометрия сравнивается
по `bounds` из дампов скриптом `bounds_diff.py` в каталоге scratchpad (не в
репозитории):

```python
import re
import sys

def bounds(path):
    with open(path, encoding="utf-8") as f:
        return re.findall(r'bounds="(\[\d+,\d+\]\[\d+,\d+\])"', f.read())

before, after = bounds(sys.argv[1]), bounds(sys.argv[2])
print(f"узлов до: {len(before)}, после: {len(after)}")
for i, (b, a) in enumerate(zip(before, after)):
    if b != a:
        print(f"{i}: {b} -> {a}")
```

Запуск: `python -I <scratchpad>\bounds_diff.py <до>.xml <после>.xml`.
Совпадать должны шапки, панели и первые карточки; содержимое из сети может
отличаться, поэтому расхождения в середине длинных списков разбираются по
снимкам, а не считаются дефектом автоматически.

## Приложение Б. Гейт

PowerShell, из корня репозитория:

`cmd /c ".\gradlew.bat testDebugUnitTest detekt :app:lintRelease --continue > build\gate.log 2>&1"; "exit=$LASTEXITCODE"`

- Итог — `exit=0`. Суммы тестов — из
  `<модуль>/build/test-results/testDebugUnitTest/*.xml` (`tests`,
  `failures`, `errors`), консоль при успехе молчит.
- Ошибки lint — `app/build/reports/lint-results-release.xml`, `severity`
  `Error`/`Fatal`; предупреждения гейт не валят.
- Нестабильные тесты: `KDownloaderQueueTest` (`:core`) и
  `ScreenX_ChannelSMPageTest` (`:feature-x`) иногда падают сами по себе. При
  падении только их — перезапустить гейт; повторное падение — разбирать.
- Команду не сокращать до части модулей: гейт — всегда целиком.
