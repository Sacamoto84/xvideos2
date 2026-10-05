package com.client.xvideos.l.net

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import com.client.xvideos.common.util.replaceWith
import com.client.xvideos.l.model.AlbumListTopHits
import com.client.xvideos.l.net.graphQl.getAlbumListTopHitsQuery
import com.client.xvideos.l.net.json.LJson
import com.client.xvideos.l.repository.Repository
import com.client.xvideos.l.repository.toLUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * О сбое сообщает [loadError] — экран показывает его вместе с кнопкой повтора.
 *
 * @property repository Репозиторий сетевых запросов.
 * @property scope Область экрана: уход с вкладки отменяет запрос.
 */
@Stable
class AlbumTopHitsImpl(
    val repository: Repository,
    val scope: CoroutineScope,
) {

    /** Реактивный список топовых альбомов. */
    val items = mutableStateListOf<AlbumListTopHits>()

    private val _loadError = MutableStateFlow<String?>(null)
    /** Текст сбоя последней загрузки для экрана; `null`, пока она идёт или удалась. */
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private var loadJob: Job? = null

    init {
        reload()
    }

    /**
     * Загружает топ заново. Раньше загрузка шла один раз при создании: после
     * сбоя вкладка оставалась пустой, пока экран не пересоздан.
     */
    fun reload() {
        loadJob?.cancel()
        _loadError.value = null
        loadJob = scope.launch(Dispatchers.IO) {
            val list = try {
                Timber.d("getAlbumTopHits")
                val query = getAlbumListTopHitsQuery()
                val raw = repository.openURI(query).getOrElse { error ->
                    // Раньше отказ молча оставлял вкладку пустой.
                    _loadError.value = error.toLUserMessage()
                    return@launch
                }
                if (raw.isBlank()) return@launch
                val json = LJson.parseToJsonElement(raw).jsonObject
                val get =
                    json["data"]?.jsonObject?.get("album")?.jsonObject?.get("list_top_hits")?.jsonArray
                get?.mapNotNull { element ->
                    runCatching { LJson.decodeFromJsonElement<AlbumListTopHits>(element) }
                        .onFailure { Timber.w("getAlbumTopHits: раздел не разобран и пропущен: ${it.javaClass.simpleName}") }
                        .getOrNull()
                }.orEmpty()
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                Timber.w("getAlbumTopHits error: ${t.javaClass.simpleName}")
                _loadError.value = t.toLUserMessage()
                return@launch
            }

            withContext(Dispatchers.Main) {
                items.replaceWith(list)
            }
        }
    }

    val isEmpty: Boolean get() = items.isEmpty()
    val isNotEmpty: Boolean get() = items.isNotEmpty()
    val hasHits: Boolean get() = items.isNotEmpty()
    val count: Int get() = items.size
    val firstOrNull: AlbumListTopHits? get() = items.firstOrNull()
    val allTitles: List<String> get() = items.map { it.title }
}
