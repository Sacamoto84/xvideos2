package com.client.xvideos.r.auth

import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.util.runCatchingCancellable
import kotlinx.coroutines.Dispatchers
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

sealed interface RAccountStatus {
    data object Loading : RAccountStatus
    data object SignedOut : RAccountStatus
    data object Expired : RAccountStatus
    data class SignedIn(val username: String, val expiresAtMillis: Long) : RAccountStatus
}

object RAccountSession {
    private val mutex = Mutex()
    private var session: RAccountCredentials? = null
    private val mutableStatus = MutableStateFlow<RAccountStatus>(RAccountStatus.Loading)
    val status = mutableStatus.asStateFlow()

    suspend fun restore() {
        if (status.value != RAccountStatus.Loading) return
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (status.value != RAccountStatus.Loading) return@withLock
                val stored = runCatchingCancellable {
                    Settings.readRedgifsSession()?.let { Json.decodeFromString<RAccountCredentials>(it) }
                }.getOrNull()
                val profile = stored?.profile()
                if (profile != null && profile.expiresAtMillis > System.currentTimeMillis()) {
                    session = stored
                    mutableStatus.value = profile
                } else if (stored != null) {
                    expire()
                } else {
                    mutableStatus.value = RAccountStatus.SignedOut
                }
            }
        }
    }

    internal suspend fun save(credentials: RAccountCredentials): Boolean = withContext(Dispatchers.IO) {
        val profile = credentials.profile() ?: return@withContext false
        mutex.withLock {
            if (profile.expiresAtMillis <= System.currentTimeMillis()) return@withLock false
            if (!Settings.writeRedgifsSession(Json.encodeToString(credentials))) return@withLock false
            session = credentials
            mutableStatus.value = profile
            true
        }
    }

    suspend fun signOut(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!Settings.writeRedgifsSession(null)) return@withLock false
            session = null
            mutableStatus.value = RAccountStatus.SignedOut
            true
        }
    }

    internal suspend fun activeToken(expectedAccount: RAccountStatus.SignedIn? = null): String? {
        restore()
        val expiredToken = mutex.withLock {
            val profile = status.value as? RAccountStatus.SignedIn ?: return null
            if (expectedAccount != null && profile != expectedAccount) return null
            val token = session?.token ?: return null
            if (profile.expiresAtMillis > System.currentTimeMillis()) return token
            token
        }
        reject(expiredToken)
        return null
    }

    internal suspend fun reject(token: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (session?.token != token) return@withLock
            expire()
        }
    }

    suspend fun checkExpiry() {
        activeToken()
    }

    private fun expire() {
        session = null
        mutableStatus.value = RAccountStatus.Expired
        runCatchingCancellable { Settings.writeRedgifsSession(null) }
    }
}

@Serializable
internal class RAccountCredentials(val token: String, val username: String) {
    fun profile(): RAccountStatus.SignedIn? = runCatching {
        require(token.length <= 16_384 && username.isNotBlank() && username.length <= 128)
        val parts = token.split('.')
        require(parts.size == 3 && parts.all { it.isNotEmpty() })
        val claims = Json.parseToJsonElement(Base64.getUrlDecoder().decode(parts[1]).decodeToString()).jsonObject
        require(claims["iss"]?.jsonPrimitive?.content == "https://auth2.redgifs.com")
        require(!claims["sub"]?.jsonPrimitive?.content.isNullOrBlank())
        val expires = requireNotNull(claims["exp"]?.jsonPrimitive?.longOrNull)
        RAccountStatus.SignedIn(username, Math.multiplyExact(expires, 1_000L))
    }.getOrNull()
}
