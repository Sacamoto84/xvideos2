package com.client.xvideos.common.p2p

import org.junit.Assert.assertEquals
import org.junit.Test

class P2pManifestCodecTest {

    private val sample = P2pManifest(
        type = P2pType.L,
        metadataFileName = "metadata.json",
        files = listOf(
            P2pManifestFile(name = "media.jpg", relativePath = "album_x/media.jpg", payloadId = 10L, size = 123L),
            P2pManifestFile(name = "metadata.json", relativePath = "album_x/metadata.json", payloadId = 11L, size = 456L),
        ),
    )

    @Test
    fun `json round trip preserves all fields`() {
        val json = P2pManifestCodec.toJson(sample)
        val back = P2pManifestCodec.fromJson(json)
        assertEquals(sample, back)
    }

    @Test
    fun `bytes round trip preserves all fields`() {
        val bytes = P2pManifestCodec.toBytes(sample)
        val back = P2pManifestCodec.fromBytes(bytes)
        assertEquals(sample, back)
    }

    @Test
    fun `album manifest round trip`() {
        val m = P2pManifest(
            type = P2pType.L_ALBUM,
            metadataFileName = "42.album",
            files = listOf(P2pManifestFile("42.album", "42.album", 1L, 10L)),
        )
        assertEquals(m, P2pManifestCodec.fromBytes(P2pManifestCodec.toBytes(m)))
    }

    @Test
    fun `collection manifest round trip`() {
        val m = P2pManifest(
            type = P2pType.L_COLLECTION,
            metadataFileName = null,
            files = listOf(P2pManifestFile("MyCol.zip", "MyCol.zip", 1L, 100L)),
        )
        assertEquals(m, P2pManifestCodec.fromBytes(P2pManifestCodec.toBytes(m)))
    }
}
