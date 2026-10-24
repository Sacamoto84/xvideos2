package com.client.xvideos.x.screens.rubrics

import com.client.xvideos.x.feature.rubric.XRubricCatalog
import com.client.xvideos.x.feature.rubric.buildRubricMenuUrl
import com.client.xvideos.x.model.XCategory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Вкладка «Категории»: загрузка списка ориентации, ошибка и повтор, смена
 * ориентации во время загрузки. Без runTest: свой планировщик гоняется вручную.
 */
class XRubricsSMTest {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val scope = CoroutineScope(dispatcher)

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun menuOf(vararg titles: String) = titles.mapIndexed { i, title ->
        """<li class="dyntop-cat"><a href="/c/R$i-$i">$title</a></li>"""
    }.joinToString("", """<ul id="main-cats-sub-list">""", "</ul>")

    private fun catalog(fetch: suspend (String) -> String) = XRubricCatalog(dispatcher, fetch)

    private fun XRubricsSM.titles() = (state as XRubricsState.Ready).rubrics.map { it.title }

    @Test
    fun `показ ориентации грузит её список`() {
        val sm = XRubricsSM(catalog { menuOf("Арбуз") }, scope)

        sm.show(XCategory.STRAIGHT)
        assertEquals(XRubricsState.Loading, sm.state)
        scheduler.advanceUntilIdle()

        assertEquals(listOf("Арбуз"), sm.titles())
    }

    @Test
    fun `уже загруженный список показывается сразу`() {
        val shared = catalog { menuOf("Арбуз") }
        XRubricsSM(shared, scope).show(XCategory.STRAIGHT)
        scheduler.advanceUntilIdle()

        val second = XRubricsSM(shared, scope)
        second.show(XCategory.STRAIGHT)

        assertEquals(listOf("Арбуз"), second.titles())
    }

    @Test
    fun `ошибка — Failed, retry грузит заново`() {
        var offline = true
        val sm = XRubricsSM(catalog { if (offline) throw IOException("нет сети") else menuOf("Арбуз") }, scope)

        sm.show(XCategory.STRAIGHT)
        scheduler.advanceUntilIdle()
        assertEquals(XRubricsState.Failed, sm.state)

        offline = false
        sm.retry()
        scheduler.advanceUntilIdle()
        assertEquals(listOf("Арбуз"), sm.titles())
    }

    @Test
    fun `побеждает последняя выбранная ориентация`() {
        val answers = mapOf(
            buildRubricMenuUrl(XCategory.GAY) to CompletableDeferred<String>(),
            buildRubricMenuUrl(XCategory.TRANS) to CompletableDeferred(),
        )
        val sm = XRubricsSM(catalog { url -> answers.getValue(url).await() }, scope)

        sm.show(XCategory.GAY)
        scheduler.advanceUntilIdle()
        sm.show(XCategory.TRANS)
        scheduler.advanceUntilIdle()
        answers.getValue(buildRubricMenuUrl(XCategory.TRANS)).complete(menuOf("Трансы"))
        scheduler.advanceUntilIdle()
        answers.getValue(buildRubricMenuUrl(XCategory.GAY)).complete(menuOf("Геи"))
        scheduler.advanceUntilIdle()

        assertEquals(listOf("Трансы"), sm.titles())
    }

    @Test
    fun `повторный show той же ориентации не перезагружает`() {
        var calls = 0
        val never = CompletableDeferred<String>()
        val sm = XRubricsSM(catalog { calls++; never.await() }, scope)

        sm.show(XCategory.STRAIGHT)
        scheduler.advanceUntilIdle()
        sm.show(XCategory.STRAIGHT)
        scheduler.advanceUntilIdle()

        assertEquals(1, calls)
        assertTrue(sm.state is XRubricsState.Loading)
    }
}
