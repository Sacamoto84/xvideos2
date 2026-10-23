package com.client.xvideos.x.screens.actresses.model

import com.client.xvideos.x.model.CatalogCountry
import com.client.xvideos.x.model.CatalogProfileHit
import com.client.xvideos.x.model.CatalogProfileKind
import com.client.xvideos.x.search.model.Channel
import com.client.xvideos.x.search.model.Pornstar
import com.client.xvideos.x.search.model.SearchResult
import com.client.xvideos.x.search.model.SearchSuggestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CatalogSearchTest {

    private val ru = Locale.forLanguageTag("ru")
    private val countries = listOf(
        CatalogCountry("EE", "estonia"),
        CatalogCountry("ES", "spain"),
        CatalogCountry("RU", "russia"),
        CatalogCountry("US", "united_states"),
    )
    private val template = "/channels-index/%country_token%"

    @Test
    fun `страна ищется по началу русского названия`() {
        val found = searchCatalogCountries(countries, template, "эст", ru)

        assertEquals(listOf("Эстония"), found.map { it.title })
        assertEquals("/channels-index/estonia", found.single().urlPath)
    }

    @Test
    fun `страна ищется и по английскому названию из адреса`() {
        assertEquals(listOf("Испания"), searchCatalogCountries(countries, template, "SPA", ru).map { it.title })
        assertEquals(listOf("Соединенные Штаты"), searchCatalogCountries(countries, template, "united", ru).map { it.title.replace('ё', 'е') })
    }

    @Test
    fun `пустой запрос и цифры стран не дают`() {
        assertTrue(searchCatalogCountries(countries, template, "  ", ru).isEmpty())
        assertTrue(searchCatalogCountries(countries, template, "123", ru).isEmpty())
    }

    private fun star(name: String, path: String) =
        Pornstar(N = name, F = path, T = "pornstar", MV = 5, M = 0, L = 0, P = "https://img/$name.jpg", RF = "1,2 к")

    private fun channel(name: String, path: String) =
        Channel(N = name, F = path, T = "channel", CPV = true, M = 0, L = 0, P = "https://img/$name.jpg", RF = "3 к")

    private val result = SearchResult(
        result = true,
        data = SearchSuggestData(
            pornstar = listOf(star("Swen Demo", "swen-demo"), star("Kira Swift", "kira-swift"), star("Other", "other")),
            channel = listOf(channel("Scout Demo Studio", "scout-demo-studio"), channel("Swing Club", "swing-club")),
        ),
    )

    @Test
    fun `на рейтинге моделей — только модели, имя содержит запрос`() {
        val hits = catalogProfileHits(result, CatalogProfileKind.MODELS, "sw")

        assertEquals(
            listOf(
                CatalogProfileHit("Swen Demo", "swen-demo", "https://img/Swen Demo.jpg", "1,2 к", isModel = true),
                CatalogProfileHit("Kira Swift", "kira-swift", "https://img/Kira Swift.jpg", "1,2 к", isModel = true),
            ),
            hits,
        )
    }

    @Test
    fun `на рейтинге каналов — только каналы`() {
        val hits = catalogProfileHits(result, CatalogProfileKind.CHANNELS, "scout")

        assertEquals(listOf("Scout Demo Studio"), hits.map { it.name })
        assertEquals(false, hits.single().isModel)
        assertEquals("scout-demo-studio", hits.single().slug)
    }
}
