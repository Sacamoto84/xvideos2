package com.client.xvideos.n.ui.screens.gallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.IOException

class NServerFavoriteStateTest {
    @Test
    fun `a failed read retains the last confirmed value and prevents a write`() {
        val error = IOException("offline")
        val state = NServerFavoriteState(saved = true, loading = true).afterRead(Result.failure(error))

        assertEquals(true, state.saved)
        assertSame(error, state.readError)
        assertNull(state.desiredUpdate)
        assertEquals(false, state.afterRead(Result.success(true)).desiredUpdate)
    }

    @Test
    fun `an unknown state can only be read and a confirmed false can be toggled`() {
        val state = NServerFavoriteState()
        assertNull(state.desiredUpdate)
        assertEquals(true, state.afterRead(Result.success(false)).desiredUpdate)
    }
}
