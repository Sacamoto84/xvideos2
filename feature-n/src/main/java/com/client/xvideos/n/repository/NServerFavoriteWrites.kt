package com.client.xvideos.n.repository

import com.client.xvideos.n.net.NNetworkResult
import com.client.xvideos.n.net.NParser
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class NServerFavoriteUpdate(val galleryId: Long, val apiKey: String, val saved: Boolean?)

internal class NServerFavoriteWrites(
    private val isCurrentKey: (String) -> Boolean = { true },
    private val send: suspend (galleryId: Long, favorite: Boolean, apiKey: String) -> NNetworkResult
) {
    private data class Target(val galleryId: Long, val apiKey: String)
    private class PendingWrites {
        val mutex = Mutex()
        var count = 0
    }
    private val pending = mutableMapOf<Target, PendingWrites>()

    private val changes = MutableStateFlow(0L)
    val revision = changes.asStateFlow()
    private val events = MutableSharedFlow<NServerFavoriteUpdate>()
    val updates = events.asSharedFlow()

    suspend fun set(galleryId: Long, favorite: Boolean, apiKey: String): Result<Boolean> {
        if (apiKey.isBlank()) return Result.failure(IllegalStateException("Для изменения избранного нужен API-ключ N"))
        val target = Target(galleryId, apiKey)
        val queue = synchronized(pending) { pending.getOrPut(target, ::PendingWrites).also { it.count++ } }
        return try {
            queue.mutex.withLock {
                currentCoroutineContext().ensureActive()
                if (isCurrentKey(apiKey)) setAndPublish(galleryId, favorite, apiKey)
                else Result.failure(IllegalStateException("Аккаунт N изменился"))
            }
        } finally {
            synchronized(pending) {
                if (--queue.count == 0) pending.remove(target)
            }
        }
    }

    private suspend fun setAndPublish(galleryId: Long, favorite: Boolean, apiKey: String): Result<Boolean> {
        // Уже отправленная запись может дойти до сервера после закрытия экрана.
        return withContext(NonCancellable) {
            val response = send(galleryId, favorite, apiKey)
            val parsed = parseServerFavoriteResult(response)
            if (response.mayHaveChangedFavorites()) {
                changes.update { it + 1 }
                events.emit(NServerFavoriteUpdate(galleryId, apiKey, parsed.getOrNull()))
            }
            parsed.mapCatching { actual ->
                check(actual == favorite) { "Сервер вернул другое состояние избранного" }
                actual
            }
        }
    }

    private fun NNetworkResult.mayHaveChangedFavorites(): Boolean = when (this) {
        is NNetworkResult.Success, is NNetworkResult.Failure -> true
        is NNetworkResult.Error -> code in 500..599
        is NNetworkResult.CloudflareRequired -> false
    }
}

internal fun parseServerFavoriteResult(result: NNetworkResult): Result<Boolean> = when (result) {
    is NNetworkResult.Success -> runCatching { NParser.parseFavoriteState(result.body) }
    is NNetworkResult.CloudflareRequired -> Result.failure(result.toException())
    is NNetworkResult.Error -> Result.failure(result.toHttpException())
    is NNetworkResult.Failure -> Result.failure(result.throwable)
}
