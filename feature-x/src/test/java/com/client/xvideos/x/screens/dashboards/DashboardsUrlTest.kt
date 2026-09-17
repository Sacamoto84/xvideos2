package com.client.xvideos.x.screens.dashboards

import com.client.xvideos.x.urlStart
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardsUrlTest {

    @Test
    fun `dashboard url for page 0 points to root normalized url`() {
        val url = buildDashboardUrl(0)
        assertEquals(urlStart, url)
    }

    @Test
    fun `dashboard url for positive page points to new segment`() {
        val url1 = buildDashboardUrl(1)
        assertEquals("$urlStart/new/1", url1)

        val url42 = buildDashboardUrl(42)
        assertEquals("$urlStart/new/42", url42)
    }

    @Test
    fun `dashboard url for negative page clamps to 0`() {
        val url = buildDashboardUrl(-10)
        assertEquals(urlStart, url)
    }

    @Test
    fun `dashboard url for page above maximum clamps to 19999`() {
        val url = buildDashboardUrl(25000)
        assertEquals("$urlStart/new/19999", url)
    }
}
