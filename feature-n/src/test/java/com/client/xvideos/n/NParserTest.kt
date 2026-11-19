package com.client.xvideos.n

import com.client.xvideos.n.model.NPage
import com.client.xvideos.n.net.NParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Юнит-тесты парсера nhentai API и HTML.
 */
class NParserTest {

    @Test
    fun `mixed page numbers fall back to a unique sequence`() {
        val json = """{"id":42,"media_id":17,"pages":[{"number":2,"path":"galleries/17/1.webp"},{"path":"galleries/17/2.webp"}]}"""
        val pages = NParser.parseGalleryJson(json)?.pages.orEmpty()
        assertEquals(listOf(1, 2), pages.map { it.pageNumber })
        assertEquals("galleries/17/1.webp", pages.first().path)
    }

    @Test
    fun `HTML skips unrelated albums when a gallery id is requested`() {
        val html = """
            <script type="application/json">{"id":41,"media_id":16}</script>
            <script type="application/json">{"gallery":{"id":42,"media_id":17}}</script>
        """.trimIndent()
        assertEquals(42L, NParser.parseGalleryHtml(html, expectedGalleryId = 42)?.id)
        assertNull(NParser.parseGalleryHtml(html, expectedGalleryId = 43))
    }

    @Test
    fun `optional recommendations do not prevent opening an album`() {
        val missing = """{"id":42,"media_id":17,"pages":[{}],"related":null}"""
        val malformed = """{"id":42,"media_id":17,"pages":[{}],"related":[null,"unknown"]}"""
        assertEquals(1, NParser.parseGalleryJson(missing)?.pages?.size)
        assertEquals(1, NParser.parseGalleryJson(malformed)?.pages?.size)
    }

    @Test
    fun `HTML can embed gallery under a JSON key with sibling fields`() {
        val html = """<script type="application/json">{"gallery":{"id":42,"media_id" : 17},"other":true}</script>"""
        assertEquals(42L, NParser.parseGalleryHtml(html)?.id)
    }

    @Test
    fun `HTML pagination never drops the current page when it is not a link`() {
        val html = """<section class="pagination"><a class="page" href="?page=4">4</a><span>5</span></section>"""
        val page = NParser.parseSearchHtmlPage(html, defaultPage = 5)
        assertEquals(5, page.page)
        assertEquals(5, page.totalPages)
    }

    @Test
    fun `favorites page retains albums and pagination from API v2`() {
        val json = """{"result":[{"id":42,"media_id":"17","english_title":"Album","japanese_title":null,
            "thumbnail":"galleries/17/thumb.webp","thumbnail_width":250,"thumbnail_height":350}],"num_pages":4}"""
        val page = NParser.parseGalleryPageV2(json, 2)
        assertEquals(42L, page.items.single().id)
        assertEquals("Album", page.items.single().title)
        assertEquals(2, page.page)
        assertEquals(4, page.totalPages)
    }

    @Test
    fun `empty favorites page is valid`() {
        val page = NParser.parseGalleryPageV2("""{"result":[],"num_pages":0}""", 1)
        assertEquals(0, page.items.size)
        assertEquals(1, page.totalPages)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unexpected favorites payload is an error`() {
        NParser.parseGalleryPageV2("""{"detail":"Unauthorized"}""", 1)
    }

    @Test
    fun `reads server favorite status from check and mutation responses`() {
        assertTrue(NParser.parseFavoriteState("""{"favorited": true, "num_favorites": 12}"""))
        assertFalse(NParser.parseFavoriteState("""{"favorited": false}"""))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `missing server favorite status is an error`() {
        NParser.parseFavoriteState("""{"error": "Authentication required"}""")
    }

    private val sampleJson = """
        {
          "id": 123456,
          "media_id": 987654,
          "title": {
            "english": "Test Manga English",
            "japanese": "テストマンガ",
            "pretty": "Test Manga"
          },
          "images": {
            "pages": [
              { "t": "j", "w": 1280, "h": 1800 },
              { "t": "p", "w": 1280, "h": 1800 }
            ],
            "cover": { "t": "j", "w": 350, "h": 500 },
            "thumbnail": { "t": "j", "w": 250, "h": 350 }
          },
          "upload_date": 1600000000,
          "tags": [
            { "id": 1, "type": "artist", "name": "cool_artist", "url": "/artist/cool_artist/", "count": 120 },
            { "id": 2, "type": "tag", "name": "full color", "url": "/tag/full-color/", "count": 4500 },
            { "id": 3, "type": "language", "name": "english", "url": "/language/english/", "count": 89000 }
          ],
          "num_pages": 2,
          "num_favorites": 1450
        }
    """.trimIndent()

    private val sampleJsonV2 = """
        {
          "id": 697415,
          "media_id": "4330179",
          "title": {
            "english": "Test Doujinshi English",
            "japanese": "テスト",
            "pretty": "Test Doujinshi"
          },
          "cover": {
            "path": "galleries/4330179/cover.jpg.webp",
            "width": 350,
            "height": 184
          },
          "thumbnail": {
            "path": "galleries/4330179/thumb.jpg.webp",
            "width": 250,
            "height": 131
          },
          "upload_date": 1794773148,
          "tags": [
            { "id": 1, "type": "artist", "name": "cool_artist", "url": "/artist/cool_artist/", "count": 100 }
          ],
          "num_pages": 2,
          "num_favorites": 42,
          "pages": [
            {
              "number": 1,
              "path": "galleries/4330179/1.jpg",
              "width": 1200,
              "height": 630,
              "thumbnail": "galleries/4330179/1t.jpg.webp",
              "thumbnail_width": 200,
              "thumbnail_height": 105
            },
            {
              "number": 2,
              "path": "galleries/4330179/2.webp",
              "width": 1100,
              "height": 1550,
              "thumbnail": "galleries/4330179/2t.webp.webp",
              "thumbnail_width": 200,
              "thumbnail_height": 282
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `parseGalleryJson корректно извлекает метаданные и страницы`() {
        val gallery = NParser.parseGalleryJson(sampleJson)

        assertNotNull(gallery)
        assertEquals(123456L, gallery?.id)
        assertEquals(987654L, gallery?.mediaId)
        assertEquals("Test Manga", gallery?.title)
        assertEquals("Test Manga English", gallery?.englishTitle)
        assertEquals(2, gallery?.numPages)
        assertEquals(1450, gallery?.numFavorites)

        assertEquals(2, gallery?.pages?.size)
        assertEquals("jpg", gallery?.pages?.get(0)?.ext)
        assertEquals("png", gallery?.pages?.get(1)?.ext)

        assertEquals(
            "https://i.nhentai.net/galleries/987654/1.jpg",
            gallery?.pages?.get(0)?.imageUrl(gallery.mediaId)
        )
        assertEquals(
            "https://i.nhentai.net/galleries/987654/2.png",
            gallery?.pages?.get(1)?.imageUrl(gallery.mediaId)
        )
    }

    @Test
    fun `parseGalleryJson корректно обрабатывает формат API v2 со строковым media_id и прямыми путями к webp`() {
        val gallery = NParser.parseGalleryJson(sampleJsonV2)

        assertNotNull(gallery)
        assertEquals(697415L, gallery?.id)
        assertEquals(4330179L, gallery?.mediaId)
        assertEquals("Test Doujinshi", gallery?.title)
        assertEquals(2, gallery?.numPages)
        assertEquals(42, gallery?.numFavorites)

        assertEquals(2, gallery?.pages?.size)
        assertEquals("jpg", gallery?.pages?.get(0)?.ext)
        assertEquals("webp", gallery?.pages?.get(1)?.ext)

        assertEquals(
            "https://i.nhentai.net/galleries/4330179/1.jpg",
            gallery?.pages?.get(0)?.imageUrl(gallery.mediaId)
        )
        assertEquals(
            "https://i.nhentai.net/galleries/4330179/2.webp",
            gallery?.pages?.get(1)?.imageUrl(gallery.mediaId)
        )

        assertEquals(
            "https://t.nhentai.net/galleries/4330179/1t.jpg.webp",
            gallery?.pages?.get(0)?.thumbnailUrl(gallery.mediaId)
        )
        assertEquals(
            "https://t.nhentai.net/galleries/4330179/cover.jpg.webp",
            gallery?.coverUrl
        )
        assertEquals(
            "https://t.nhentai.net/galleries/4330179/thumb.jpg.webp",
            gallery?.thumbnailUrl
        )
    }

    @Test
    fun `NPage extFromApiType преобразует символы API в расширения файлов`() {
        assertEquals("jpg", NPage.extFromApiType("j"))
        assertEquals("png", NPage.extFromApiType("p"))
        assertEquals("gif", NPage.extFromApiType("g"))
        assertEquals("webp", NPage.extFromApiType("w"))
        assertEquals("jpg", NPage.extFromApiType("unknown"))
    }

    @Test
    fun `parseGalleryHtml извлекает JSON из тега script`() {
        val html = """
            <html>
            <head>
              <script>window._gallery = JSON.parse("$sampleJson");</script>
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val gallery = NParser.parseGalleryHtml(html)
        assertNotNull(gallery)
        assertEquals(123456L, gallery?.id)
        assertEquals("Test Manga", gallery?.title)
    }

    @Test
    fun `parseSearchHtml парсит элементы списка gallery`() {
        val html = """
            <html>
            <body>
              <div class="gallery">
                <a href="/g/543210/" class="cover">
                  <img src="https://t.nhentai.net/galleries/112233/thumb.jpg" />
                  <div class="caption">Awesome Doujinshi Title</div>
                </a>
              </div>
            </body>
            </html>
        """.trimIndent()

        val list = NParser.parseSearchHtml(html)
        assertEquals(1, list.size)
        assertEquals(543210L, list[0].id)
        assertEquals(112233L, list[0].mediaId)
        assertEquals("Awesome Doujinshi Title", list[0].title)
    }

    @Test
    fun `parseGalleryListJson парсит ответ API v2 со структурой result`() {
        val json = """
            {
              "result": [
                {
                  "id": 697545,
                  "media_id": "4330814",
                  "english_title": "Test Title [English]",
                  "japanese_title": "テスト",
                  "thumbnail": "galleries/4330814/thumb.webp",
                  "thumbnail_width": 250,
                  "thumbnail_height": 353,
                  "num_pages": 318,
                  "num_favorites": 1200,
                  "tag_ids": [12227]
                }
              ],
              "num_pages": 10,
              "per_page": 25,
              "total": 250
            }
        """.trimIndent()

        val list = NParser.parseGalleryListJson(json)
        assertEquals(1, list.size)
        assertEquals(697545L, list[0].id)
        assertEquals(4330814L, list[0].mediaId)
        assertEquals("Test Title [English]", list[0].title)
        assertEquals("https://t.nhentai.net/galleries/4330814/thumb.webp", list[0].thumbnailUrl)
        assertEquals(318, list[0].numPages)
        assertEquals("English", list[0].language)
    }

    @Test
    fun `parseGalleryListJson парсит прямой массив API v2 popular`() {
        val json = """
            [
              {
                "id": 697416,
                "media_id": "4330180",
                "english_title": "Popular Doujin [Russian]",
                "japanese_title": "テスト",
                "thumbnail": "galleries/4330180/thumb.webp",
                "num_pages": 52,
                "num_favorites": 7418,
                "tag_ids": []
              }
            ]
        """.trimIndent()

        val list = NParser.parseGalleryListJson(json)
        assertEquals(1, list.size)
        assertEquals(697416L, list[0].id)
        assertEquals("Popular Doujin [Russian]", list[0].title)
        assertEquals("https://t.nhentai.net/galleries/4330180/thumb.webp", list[0].thumbnailUrl)
        assertEquals(52, list[0].numPages)
        assertEquals("Russian", list[0].language)
    }

    @Test
    fun `parseTagsJson парсит массив тегов из API v2 tags search`() {
        val json = """
            [
              {
                "id": 123503,
                "type": "parody",
                "name": "genshin impact",
                "slug": "genshin-impact",
                "url": "/parody/genshin-impact/",
                "count": 4518
              }
            ]
        """.trimIndent()

        val tags = NParser.parseTagsJson(json)
        assertEquals(1, tags.size)
        assertEquals(123503L, tags[0].id)
        assertEquals("parody", tags[0].type)
        assertEquals("genshin impact", tags[0].name)
        assertEquals(4518, tags[0].count)
        assertEquals("/parody/genshin-impact/", tags[0].url)
    }

    @Test
    fun `parseTagPage reads catalog rows and page count`() {
        val response = """{
            "result":[{"id":12,"type":"artist","name":"sample artist","count":42,"url":"/artist/sample-artist/"}],
            "num_pages":7
        }"""

        val result = NParser.parseTagPage(response)

        assertEquals(7, result.totalPages)
        val tag = result.tags.single()
        assertEquals(12L, tag.id)
        assertEquals("artist", tag.type)
        assertEquals(42, tag.count)
    }

    @Test
    fun `parseGalleryJson парсит связанные галереи related`() {
        val json = """
            {
              "id": 100,
              "media_id": "200",
              "title": { "english": "Main Title" },
              "images": { "pages": [] },
              "num_pages": 1,
              "related": [
                {
                  "id": 101,
                  "media_id": "201",
                  "english_title": "Related Doujin",
                  "thumbnail": "galleries/201/thumb.webp",
                  "num_pages": 24
                }
              ]
            }
        """.trimIndent()

        val gallery = NParser.parseGalleryJson(json)
        assertNotNull(gallery)
        assertEquals(1, gallery?.relatedGalleries?.size)
        assertEquals(101L, gallery?.relatedGalleries?.get(0)?.id)
        assertEquals("Related Doujin", gallery?.relatedGalleries?.get(0)?.title)
        assertEquals("https://t.nhentai.net/galleries/201/thumb.webp", gallery?.relatedGalleries?.get(0)?.thumbnailUrl)
    }

    @Test
    fun `parseGalleryPageJson парсит галереи и общее количество страниц num_pages`() {
        val json = """
            {
              "result": [
                {
                  "id": 501,
                  "media_id": "901",
                  "title": { "english": "Book 1" },
                  "images": { "pages": [] },
                  "num_pages": 10
                },
                {
                  "id": 502,
                  "media_id": "902",
                  "title": { "english": "Book 2" },
                  "images": { "pages": [] },
                  "num_pages": 15
                }
              ],
              "num_pages": 42,
              "per_page": 25
            }
        """.trimIndent()

        val pageResult = NParser.parseGalleryPageJson(json, defaultPage = 3)
        assertEquals(2, pageResult.items.size)
        assertEquals(3, pageResult.page)
        assertEquals(42, pageResult.totalPages)
        assertEquals(501L, pageResult.items[0].id)
        assertEquals(502L, pageResult.items[1].id)
    }

    @Test
    fun `parseSearchHtmlPage парсит пагинацию из HTML разметки`() {
        val html = """
            <html>
              <body>
                <div class="gallery">
                  <a href="/g/777/"><img src="https://t.nhentai.net/galleries/888/thumb.jpg"/></a>
                  <div class="caption">HTML Doujin</div>
                </div>
                <section class="pagination">
                  <a href="?page=1" class="page">1</a>
                  <a href="?page=2" class="page current">2</a>
                  <a href="?page=20" class="last">20</a>
                </section>
              </body>
            </html>
        """.trimIndent()

        val pageResult = NParser.parseSearchHtmlPage(html, defaultPage = 2)
        assertEquals(1, pageResult.items.size)
        assertEquals(2, pageResult.page)
        assertEquals(20, pageResult.totalPages)
        assertEquals(777L, pageResult.items[0].id)
    }
}
