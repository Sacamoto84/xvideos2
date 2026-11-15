package com.client.xvideos.n

import com.client.xvideos.n.model.NPage
import com.client.xvideos.n.net.NParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Юнит-тесты парсера nhentai API и HTML.
 */
class NParserTest {

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
}
