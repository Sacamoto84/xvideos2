package com.client.xvideos.l.model.enum

import kotlinx.serialization.Serializable

/**
 * Диапазон количества картинок в альбоме для фильтрации выдачи:
 *
 * - All (-1)
 * - C0_25 (0..25)
 * - C25_50 (25..50)
 * - C50_100 (50..100)
 * - C100_200 (100..200)
 * - C200_800 (200..800)
 * - C800_3200 (800..3200)
 * - C3200_12800 (3200..12800)
 *
 * @property count Числовой ранг фильтрации.
 */
@Serializable
enum class PictureCountRank(val count: Int){
    All(-1),
    C0_25(0),       //0 to 25
    C25_50(1),      //25 to 50
    C50_100(2),     //50 to 100
    C100_200(3),    //100 to 200
    C200_800(4),    //200 to 800
    C800_3200(5),   //800 to 3200
    C3200_12800(6); //3200 to 12800

    val isAll: Boolean get() = this == All
    val isSpecific: Boolean get() = this != All

    companion object {
        val DEFAULT = All
        fun fromCountOrNull(count: Int?): PictureCountRank? =
            if (count != null) entries.firstOrNull { it.count == count } else null

        fun fromCount(count: Int?, default: PictureCountRank = DEFAULT): PictureCountRank =
            fromCountOrNull(count) ?: default

        fun fromStringOrNull(value: String?): PictureCountRank? =
            value?.toIntOrNull()?.let { fromCountOrNull(it) }
    }
}
