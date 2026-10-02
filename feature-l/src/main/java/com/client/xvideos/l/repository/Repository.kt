package com.client.xvideos.l.repository

import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.toMD5
import com.client.xvideos.l.KtorRequestHandler
import com.client.xvideos.l.model.UserProfile
import com.client.xvideos.l.net.json.LJson
import io.ktor.client.engine.HttpClientEngine
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
 * - отправку GraphQL-запросов: вошедшего — POST на [LusciousEndpoints.API], анонима —
 *   как у сайта, GET на [LusciousEndpoints.API_ANONYMOUS] без cookies;
 * - многоуровневое кэширование ответов (RAM LRU-кэш, постоянный ROM-кэш);
 * - обработку антибот-защиты (HTML challenge/Cloudflare) с экспоненциальным кулдауном;
 * - троттлинг сетевых запросов с минимальным интервалом.
 *
 * @param fileDb Локальная файловая база данных для доступа к ROM-кэшам.
 * @param credentials Сохранённые логин и пароль; пустой профиль — анонимный режим.
 * @param engineFactory Движок HTTP для тестов; `null` — боевой OkHttp.
 * @param notifyAnonymousFallback Сообщение пользователю, что вход не удался и запросы идут анонимно.
 */
open class Repository(
    fileDb: AppFileDatabase,
    private val credentials: () -> UserProfile = ::savedCredentials,
    private val engineFactory: (() -> HttpClientEngine)? = null,
    private val notifyAnonymousFallback: (String) -> Unit = SnackBar::warning,
) {

    /** Точка входа для GraphQL API Luscious. */
    val apiUrl = LusciousEndpoints.API

    @Volatile
    private var handler = createHandler()

    private val authMutex = Mutex()

    /**
     * Профиль, вход по которому в этой сессии не удался. Пока сохранены те же
     * логин и пароль, запросы идут анонимно без новых попыток входа.
     * Читается и пишется только под [authMutex].
     */
    private var anonymousFallbackFor: UserProfile? = null

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
            backoffFactor = 1000,
            engineOverride = engineFactory?.invoke(),
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
                anonymousFallbackFor = null
                val oldHandler = handler
                handler = createHandler()
                oldHandler.close()
            }
        }
        clearHtmlChallengeUiState()
    }

    /**
     * Проверяет и при необходимости выполняет авторизацию пользователя по сохраненным логину и паролю.
     *
     * Неудачный вход не закрывает раздел: до смены логина или пароля запросы
     * идут анонимно, как после «Пропустить», а пользователь получает одно
     * предупреждение. Раньше отказ входа возвращался ошибкой из каждого
     * openURI и вход повторялся на каждом запросе — при сломанной авторизации
     * на сервере L не открывался вовсе, хотя анонимные запросы проходили.
     */
    private suspend fun ensureAuthenticated(): Result<Unit> {
        return try {
            authMutex.withLock {
                val profile = credentials()
                // Анонимный режим: без логина/пароля работаем без авторизации
                // (Luscious отдаёт меньше альбомов). С кредами — авторизуемся.
                if (profile.isValid && profile != anonymousFallbackFor) {
                    handler.setCredentials(profile.email, profile.password)
                    if (!handler.loggedIn) {
                        handler.login().onFailure { fallBackToAnonymous(profile, it) }
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

    /** Запоминает неудачный вход и предупреждает пользователя. Вызывать под [authMutex]. */
    private fun fallBackToAnonymous(profile: UserProfile, cause: Throwable) {
        anonymousFallbackFor = profile
        val reason = cause.message?.replaceFirstChar { it.lowercase() } ?: cause.javaClass.simpleName
        Timber.w(cause, "L login failed, continuing anonymously")
        notifyAnonymousFallback("Вход в L не удался: $reason. Работаем без авторизации")
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
                message = error?.message ?: HTML_INSTEAD_OF_JSON_PREFIX
            )
            Timber.w("openURI() HTML challenge response, retry after ${delayMs}ms")
        }

        return lastFailure ?: Result.failure(IllegalStateException(HTML_INSTEAD_OF_JSON_PREFIX))
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
                // Как у сайта: вошедший — POST на адрес участников с cookie
                // сессии; аноним (нет логина или вход не удался) — GET на
                // анонимный адрес без cookies. Раньше аноним тоже ходил на
                // адрес участников и не попадал в кэш Cloudflare, на котором
                // сайт продолжал работать при отказе сервера.
                if (handler.loggedIn) {
                    handler.postJson(apiUrl, data)
                } else {
                    handler.graphQlAnonymous(LusciousEndpoints.API_ANONYMOUS, data)
                }
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
                "$HTML_INSTEAD_OF_JSON_PREFIX: ${normalized.previewForLog()}"
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
        return message.startsWith(HTML_INSTEAD_OF_JSON_PREFIX)
    }


    private companion object {
        val SUCCESS_UNIT = Result.success(Unit)
        // Без 500: это отказ приложения сервера, а не шлюза. Пять повторов с
        // паузами на упавшем сервере только оттягивали ошибку на экране.
        val RETRY_STATUS_CODES = setOf(413, 429, 502, 503, 504)
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

/** Логин и пароль L из настроек приложения. */
private fun savedCredentials(): UserProfile =
    UserProfile(email = Settings.l_login.field.value.trim(), password = Settings.l_pass.field.value)
