package com.client.xvideos.x.screens.videoplayer.model

import androidx.compose.runtime.Immutable

/**
 * Палец ведёт ползунок: позиция под пальцем [positionSec] и границы полосы
 * перемотки в координатах корня — по ним оверлей ставит кадр A над бегунком.
 */
@Immutable
data class SeekScrub(val positionSec: Float, val trackStartPx: Float, val trackWidthPx: Float)
