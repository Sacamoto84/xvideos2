package com.client.xvideos.n.repository

import com.client.xvideos.n.net.NNetworkResult
import com.client.xvideos.n.net.NParser
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

internal class NServerFavoriteUpdate(val galleryId: Long, val apiKey: String, val saved: Boolean?)

internal class NServerFavoriteWrites(
    private val send: suspend (galleryId: Long, favorite: Boolean, apiKey: String) -> NNetworkResult
) {
    private val changes = MutableStateFlow(0L)
    val revision = changes.asStateFlow()
    private val events = MutableSharedFlow<NServerFavoriteUpdate>()
    val updates = events.asSharedFlow()

    suspend fun set(galleryId: Long, favorite: Boolean, apiKey: String): Result<Boolean> {
        if (apiKey.isBlank()) return Result.failure(IllegalStateException("Для изменения избранного нужен API-ключ N"))
        // Уже отправленная запись может дойти до сервера после закрытия экрана.
        return withContext(NonCancellable) {
            val response = send(galleryId, favorite, apiKey)
            val parsed = parseServerFavoriteResult(response)
            if (response is NNetworkResult.Success || response is NNetworkResult.Failure ||
                (response is NNetworkResult.Error && response.code in 500..599)
            ) {
                changes.update { it + 1 }
                events.emit(NServerFavoriteUpdate(galleryId, apiKey, parsed.getOrNull()))
            }
            parsed.mapCatching { actual ->
                check(actual == favorite) { "Сервер вернул другое состояние избранного" }
                actual
            }
        }
    }
}

internal fun parseServerFavoriteResult(result: NNetworkResult): Result<Boolean> = when (result) {
    is NNetworkResult.Success -> runCatching { NParser.parseFavoriteState(result.body) }
    is NNetworkResult.CloudflareRequired -> Result.failure(result.toException())
    is NNetworkResult.Error -> Result.failure(IllegalStateException("Ошибка HTTP: ${result.code}"))
    is NNetworkResult.Failure -> Result.failure(result.throwable)
}
