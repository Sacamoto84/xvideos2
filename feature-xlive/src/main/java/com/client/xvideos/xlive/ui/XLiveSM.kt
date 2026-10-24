package com.client.xvideos.xlive.ui

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.client.xvideos.xlive.model.XLiveRoom
import com.client.xvideos.xlive.network.XLiveApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException

/**
 * Состояние экрана XLive: адрес потока комнаты [room].
 *
 * Комната не в открытом эфире — берётся первая открытая из списка сайта, чтобы
 * экран показывал живой поток в любой момент.
 *
 * @param loadRoom источник комнаты; подменяется в тестах.
 * @param loadPublicRooms ники открытых комнат; подменяется в тестах.
 */
class XLiveSM(
    private val room: String,
    private val loadRoom: suspend (String) -> XLiveRoom = XLiveApi::room,
    private val loadPublicRooms: suspend () -> List<String> = { XLiveApi.publicRooms() },
) : ScreenModel {

    private val _state = MutableStateFlow<XLiveState>(XLiveState.Loading)
    val state: StateFlow<XLiveState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        reload()
    }

    /** Свежий адрес потока: и первый запуск, и повтор после ошибки — токен короткий. */
    fun reload() {
        loadJob?.cancel()
        _state.value = XLiveState.Loading
        loadJob = screenModelScope.launch {
            val next = try {
                val info = loadRoom(room)
                if (info.isLive) XLiveState.Live(room, info.hlsUrl) else replacement(info.status)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                XLiveState.Failed(e.javaClass.simpleName)
            }
            Timber.i("XLive: комната — %s", next.javaClass.simpleName)
            _state.value = next
        }
    }

    /** Первая открытая комната из списка вместо запрошенной со статусом [requestedStatus]. */
    private suspend fun replacement(requestedStatus: String): XLiveState {
        val other = loadPublicRooms().firstOrNull { it != room } ?: return XLiveState.Offline(requestedStatus)
        val info = loadRoom(other)
        return if (info.isLive) {
            XLiveState.Live(other, info.hlsUrl, requestedStatus)
        } else {
            XLiveState.Offline(requestedStatus)
        }
    }

    fun onPlayerError(message: String) {
        Timber.w("XLive: ошибка плеера — %s", message)
        _state.value = XLiveState.Failed(message)
    }

    /**
     * Плеер ждёт данных или снова играет. Позиция для этого не годится: у
     * прямого эфира она считается от начала скользящего окна и почти не растёт.
     */
    fun onBuffering(isBuffering: Boolean) {
        Timber.i("XLive: буферизация — %s", if (isBuffering) "да" else "нет")
    }
}
