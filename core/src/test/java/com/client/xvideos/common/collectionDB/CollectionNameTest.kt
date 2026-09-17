package com.client.xvideos.common.collectionDB

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionNameTest {

    @Test
    fun `обычное имя проходит и обрезается по краям`() {
        assertEquals("Мои гифки", CollectionName.normalizeOrNull("  Мои гифки  "))
        assertEquals("cats_2024", CollectionName.normalizeOrNull("cats_2024"))
    }

    @Test
    fun `пустое имя отвергается`() {
        assertEquals(null, CollectionName.normalizeOrNull(""))
        assertEquals(null, CollectionName.normalizeOrNull("   "))
    }

    @Test
    fun `разделители пути отвергаются`() {
        assertEquals(null, CollectionName.normalizeOrNull("a/b"))
        assertEquals(null, CollectionName.normalizeOrNull("a\\b"))
        assertEquals(null, CollectionName.normalizeOrNull("C:name"))
    }

    @Test
    fun `точечные имена отвергаются`() {
        assertEquals(null, CollectionName.normalizeOrNull("."))
        assertEquals(null, CollectionName.normalizeOrNull(".."))
    }

    @Test
    fun `имя с ведущей точкой отвергается`() {
        // .xlr_old_ — служебный префикс XlrBackupManager: коллекция с таким
        // именем была бы стёрта восстановлением бэкапа.
        assertEquals(null, CollectionName.normalizeOrNull(".xlr_old_L"))
        assertEquals(null, CollectionName.normalizeOrNull(".hidden"))
    }

    @Test
    fun `имя с нулевым байтом отвергается`() {
        assertEquals(null, CollectionName.normalizeOrNull("name\u0000injection"))
        assertEquals(null, CollectionName.normalizeOrNull("\u0000"))
        assertFalse(CollectionName.isValid("name\u0000injection"))
    }

    @Test
    fun `имя с управляющими символами отвергается`() {
        assertEquals(null, CollectionName.normalizeOrNull("name\nline"))
        assertEquals(null, CollectionName.normalizeOrNull("name\rline"))
        assertEquals(null, CollectionName.normalizeOrNull("name\ttab"))
        assertEquals(null, CollectionName.normalizeOrNull("\u001Ftest"))
        assertFalse(CollectionName.isValid("name\ninjection"))
    }

    @Test
    fun `isValid согласован с normalizeOrNull`() {
        assertTrue(CollectionName.isValid("нормальное"))
        assertFalse(CollectionName.isValid("../побег"))
    }
}
