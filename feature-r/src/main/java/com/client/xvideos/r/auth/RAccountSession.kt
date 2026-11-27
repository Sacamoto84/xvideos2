package com.client.xvideos.r.auth

import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.util.runCatchingCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.util.Base64
import java.util.UUID

sealed interface RAccountStatus {
    data object Loading : RAccountStatus
    data object SignedOut : RAccountStatus
    data object Expired : RAccountStatus
    data class SignedIn(val username: String, internal val sessionId: String) : RAccountStatus
}

object RAccountSession {
    private val store = RAccountSessionStore(
        read = Settings::readRedgifsSession,
        write = Settings::writeRedgifsSession,
        renew = ::renewRWebSession,
        clearProfile = ::clearRWebProfile,
    )
    val status = store.status

    suspend fun restore() {
        store.authorization()
    }

    suspend fun signOut(): Boolean = store.signOut()

    internal suspend fun save(credentials: RAccountCredentials): Boolean = store.save(credentials)

    internal suspend fun activeToken(): String? = store.authorization()?.token

    internal suspend fun authorization(
        expectedAccount: RAccountStatus.SignedIn?,
        rejectedToken: String? = null,
    ): RAccountAuthorization? = store.authorization(expectedAccount, rejectedToken)
}

internal data class RAccountAuthorization(val account: RAccountStatus.SignedIn, val token: String)

internal class RAccountSessionStore(
    private val read: () -> String?,
    private val write: (String?) -> Boolean,
    private val renew: suspend (String) -> RWebSessionResult,
    private val clearProfile: suspend (String) -> Unit,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private var session: Session? = null
    private val mutableStatus = MutableStateFlow<RAccountStatus>(RAccountStatus.Loading)
    val status = mutableStatus.asStateFlow()

    private class Session(
        var credentials: RAccountCredentials,
        var claims: RAccountTokenClaims,
        val account: RAccountStatus.SignedIn,
        var nextRenewalAt: Long = 0L,
    )

    suspend fun save(credentials: RAccountCredentials): Boolean = withContext(Dispatchers.IO) {
        val claims = credentials.claims() ?: return@withContext false
        mutex.withLock {
            if (claims.expiresAtMillis <= now()) return@withLock false
            load()
            if (!persist(credentials)) return@withLock false
            val previousProfile = session?.credentials?.webProfile
            setSession(credentials, claims)
            if (previousProfile != credentials.webProfile) previousProfile?.let { clearProfile(it) }
            true
        }
    }

    suspend fun signOut(): Boolean = withContext(Dispatchers.IO + NonCancellable) {
        mutex.withLock {
            load()
            if (!write(null)) return@withLock false
            val profile = session?.credentials?.webProfile
            session = null
            mutableStatus.value = RAccountStatus.SignedOut
            profile?.let { clearProfile(it) }
            true
        }
    }

    suspend fun authorization(
        expectedAccount: RAccountStatus.SignedIn? = null,
        rejectedToken: String? = null,
    ): RAccountAuthorization? = withContext(Dispatchers.IO) {
        mutex.withLock {
            load()
            val current = session ?: return@withLock null
            if (expectedAccount != null && current.account != expectedAccount) return@withLock null
            val token = current.credentials.token
            if (current.claims.expiresAtMillis > now() && token != rejectedToken) {
                return@withLock RAccountAuthorization(current.account, token)
            }

            val webProfile = current.credentials.webProfile
            if (webProfile == null) {
                expire()
                return@withLock null
            }
            if (current.nextRenewalAt > now()) return@withLock null
            when (val result = renew(webProfile)) {
                is RWebSessionResult.Renewed -> {
                    if (replaceToken(current, result.token)) return@withLock RAccountAuthorization(current.account, result.token)
                    current.nextRenewalAt = now() + RETRY_DELAY_MILLIS
                }
                RWebSessionResult.Unavailable -> current.nextRenewalAt = now() + RETRY_DELAY_MILLIS
                RWebSessionResult.Ended -> {
                    expire()
                    clearProfile(webProfile)
                }
            }
            null
        }
    }

    private fun load() {
        if (status.value != RAccountStatus.Loading) return
        val stored = runCatchingCancellable { read()?.let { Json.decodeFromString<RAccountCredentials>(it) } }
            .getOrNull()
        val claims = stored?.claims()
        when {
            stored == null -> mutableStatus.value = RAccountStatus.SignedOut
            claims == null || stored.webProfile == null && claims.expiresAtMillis <= now() -> expire()
            else -> setSession(stored, claims)
        }
    }

    private fun setSession(credentials: RAccountCredentials, claims: RAccountTokenClaims) {
        val account = RAccountStatus.SignedIn(credentials.username, UUID.randomUUID().toString())
        session = Session(credentials, claims, account)
        mutableStatus.value = account
    }

    private fun replaceToken(current: Session, token: String): Boolean {
        if (token == current.credentials.token) return false
        val renewed = current.credentials.copy(token = token)
        val claims = renewed.claims() ?: return false
        if (claims.subject != current.claims.subject || claims.expiresAtMillis <= now()) return false
        if (!persist(renewed)) return false
        current.credentials = renewed
        current.claims = claims
        current.nextRenewalAt = 0L
        return true
    }

    private fun expire() {
        session = null
        mutableStatus.value = RAccountStatus.Expired
        runCatchingCancellable { write(null) }
    }

    private fun persist(credentials: RAccountCredentials): Boolean =
        write(Json.encodeToString(credentials))

    private companion object {
        const val RETRY_DELAY_MILLIS = 5_000L
    }
}

internal data class RAccountTokenClaims(val subject: String, val expiresAtMillis: Long)

@Serializable
internal data class RAccountCredentials(
    val token: String,
    val username: String,
    val webProfile: String? = null,
) {
    fun claims(): RAccountTokenClaims? = runCatching {
        require(token.length <= 16_384 && username.isNotBlank() && username.length <= 128)
        require(webProfile == null || isRWebProfileName(webProfile))
        val parts = token.split('.')
        require(parts.size == 3 && parts.all { it.isNotEmpty() })
        val claims = Json.parseToJsonElement(Base64.getUrlDecoder().decode(parts[1]).decodeToString()).jsonObject
        require(claims["iss"]?.jsonPrimitive?.content == R_PROVIDER_ORIGIN)
        val subject = requireNotNull(claims["sub"]?.jsonPrimitive?.content)
        require(subject.isNotBlank())
        val expires = requireNotNull(claims["exp"]?.jsonPrimitive?.longOrNull)
        RAccountTokenClaims(subject, Math.multiplyExact(expires, 1_000L))
    }.getOrNull()
}
