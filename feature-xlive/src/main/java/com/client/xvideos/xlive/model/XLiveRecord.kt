package com.client.xvideos.xlive.model

import kotlinx.serialization.Serializable

/** Где запись в своём пути: пишется, склеивается, готова или не склеилась. */
@Serializable
enum class XLiveRecordStatus { RECORDING, FINISHING, READY, BROKEN }

/**
 * Комната на момент начала записи — то, что показывает шапка комнаты.
 *
 * @property broadcastStartedAt начало эфира, unix-секунды; 0 — неизвестно.
 */
@Serializable
data class XLiveRecordRoom(
    val title: String = "",
    val gender: String = "",
    val viewers: Int = 0,
    val ratingPercent: Int? = null,
    val broadcastStartedAt: Long = 0L,
)

/**
 * Запись эфира — то, что лежит в `record.json` её папки.
 *
 * @property id время начала записи, unix-мс; оно же имя папки.
 * @property gaps сколько раз запись не успела за эфиром и потеряла кусок.
 * @property positionSec где остановился просмотр записи, секунды.
 * @property bio анкета модели; `null` — не пришла.
 * @property hasFrame есть кадр начала из самого видео — картинка карточки;
 *   иначе карточка показывает снимок сайта.
 * @property frameVersion каким способом брался кадр; старый — пересобрать.
 */
@Serializable
data class XLiveRecord(
    val id: Long,
    val username: String,
    val status: XLiveRecordStatus = XLiveRecordStatus.RECORDING,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val gaps: Int = 0,
    val positionSec: Float = 0f,
    val room: XLiveRecordRoom = XLiveRecordRoom(),
    val bio: XLiveBio? = null,
    val hasFrame: Boolean = false,
    val frameVersion: Int = 0,
)
