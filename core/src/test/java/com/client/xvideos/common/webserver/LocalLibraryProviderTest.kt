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
}
