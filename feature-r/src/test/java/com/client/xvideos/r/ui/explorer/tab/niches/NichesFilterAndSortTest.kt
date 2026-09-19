package com.client.xvideos.r.ui.explorer.tab.niches

import com.client.xvideos.r.model.Niche
import com.client.xvideos.r.model.Order
import org.junit.Assert.assertEquals
import org.junit.Test

class NichesFilterAndSortTest {

    private val sampleNiches = listOf(
        Niche(id = "1", name = "Amateurs", subscribers = 1000L, gifs = 50L),
        Niche(id = "2", name = "Anal", subscribers = 5000L, gifs = 200L),
        Niche(id = "3", name = "Blowjob", subscribers = 3000L, gifs = 150L),
        Niche(id = "4", name = "Babe", subscribers = 2000L, gifs = 100L)
    )

    @Test
    fun `blank query retains all niches`() {
        val result = filterAndSortNiches(sampleNiches, query = "", order = Order.NICHES_SUBSCRIBERS_D)
        assertEquals(4, result.size)
        assertEquals("Anal", result.first().name)
    }

    @Test
    fun `query filters niches case-insensitively`() {
        val result = filterAndSortNiches(sampleNiches, query = "am", order = Order.NICHES_NAME_A_Z)
        assertEquals(1, result.size)
        assertEquals("Amateurs", result[0].name)
    }

    @Test
    fun `sorts by subscribers descending`() {
        val result = filterAndSortNiches(sampleNiches, query = "", order = Order.NICHES_SUBSCRIBERS_D)
        assertEquals(listOf("Anal", "Blowjob", "Babe", "Amateurs"), result.map { it.name })
    }

    @Test
    fun `sorts by subscribers ascending`() {
        val result = filterAndSortNiches(sampleNiches, query = "", order = Order.NICHES_SUBSCRIBERS_A)
        assertEquals(listOf("Amateurs", "Babe", "Blowjob", "Anal"), result.map { it.name })
    }

    @Test
    fun `sorts by posts descending`() {
        val result = filterAndSortNiches(sampleNiches, query = "", order = Order.NICHES_POST_D)
        assertEquals(listOf("Anal", "Blowjob", "Babe", "Amateurs"), result.map { it.name })
    }

    @Test
    fun `sorts by name ascending and descending`() {
        val asc = filterAndSortNiches(sampleNiches, query = "", order = Order.NICHES_NAME_A_Z)
        assertEquals(listOf("Amateurs", "Anal", "Babe", "Blowjob"), asc.map { it.name })

        val desc = filterAndSortNiches(sampleNiches, query = "", order = Order.NICHES_NAME_Z_A)
        assertEquals(listOf("Blowjob", "Babe", "Anal", "Amateurs"), desc.map { it.name })
    }

    @Test
    fun `sorts by name case-insensitively with mixed casing`() {
        val mixedNiches = listOf(
            Niche(id = "1", name = "babe", subscribers = 10L, gifs = 5L),
            Niche(id = "2", name = "Amateurs", subscribers = 20L, gifs = 10L),
            Niche(id = "3", name = "anal", subscribers = 30L, gifs = 15L),
            Niche(id = "4", name = "Blowjob", subscribers = 40L, gifs = 20L)
        )

        val asc = filterAndSortNiches(mixedNiches, query = "", order = Order.NICHES_NAME_A_Z)
        assertEquals(listOf("Amateurs", "anal", "babe", "Blowjob"), asc.map { it.name })

        val desc = filterAndSortNiches(mixedNiches, query = "", order = Order.NICHES_NAME_Z_A)
        assertEquals(listOf("Blowjob", "babe", "anal", "Amateurs"), desc.map { it.name })
    }
}
