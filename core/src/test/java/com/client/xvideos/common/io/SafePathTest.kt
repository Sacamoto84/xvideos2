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
    fun `пустое имя и двоеточие отвергаются`() {
        assertThrows(IllegalArgumentException::class.java) { normalizeRelativePath("   ") }
        assertThrows(IllegalArgumentException::class.java) { normalizeRelativePath("C:/data") }
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
}
