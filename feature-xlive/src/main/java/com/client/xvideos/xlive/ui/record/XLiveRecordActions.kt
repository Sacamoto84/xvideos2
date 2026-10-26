package com.client.xvideos.xlive.ui.record

import androidx.compose.runtime.Immutable

/**
 * Нажатия экрана записи одним значением — как у комнаты.
 *
 * @property onBack «Назад» из плеера в портрете.
 * @property onOpenRoom «открыть комнату»: живая комната по нику записи.
 * @property onOpenTag тап по тегу темы; аргумент — тег без `#`.
 * @property onToggleSaved сохранить в «Мои» или убрать.
 * @property onSaveToGallery сохранить видеозапись в системную галерею.
 * @property onSavePosition позиция просмотра и длина, секунды — от плеера.
 */
@Immutable
data class XLiveRecordActions(
    val onBack: () -> Unit = {},
    val onOpenRoom: () -> Unit = {},
    val onOpenTag: (String) -> Unit = {},
    val onToggleSaved: () -> Unit = {},
    val onSaveToGallery: () -> Unit = {},
    val onToggleMute: () -> Unit = {},
    val onToggleFullScreen: () -> Unit = {},
    val onExitFullScreen: () -> Unit = {},
    val onToggleAbout: () -> Unit = {},
    val onSavePosition: (Float, Int) -> Unit = { _, _ -> },
)
