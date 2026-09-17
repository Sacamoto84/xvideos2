package com.client.xvideos.common.io

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SafePathTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `нормализация приводит разделители и убирает пустые сегменты`() {
        assertEquals("a/b/c.txt", normalizeRelativePath("a\\b//c.txt"))
        assertEquals("a/b", normalizeRelativePath("/a/b/"))
    }

    @Test
    fun `путь с двумя точками отвергается`() {
        assertThrows(IllegalArgumentException::class.java) {
            normalizeRelativePath("../escape.txt")
        }
        assertThrows(IllegalArgumentException::class.java) {
            normalizeRelativePath("a/../../escape.txt")
        }
    }

    @Test
    fun `пустое имя, двоеточие и нулевой байт отвергаются`() {
        assertThrows(IllegalArgumentException::class.java) { normalizeRelativePath("   ") }
        assertThrows(IllegalArgumentException::class.java) { normalizeRelativePath("C:/data") }
        assertThrows(IllegalArgumentException::class.java) { normalizeRelativePath("data/\u0000/file.txt") }
        assertThrows(IllegalArgumentException::class.java) { normalizeRelativePath("data/file\u0000.txt") }
    }

    @Test
    fun `абсолютный путь обезвреживается, а не отвергается`() {
        // Ведущий слеш срезается: итог относительный и остаётся внутри корня.
        // Отказ здесь ломал бы zip от архиваторов, пишущих имена с ведущим
        // слешем, — при том что выйти за корень таким путём всё равно нельзя.
        assertEquals("data/a.jpg", normalizeRelativePath("/data/a.jpg"))
    }

    @Test
    fun `requireInside пропускает цель внутри корня`() {
        val root = tmp.newFolder("root")
        requireInside(root, File(root, "a/b.txt"))
        requireInside(root, root)
    }

    @Test
    fun `requireInside отвергает цель снаружи`() {
        val root = tmp.newFolder("root2")
        val outside = File(root.parentFile, "outside.txt")
        assertThrows(IllegalArgumentException::class.java) { requireInside(root, outside) }
    }

    @Test
    fun `requireInside не путает соседа с общим префиксом имени`() {
        val root = tmp.newFolder("data")
        val sibling = File(root.parentFile, "data_backup")
        assertThrows(IllegalArgumentException::class.java) { requireInside(root, sibling) }
    }

    @Test
    fun `isUnsafeItemName отвергает опасные имена и пропускает допустимые`() {
        // Опасные: пустые, текущий/родительский каталог, с разделителями, с null-байтом
        org.junit.Assert.assertTrue(isUnsafeItemName(""))
        org.junit.Assert.assertTrue(isUnsafeItemName("   "))
        org.junit.Assert.assertTrue(isUnsafeItemName("."))
        org.junit.Assert.assertTrue(isUnsafeItemName(".."))
        org.junit.Assert.assertTrue(isUnsafeItemName("../escape"))
        org.junit.Assert.assertTrue(isUnsafeItemName("a/b"))
        org.junit.Assert.assertTrue(isUnsafeItemName("a\\b"))
        org.junit.Assert.assertTrue(isUnsafeItemName("..\\escape"))
        org.junit.Assert.assertTrue(isUnsafeItemName("file\u0000.txt"))
        org.junit.Assert.assertTrue(isUnsafeItemName("\u0000"))
        org.junit.Assert.assertTrue(isUnsafeItemName("line\nbreak"))
        org.junit.Assert.assertTrue(isUnsafeItemName("line\rbreak"))
        org.junit.Assert.assertTrue(isUnsafeItemName("tab\titem"))

        // Безопасные: обычные имена, имена с точками, дефисами, двоеточиями
        org.junit.Assert.assertFalse(isUnsafeItemName("normal_name"))
        org.junit.Assert.assertFalse(isUnsafeItemName("12345"))
        org.junit.Assert.assertFalse(isUnsafeItemName(".hidden_item"))
        org.junit.Assert.assertFalse(isUnsafeItemName("id:456"))
        org.junit.Assert.assertFalse(isUnsafeItemName("item.with.dots"))
    }

    @Test
    fun `путь с управляющими символами отвергается`() {
        assertThrows(IllegalArgumentException::class.java) { normalizeRelativePath("data/\n/file.txt") }
        assertThrows(IllegalArgumentException::class.java) { normalizeRelativePath("data/\r/file.txt") }
        assertThrows(IllegalArgumentException::class.java) { normalizeRelativePath("data/\u0007/bell.txt") }
    }
}
