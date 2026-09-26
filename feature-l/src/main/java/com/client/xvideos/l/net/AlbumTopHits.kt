package com.client.xvideos.l.net

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import com.client.xvideos.common.util.replaceWith
import com.client.xvideos.l.model.AlbumListTopHits
import com.client.xvideos.l.net.graphQl.getAlbumListTopHitsQuery
import com.client.xvideos.l.net.json.LJson
import com.client.xvideos.l.repository.Repository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import timber.log.Timber

/**
 * Загрузчик и реактивный держатель топовых популярных альбомов Luscious (Top Hits).
 *
 * Выполняет запрос `getAlbumListTopHitsQuery()` и заполняет snapshot-список [items].
 *
 * @property repository Репозиторий сетевых запросов.
 * @property scope CoroutineScope выполнения фонового запроса.
 */
@Stable
class AlbumTopHitsImpl(
    val repository: Repository,
    val scope: CoroutineScope,
) {

    /** Реактивный список топовых альбомов. */
    val items = mutableStateListOf<AlbumListTopHits>()

    init {
        scope.launch(Dispatchers.IO) {
            val list = try {
                Timber.d("getAlbumTopHits")
                val query = getAlbumListTopHitsQuery()
                val res = repository.openURI(query)
                if (res.isFailure) return@launch
                val raw = res.getOrNull().orEmpty()
                if (raw.isBlank()) return@launch
                val json = LJson.parseToJsonElement(raw).jsonObject
                val get =
                    json["data"]?.jsonObject?.get("album")?.jsonObject?.get("list_top_hits")?.jsonArray
                get?.mapNotNull { element ->
                    runCatching { LJson.decodeFromJsonElement<AlbumListTopHits>(element) }.getOrNull()
                }.orEmpty()
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                Timber.w(t, "getAlbumTopHits error")
                return@launch
            }

            withContext(Dispatchers.Main) {
                items.replaceWith(list)
            }
        }

    }

    val isEmpty: Boolean get() = items.isEmpty()
    val isNotEmpty: Boolean get() = items.isNotEmpty()
    val count: Int get() = items.size
}
