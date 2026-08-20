package com.client.xvideos.l

import com.client.xvideos.l.featured.saved.isPartialDownload
import com.client.xvideos.l.featured.saved.lIsInside
import com.client.xvideos.l.featured.saved.lPicsDetailsIdentityKey
import com.client.xvideos.l.featured.saved.sanitizeFilePart
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.Thumbnails
import com.client.xvideos.l.model.lFullScreenImageUrls
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.selectionKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Чистые функции пакета L: выбор URL картинки, ключи идентичности элемента
 * и защита файловых операций. Раньше на `l/` не было ни одного теста, при этом
 * именно логика выбора URL уже приводила к правкам в полноэкранном просмотре.
 */
class LPureFunctionsTest {

    private fun picture(
        urlToOriginal: String? = null,
        urlToVideo: String? = null,
        album: String? = "123",
        thumbnails: List<Thumbnails>? = emptyList(),
        width: Int = 100,
        height: Int = 200,
        isAnimated: Boolean = false
    ) = PicsDetails(
        height = height,
        width = width,
        is_animated = isAnimated,
        url_to_original = urlToOriginal,
        url_to_video = urlToVideo,
        album = album,
        thumbnails = thumbnails
    )

    private fun thumb(url: String?, width: Int, height: Int, size: String? = null) =
        Thumbnails(width = width, height = height, size = size, url = url)

    /* ---------- lFullScreenImageUrls ---------- */

    @Test
    fun `превью отдаются от большего к меньшему`() {
        val item = picture(
            thumbnails = listOf(
                thumb("https://cdn/small.jpg", 320, 480, "small"),
                thumb("https://cdn/max.jpg", 1680, 2453, "xMax"),
                thumb("https://cdn/mid.jpg", 640, 935, "large_thumbnail")
            )
        )

        assertEquals(
            listOf("https://cdn/max.jpg", "https://cdn/mid.jpg", "https://cdn/small.jpg"),
            item.lFullScreenImageUrls()
        )
    }

    @Test
    fun `оригинал идёт запасным вариантом только когда превью нет`() {
        val withThumbs = picture(
            urlToOriginal = "https://cdn/original.jpg",
            thumbnails = listOf(thumb("https://cdn/thumb.jpg", 640, 480))
        )
        assertEquals(listOf("https://cdn/thumb.jpg"), withThumbs.lFullScreenImageUrls())

        val withoutThumbs = picture(urlToOriginal = "https://cdn/original.jpg", thumbnails = emptyList())
        assertEquals(listOf("https://cdn/original.jpg"), withoutThumbs.lFullScreenImageUrls())
    }

    @Test
    fun `битые превью не попадают в список`() {
        val item = picture(
            thumbnails = listOf(
                thumb(null, 800, 600),
                thumb("", 800, 600),
                thumb("https://cdn/ok.jpg", 400, 300)
            )
        )

        assertEquals(listOf("https://cdn/ok.jpg"), item.lFullScreenImageUrls())
    }

    @Test
    fun `у элемента без единой ссылки список пустой`() {
        assertEquals(emptyList<String>(), picture().lFullScreenImageUrls())
    }

    /* ---------- ключи идентичности ---------- */

    @Test
    fun `ключ элемента не зависит от query и якоря`() {
        val withQuery = picture(urlToOriginal = "https://cdn/a.jpg?md5=xxx&expires=1")
        val clean = picture(urlToOriginal = "https://cdn/a.jpg")

        assertEquals(lPicsDetailsIdentityKey(clean), lPicsDetailsIdentityKey(withQuery))
    }

    @Test
    fun `элемент без ссылок получает ключ из размеров`() {
        val item = picture(album = "77", width = 10, height = 20, isAnimated = true)

        assertEquals("77-10-20-true", lPicsDetailsIdentityKey(item))
    }

    @Test
    fun `selectionKey берёт первую доступную ссылку`() {
        assertEquals("https://cdn/orig.jpg", picture(urlToOriginal = "https://cdn/orig.jpg").selectionKey())
        assertEquals("https://cdn/v.mp4", picture(urlToVideo = "https://cdn/v.mp4").selectionKey())
        assertEquals(
            "https://cdn/t.jpg",
            picture(thumbnails = listOf(thumb("https://cdn/t.jpg", 100, 100))).selectionKey()
        )
    }

    /* ---------- файловые операции ---------- */

    @Test
    fun `имя файла чистится от опасных символов`() {
        assertEquals("a_b_c", "a/b\\c".sanitizeFilePart())
        assertEquals("file.jpg", "file.jpg".sanitizeFilePart())
        assertEquals("", "///".sanitizeFilePart())
        // Точки остаются, но разделитель пути превращается в подчёркивание,
        // поэтому ".." перестаёт быть переходом на уровень выше.
        assertEquals(".._name", "../name".sanitizeFilePart())
    }

    @Test
    fun `выход за пределы корня не проходит`() {
        val root = File(System.getProperty("java.io.tmpdir"), "l_root")

        assertTrue(lIsInside(root, File(root, "item/media.jpg")))
        assertTrue(lIsInside(root, root))
        assertFalse(lIsInside(root, File(root, "../outside.jpg")))
        assertFalse(lIsInside(root, File(root.parentFile, "l_root_sibling/media.jpg")))
    }

    @Test
    fun `недокачанный файл распознаётся по расширению`() {
        assertTrue(File("/tmp/media.jpg.part").isPartialDownload())
        assertFalse(File("/tmp/media.jpg").isPartialDownload())
        assertFalse(File("/tmp/metadata.json").isPartialDownload())
    }
}
