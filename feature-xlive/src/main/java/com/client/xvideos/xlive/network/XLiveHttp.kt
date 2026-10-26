package com.client.xvideos.xlive.network

import com.client.xvideos.xlive.model.XLiveRoom

/**
 * Ответ сервера с кодом: запись решает по коду (403 — свежий адрес), поэтому
 * код ошибки — не исключение. `body` есть только у успешного ответа.
 */
data class XLiveHttp<T>(val code: Int, val body: T?) {
    val isSuccess: Boolean get() = code in SUCCESS && body != null

    private companion object {
        val SUCCESS = 200..299
    }
}

/** Сеть записи: комната за свежим адресом, плейлисты и сегменты. Подменяется в тестах. */
interface XLiveRecordNet {

    /** @throws java.io.IOException нет сети или ответ без статуса комнаты. */
    suspend fun room(name: String): XLiveRoom

    /** @throws java.io.IOException нет сети. */
    suspend fun text(url: String): XLiveHttp<String>

    /** @throws java.io.IOException нет сети. */
    suspend fun bytes(url: String): XLiveHttp<ByteArray>
}

/** Боевая сеть записи — клиент раздела. */
object XLiveApiRecordNet : XLiveRecordNet {
    override suspend fun room(name: String): XLiveRoom = XLiveApi.room(name)
    override suspend fun text(url: String): XLiveHttp<String> = XLiveApi.fetchText(url)
    override suspend fun bytes(url: String): XLiveHttp<ByteArray> = XLiveApi.fetchBytes(url)
}
