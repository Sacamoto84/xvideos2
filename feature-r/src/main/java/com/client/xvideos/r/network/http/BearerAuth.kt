package com.client.xvideos.r.network.http

import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Анонимный bearer-токен R и выполнение запросов под ним.
 *
 * Токен получается при первом запросе, хранится до ответа 401 и после него
 * обновляется один раз, сколько бы запросов ни получили 401 одновременно.
 *
 * @param fetchToken Получение нового токена у сервера; в тестах подменяется.
 */
class BearerAuth(private val fetchToken: suspend () -> String) {

    /** Текущий токен. Пишется только под [mutex], читается из любого потока. */
    @Volatile
    var token: String? = null
        private set

    private val mutex = Mutex()

    /** Принудительно получает новый токен. */
    suspend fun login(): Result<Boolean> = mutex.withLock { loginLocked() }

    /**
     * Выполняет [perform] с актуальным токеном; при 401 один раз обновляет
     * токен и повторяет.
     *
     * @param perform Запрос; получает токен, с которым его нужно отправить.
     */
    suspend fun <T> withAuth(perform: suspend (token: String?) -> T): Result<T> {
        ensureToken().onFailure { return Result.failure(it) }
        // Токен, с которым запрос уходит. Запоминается до запроса: после 401 по
        // нему видно, протух ли он или его уже обновил соседний запрос. Раньше
        // токен читался после 401 — к этому моменту сосед мог его обновить, свежий
        // токен принимался за протухший, и вход выполнялся ещё раз.
        val tokenUsed = token
        return try {
            Result.success(perform(tokenUsed))
        } catch (e: CancellationException) {
            // Экран закрыли посреди запроса — это не сбой сети. Раньше отмена
            // превращалась в Result.failure, и вызывающий показывал снекбар с
            // текстом отмены корутины уже на предыдущем экране.
            throw e
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                Timber.w("Red ApiClient 401 Unauthorized, retrying login...")
                if (refreshToken(tokenUsed).isSuccess) {
                    return retryAfterRefresh(perform)
                }
            }
            Timber.e(e, "Red ApiClient request FAILED")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Red ApiClient request FAILED")
            Result.failure(e)
        }
    }

    private suspend fun <T> retryAfterRefresh(perform: suspend (token: String?) -> T): Result<T> = try {
        Result.success(perform(token))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Red ApiClient request FAILED after retry")
        Result.failure(e)
    }

    /**
     * Гарантирует наличие токена. При параллельных запросах без токена вход
     * выполняется ровно один раз (double-checked под [mutex]).
     */
    private suspend fun ensureToken(): Result<Unit> {
        if (token != null) return SUCCESS_UNIT
        return mutex.withLock {
            if (token != null) SUCCESS_UNIT else loginLocked().map { }
        }
    }

    /**
     * Обновляет токен после 401, но только если его ещё не заменили: иначе
     * несколько параллельных 401 устроили бы шторм входов.
     *
     * @param previousToken Токен, на котором получен 401.
     */
    private suspend fun refreshToken(previousToken: String?): Result<Unit> {
        return mutex.withLock {
            if (token != previousToken) {
                SUCCESS_UNIT
            } else {
                token = null
                loginLocked().map { }
            }
        }
    }

    /** Выполняет вход. Вызывать только удерживая [mutex]. */
    private suspend fun loginLocked(): Result<Boolean> {
        return try {
            Timber.d("Red ApiClient login()")
            token = fetchToken()
            Timber.d("Red ApiClient login() SUCCESS - token received")
            Result.success(true)
        } catch (e: CancellationException) {
            // Отмена корутины — не ошибка сети. Без этого catch она превращалась
            // в Result.failure и уезжала вызывающему как настоящий сбой входа.
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Red ApiClient login() FAILED: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    private companion object {
        /** Кэшированный успех, чтобы не аллоцировать Result на каждый запрос. */
        val SUCCESS_UNIT = Result.success(Unit)
    }
}
