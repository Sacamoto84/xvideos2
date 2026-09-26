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

    /** Шаг к следующей скорости циклически. */
    fun nextSpeed(): PlayerSpeed {
        val nextOrdinal = (ordinal + 1) % entries.size
        return entries[nextOrdinal]
    }

    /** Шаг к предыдущей скорости циклически. */
    fun prevSpeed(): PlayerSpeed {
        val prevOrdinal = if (ordinal == 0) entries.size - 1 else ordinal - 1
        return entries[prevOrdinal]
    }

    /** Увеличивает скорость без зацикливания (ограничено X2). */
    fun faster(): PlayerSpeed =
        if (ordinal < entries.size - 1) entries[ordinal + 1] else this

    /** Уменьшает скорость без зацикливания (ограничено X0_25). */
    fun slower(): PlayerSpeed =
        if (ordinal > 0) entries[ordinal - 1] else this

    companion object {
        val DEFAULT = X1

        fun fromSpeed(speed: Float): PlayerSpeed =
            entries.find { it.speed == speed } ?: DEFAULT

        fun fromSpeedOrDefault(speed: Float, default: PlayerSpeed = DEFAULT): PlayerSpeed =
            entries.find { it.speed == speed } ?: default

        fun fromDisplayName(displayName: String?): PlayerSpeed? {
            if (displayName.isNullOrBlank()) return null
            return entries.find { it.displayName.equals(displayName, ignoreCase = true) }
        }

        fun closestSpeed(rawSpeed: Float): PlayerSpeed =
            entries.minByOrNull { kotlin.math.abs(it.speed - rawSpeed) } ?: DEFAULT

        fun fromOrdinalOrDefault(ordinal: Int, default: PlayerSpeed = DEFAULT): PlayerSpeed =
            entries.getOrNull(ordinal) ?: default
    }
}

enum class ScreenResize {
    FIT, FILL;

    val isFit: Boolean get() = this == FIT
    val isFill: Boolean get() = this == FILL

    /** Переключение между FIT и FILL. */
    fun toggle(): ScreenResize = if (this == FIT) FILL else FIT

    companion object {
        val DEFAULT = FIT

        fun fromOrdinalOrDefault(ordinal: Int, default: ScreenResize = DEFAULT): ScreenResize =
            entries.getOrNull(ordinal) ?: default
    }
}

enum class PlayerOption {
    NONE, SPEED, QUALITY, AUDIO_TRACK, SUBTITLES;

    val isNone: Boolean get() = this == NONE
    val isSpeed: Boolean get() = this == SPEED
    val isQuality: Boolean get() = this == QUALITY
    val isAudioTrack: Boolean get() = this == AUDIO_TRACK
    val isSubtitles: Boolean get() = this == SUBTITLES
    val hasSubmenu: Boolean get() = this != NONE

    companion object {
        val DEFAULT = NONE

        fun fromOrdinalOrDefault(ordinal: Int, default: PlayerOption = DEFAULT): PlayerOption =
            entries.getOrNull(ordinal) ?: default
    }
}
