package com.client.xvideos.r.common.block

import com.client.xvideos.r.common.block.useCase.blockGetGifsByUserNameAsListString
import com.client.xvideos.r.common.block.useCase.blockGetGifsInfoByUserName
import com.client.xvideos.r.common.block.useCase.blockItem
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.network.json.RJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockItemSafePathTest {

    @Test
    fun `blockItem отвергает небезопасные userName и id`() {
        val unsafeUser = RJson.decodeFromString<GifsInfo>("""{"id":"123","userName":"../escape"}""")
        assertTrue(blockItem(unsafeUser).isFailure)

        val unsafeId = RJson.decodeFromString<GifsInfo>("""{"id":"../123","userName":"valid_user"}""")
        assertTrue(blockItem(unsafeId).isFailure)

        val dotUser = RJson.decodeFromString<GifsInfo>("""{"id":"123","userName":".."}""")
        assertTrue(blockItem(dotUser).isFailure)

        val slashUser = RJson.decodeFromString<GifsInfo>("""{"id":"123","userName":"sub/dir"}""")
        assertTrue(blockItem(slashUser).isFailure)
    }

    @Test
    fun `blockGetGifsInfoByUserName отвергает небезопасные имена`() {
        assertEquals(emptyList<GifsInfo>(), blockGetGifsInfoByUserName(".."))
        assertEquals(emptyList<GifsInfo>(), blockGetGifsInfoByUserName("../foo"))
        assertEquals(emptyList<GifsInfo>(), blockGetGifsInfoByUserName("a/b"))
        assertEquals(emptyList<String>(), blockGetGifsByUserNameAsListString(".."))
        assertEquals(emptyList<String>(), blockGetGifsByUserNameAsListString("../foo"))
        assertEquals(emptyList<String>(), blockGetGifsByUserNameAsListString("a/b"))
    }
}
