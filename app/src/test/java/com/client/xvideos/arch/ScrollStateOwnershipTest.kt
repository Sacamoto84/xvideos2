package com.client.xvideos.arch

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Сторож: объекты состояния ленивых списков и пейджеров создаёт только композиция.
 *
 * `LazyListState`, `LazyGridState`, `LazyStaggeredGridState` и `PagerState`
 * запоминают узел своей раскладки и последний замер. В ScreenModel или хосте,
 * который живёт дольше экрана, они держали старую Activity после её
 * пересоздания (проход 199, T1). Позиция, которая должна пережить переход, —
 * `LazyListPosition` / `LazyGridPosition` / `LazyStaggeredGridPosition` /
 * `PagerPosition` из `common/ui/scroll/`; в композиции — `rememberLazy…State()`
 * или `position.rememberState()`.
 */
class ScrollStateOwnershipTest {

    @Test
    fun `состояния списков и пейджеров создаются только в common-ui-scroll`() {
        val found = ProjectSources.kotlinSourcesWithoutComments()
            .filterNot { (path, _) -> path.startsWith(SCROLL_PACKAGE) }
            .filter { (_, code) -> CONSTRUCTOR.containsMatchIn(code) }
            .map { (path, _) -> path }
            .toSortedSet()

        assertEquals(
            "Объект состояния списка или пейджера создан вне композиции. В ScreenModel и " +
                "хостах держите позицию из common/ui/scroll/, в composable — rememberLazy…State() " +
                "или position.rememberState().",
            emptySet<String>(),
            found,
        )
    }

    @Test
    fun `правило ловит конструкторы и наследника, но не remember-функции и типы`() {
        assertEquals(true, CONSTRUCTOR.containsMatchIn("val state = LazyListState()"))
        assertEquals(true, CONSTRUCTOR.containsMatchIn("getOrPut(page) { LazyGridState(cacheWindow = w) }"))
        assertEquals(true, CONSTRUCTOR.containsMatchIn("val s = LazyStaggeredGridState(0, 0)"))
        assertEquals(true, CONSTRUCTOR.containsMatchIn("class P(page: Int) : PagerState(page, 0f) {"))
        assertEquals(false, CONSTRUCTOR.containsMatchIn("val state = rememberLazyListState()"))
        assertEquals(false, CONSTRUCTOR.containsMatchIn("val pager = rememberPagerState { 3 }"))
        assertEquals(false, CONSTRUCTOR.containsMatchIn("gridState: LazyGridState,"))
        assertEquals(false, CONSTRUCTOR.containsMatchIn("getGrid: (Int) -> LazyGridState?"))
    }

    private companion object {
        const val SCROLL_PACKAGE = "common/ui/scroll/"

        val CONSTRUCTOR = Regex("""(?<![\w.])(?:LazyListState|LazyGridState|LazyStaggeredGridState|PagerState)\s*\(""")
    }
}
