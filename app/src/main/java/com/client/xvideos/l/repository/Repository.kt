package com.client.xvideos.l.repository

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.toMD5
import com.client.xvideos.l.KtorRequestHandler
import com.client.xvideos.l.net.Luscious
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.LinkedHashMap

data class LRepositoryProtectionUiState(
    val active: Boolean = false,
    val message: String = "",
    val retryAtMs: Long = 0L,
    val retryDelayMs: Long = 0L,
    val requestHash: String? = null,
    val updatedAtMs: Long = 0L
) {
    fun remainingMs(nowMs: Long = System.currentTimeMillis()): Long {
        return (retryAtMs - nowMs).coerceAtLeast(0L)
    }
}

class Repository(
    fileDb: AppFileDatabase,
    //private val luscious: Luscious,
    private val scope: CoroutineScope,
    //private val saved: SavedL
    context: Context
) {

    //Точка входа для GraphQL
    val apiUrl = Luscious.Companion.API

    @Volatile
    private var handler = createHandler()

    private val authMutex = Mutex()
    private val requestMutex = Mutex()
    private val ramCacheMutex = Mutex()
    private val ramCache = object : LinkedHashMap<String, String>(RAM_CACHE_MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
            return size > RAM_CACHE_MAX_ENTRIES
        }
    }

    @Volatile
    private var lastNetworkRequestAtMs = 0L

    @Volatile
    private var htmlChallengeCooldownUntilMs = 0L

    var protectionUiState by mutableStateOf(LRepositoryProtectionUiState())
        private set

    private val cacheUrlStringRomDao = fileDb.cacheUrlStringRom
    private val legacyCacheUrlStringRamDao = fileDb.cacheUrlStringRam
    private val lAlbumBundleCacheDao = fileDb.lAlbumBundleCache

    init {
        clearRamDao()
    }

    private fun createHandler(): KtorRequestHandler {
        return KtorRequestHandler(
            timeoutMillis = 5000,
            maxRetries = 5,
            retryStatusCodes = setOf(413, 429, 500, 502, 503, 504),
            backoffFactor = 1000
        )
    }

    fun logout() {
        Settings.l_login.setValue("")
        Settings.l_pass.setValue("")
        val oldHandler = handler
        handler = createHandler()
        oldHandler.close()
        clearHtmlChallengeUiState()
    }

    suspend fun openURI(
        data: String,
        type: RepositoryUriType = RepositoryUriType.POST,
        config: RepositoryUriConfig = RepositoryUriConfig.DIRECT
    ): Result<String> {
        Timber.i("!!! openURI()")
        //Timber.i("!!! openURI() data:$data type:$type config:$config")

        try {
            authMutex.withLock {
                val username = Settings.l_login.field.value.trim()
                val password = Settings.l_pass.field.value
                if (username.isBlank() || password.isBlank()) {
                    return Result.failure(IllegalStateException("Luscious credentials are not configured"))
                }
                handler.setCredentials(username, password)
                if (!handler.loggedIn) {
                    val loggedIn = handler.login()
                    if (!loggedIn) {
                        return Result.failure(IllegalStateException("Luscious login failed"))
                    }
                }
            }
        }
        catch (e: Exception){
            Timber.e(e, "!!! openURI() login error")
            return Result.failure(e)
        }

        if (type == RepositoryUriType.POST) {

            when (config) {

                //Запрос без кеширования
                RepositoryUriConfig.DIRECT -> {
                    return postJsonValidated(data)
                }

                // Read from persistent file cache, or request and store it.
                RepositoryUriConfig.CACHE_ROM -> {
                    try {
                        val cacheKey = data.toMD5()
                        val res = cacheUrlStringRomDao.get(cacheKey)
                        if (res != null) {
                            val cached = validateJsonResponse(res.content)
                            if (cached.isSuccess) {
                                //Timber.i("!!! openURI() CACHE_ROM res != null response:${res.content}")
                                return cached
                            }
                            Timber.w("!!! openURI() CACHE_ROM malformed cache: ${cached.exceptionOrNull()?.message}")
                            cacheUrlStringRomDao.delete(cacheKey)
                        }
                        val checkedResponse = postJsonValidated(data)
                        if (checkedResponse.isFailure) return checkedResponse

                        if (checkedResponse.getOrThrow().contains("{\"errors\":")){
                            SnackBar.error(checkedResponse.getOrThrow())
                            return Result.failure(Exception(checkedResponse.getOrThrow()))
                        }

                        cacheUrlStringRomDao.put(cacheKey, checkedResponse.getOrThrow())

                        //Timber.i("!!! openURI() CACHE_ROM net response:$response")
                        return checkedResponse
                    }
                    catch (e: Exception){
                        Timber.e(e, "!!! openURI() CACHE_ROM error")
                        return Result.failure(e)
                    }
                }

                // Read from temporary in-memory LRU cache, or request and store it in RAM only.
                RepositoryUriConfig.CACHE_RAM -> {
                    try {
                        val cacheKey = data.toMD5()
                        val res = getRamCache(cacheKey)
                        if (res != null) {
                            val cached = validateJsonResponse(res)
                            if (cached.isSuccess) {
                                //Timber.i("!!! openURI() CACHE_RAM res != null response:${res.content}")
                                return cached
                            }
                            Timber.w("!!! openURI() CACHE_RAM malformed cache: ${cached.exceptionOrNull()?.message}")
                            deleteRamCache(cacheKey)
                        }
                        val checkedResponse = postJsonValidated(data)
                        if (checkedResponse.isFailure) return checkedResponse

                        val checkedContent = checkedResponse.getOrThrow()
                        if (checkedContent.contains("{\"errors\":")){
                            SnackBar.error(checkedContent)
                            return Result.failure(Exception(checkedContent))
                        }

                        putRamCache(cacheKey, checkedContent)

                        //Timber.i("!!! openURI() CACHE_RAM net response:$response")

                        if (checkedContent.contains("{\"errors\":")) { SnackBar.error(checkedContent) }
                        return checkedResponse
                    }
                    catch (e: Exception){
                        Timber.e(e, "!!! openURI() CACHE_RAM error")
                        SnackBar.error(e.message?: "openURI() CACHE_RAM error")
                        return Result.failure(e)
                    }
                }
            }

        }

        return Result.failure(Exception("Некорректный тип запроса"))
    }

    private suspend fun postJsonValidated(data: String): Result<String> {
        var lastFailure: Result<String>? = null
        val requestHash = data.toMD5()

        for (attempt in 0 until HTML_CHALLENGE_RETRY_ATTEMPTS) {
            val response = try {
                postJsonThrottled(data)
            } catch (e: Exception) {
                return Result.failure(e)
            }

            val checkedResponse = validateJsonResponse(response)
            if (checkedResponse.isSuccess) {
                clearHtmlChallengeUiState()
                return checkedResponse
            }

            val error = checkedResponse.exceptionOrNull()
            if (!error.isHtmlChallengeResponse()) {
                return checkedResponse
            }

            lastFailure = checkedResponse
            val delayMs = htmlChallengeRetryDelay(attempt)
            scheduleHtmlChallengeCooldown(
                delayMs = delayMs,
                requestHash = requestHash,
                message = error?.message ?: "Server returned HTML instead of JSON"
            )
            Timber.w("!!! openURI() HTML challenge response, retry after ${delayMs}ms")
        }

        return lastFailure ?: Result.failure(IllegalStateException("Server returned HTML instead of JSON"))
    }

    private suspend fun postJsonThrottled(data: String): String {
        return requestMutex.withLock {
            val now = System.currentTimeMillis()
            val intervalWaitMs = MIN_NETWORK_REQUEST_INTERVAL_MS - (now - lastNetworkRequestAtMs)
            val challengeWaitMs = htmlChallengeCooldownUntilMs - now
            val waitMs = maxOf(intervalWaitMs, challengeWaitMs, 0L)
            if (waitMs > 0) delay(waitMs)

            try {
                handler.postJson(apiUrl, data)
            } finally {
                lastNetworkRequestAtMs = System.currentTimeMillis()
            }
        }
    }

    private fun scheduleHtmlChallengeCooldown(
        delayMs: Long,
        requestHash: String,
        message: String
    ) {
        val cooldownUntil = System.currentTimeMillis() + delayMs
        if (cooldownUntil > htmlChallengeCooldownUntilMs) {
            htmlChallengeCooldownUntilMs = cooldownUntil
        }
        protectionUiState = LRepositoryProtectionUiState(
            active = true,
            message = message,
            retryAtMs = htmlChallengeCooldownUntilMs,
            retryDelayMs = delayMs,
            requestHash = requestHash,
            updatedAtMs = System.currentTimeMillis()
        )
    }

    private fun clearHtmlChallengeUiState() {
        if (!protectionUiState.active) return
        protectionUiState = LRepositoryProtectionUiState(
            active = false,
            updatedAtMs = System.currentTimeMillis()
        )
    }

    private fun htmlChallengeRetryDelay(attempt: Int): Long {
        return HTML_CHALLENGE_RETRY_DELAYS_MS.getOrElse(attempt) {
            HTML_CHALLENGE_RETRY_DELAYS_MS.last()
        }
    }

    private fun validateJsonResponse(response: String): Result<String> {
        val normalized = response.trim()
        if (!normalized.startsWith("{")) {
            val message = if (normalized.startsWith("<!DOCTYPE", ignoreCase = true) || normalized.startsWith("<html", ignoreCase = true)) {
                "Server returned HTML instead of JSON: ${normalized.previewForLog()}"
            } else {
                "Server returned non-JSON response: ${normalized.previewForLog()}"
            }
            Timber.w("!!! openURI() $message")
            return Result.failure(IllegalStateException(message))
        }

        return runCatching {
            val json = JsonParser.parseString(normalized)
            if (!json.isJsonObject) {
                error("Response is not a JSON object: ${normalized.previewForLog()}")
            }
            if (json.asJsonObject.has("errors")) {
                error("GraphQL errors: ${normalized.previewForLog()}")
            }
            normalized
        }.onFailure {
            Timber.w("!!! openURI() malformed JSON response: ${normalized.previewForLog()} (${it.message})")
        }
    }

    suspend fun deleteCache(data: String, config: RepositoryUriConfig) {
        val cacheKey = data.toMD5()
        when (config) {
            RepositoryUriConfig.CACHE_RAM -> deleteRamCache(cacheKey)
            RepositoryUriConfig.CACHE_ROM -> cacheUrlStringRomDao.delete(cacheKey)
            RepositoryUriConfig.DIRECT -> Unit
        }
    }

    suspend fun getAlbumBundleCache(albumId: Int, maxAgeMs: Long): String? {
        val key = albumId.toString()
        val entry = lAlbumBundleCacheDao.get(key) ?: return null
        val ageMs = System.currentTimeMillis() - entry.timeCreate
        if (ageMs < 0L || ageMs > maxAgeMs) {
            lAlbumBundleCacheDao.delete(key)
            return null
        }
        return entry.content
    }

    suspend fun putAlbumBundleCache(albumId: Int, content: String) {
        lAlbumBundleCacheDao.put(albumId.toString(), content)
    }

    suspend fun deleteAlbumBundleCache(albumId: Int) {
        lAlbumBundleCacheDao.delete(albumId.toString())
    }

    private suspend fun getRamCache(cacheKey: String): String? {
        return ramCacheMutex.withLock { ramCache[cacheKey] }
    }

    private suspend fun putRamCache(cacheKey: String, content: String) {
        ramCacheMutex.withLock { ramCache[cacheKey] = content }
    }

    private suspend fun deleteRamCache(cacheKey: String) {
        ramCacheMutex.withLock { ramCache.remove(cacheKey) }
    }

    private fun String.previewForLog(): String {
        return replace(Regex("\\s+"), " ").take(200)
    }

    private fun Throwable?.isHtmlChallengeResponse(): Boolean {
        val message = this?.message ?: return false
        return message.startsWith("Server returned HTML instead of JSON")
    }

    private fun clearRamDao(){
        scope.launch(Dispatchers.IO) {
            ramCacheMutex.withLock { ramCache.clear() }
            legacyCacheUrlStringRamDao.deleteAll()
        }
    }

    private companion object {
        const val MIN_NETWORK_REQUEST_INTERVAL_MS = 300L
        const val HTML_CHALLENGE_RETRY_ATTEMPTS = 3
        const val RAM_CACHE_MAX_ENTRIES = 256
        val HTML_CHALLENGE_RETRY_DELAYS_MS = longArrayOf(5_000L, 10_000L, 15_000L)
    }

}


enum class RepositoryUriType {
    GET,
    POST,
}

enum class RepositoryUriConfig {
    DIRECT,    // Direct network request
    CACHE_RAM, // Temporary in-memory LRU cache, cleared with the process
    CACHE_ROM  // Persistent file cache
}
