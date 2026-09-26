package com.client.xvideos.common.videoplayer.model

enum class PlayerSpeed(val speed: Float, val displayName: String) {
    X0_25(0.25f, "0.25x"),
    X0_5(0.5f, "0.5x"),
    X0_75(0.75f, "0.75x"),
    X1(1.0f, "1.0x"),
    X1_25(1.25f, "1.25x"),
    X1_5(1.5f, "1.5x"),
    X2(2.0f, "2.0x");

    val isNormal: Boolean get() = this == X1
    val isSlow: Boolean get() = speed < 1.0f
    val isFast: Boolean get() = speed > 1.0f

    companion object {
        val DEFAULT = X1

        fun fromSpeed(speed: Float): PlayerSpeed =
            entries.find { it.speed == speed } ?: DEFAULT
    }
}

enum class ScreenResize {
    FIT, FILL;

    val isFit: Boolean get() = this == FIT
    val isFill: Boolean get() = this == FILL

    companion object {
        val DEFAULT = FIT
    }
}

enum class PlayerOption {
    NONE, SPEED, QUALITY, AUDIO_TRACK, SUBTITLES;

    val isNone: Boolean get() = this == NONE
    val isSpeed: Boolean get() = this == SPEED
    val isQuality: Boolean get() = this == QUALITY
    val isAudioTrack: Boolean get() = this == AUDIO_TRACK
    val isSubtitles: Boolean get() = this == SUBTITLES

    companion object {
        val DEFAULT = NONE
    }
}
