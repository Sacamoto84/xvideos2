package com.client.xvideos.x.parcer

import com.client.xvideos.x.feature.country.CountryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тестирование устойчивости парсера тегов видео и состояния выбора страны.
 */
class VideoTagsResilienceTest {

    @Test
    fun `parserItemVideoTags on blank input returns empty model`() {
        val emptyResult = parserItemVideoTags("")
        assertTrue(emptyResult.mainUploader.isEmpty())
        assertTrue(emptyResult.pornstars.isEmpty())
        assertTrue(emptyResult.tags.isEmpty())

        val whitespaceResult = parserItemVideoTags("   \n\t  ")
        assertTrue(whitespaceResult.mainUploader.isEmpty())
        assertTrue(whitespaceResult.pornstars.isEmpty())
        assertTrue(whitespaceResult.tags.isEmpty())
    }

    @Test
    fun `parserItemVideoTags deduplicates and sorts keywords`() {
        val html = """
            <ul>
                <li><a class="is-keyword" href="/tags/vr"> vr </a></li>
                <li><a class="is-keyword" href="/tags/4k">4k</a></li>
                <li><a class="is-keyword" href="/tags/vr">vr</a></li>
                <li><a class="is-keyword" href="/tags/amateur">amateur</a></li>
                <li><a class="is-keyword" href="/tags/empty">   </a></li>
            </ul>
        """.trimIndent()

        val result = parserItemVideoTags(html)
        assertEquals(listOf("4k", "amateur", "vr"), result.tags)
    }

    @Test
    fun `parserItemVideoTags handles uploaders and models with default count`() {
        val html = """
            <ul>
                <li class="main-uploader">
                    <a href="/profiles/top_creator">
                        <span class="name">TopCreator</span>
                    </a>
                </li>
                <li class="model">
                    <a href="/models/actress">
                        <span class="name">ActressName</span>
                        <span class="count">12.5k</span>
                    </a>
                </li>
            </ul>
        """.trimIndent()

        val result = parserItemVideoTags(html)

        assertEquals(1, result.mainUploader.size)
        assertEquals("TopCreator", result.mainUploader[0].name)
        assertEquals("/profiles/top_creator", result.mainUploader[0].href)
        assertEquals("0", result.mainUploader[0].count)

        assertEquals(1, result.pornstars.size)
        assertEquals("ActressName", result.pornstars[0].name)
        assertEquals("/models/actress", result.pornstars[0].href)
        assertEquals("12.5k", result.pornstars[0].count)
    }

    @Test
    fun `CountryState updates current and increments epoch`() {
        val initialEpoch = CountryState.userSelectionEpoch
        CountryState.onCountrySelected("🇩🇪")

        assertEquals("🇩🇪", CountryState.current)
        assertEquals(initialEpoch + 1, CountryState.userSelectionEpoch)

        CountryState.updateCurrent("🇺🇸")
        assertEquals("🇺🇸", CountryState.current)
        // updateCurrent не увеличивает epoch выбора пользователя
        assertEquals(initialEpoch + 1, CountryState.userSelectionEpoch)
    }
}
