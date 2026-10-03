package com.client.xvideos.common.ui.statusbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusBarRequestsTest {

    @Test
    fun `вырез есть и заявок нет - бар виден`() {
        assertTrue(shouldShowStatusBar(hasTopCutout = true, hideRequests = 0))
    }

    @Test
    fun `выреза нет - бар скрыт при любом числе заявок`() {
        assertFalse(shouldShowStatusBar(hasTopCutout = false, hideRequests = 0))
        assertFalse(shouldShowStatusBar(hasTopCutout = false, hideRequests = 1))
    }

    @Test
    fun `есть заявка - бар скрыт даже с вырезом`() {
        assertFalse(shouldShowStatusBar(hasTopCutout = true, hideRequests = 1))
        assertFalse(shouldShowStatusBar(hasTopCutout = true, hideRequests = 2))
    }

    @Test
    fun `счётчик держит бар скрытым пока жива хоть одна заявка`() {
        val requests = StatusBarRequests()
        requests.acquireHide()
        requests.acquireHide()
        requests.releaseHide()
        assertEquals(1, requests.hideRequests)
        requests.releaseHide()
        assertEquals(0, requests.hideRequests)
    }

    @Test
    fun `лишний release не уводит счётчик в минус`() {
        val requests = StatusBarRequests()
        requests.releaseHide()
        assertEquals(0, requests.hideRequests)
    }
}
