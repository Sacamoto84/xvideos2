package com.client.xvideos.xlive.record

import com.client.xvideos.xlive.model.XLiveBio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Что видит комната в записи: кнопка, плашка с временем.
 *
 * @property startedAtElapsedMs начало записи по монотонным часам — от него плашка считает время.
 */
data class XLiveRecordingUi(
    val isRecording: Boolean = false,
    val isFinishing: Boolean = false,
    val startedAtElapsedMs: Long = 0L,
)

/** Запись, как её видит комната. Подменяется в тестах комнаты. */
interface XLiveRecordingHandle {
    val state: StateFlow<XLiveRecordingUi>

    /** Дописать анкету, пришедшую после начала записи. */
    fun attachBio(bio: XLiveBio)

    /** Закончить запись; повторный вызов ничего не меняет. */
    fun stop(reason: XLiveRecordStop)
}

/** Одна идущая запись: её id (он же папка), рекордер и состояние для комнаты. */
class XLiveRecordingSession internal constructor(
    val id: Long,
    private val recorder: XLiveRecorder,
    private val store: XLiveRecordStore,
    startedAtElapsedMs: Long,
) : XLiveRecordingHandle {

    private val _state = MutableStateFlow(XLiveRecordingUi(isRecording = true, startedAtElapsedMs = startedAtElapsedMs))
    override val state: StateFlow<XLiveRecordingUi> = _state.asStateFlow()

    override fun attachBio(bio: XLiveBio) {
        store.update(id) { it.copy(bio = bio) }
    }

    override fun stop(reason: XLiveRecordStop) {
        recorder.requestStop(reason)
    }

    internal fun finishing() {
        _state.value = XLiveRecordingUi(isFinishing = true)
    }

    internal fun done() {
        _state.value = XLiveRecordingUi()
    }
}
