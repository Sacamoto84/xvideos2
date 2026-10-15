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
 * Тест падает в обе стороны: и на новом нарушении, и на записи [ALLOWED],
 * которая больше ничего не разрешает.
 */
class AdaptiveLayoutTest {

    @Test
    fun `сетки и ряды берут колонки и ширину из common-ui-adaptive`() {
        val found = ProjectSources.kotlinSourcesWithoutComments()
            .filterNot { (path, _) -> path.startsWith(ADAPTIVE_PACKAGE) }
            .flatMap { (path, code) -> violations(code).map { name -> "$path: $name" } }
            .toSortedSet()

        assertEquals(
            "Сетка или ряд снова завязаны на жёсткое число колонок или на размер экрана. " +
                "Колонки — adaptiveGridColumns() / adaptiveUserGridColumns(), ширина — " +
                "currentWindowWidthDp() / rowItemWidth(), полная строка — GridItemSpan(maxLineSpan). " +
                "Запись, которая больше ничего не разрешает, уберите из ALLOWED.",
            ALLOWED.keys,
            found,
        )
    }

    @Test
    fun `правила ловят именованный аргумент и ряды числом, но не одну ячейку`() {
        assertEquals(setOf("колонки числом"), violations("LazyVerticalGrid(columns = GridCells.Fixed(count = 2))"))
        assertEquals(setOf("колонки числом"), violations("StaggeredGridCells.Fixed(2)"))
        assertEquals(setOf("строка сетки числом"), violations("item(span = { GridItemSpan(2) })"))
        assertEquals(setOf("строка сетки числом"), violations("item(span = { GridItemSpan(12) })"))
        assertEquals(setOf("строка сетки числом"), violations("GridItemSpan(currentLineSpan = 2)"))
        assertEquals(setOf("ряд FlowRow числом"), violations("FlowRow(maxItemsInEachRow = 3) {}"))
        assertEquals(setOf("ряды chunked числом"), violations("loaded.chunked(4)"))
        assertEquals(setOf("ряды chunked числом"), violations("loaded.chunked(size = 4)"))
        // Одна ячейка и число из adaptive — не нарушения.
        assertEquals(emptySet<String>(), violations("item(span = { GridItemSpan(1) })"))
        assertEquals(emptySet<String>(), violations("GridItemSpan(currentLineSpan = 1)"))
        assertEquals(emptySet<String>(), violations("item(span = { GridItemSpan(maxLineSpan) })"))
        assertEquals(emptySet<String>(), violations("FlowRow(maxItemsInEachRow = itemsPerRow) {}"))
        assertEquals(emptySet<String>(), violations("loaded.chunked(itemsPerRow)"))
    }

    private companion object {
        const val ADAPTIVE_PACKAGE = "common/ui/adaptive/"

        fun violations(code: String): Set<String> =
            RULES.filter { (_, pattern) -> pattern.containsMatchIn(code) }.mapTo(sortedSetOf()) { (name, _) -> name }

        val RULES = listOf(
            "колонки числом" to Regex("""\b(?:Staggered)?GridCells\.Fixed\(\s*(?:count\s*=\s*)?\d"""),
            // GridItemSpan(1) — одна ячейка: от числа колонок не зависит.
            "строка сетки числом" to Regex("""\bGridItemSpan\(\s*(?:currentLineSpan\s*=\s*)?(?!1\s*\))\d"""),
            "ряд FlowRow числом" to Regex("""\bmaxItemsInEachRow\s*=\s*\d"""),
            "ряды chunked числом" to Regex("""\.chunked\(\s*(?:size\s*=\s*)?\d"""),
            "ширина экрана" to Regex("""\bscreenWidthDp\b"""),
            "ориентация экрана" to Regex("""\bConfiguration\.ORIENTATION_(?:LANDSCAPE|PORTRAIT)\b"""),
        )

        /**
         * Места, где число не про ширину окна, — с причиной. Неисправленных
         * мест здесь нет: новое нарушение исправляется, а не вписывается сюда.
         */
        val ALLOWED = sortedMapOf(
            "common/ui/keyboard/KeyboardNumber.kt: ряд FlowRow числом" to
                "цифровая клавиатура: три кнопки в ряду, как на телефоне",
            "common/videoplayer/util/util.android.kt: ряды chunked числом" to
                "hex-строка режется по два символа, это не раскладка",
        )
    }
}
