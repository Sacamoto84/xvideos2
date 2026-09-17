package com.client.xvideos.common.webserver

import org.junit.Assert.assertNull
import org.junit.Test

class LocalLibraryProviderTest {

    @Test
    fun `resolveVideo rejects path traversal attempts with dots and slashes`() {
        assertNull(LocalLibraryProvider.resolveVideo("x", "../secret"))
        assertNull(LocalLibraryProvider.resolveVideo("x", "..\\secret"))
        assertNull(LocalLibraryProvider.resolveVideo("x", "../../etc/passwd"))
        assertNull(LocalLibraryProvider.resolveVideo("r", "../../../root"))
        assertNull(LocalLibraryProvider.resolveVideo("x", "test/file"))
    }

    @Test
    fun `resolvePoster rejects path traversal attempts`() {
        assertNull(LocalLibraryProvider.resolvePoster("x", "../../../secret"))
        assertNull(LocalLibraryProvider.resolvePoster("r", "folder/../file"))
    }

    @Test
    fun `resolveLMedia rejects path traversal in folder or fileName`() {
        assertNull(LocalLibraryProvider.resolveLMedia("../folder", "media.mp4"))
        assertNull(LocalLibraryProvider.resolveLMedia("folder", "../media.mp4"))
        assertNull(LocalLibraryProvider.resolveLMedia("folder", "../../secret.key"))
    }

    @Test
    fun `resolveVideo returns null for unknown sections`() {
        assertNull(LocalLibraryProvider.resolveVideo("unknown", "12345"))
    }

    @Test
    fun `resolveLCollectionMedia rejects path traversal attempts`() {
        assertNull(LocalLibraryProvider.resolveLCollectionMedia("../col", "item", "media.mp4"))
        assertNull(LocalLibraryProvider.resolveLCollectionMedia("col", "../item", "media.mp4"))
        assertNull(LocalLibraryProvider.resolveLCollectionMedia("col", "item", "../media.mp4"))
        assertNull(LocalLibraryProvider.resolveLCollectionMedia("col/..", "item", "media.mp4"))
        assertNull(LocalLibraryProvider.resolveLCollectionMedia("col", "item/..", "media.mp4"))
        assertNull(LocalLibraryProvider.resolveLCollectionMedia("col", "item", "media.mp4/.."))
        assertNull(LocalLibraryProvider.resolveLCollectionMedia("col", "item", "../../etc/passwd"))
    }

    @Test
    fun `resolveCollectionCover rejects invalid collection names and path traversal`() {
        assertNull(LocalLibraryProvider.resolveCollectionCover("r", "../secret"))
        assertNull(LocalLibraryProvider.resolveCollectionCover("r", "..\\secret"))
        assertNull(LocalLibraryProvider.resolveCollectionCover("l", "../../etc"))
        assertNull(LocalLibraryProvider.resolveCollectionCover("unknown", "validName"))
    }

    @Test
    fun `getCollectionItems returns empty list for invalid names or unknown sections`() {
        val result1 = LocalLibraryProvider.getCollectionItems("r", "../invalid")
        org.junit.Assert.assertTrue(result1.items.isEmpty())

        val result2 = LocalLibraryProvider.getCollectionItems("unknown", "validName")
        org.junit.Assert.assertTrue(result2.items.isEmpty())
    }
}
