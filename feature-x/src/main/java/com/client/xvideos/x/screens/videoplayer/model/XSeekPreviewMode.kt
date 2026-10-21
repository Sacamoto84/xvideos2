package com.client.xvideos.x.screens.videoplayer.model

/**
 * Вид превью кадра при перемотке в полноэкранном плеере X. [settingValue] —
 * значение `Settings.x_seek_preview_mode`, [label] — подпись в «Настройках».
 */
enum class XSeekPreviewMode(val settingValue: Int, val label: String) {
    /** A: маленький кадр едет над полосой за бегунком, видео видно. */
    FOLLOW(0, "У ползунка"),

    /** B: кадр растянут на весь плеер. */
    FULLSCREEN(1, "На весь экран");

    fun toggled(): XSeekPreviewMode = if (this == FOLLOW) FULLSCREEN else FOLLOW

    companion object {
        /** Неизвестное значение (настройка из будущей версии) — вид по умолчанию. */
        fun fromSetting(value: Int): XSeekPreviewMode = entries.firstOrNull { it.settingValue == value } ?: FOLLOW
    }
}
