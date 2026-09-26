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

    val rangeDescription: String
        get() = when (this) {
            All -> "All"
            C0_25 -> "0 - 25"
            C25_50 -> "25 - 50"
            C50_100 -> "50 - 100"
            C100_200 -> "100 - 200"
            C200_800 -> "200 - 800"
            C800_3200 -> "800 - 3200"
            C3200_12800 -> "3200 - 12800"
        }

    fun matchesCount(picturesCount: Int): Boolean = when (this) {
        All -> true
        C0_25 -> picturesCount in 0..25
        C25_50 -> picturesCount in 25..50
        C50_100 -> picturesCount in 50..100
        C100_200 -> picturesCount in 100..200
        C200_800 -> picturesCount in 200..800
        C800_3200 -> picturesCount in 800..3200
        C3200_12800 -> picturesCount in 3200..12800
    }

    /** Переход к следующему диапазону циклически. */
    fun next(): PictureCountRank {
        val nextOrdinal = (ordinal + 1) % entries.size
        return entries[nextOrdinal]
    }

    /** Переход к предыдущему диапазону циклически. */
    fun prev(): PictureCountRank {
        val prevOrdinal = if (ordinal == 0) entries.size - 1 else ordinal - 1
        return entries[prevOrdinal]
    }

    companion object {
        val DEFAULT = All

        val allCounts: List<Int> = entries.map { it.count }
        val allRanges: List<String> = entries.map { it.rangeDescription }
        val allNames: List<String> = entries.map { it.name }

        fun fromCountOrNull(count: Int?): PictureCountRank? =
            if (count != null) entries.firstOrNull { it.count == count } else null

        fun fromCount(count: Int?, default: PictureCountRank = DEFAULT): PictureCountRank =
            fromCountOrNull(count) ?: default

        fun fromRankOrDefault(rank: Int, default: PictureCountRank = DEFAULT): PictureCountRank =
            fromCount(rank, default)

        fun fromOrdinalOrDefault(ordinal: Int, default: PictureCountRank = DEFAULT): PictureCountRank =
            entries.getOrNull(ordinal) ?: default

        fun fromNameOrNull(name: String?): PictureCountRank? =
            if (name != null) entries.firstOrNull { it.name.equals(name, ignoreCase = true) } else null

        fun fromStringOrNull(value: String?): PictureCountRank? =
            value?.toIntOrNull()?.let { fromCountOrNull(it) } ?: fromNameOrNull(value)

        fun fromString(value: String?, default: PictureCountRank = DEFAULT): PictureCountRank =
            fromStringOrNull(value) ?: default
    }
}
