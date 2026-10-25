package com.client.xvideos.xlive.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XLiveOnlineParserTest {

    @Test
    fun `признак эфира`() {
        assertEquals(true, parseXLiveOnline("""{"online": true}"""))
        assertEquals(false, parseXLiveOnline("""{"online": false}"""))
    }

    @Test
    fun `не JSON или без признака — null`() {
        assertNull(parseXLiveOnline("<!DOCTYPE html>"))
        assertNull(parseXLiveOnline("""{"status": "ok"}"""))
    }

    @Test
    fun `снимок по нику — метка округлена до минуты`() {
        assertEquals("https://thumb.live.mmcdn.com/riw/room_a.jpg?1792955400", xLiveSnapshotUrl("room_a", 1_792_955_430))
        assertEquals(xLiveSnapshotUrl("room_a", 1_792_955_400), xLiveSnapshotUrl("room_a", 1_792_955_459))
    }
}
