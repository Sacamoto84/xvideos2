package com.client.xvideos.r.network.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

/**
 * Токен R обновляется после 401 один раз, сколько бы запросов ни получили
 * 401 на одном и том же токене.
 */
class BearerAuthTest {

    private val logins = AtomicInteger()
    private val auth = BearerAuth { "t${logins.incrementAndGet()}" }

    /** Клиент, который на любой запрос отвечает 401 — так же, как сервер на протухший токен. */
    private val unauthorized = HttpClient(MockEngine { respond("", HttpStatusCode.Unauthorized) }) {
        expectSuccess = true
    }

    private suspend fun rejectToken(): Nothing {
        unauthorized.get("http://localhost/")
        error("ожидался ответ 401")
    }

    @Test
    fun `первый запрос получает токен один раз`() = runBlocking {
        val first = auth.withAuth { token -> token }
        val second = auth.withAuth { token -> token }

        assertEquals("t1", first.getOrNull())
        assertEquals("t1", second.getOrNull())
        assertEquals(1, logins.get())
    }

    @Test
    fun `ответ 401 обновляет токен и повторяет запрос`() = runBlocking {
        val used = Collections.synchronizedList(mutableListOf<String?>())

        val result = auth.withAuth { token ->
            used += token
            if (token == "t1") rejectToken() else "ok"
        }

        assertEquals("ok", result.getOrNull())
        assertEquals(listOf("t1", "t2"), used)
        assertEquals(2, logins.get())
    }

    @Test
    fun `запоздавший 401 на старом токене не вызывает второго входа`() = runBlocking {
        auth.login()
        val secondSent = CompletableDeferred<Unit>()
        val answerSecond = CompletableDeferred<Unit>()

        // Второй запрос ушёл со старым токеном и ждёт ответа.
        val second = async {
            auth.withAuth { token ->
                if (token == "t1") {
                    secondSent.complete(Unit)
                    answerSecond.await()
                    rejectToken()
                } else {
                    "ok-second"
                }
            }
        }
        secondSent.await()

        // Первый получает 401 на том же токене и обновляет его.
        val first = auth.withAuth { token -> if (token == "t1") rejectToken() else "ok-first" }
        // Теперь 401 приходит и второму: токен уже новый, входить заново незачем.
        answerSecond.complete(Unit)

        assertEquals("ok-first", first.getOrNull())
        assertEquals("ok-second", second.await().getOrNull())
        assertEquals("вход выполнен лишний раз", 2, logins.get())
    }
}
