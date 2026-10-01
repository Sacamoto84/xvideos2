package com.client.xvideos.common.ui.composition

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal для динамического управления прозрачностью / видимостью скроллбара.
 * Передаётся как lambda `() -> Float` (от 0f до 1f), чтобы чтение происходило
 * исключительно в фазе отрисовки (внутри Canvas) без рекомпозиции всего экрана.
 */
val LocalScrollbarAlpha = compositionLocalOf<() -> Float> { { 1f } }
