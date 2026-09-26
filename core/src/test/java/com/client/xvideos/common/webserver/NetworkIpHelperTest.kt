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

    @Test
    fun `buildServerUrl handles blank ip and invalid ports safely`() {
        assertEquals("", NetworkIpHelper.buildServerUrl("", 8080))
        assertEquals("", NetworkIpHelper.buildServerUrl("   ", 8080))
        assertEquals("", NetworkIpHelper.buildServerUrl("192.168.1.1", 0))
        assertEquals("", NetworkIpHelper.buildServerUrl("192.168.1.1", -1))
        assertEquals("", NetworkIpHelper.buildServerUrl("192.168.1.1", 70000))
    }

    @Test
    fun `isValidPort correctly validates ports`() {
        assertEquals(true, NetworkIpHelper.isValidPort(80))
        assertEquals(true, NetworkIpHelper.isValidPort(8080))
        assertEquals(true, NetworkIpHelper.isValidPort(65535))
        assertEquals(false, NetworkIpHelper.isValidPort(0))
        assertEquals(false, NetworkIpHelper.isValidPort(-5))
        assertEquals(false, NetworkIpHelper.isValidPort(65536))
    }

    @Test
    fun `isValidIpv4 correctly validates IPv4 addresses`() {
        assertEquals(true, NetworkIpHelper.isValidIpv4("192.168.1.1"))
        assertEquals(true, NetworkIpHelper.isValidIpv4("127.0.0.1"))
        assertEquals(true, NetworkIpHelper.isValidIpv4("0.0.0.0"))
        assertEquals(true, NetworkIpHelper.isValidIpv4("255.255.255.255"))

        assertEquals(false, NetworkIpHelper.isValidIpv4(null))
        assertEquals(false, NetworkIpHelper.isValidIpv4(""))
        assertEquals(false, NetworkIpHelper.isValidIpv4("   "))
        assertEquals(false, NetworkIpHelper.isValidIpv4("192.168.1"))
        assertEquals(false, NetworkIpHelper.isValidIpv4("192.168.1.256"))
        assertEquals(false, NetworkIpHelper.isValidIpv4("192.168.01.1"))
        assertEquals(false, NetworkIpHelper.isValidIpv4("abc.def.ghi.jkl"))
    }
}
