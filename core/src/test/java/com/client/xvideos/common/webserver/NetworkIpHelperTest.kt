package com.client.xvideos.common.webserver

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkIpHelperTest {

    @Test
    fun `buildServerUrl formats correct http url`() {
        val url = NetworkIpHelper.buildServerUrl("192.168.1.55", 8080)
        assertEquals("http://192.168.1.55:8080", url)
    }

    @Test
    fun `buildServerUrl formats non standard port`() {
        val url = NetworkIpHelper.buildServerUrl("10.0.0.2", 9090)
        assertEquals("http://10.0.0.2:9090", url)
    }
}
