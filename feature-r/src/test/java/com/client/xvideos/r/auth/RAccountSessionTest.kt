package com.client.xvideos.r.auth

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class RAccountSessionTest {

    @Test
    fun `без защищённого хранилища restore переводит сессию в SignedOut`() = runBlocking {
        RAccountSession.restore()
        assertEquals(RAccountStatus.SignedOut, RAccountSession.status.value)
        assertNull(RAccountSession.activeToken())
    }

    @Test
    fun `save отклоняет просроченный и некорректный токен, а также не сохраняет без keystore`() = runBlocking {
        RAccountSession.restore()

        val expired = tokenWithClaims("""{"iss":"https://auth2.redgifs.com","sub":"user","exp":1000}""")
        assertFalse(RAccountSession.save(RAccountCredentials(expired, "user")))

        val wrongIssuer = tokenWithClaims("""{"iss":"https://evil.example","sub":"user","exp":${futureExp()}}""")
        assertFalse(RAccountSession.save(RAccountCredentials(wrongIssuer, "user")))

        // Защищённое хранилище в JVM-тестах не инициализировано -> fail-closed: save возвращает false, статус не становится SignedIn.
        val validJwt = tokenWithClaims("""{"iss":"https://auth2.redgifs.com","sub":"user","exp":${futureExp()}}""")
        assertFalse(RAccountSession.save(RAccountCredentials(validJwt, "user")))
        assertEquals(RAccountStatus.SignedOut, RAccountSession.status.value)
        assertNull(RAccountSession.activeToken())
        assertFalse(RAccountSession.signOut())
    }

    private fun futureExp(): Long = (System.currentTimeMillis() / 1000L) + 3600L

    private fun tokenWithClaims(json: String): String {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(json.encodeToByteArray())
        return "header.$payload.signature"
    }
}
