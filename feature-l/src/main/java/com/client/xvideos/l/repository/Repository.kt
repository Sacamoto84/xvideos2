package com.client.xvideos.l.repository

import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.util.toMD5
import com.client.xvideos.l.KtorRequestHandler
import com.client.xvideos.l.net.json.LJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.LinkedHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Состояние антибот-защиты (Cloudflare / HTML challenge) для отображения в UI и управления повторными попытками.
 *
 * @property active Активен ли режим ожидания кулдауна после получения HTML вместо JSON.
 * @property message Текстовое описание ошибки или причины блокировки.
 * @property retryAtMs Метка времени (UTC мс), до которой сетевые запросы заблокированы кулдауном.
 * @property retryDelayMs Длительность задержки перед повторной попыткой в миллисекундах.
 * @property requestHash Хэш запроса, вызвавшего срабатывание защиты.
 * @property updatedAtMs Время последнего обновления состояния защиты.
 */
data class LRepositoryProtectionUiState(
    val active: Boolean = false,
    val message: String = "",
    val retryAtMs: Long = 0L,
    val retryDelayMs: Long = 0L,
    val requestHash: String? = null,
    val updatedAtMs: Long = 0L
) {
    /**
     * Возвращает оставшееся время кулдауна в миллисекундах от текущего момента [nowMs].
     */
    fun remainingMs(nowMs: Long = System.currentTimeMillis()): Long {
        return (retryAtMs - nowMs).coerceAtLeast(0L)
    }
}

/**
 * Центральный сетевой репозиторий модуля Luscious.
 *
 * Отвечает за:
 * - управление сессией пользователя и авторизацией в [KtorRequestHandler];
 * - отправку GraphQL POST-запросов на [LusciousEndpoints.API];
 * - многоуровневое кэширование ответов (RAM LRU-кэш, постоянный ROM-кэш);
 * - обработку антибот-защиты (HTML challenge/Cloudflare) с экспоненциальным кулдауном;
 * - троттлинг сетевых запросов с минимальным интервалом.
 *
 * @param fileDb Локальная файловая база данных для доступа к ROM-кэшам.
 */
open class Repository(
    fileDb: AppFileDatabase,
) {

    /** Точка входа для GraphQL API Luscious. */
    val apiUrl = LusciousEndpoints.API

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

    // AtomicLong, а не @Volatile: cooldown продлевают параллельные запросы,
    // и "сравнил-записал" на volatile терял большее значение.
    private val htmlChallengeCooldownUntilMs = AtomicLong(0L)

    /**
     * Состояние антибот-защиты для экрана альбома.
     *
     * `StateFlow`, а не `mutableStateOf`: слой данных не должен зависеть от
     * рантайма Compose. Сторож слоёв это не ловил — он сравнивает только
     * импорты своего раздела и про `androidx.compose` ничего не знает. Писалось
     * оно к тому же из фоновых корутин, то есть snapshot-состояние менялось вне
     * главного потока.
     */
    private val _protectionUiState = MutableStateFlow(LRepositoryProtectionUiState())
    val protectionUiState: StateFlow<LRepositoryProtectionUiState> = _protectionUiState.asStateFlow()

    private val cacheUrlStringRomDao = fileDb.cacheUrlStringRom
    private val lAlbumBundleCacheDao = fileDb.lAlbumBundleCache

    private fun createHandler(): KtorRequestHandler {
        return KtorRequestHandler(
            timeoutMillis = 5000,
            maxRetries = 5,
            retryStatusCodes = RETRY_STATUS_CODES,
            backoffFactor = 1000
        )
    }

    /**
     * Выполняет выход из аккаунта Luscious: очищает сохраненные учетные данные,
     * пересоздает сетевой обработчик [KtorRequestHandler] и сбрасывает статус защиты.
     *
     * Под обоими мьютексами: без них close() старого клиента приходился
     * на середину выполняющегося запроса.
     */
    suspend fun logout() {
        authMutex.withLock {
            requestMutex.withLock {
                Settings.l_login.setValue("")
                Settings.l_pass.setValue("")
                val oldHandler = handler
                handler = createHandler()
                oldHandler.close()
            }
        }
        clearHtmlChallengeUiState()
    }

    /**
     * Проверяет и при необходимости выполняет авторизацию пользователя по сохраненным логину и паролю.
     */
    private suspend fun ensureAuthenticated(): Result<Unit> {
        return try {
            authMutex.withLock {
                val username = Settings.l_login.field.value.trim()
                val password = Settings.l_pass.field.value
                // Анонимный режим: без логина/пароля работаем без авторизации
                // (Luscious отдаёт меньше альбомов). С кредами — авторизуемся.
                if (username.isNotBlank() && password.isNotBlank()) {
                    handler.setCredentials(username, password)
                    if (!handler.loggedIn) {
                        val loggedIn = handler.login()
                        if (!loggedIn) {
                            return Result.failure(IllegalStateException("Luscious login failed"))
                        }
                    }
                }
                SUCCESS_UNIT
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "openURI() login error")
            Result.failure(e)
        }
    }

    /**
     * Запрашивает данные с использованием постоянного ROM-кэша (с проверкой срока жизни).
     */
    private suspend fun getFromCacheRom(data: String): Result<String> {
        return try {
            val cacheKey = data.toMD5()
            val res = cacheUrlStringRomDao.get(cacheKey)
            if (res != null) {
                val ageMs = System.currentTimeMillis() - res.timeCreate
                val cached = if (ageMs in 0L..ROM_CACHE_MAX_AGE_MS) {
                    validateJsonResponse(res.content)
                } else {
                    Timber.d("openURI() CACHE_ROM stale entry, ageMs:$ageMs")
                    Result.failure(IllegalStateException("stale"))
                }
                if (cached.isSuccess) {
                    return cached
                }
                cacheUrlStringRomDao.delete(cacheKey)
            }
            val checkedResponse = postJsonValidated(data, cacheKey)
            if (checkedResponse.isFailure) return checkedResponse

            cacheUrlStringRomDao.put(cacheKey, checkedResponse.getOrThrow())
            checkedResponse
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "openURI() CACHE_ROM error")
            Result.failure(e)
        }
    }

    /**
     * Запрашивает данные с использованием оперативного RAM-кэша в памяти.
     */
    private suspend fun getFromCacheRam(data: String): Result<String> {
        return try {
            val cacheKey = data.toMD5()
            val res = getRamCache(cacheKey)
            if (res != null) {
                val cached = validateJsonResponse(res)
                if (cached.isSuccess) {
                    return cached
                }
                Timber.w("openURI() CACHE_RAM malformed cache: ${cached.exceptionOrNull()?.message}")
                deleteRamCache(cacheKey)
            }
            val checkedResponse = postJsonValidated(data, cacheKey)
            if (checkedResponse.isFailure) return checkedResponse

            putRamCache(cacheKey, checkedResponse.getOrThrow())
            checkedResponse
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "openURI() CACHE_RAM error")
            Result.failure(e)
        }
    }

    /**
     * Выполняет GraphQL-запрос [data] к серверу Luscious в соответствии с политикой кэширования [config].
     *
     * @param data Сериализованное тело GraphQL-запроса (JSON).
     * @param config Политика кэширования (напрямую в сеть, RAM или ROM кэш).
     * @return Успешная строка валидного JSON-ответа либо [Result.failure].
     */
    open suspend fun openURI(
        data: String,
        config: RepositoryUriConfig = RepositoryUriConfig.DIRECT
    ): Result<String> {
        Timber.d("openURI()")

        val authResult = ensureAuthenticated()
        if (authResult.isFailure) {
            return Result.failure(authResult.exceptionOrNull() ?: IllegalStateException("Auth failed"))
        }

        return when (config) {
            RepositoryUriConfig.DIRECT -> postJsonValidated(data)
            RepositoryUriConfig.CACHE_ROM -> getFromCacheRom(data)
            RepositoryUriConfig.CACHE_RAM -> getFromCacheRam(data)
        }
    }

    /**
     * Выполняет POST-запрос с автоматическим повтором при обнаружении HTML challenge страницы.
     */
    private suspend fun postJsonValidated(
        data: String,
        requestHash: String = data.toMD5()
    ): Result<String> {
        var lastFailure: Result<String>? = null

        for (attempt in 0 until HTML_CHALLENGE_RETRY_ATTEMPTS) {
            val response = try {
                postJsonThrottled(data)
            } catch (e: CancellationException) {
                throw e
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
            Timber.w("openURI() HTML challenge response, retry after ${delayMs}ms")
        }

        return lastFailure ?: Result.failure(IllegalStateException("Server returned HTML instead of JSON"))
    }

    /**
     * Выполняет сетевой запрос с троттлингом минимального интервала между запросами.
     */
    private suspend fun postJsonThrottled(data: String): String {
        return requestMutex.withLock {
            val now = System.currentTimeMillis()
            val intervalWaitMs = MIN_NETWORK_REQUEST_INTERVAL_MS - (now - lastNetworkRequestAtMs)
            val challengeWaitMs = htmlChallengeCooldownUntilMs.get() - now
            val waitMs = maxOf(intervalWaitMs, challengeWaitMs, 0L)
            if (waitMs > 0) delay(waitMs)

            try {
                handler.postJson(apiUrl, data)
            } finally {
                lastNetworkRequestAtMs = System.currentTimeMillis()
            }
        }
    }

    /**
     * Устанавливает кулдаун ожидания после обнаружения антибот-челленджа.
     */
    private fun scheduleHtmlChallengeCooldown(
        delayMs: Long,
        requestHash: String,
        message: String
    ) {
        val cooldownUntil = System.currentTimeMillis() + delayMs
        val effectiveCooldown = htmlChallengeCooldownUntilMs.updateAndGet { current ->
            maxOf(current, cooldownUntil)
        }
        _protectionUiState.value = LRepositoryProtectionUiState(
            active = true,
            message = message,
            retryAtMs = effectiveCooldown,
            retryDelayMs = delayMs,
            requestHash = requestHash,
            updatedAtMs = System.currentTimeMillis()
        )
    }

    /**
     * Сбрасывает флаг активности защиты после успешного получения ответа.
     */
    private fun clearHtmlChallengeUiState() {
        if (!_protectionUiState.value.active) return
        _protectionUiState.value = LRepositoryProtectionUiState(
            active = false,
            updatedAtMs = System.currentTimeMillis()
        )
    }

    private fun htmlChallengeRetryDelay(attempt: Int): Long {
        return HTML_CHALLENGE_RETRY_DELAYS_MS.getOrElse(attempt) {
            HTML_CHALLENGE_RETRY_DELAYS_MS.last()
        }
    }

    /**
     * Проверяет полученный текст ответа на корректность формата JSON и отсутствие корневых ошибок GraphQL.
     */
    private fun validateJsonResponse(response: String): Result<String> {
        val normalized = response.trim()
        if (!normalized.startsWith("{")) {
            val message = if (normalized.startsWith("<!DOCTYPE", ignoreCase = true) || normalized.startsWith("<html", ignoreCase = true)) {
                "Server returned HTML instead of JSON: ${normalized.previewForLog()}"
            } else {
                "Server returned non-JSON response: ${normalized.previewForLog()}"
            }
            Timber.w("openURI() $message")
            return Result.failure(IllegalStateException(message))
        }

        return runCatching {
            val json = LJson.parseToJsonElement(normalized)
            if (json !is kotlinx.serialization.json.JsonObject) {
                error("Response is not a JSON object: ${normalized.previewForLog()}")
            }
            if (json.containsKey("errors")) {
                error("GraphQL errors: ${normalized.previewForLog()}")
            }
            normalized
        }.onFailure {
            Timber.w("openURI() malformed JSON response: ${normalized.previewForLog()} (${it.message})")
        }
    }

    /**
     * Удаляет закэшированный ответ для переданного тела запроса [data] в соответствии с [config].
     */
    suspend fun deleteCache(data: String, config: RepositoryUriConfig) {
        if (config == RepositoryUriConfig.DIRECT) return
        val cacheKey = data.toMD5()
        when (config) {
            RepositoryUriConfig.CACHE_RAM -> deleteRamCache(cacheKey)
            RepositoryUriConfig.CACHE_ROM -> cacheUrlStringRomDao.delete(cacheKey)
            RepositoryUriConfig.DIRECT -> Unit
        }
    }

    /**
     * Считывает закэшированный бандл альбома из локальной БД по ID [albumId], проверяя возраст кэша.
     */
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

    /**
     * Сохраняет бандл альбома [content] в кэш БД по [albumId].
     */
    suspend fun putAlbumBundleCache(albumId: Int, content: String) {
        lAlbumBundleCacheDao.put(albumId.toString(), content)
    }

    /**
     * Удаляет запись бандла альбома из кэша БД по [albumId].
     */
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
        return replace(LOG_PREVIEW_WHITESPACE_REGEX, " ").take(200)
    }

    private fun Throwable?.isHtmlChallengeResponse(): Boolean {
        val message = this?.message ?: return false
        return message.startsWith("Server returned HTML instead of JSON")
    }


    private companion object {
        val SUCCESS_UNIT = Result.success(Unit)
        val RETRY_STATUS_CODES = setOf(413, 429, 500, 502, 503, 504)
        val LOG_PREVIEW_WHITESPACE_REGEX = Regex("\\s+")
        const val MIN_NETWORK_REQUEST_INTERVAL_MS = 300L
        const val HTML_CHALLENGE_RETRY_ATTEMPTS = 3
        const val RAM_CACHE_MAX_ENTRIES = 256
        // CACHE_ROM держит только справочник категорий (MediaCategoriesBootstrap),
        // он меняется редко, но не никогда — раньше запись жила вечно.
        const val ROM_CACHE_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000
        val HTML_CHALLENGE_RETRY_DELAYS_MS = longArrayOf(5_000L, 10_000L, 15_000L)
    }

}

/**
 * Политика кэширования для выполнения запросов через [Repository.openURI].
 */
enum class RepositoryUriConfig {
    /** Прямой сетевой запрос без кэширования. */
    DIRECT,
    /** Временный LRU-кэш в оперативной памяти (очищается при перезапуске процесса). */
    CACHE_RAM,
    /** Постоянный файловый кэш в локальной базе данных ROM. */
    CACHE_ROM
}
