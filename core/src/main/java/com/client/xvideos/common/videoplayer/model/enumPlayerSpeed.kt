package com.client.xvideos.common.videoplayer.model

enum class PlayerSpeed(val speed: Float, val displayName: String) {
    X0_25(0.25f, "0.25x"),
    X0_5(0.5f, "0.5x"),
    X0_75(0.75f, "0.75x"),
    X1(1.0f, "1.0x"),
    X1_25(1.25f, "1.25x"),
    X1_5(1.5f, "1.5x"),
    X2(2.0f, "2.0x");

    companion object {
        val DEFAULT = X1
    }
}

enum class ScreenResize {
    FIT, FILL
}

enum class PlayerOption {
    NONE, SPEED, QUALITY, AUDIO_TRACK, SUBTITLES
}
