package com.client.xvideos.xlive.ui.room

import com.client.xvideos.xlive.model.XLiveRoom

/** Состояние экрана комнаты XLive. */
sealed interface XLiveRoomState {

    /** Запрос комнаты в пути. */
    data object Loading : XLiveRoomState

    /**
     * Комната [room] в открытом эфире, поток можно смотреть.
     *
     * @property requestedStatus статус запрошенной комнаты, если она была не в
     * открытом эфире и вместо неё показывается [room] из списка; иначе `null`.
     */
    data class Live(val room: XLiveRoom, val requestedStatus: String? = null) : XLiveRoomState

    /** Эфира нет или он закрытый, и открытой замены не нашлось; [status] — статус комнаты от сайта. */
    data class Offline(val status: String) : XLiveRoomState

    /** Комната не получена или плеер упал; [reason] — короткая причина без адреса. */
    data class Failed(val reason: String) : XLiveRoomState
}
