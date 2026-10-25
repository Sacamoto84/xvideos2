package com.client.xvideos.xlive.ui.room

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.client.xvideos.xlive.model.XLiveBio
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
 * Комната [room]: поток, шапка и анкета модели.
 *
 * Анкета грузится отдельно от комнаты: видео не ждёт её.
 *
 * @param loadRoom источник комнаты; подменяется в тестах.
 * @param loadBio источник анкеты; подменяется в тестах.
 * @param nowMs монотонные часы в миллисекундах; подменяются в тестах.
 */
class XLiveRoomSM(
    private val room: String,
    private val loadRoom: suspend (String) -> XLiveRoom = XLiveApi::room,
    private val loadBio: suspend (String) -> XLiveBio = XLiveApi::bio,
    private val nowMs: () -> Long = { System.nanoTime() / NANOS_PER_MS },
) : ScreenModel {

    private val _state = MutableStateFlow<XLiveRoomState>(XLiveRoomState.Loading)
    val state: StateFlow<XLiveRoomState> = _state.asStateFlow()

    private val _bio = MutableStateFlow<XLiveBio?>(null)

    /** Анкета показанной комнаты; `null`, пока не пришла или если не удалась. */
    val bio: StateFlow<XLiveBio?> = _bio.asStateFlow()

    private val _muted = MutableStateFlow(true)

    /** Звук эфира выключен. Комната открывается без звука, включает его кнопка на видео. */
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    private val _aboutExpanded = MutableStateFlow(false)

    /** «О себе» в карточке модели развёрнуто целиком. */
    val aboutExpanded: StateFlow<Boolean> = _aboutExpanded.asStateFlow()

    private var loadJob: Job? = null
    private var bioJob: Job? = null
    private var bioRoom: String? = null
    private var loadedAtMs = 0L

    /** Адрес потока, который уже отдан плееру. Второй плеер с ним получит 403. */
    private var startedUrl: String? = null

    init {
        reload()
    }

    /** Комната с нуля: экран загрузки и свежий адрес потока. Первый запуск и кнопка «Повторить». */
    fun reload() {
        _state.value = XLiveRoomState.Loading
        load()
    }

    /**
     * Свежий адрес потока без экрана загрузки: шапка и анкета остаются на месте,
     * плеер на время запроса пустой. Без открытого эфира — то же, что [reload].
     */
    private fun refreshStream() {
        val live = _state.value as? XLiveRoomState.Live ?: return reload()
        _state.value = live.copy(room = live.room.copy(hlsUrl = ""))
        load()
    }

    private fun load() {
        loadJob?.cancel()
        startedUrl = null
        loadJob = screenModelScope.launch {
            val next = try {
                resolve()
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                XLiveRoomState.Failed(e.javaClass.simpleName)
            }
            Timber.i("XLive: комната — %s", next.javaClass.simpleName)
            loadedAtMs = nowMs()
            _state.value = next
            if (next is XLiveRoomState.Live) loadBioOf(next.room.username)
        }
    }

    private suspend fun resolve(): XLiveRoomState {
        val info = loadRoom(room)
        return if (info.isLive) XLiveRoomState.Live(info.withUsername(room)) else XLiveRoomState.Offline(info.status)
    }

    /** Анкета той же комнаты не перезапрашивается: повтор потока не должен мигать карточкой. */
    private fun loadBioOf(username: String) {
        if (username == bioRoom) return
        bioJob?.cancel()
        bioRoom = username
        _bio.value = null
        _aboutExpanded.value = false
        bioJob = screenModelScope.launch {
            try {
                _bio.value = loadBio(username)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Timber.w("XLive: анкета не получена — %s", e.javaClass.simpleName)
                bioRoom = null
            }
        }
    }

    /**
     * Плеер начал играть [hlsUrl].
     *
     * Сайт отдаёт адрес на один плеер: второй плеер с тем же адресом получает
     * 403. А плеер создаётся заново при каждом пересоздании Activity — поворот,
     * смена плотности, сложение складного. Поэтому повторный старт с уже
     * отданным адресом означает «плеер пересоздан», и берётся свежий адрес.
     */
    fun onPlayerStarted(hlsUrl: String) {
        val live = _state.value as? XLiveRoomState.Live ?: return
        if (hlsUrl.isBlank() || hlsUrl != live.room.hlsUrl) return
        if (hlsUrl == startedUrl) {
            Timber.i("XLive: плеер пересоздан — беру свежий адрес")
            refreshStream()
        } else {
            startedUrl = hlsUrl
        }
    }

    /**
     * Плеер упал. Если поток играл дольше [SILENT_RETRY_AFTER_MS], скорее всего
     * истёк токен или сессия потока — молча берётся свежий адрес. Ошибка сразу
     * после загрузки показывается: повтор зациклился бы.
     *
     * В счёт только ошибка плеера, который играет текущий адрес: плеер шлёт одну
     * ошибку дважды, и вторая не должна сработать по свежему адресу, который ещё
     * не начал играть.
     */
    fun onPlayerError(message: String) {
        val live = _state.value as? XLiveRoomState.Live ?: return
        if (live.room.hlsUrl.isBlank() || live.room.hlsUrl != startedUrl) return
        if (nowMs() - loadedAtMs >= SILENT_RETRY_AFTER_MS) {
            Timber.w("XLive: ошибка плеера — %s, беру свежий адрес", message)
            refreshStream()
        } else {
            Timber.w("XLive: ошибка плеера — %s", message)
            _state.value = XLiveRoomState.Failed(message)
        }
    }

    /**
     * Плеер ждёт данных или снова играет. Позиция для этого не годится: у
     * прямого эфира она считается от начала скользящего окна и почти не растёт.
     */
    fun onBuffering(isBuffering: Boolean) {
        Timber.i("XLive: буферизация — %s", if (isBuffering) "да" else "нет")
    }

    fun toggleMute() {
        _muted.value = !_muted.value
    }

    fun toggleAbout() {
        _aboutExpanded.value = !_aboutExpanded.value
    }

    private companion object {
        const val NANOS_PER_MS = 1_000_000L
        const val SILENT_RETRY_AFTER_MS = 30_000L

        fun XLiveRoom.withUsername(fallback: String): XLiveRoom =
            if (username.isBlank()) copy(username = fallback) else this
    }
}
