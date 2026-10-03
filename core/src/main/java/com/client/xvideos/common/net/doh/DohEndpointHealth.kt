package com.client.xvideos.common.net.doh

import java.util.concurrent.ConcurrentHashMap

/**
 * Память об отказавших адресах DoH.
 *
 * Адрес, который не ответил, не опрашивается [retryAfterMs]: иначе каждый
 * поиск имени заново ждал бы на нём таймаут. Так и было: при недоступных
 * адресах DoH поиск занимал дольше, чем таймаут запроса, и до отката на
 * системный DNS запрос не доживал — раздел не работал при исправном
 * системном DNS.
 *
 * @param retryAfterMs Сколько отказавший адрес не опрашивается.
 * @param nowMs Текущее время в миллисекундах; подменяется в тестах.
 */
internal class DohEndpointHealth(
    private val retryAfterMs: Long,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    private val downUntilMs = ConcurrentHashMap<String, Long>()

    /** `true`, пока пауза после отказа [endpoint] не истекла. */
    fun isDown(endpoint: String): Boolean {
        val until = downUntilMs[endpoint] ?: return false
        if (nowMs() < until) return true
        downUntilMs.remove(endpoint, until)
        return false
    }

    fun markDown(endpoint: String) {
        downUntilMs[endpoint] = nowMs() + retryAfterMs
    }

    fun markUp(endpoint: String) {
        downUntilMs.remove(endpoint)
    }

    fun clear() {
        downUntilMs.clear()
    }
}

/**
 * Итог опроса адресов DoH.
 *
 * @property addresses Адреса; пусто — DoH ответа не дал.
 * @property error Ошибка последнего отказавшего адреса, если такие были.
 */
internal class DohOutcome<T>(val addresses: List<T>, val error: Exception?)

/**
 * Опрашивает [endpoints] по очереди до первого непустого ответа.
 *
 * Адрес, бросивший исключение, помечается в [health] и в следующих поисках
 * пропускается, пока не истечёт пауза. Пустой ответ отказом не считается:
 * сервер жив, у имени просто нет записей.
 *
 * @param skipDown Пропускать отказавшие адреса. `false` — когда отката на
 * системный DNS нет и другого способа найти имя не остаётся: тогда опрашиваются
 * все адреса.
 * @param onFailure Сообщение об отказе адреса, для лога.
 * @param query Запрос к одному адресу.
 */
internal fun <T> queryDohEndpoints(
    endpoints: List<String>,
    health: DohEndpointHealth,
    skipDown: Boolean,
    onFailure: (endpoint: String, error: Exception) -> Unit = { _, _ -> },
    query: (endpoint: String) -> List<T>,
): DohOutcome<T> {
    val candidates = if (skipDown) endpoints.filterNot(health::isDown) else endpoints
    var lastError: Exception? = null
    for (endpoint in candidates) {
        try {
            val addresses = query(endpoint)
            health.markUp(endpoint)
            if (addresses.isNotEmpty()) return DohOutcome(addresses, null)
        } catch (e: Exception) {
            health.markDown(endpoint)
            lastError = e
            onFailure(endpoint, e)
        }
    }
    return DohOutcome(emptyList(), lastError)
}
