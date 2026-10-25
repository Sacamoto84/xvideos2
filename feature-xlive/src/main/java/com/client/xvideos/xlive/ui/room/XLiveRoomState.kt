package com.client.xvideos.xlive.ui.room

import com.client.xvideos.xlive.model.XLiveRoom

/** Состояние экрана комнаты XLive. */
sealed interface XLiveRoomState {

    /** Запрос комнаты в пути. */
    data object Loading : XLiveRoomState

    /** Комната [room] в открытом эфире, поток можно смотреть. */
    data class Live(val room: XLiveRoom) : XLiveRoomState

    /** Эфира нет или он закрытый; [status] — статус комнаты от сайта. */
    data class Offline(val status: String) : XLiveRoomState

    /** Комната не получена или плеер упал; [reason] — короткая причина без адреса. */
    data class Failed(val reason: String) : XLiveRoomState
}
