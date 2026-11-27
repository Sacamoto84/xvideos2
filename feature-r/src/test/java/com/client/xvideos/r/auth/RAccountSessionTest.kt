package com.client.xvideos.r.auth

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun `expired token renews once after restart for parallel requests`() = runBlocking {
        val fixture = StoredSession()
        assertTrue(fixture.open().save(fixture.credentials(exp = 2L)))
        fixture.now = 3_000L
        val renewed = fixture.credentials(exp = 10L)
        fixture.result = RWebSessionResult.Renewed(renewed.token)

        val restarted = fixture.open()
        val authorizations = List(4) { async { restarted.authorization() } }.awaitAll()

        assertEquals(listOf(renewed.token), authorizations.map { it?.token }.distinct())
        assertEquals(1, fixture.renewals)
        assertEquals(renewed, fixture.stored?.let { Json.decodeFromString<RAccountCredentials>(it) })
        assertEquals(renewed.token, fixture.open().authorization()?.token)
        assertEquals(1, fixture.renewals)
    }

    @Test
    fun `a rejected token is replaced without changing the signed in account`() = runBlocking {
        val fixture = StoredSession()
        val old = fixture.credentials(exp = 5L)
        val accountStore = fixture.open()
        assertTrue(accountStore.save(old))
        val account = accountStore.authorization()!!.account
        val renewed = fixture.credentials(exp = 10L)
        fixture.result = RWebSessionResult.Renewed(renewed.token)

        assertEquals(renewed.token, accountStore.authorization(account, old.token)?.token)
        assertEquals(renewed.token, accountStore.authorization(account, old.token)?.token)
        assertEquals(account, accountStore.status.value)
        assertEquals(1, fixture.renewals)

        assertTrue(accountStore.signOut())
        assertEquals(listOf(fixture.profile), fixture.cleared)
        assertNull(fixture.stored)
        assertEquals(RAccountStatus.SignedOut, accountStore.status.value)
        assertTrue(accountStore.save(renewed))
        assertNull(accountStore.authorization(account, old.token))
    }

    @Test
    fun `temporary failures retain the session but a missing web session expires it`() = runBlocking {
        val fixture = StoredSession()
        val old = fixture.credentials(exp = 2L)
        fixture.stored = Json.encodeToString(old)
        fixture.now = 3_000L
        val accountStore = fixture.open()

        assertNull(accountStore.authorization())
        assertTrue(accountStore.status.value is RAccountStatus.SignedIn)
        assertEquals(old, fixture.stored?.let { Json.decodeFromString<RAccountCredentials>(it) })
        assertNull(accountStore.authorization())
        assertEquals(1, fixture.renewals)

        fixture.now = 9_000L
        fixture.result = RWebSessionResult.Renewed(fixture.credentials(exp = 20L, subject = "other").token)
        assertNull(accountStore.authorization())
        assertEquals(old, fixture.stored?.let { Json.decodeFromString<RAccountCredentials>(it) })

        fixture.now = 15_000L
        fixture.result = RWebSessionResult.Ended
        assertNull(accountStore.authorization())
        assertEquals(RAccountStatus.Expired, accountStore.status.value)
        assertEquals(listOf(fixture.profile), fixture.cleared)
        assertNull(fixture.stored)
    }

    @Test
    fun `legacy entries work until their original token expires`() = runBlocking {
        val fixture = StoredSession()
        val legacyToken = fixture.credentials(exp = 2L).token
        fixture.stored = Json.encodeToString(RAccountCredentials(legacyToken, "user"))
        val accountStore = fixture.open()

        assertEquals(legacyToken, accountStore.authorization()?.token)
        fixture.now = 3_000L
        assertNull(accountStore.authorization())
        assertEquals(RAccountStatus.Expired, accountStore.status.value)
        assertEquals(0, fixture.renewals)
    }

    private inner class StoredSession {
        var now = 1_000L
        var stored: String? = null
        var renewals = 0
        var result: RWebSessionResult = RWebSessionResult.Unavailable
        val profile = "redgifs-signin-12345678-1234-1234-1234-123456789012"
        val cleared = mutableListOf<String>()

        fun credentials(exp: Long, subject: String = "user"): RAccountCredentials {
            val jwt = tokenWithClaims("""{"iss":"https://auth2.redgifs.com","sub":"$subject","exp":$exp}""")
            return RAccountCredentials(jwt, "user", profile)
        }

        fun open() = RAccountSessionStore(
            read = { stored },
            write = {
                stored = it
                true
            },
            renew = {
                assertEquals(profile, it)
                renewals++
                result
            },
            clearProfile = { cleared.add(it) },
            now = { now },
        )
    }

    private fun futureExp(): Long = (System.currentTimeMillis() / 1000L) + 3600L

    private fun tokenWithClaims(json: String): String {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(json.encodeToByteArray())
        return "header.$payload.signature"
    }
}
