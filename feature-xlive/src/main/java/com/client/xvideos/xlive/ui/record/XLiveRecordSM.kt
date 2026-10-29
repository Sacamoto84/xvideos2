package com.client.xvideos.xlive.ui.record

import android.content.Context
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.client.xvideos.common.AppContextHolder
import com.client.xvideos.common.gallery.GallerySaver
import com.client.xvideos.xlive.model.XLiveRecord
import com.client.xvideos.xlive.model.XLiveSavedRoom
import com.client.xvideos.xlive.model.isRecordPlaybackFinished
import com.client.xvideos.xlive.record.XLiveRecordStore
import com.client.xvideos.xlive.record.XLiveRecordings
import com.client.xvideos.xlive.saved.XLiveRoomStore
import com.client.xvideos.xlive.saved.XLiveSaved
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/**
 * Экран записи [id]: запись с метаданными комнаты, плеер, «в мои».
 *
 * Звук при открытии выключен, как в комнате. Позицию просмотра плеер
 * сохраняет сам раз в несколько секунд и при уходе — здесь она только
 * пишется в запись.
 *
 * @param store записи; в тестах — хранилище во временной папке.
 * @param saved «Мои»; в тестах — хранилище во временной папке.
 * @param loadStoryboard сетка кадров записи для превью перемотки; подменяется в тестах.
 */
class XLiveRecordSM(
    private val id: Long,
    private val store: XLiveRecordStore = XLiveRecordings.store,
    private val saved: XLiveRoomStore = XLiveSaved.my,
    private val loadStoryboard: suspend (Long) -> File? = XLiveRecordings::storyboard,
) : ScreenModel {

    /** Запись; `null` — её нет (удалили) или список ещё не прочитан. */
    val record: StateFlow<XLiveRecord?> = store.records
        .map { list -> list.firstOrNull { it.id == id } }
        .stateIn(screenModelScope, SharingStarted.Eagerly, store.records.value.firstOrNull { it.id == id })

    /** Комната записи в «Моих». */
    val isSaved: StateFlow<Boolean> = combine(saved.rooms, record) { rooms, now ->
        now != null && rooms.any { it.username == now.username }
    }.stateIn(screenModelScope, SharingStarted.Eagerly, false)

    private val _muted = MutableStateFlow(true)
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen: StateFlow<Boolean> = _isFullScreen.asStateFlow()

    private val _aboutExpanded = MutableStateFlow(false)
    val aboutExpanded: StateFlow<Boolean> = _aboutExpanded.asStateFlow()

    private val _storyboard = MutableStateFlow<File?>(null)

    /** Сетка кадров для превью перемотки; `null` — ещё собирается или не вышло. */
    val storyboard: StateFlow<File?> = _storyboard.asStateFlow()

    /** Адрес видео для плеера. */
    val videoUrl: String = "file://" + File(store.dirOf(id), XLiveRecordStore.VIDEO).absolutePath

    init {
        screenModelScope.launch { _storyboard.value = loadStoryboard(id) }
    }

    fun toggleMute() {
        _muted.value = !_muted.value
    }

    fun toggleFullScreen() {
        _isFullScreen.value = !_isFullScreen.value
    }

    fun exitFullScreen() {
        _isFullScreen.value = false
    }

    fun toggleAbout() {
        _aboutExpanded.value = !_aboutExpanded.value
    }

    /** Кнопка «в мои»: комната записи — в «Мои» или из них. */
    fun toggleSaved() {
        val now = record.value ?: return
        if (now.username in saved) {
            saved.remove(now.username)
        } else {
            saved.put(XLiveSavedRoom(now.username, age = now.bio?.age))
        }
    }

    /** Где остановился просмотр, секунды — для «Продолжить с …». */
    fun savePosition(seconds: Float, totalSec: Int = 0) {
        val durSec = if (totalSec > 0) totalSec.toFloat() else (record.value?.durationMs ?: 0L) / 1000f
        val isFinished = isRecordPlaybackFinished(seconds, durSec)
        val target = if (isFinished) 0f else seconds
        if (record.value?.positionSec == target) return
        store.update(id) { it.copy(positionSec = target) }
    }

    /** Экспорт видеозаписи в системную галерею через MediaStore. */
    fun saveToGallery(
        context: Context = AppContextHolder.applicationContext,
        saver: (Context, File, String) -> Unit = GallerySaver::saveLocal,
    ) {
        val rec = record.value ?: return
        val src = File(store.dirOf(id), XLiveRecordStore.VIDEO)
        saver(context, src, XLiveRecordStore.galleryFileName(rec))
    }
}
