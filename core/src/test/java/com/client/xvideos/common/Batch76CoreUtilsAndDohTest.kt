package com.client.xvideos.common

import com.client.xvideos.common.net.doh.DohAnswer
import com.client.xvideos.common.net.doh.DohProvider
import com.client.xvideos.common.net.doh.DohResponse
import com.client.xvideos.common.util.formatAsBitsPerSec
import com.client.xvideos.common.util.isValidByteString
import com.client.xvideos.common.util.toHoursMinSec
import com.client.xvideos.common.util.toSecondsOrZero
import com.client.xvideos.common.videoplayer.model.PlayerOption
import com.client.xvideos.common.videoplayer.model.PlayerPlaybackConfig
import com.client.xvideos.common.videoplayer.model.PlayerSpeed
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.client.xvideos.common.webserver.WebServerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch76CoreUtilsAndDohTest {

    @Test
    fun `DohResponse and DohAnswer filtering and type helpers`() {
        val ipv4Answer = DohAnswer(name = "xvideos.com", type = 1, data = "1.2.3.4")
        val ipv6Answer = DohAnswer(name = "xvideos.com", type = 28, data = "2001:db8::1")
        val cnameAnswer = DohAnswer(name = "alias.com", type = 5, data = "xvideos.com")

        assertTrue(ipv4Answer.isA)
        assertTrue(ipv6Answer.isAaaa)
        assertTrue(cnameAnswer.isCname)
        assertFalse(ipv4Answer.isCname)

        val mixedResponse = DohResponse(
            status = 0,
            answer = listOf(ipv4Answer, ipv6Answer, cnameAnswer)
        )
        assertFalse(mixedResponse.hasOnlyIpv4)
        assertTrue(mixedResponse.hasIpv6)
        assertEquals(1, mixedResponse.filterByType(1).size)
        assertEquals(1, mixedResponse.filterByType(5).size)

        val v4Only = DohResponse(status = 0, answer = listOf(ipv4Answer))
        assertTrue(v4Only.hasOnlyIpv4)
        assertFalse(v4Only.hasIpv6)
    }

    @Test
    fun `DohProvider preset and name lookup`() {
        assertTrue(DohProvider.CLOUDFLARE.isPreset)
        assertTrue(DohProvider.GOOGLE.isPreset)
        assertTrue(DohProvider.ADGUARD.isPreset)
        assertFalse(DohProvider.CUSTOM.isPreset)

        assertEquals(DohProvider.CLOUDFLARE, DohProvider.fromNameOrNull("CLOUDFLARE"))
        assertEquals(DohProvider.GOOGLE, DohProvider.fromNameOrNull("google"))
        assertNull(DohProvider.fromNameOrNull("unknown"))

        assertTrue(DohProvider.allNames.contains("CLOUDFLARE"))
        assertTrue(DohProvider.allNames.contains("CUSTOM"))
    }

    @Test
    fun `PlayerSpeed and PlayerOption inspect helpers`() {
        assertTrue(PlayerSpeed.X2.isFastest)
        assertFalse(PlayerSpeed.X1.isFastest)
        assertTrue(PlayerSpeed.X0_25.isSlowest)
        assertFalse(PlayerSpeed.X1.isSlowest)

        assertEquals(listOf("0.25x", "0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x"), PlayerSpeed.allDisplayNames)
        assertEquals(5, PlayerOption.allOptions.size)
        assertTrue(PlayerOption.allOptions.contains(PlayerOption.SPEED))
    }

    @Test
    fun `PlayerPlaybackConfig speed checks and mutators`() {
        val normalConfig = PlayerPlaybackConfig(url = "https://cdn.example.com/v.mp4")
        assertTrue(normalConfig.isNormalSpeed)
        assertFalse(normalConfig.hasCustomSpeed)

        val fastConfig = normalConfig.withSpeed(PlayerSpeed.X1_5)
        assertFalse(fastConfig.isNormalSpeed)
        assertTrue(fastConfig.hasCustomSpeed)

        val fillConfig = normalConfig.withFitMode(ScreenResize.FILL)
        assertEquals(ScreenResize.FILL, fillConfig.size)

        val updatedUrl = normalConfig.withUrl("https://new.cdn.com/v2.mp4")
        assertEquals("https://new.cdn.com/v2.mp4", updatedUrl.url)
    }

    @Test
    fun `Formatting byte strings and bit speeds`() {
        assertTrue(isValidByteString("12.4 MB"))
        assertTrue(isValidByteString("500 B"))
        assertTrue(isValidByteString("1.25 GB"))
        assertTrue(isValidByteString("64 KB"))
        assertFalse(isValidByteString("12.4"))
        assertFalse(isValidByteString(null))

        val speedBytes = 1000L
        assertEquals("8000 bps", speedBytes.formatAsBitsPerSec())
    }

    @Test
    fun `Time formatting with hours and string parsers`() {
        val fortyFiveSeconds = 45L
        assertEquals("00:45", fortyFiveSeconds.toHoursMinSec())

        val tenMinutes = 600L
        assertEquals("10:00", tenMinutes.toHoursMinSec())

        val oneHourTenMinutes = 4200L
        assertEquals("1:10:00", oneHourTenMinutes.toHoursMinSec())

        assertEquals(123L, "123".toSecondsOrZero())
        assertEquals(0L, "".toSecondsOrZero())
        assertEquals(0L, null.toSecondsOrZero())
    }

    @Test
    fun `WebServerState error tracking and reset`() {
        WebServerState.reset()
        assertFalse(WebServerState.isRunning.value)
        assertFalse(WebServerState.isOnline)
        assertFalse(WebServerState.hasError)

        WebServerState.updateRunning(running = true, url = "http://192.168.1.10:8080", ip = "192.168.1.10", port = 8080)
        assertTrue(WebServerState.isRunning.value)
        assertTrue(WebServerState.isOnline)
        assertFalse(WebServerState.hasError)

        WebServerState.setError("Port in use")
        assertTrue(WebServerState.hasError)
        assertEquals("Port in use", WebServerState.lastError.value)

        WebServerState.reset()
        assertFalse(WebServerState.isRunning.value)
        assertFalse(WebServerState.isOnline)
        assertFalse(WebServerState.hasError)
        assertNull(WebServerState.lastError.value)
    }
}
