package com.client.xvideos.r.ui.explorer.tab.saved.tab.account

import com.client.xvideos.r.auth.RAccountStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountSavedActionsTest {

    @Test
    fun `finalizeAccountAction keeps success while the same session is active`() {
        val account = RAccountStatus.SignedIn("user", 1_000_000L)

        val actual = finalizeAccountAction(
            account = account,
            currentStatus = account,
            rawResult = Result.success("ok"),
        )

        assertEquals("ok", actual.getOrNull())
    }

    @Test
    fun `finalizeAccountAction fails when session expires or switches before completion`() {
        val account = RAccountStatus.SignedIn("user", 1_000_000L)
        val otherAccount = RAccountStatus.SignedIn("user", 2_000_000L)

        val switched = finalizeAccountAction(account, otherAccount, Result.success("ok"))
        val expired = finalizeAccountAction(account, RAccountStatus.Expired, Result.success("ok"))

        assertTrue(switched.isFailure)
        assertTrue(expired.isFailure)
    }

    @Test
    fun `finalizeAccountAction preserves original failure when request failed`() {
        val account = RAccountStatus.SignedIn("user", 1_000_000L)
        val error = IllegalStateException("network")

        val actual = finalizeAccountAction(account, RAccountStatus.Expired, Result.failure<Unit>(error))

        assertEquals(error, actual.exceptionOrNull())
    }
}
