package com.client.xvideos.common.vibrate

import android.content.Context
import org.junit.Test

class VibrateSafetyTest {

    @Test
    fun `vibrateWithPatternAndAmplitude does not crash with null service context`() {
        val fakeContext = object : FakeContext() {
            override fun getSystemService(name: String): Any? = null
        }
        // Should return early safely without throwing NullPointerException or ClassCastException
        vibrateWithPatternAndAmplitude(fakeContext)
    }

    private open class FakeContext : android.content.ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
    }
}
