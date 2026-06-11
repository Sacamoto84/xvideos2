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
}
