package com.client.xvideos.common.gallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryTargetTest {

    @Test
    fun `видео определяется по расширению`() {
        assertTrue(GalleryTarget.isVideo("clip.mp4"))
        assertTrue(GalleryTarget.isVideo("clip.webm"))
        assertTrue(GalleryTarget.isVideo("clip.mkv"))
    }

    @Test
    fun `картинки и гифки видео не считаются`() {
        assertFalse(GalleryTarget.isVideo("pic.jpg"))
        assertFalse(GalleryTarget.isVideo("pic.png"))
        // Гифки в этом приложении приходят и как .gif, и как .mp4 — по имени
        // .gif попадает в Pictures, и это правильно: галерея покажет анимацию.
        assertFalse(GalleryTarget.isVideo("anim.gif"))
    }

    @Test
    fun `расширение сравнивается без учёта регистра`() {
        assertTrue(GalleryTarget.isVideo("CLIP.MP4"))
        assertTrue(GalleryTarget.isVideo("Clip.Mp4"))
    }

    @Test
    fun `имя без расширения не видео`() {
        assertFalse(GalleryTarget.isVideo("noextension"))
        assertEquals("", GalleryTarget.extensionOf("noextension"))
    }

    @Test
    fun `точки в имени не ломают расширение`() {
        assertTrue(GalleryTarget.isVideo("my.long.name.mp4"))
        assertEquals("mp4", GalleryTarget.extensionOf("my.long.name.mp4"))
    }

    // Выбор базовой папки (Movies против Pictures) юнит-тестом не проверяется:
    // Environment.DIRECTORY_* — не константы времени компиляции, и в
    // юнит-тестах android-стаб отдаёт по ним null. Сам выбор при этом покрыт
    // тестами isVideo выше — relativePath только подставляет его результат.

    @Test
    fun `RELATIVE_PATH заканчивается папкой приложения и слэшем`() {
        // Без завершающего слэша запрос существования файла в MediaStore
        // не сматчится — значение хранится там именно в таком виде.
        assertTrue(GalleryTarget.relativePath("clip.mp4").endsWith("/${GalleryTarget.DIR}/"))
        assertTrue(GalleryTarget.relativePath("pic.jpg").endsWith("/${GalleryTarget.DIR}/"))
    }
}
