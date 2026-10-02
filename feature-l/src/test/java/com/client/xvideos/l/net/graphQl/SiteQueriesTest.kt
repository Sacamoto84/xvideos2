package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.anonymousGraphQlGetUrl
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId
import com.client.xvideos.l.repository.LusciousEndpoints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Запросы приложения совпадают с запросами сайта байт в байт.
 *
 * Пока сервер отвечает ошибкой, Cloudflare отдаёт только то, что лежит у него
 * в кэше, а ключ кэша — полный URL анонимного GET-запроса. Эталонные строки
 * параметров в `l_site_raw_queries.tsv` сняты отладочной пробой
 * `LWebProbeActivity` с настоящего сайта (релиз фронтенда от 22.09.2026):
 * если сайт поменяет запросы, их снимают заново и обновляют файл.
 */
class SiteQueriesTest {

    private val siteRawQueries: Map<String, String> by lazy {
        val stream = requireNotNull(javaClass.classLoader?.getResourceAsStream("l_site_raw_queries.tsv")) {
            "нет l_site_raw_queries.tsv в тестовых ресурсах"
        }
        stream.bufferedReader().readLines()
            .filter { it.isNotBlank() }
            .associate { it.substringBefore('\t') to it.substringAfter('\t') }
    }

    private fun appRawQuery(data: String): String =
        requireNotNull(anonymousGraphQlGetUrl(LusciousEndpoints.API_ANONYMOUS, data)) { "запрос не ушёл бы GET" }
            .substringAfter('?')

    private fun siteRawQuery(case: String): String = siteRawQueries.getValue(case)

    @Test
    fun `список манги по трендам — как в разделе сайта`() {
        val filter = AlbumListFilter(display = "date_trending", album_type = AlbumType.Manga)

        assertEquals(siteRawQuery("albumList_manga_trending_p1"), appRawQuery(getAlbumListGraphQL1(1, filter)))
    }

    @Test
    fun `список картинок с типом контента — как в разделе сайта`() {
        val filter = AlbumListFilter(
            display = "date_trending",
            album_type = AlbumType.Pictures,
            content_id = ContentId.Hentai,
        )

        assertEquals(siteRawQuery("albumList_hentai_trending_p1"), appRawQuery(getAlbumListGraphQL1(1, filter)))
    }

    @Test
    fun `фильтры раздела — как на сайте`() {
        val filter = AlbumListFilter(display = "date_trending", album_type = AlbumType.Manga)

        assertEquals(
            siteRawQuery("aggregations_manga_trending_p1"),
            appRawQuery(getAlbumListWithAggregations(1, filter))
        )
    }

    @Test
    fun `альбом — как на сайте`() {
        assertEquals(siteRawQuery("albumGet_614544"), appRawQuery(getAlbumInfo(614544)))
    }

    @Test
    fun `картинки альбома — как на сайте`() {
        assertEquals(siteRawQuery("pictures_614544_p1"), appRawQuery(getPicturesJson(614544, 1)))
    }

    @Test
    fun `выбранные пользователем аудитории и языки по-прежнему уходят в запрос`() {
        val filter = AlbumListFilter(
            display = "date_trending",
            album_type = AlbumType.Manga,
            audienceIds = "+1+2",
            languageIds = "+1",
        )

        val data = getAlbumListGraphQL1(1, filter)

        assertTrue(data, data.contains(""""name":"audience_ids","value":"+1+2""""))
        assertTrue(data, data.contains(""""name":"language_ids","value":"+1""""))
    }

    @Test
    fun `поисковый запрос с кавычками не ломает JSON`() {
        val filter = AlbumListFilter(searchQuery = """a "b" \ c""")

        val listVariables = kotlinx.serialization.json.Json.parseToJsonElement(getAlbumListGraphQL1(1, filter))
        val aggVariables = kotlinx.serialization.json.Json.parseToJsonElement(getAlbumListWithAggregations(1, filter))

        assertTrue(listVariables.toString(), listVariables.toString().contains("""a \"b\" \\ c"""))
        assertTrue(aggVariables.toString(), aggVariables.toString().contains("""a \"b\" \\ c"""))
    }
}
