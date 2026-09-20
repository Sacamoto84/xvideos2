package com.client.xvideos.l

import com.client.xvideos.l.featured.saved.isPartialDownload
import com.client.xvideos.l.featured.saved.lIsInside
import com.client.xvideos.l.featured.saved.lPicsDetailsIdentityKey
import com.client.xvideos.l.featured.saved.sanitizeFilePart
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.Thumbnails
import com.client.xvideos.l.model.isAnimatedMedia
import com.client.xvideos.l.model.lDownloadUrl
import com.client.xvideos.l.model.lFullScreenImageUrls
import com.client.xvideos.l.model.lSavedFileName
import com.client.xvideos.l.model.safeAspectRatio
import com.client.xvideos.l.net.extractIdFromUrl
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.calculateGridScrollIndex
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.selectionKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun `lSavedFileName нейтрализует path traversal и слеши в альбоме`() {
        val item = picture(
            urlToOriginal = "https://cdn/sample.jpg",
            album = "../../etc/passwd",
            width = 1920,
            height = 1080
        )
        val fileName = item.lSavedFileName()
        assertEquals("1920_1080_false_____etc_passwd_sample.jpg", fileName)
        assertFalse(fileName!!.contains('/'))
        assertFalse(fileName.contains('\\'))
        assertFalse(fileName.contains(".."))
    }

    /* ---------- extractIdFromUrl ---------- */

    @Test
    fun `extractIdFromUrl корректно извлекает id из url с префиксом названия`() {
        val url = "https://www.luscious.net/albums/favorite_pictures_123456/"
        assertEquals("123456", extractIdFromUrl(url))
    }

    @Test
    fun `extractIdFromUrl корректно извлекает id из url без префикса`() {
        val url = "https://members.luscious.net/albums/789012/"
        assertEquals("789012", extractIdFromUrl(url))
    }

    @Test
    fun `extractIdFromUrl возвращает null для неподходящих ссылок`() {
        assertEquals(null, extractIdFromUrl("https://www.luscious.net/users/john/"))
        assertEquals(null, extractIdFromUrl("random_string_without_album_path"))
    }

    @Test
    fun `extractIdFromUrl корректно извлекает id из относительного пути и ссылок с параметрами`() {
        assertEquals("374481", extractIdFromUrl("albums/animated-gifs_374481/"))
        assertEquals("374481", extractIdFromUrl("albums/374481"))
        assertEquals("555666", extractIdFromUrl("https://www.luscious.net/albums/555666/?sort=date&view=grid"))
    }

    @Test
    fun `lDownloadUrl fallback возвращает video url когда url_to_original равен null`() {
        val animatedItem = picture(
            urlToOriginal = null,
            urlToVideo = "https://cdn/video.mp4",
            isAnimated = true
        )
        assertEquals("https://cdn/video.mp4", animatedItem.lDownloadUrl())
    }

    @Test
    fun `lIsInside блокирует попытки выхода через относительные пути`() {
        val root = File("/app/storage/likes")
        val malicious = File(root, "../../system/file")
        assertFalse(lIsInside(root, malicious))
    }

    @Test
    fun `безопасный расчёт aspect ratio при нулевых размерах возвращает 1`() {
        val zeroWidth = picture(width = 0, height = 100)
        val zeroHeight = picture(width = 100, height = 0)
        val bothZero = picture(width = 0, height = 0)

        assertEquals(1f, zeroWidth.safeAspectRatio(), 0.001f)
        assertEquals(1f, zeroHeight.safeAspectRatio(), 0.001f)
        assertEquals(1f, bothZero.safeAspectRatio(), 0.001f)
        assertEquals(0.5f, picture(width = 100, height = 200).safeAspectRatio(), 0.001f)
    }

    @Test
    fun `calculateGridScrollIndex корректно сдвигает индекс с учётом заголовков`() {
        // Без лоадера: 1 заголовок (itemBefore), picture 0 -> grid index 1
        assertEquals(1, calculateGridScrollIndex(position = 0, itemCount = 10, showInitialLoading = false))
        assertEquals(6, calculateGridScrollIndex(position = 5, itemCount = 10, showInitialLoading = false))

        // С лоадером: 2 заголовка (itemBefore + loader), picture 0 -> grid index 2
        assertEquals(2, calculateGridScrollIndex(position = 0, itemCount = 10, showInitialLoading = true))
        assertEquals(7, calculateGridScrollIndex(position = 5, itemCount = 10, showInitialLoading = true))

        // Невалидный или выходящий за границы диапазона индекс -> null
        assertNull(calculateGridScrollIndex(position = -1, itemCount = 10, showInitialLoading = false))
        assertNull(calculateGridScrollIndex(position = 10, itemCount = 10, showInitialLoading = false))
        assertNull(calculateGridScrollIndex(position = 0, itemCount = 0, showInitialLoading = false))
    }

    @Test
    fun `безопасный доступ к элементу страницы возвращает fallback при выходе за границы`() {
        val list = listOf(picture(urlToOriginal = "https://cdn/0.jpg"))
        val fallback = picture(urlToOriginal = "https://cdn/fallback.jpg")

        assertEquals("https://cdn/0.jpg", (list.getOrNull(0) ?: fallback).url_to_original)
        assertEquals("https://cdn/fallback.jpg", (list.getOrNull(-1) ?: fallback).url_to_original)
        assertEquals("https://cdn/fallback.jpg", (list.getOrNull(1) ?: fallback).url_to_original)
    }

    @Test
    fun `isAnimatedMedia распознает анимации по флагу, video url и gif расширению`() {
        assertTrue(picture(isAnimated = true).isAnimatedMedia())
        assertTrue(picture(isAnimated = false, urlToVideo = "https://cdn/video.mp4").isAnimatedMedia())
        assertTrue(picture(isAnimated = false, urlToOriginal = "https://cdn/anim.gif?md5=123").isAnimatedMedia())
        assertFalse(picture(isAnimated = false, urlToOriginal = "https://cdn/pic.jpg").isAnimatedMedia())
    }
}
