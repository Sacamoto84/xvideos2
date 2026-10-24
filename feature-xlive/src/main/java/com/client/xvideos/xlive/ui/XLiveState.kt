package com.client.xvideos.xlive.ui

/** Состояние экрана XLive. */
sealed interface XLiveState {

    /** Запрос контекста комнаты в пути. */
    data object Loading : XLiveState

    /**
     * Поток комнаты [room]: [hlsUrl] — свежий адрес мастер-плейлиста.
     *
     * @property requestedStatus статус запрошенной комнаты, если она была не в
     * открытом эфире и вместо неё показывается [room] из списка; иначе `null`.
     */
    data class Live(val room: String, val hlsUrl: String, val requestedStatus: String? = null) : XLiveState

    /** Эфира нет или он закрытый, и открытой замены не нашлось; [status] — статус комнаты от сайта. */
    data class Offline(val status: String) : XLiveState

    /** Поток не получен или плеер упал; [reason] — короткая причина без адреса. */
    data class Failed(val reason: String) : XLiveState
}
