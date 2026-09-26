package com.client.xvideos.common.settings

/**
 * Варианты визуального эффекта плавающих кнопок быстрой прокрутки ("Вверх" и "Вниз").
 *
 * Позволяет пользователю выбрать баланс между эстетикой и производительностью:
 * - [FLAT]: Сплошной цвет с легкой прозрачностью, без шейдеров Haze (0% нагрузки на GPU, для слабых устройств).
 * - [BLUR]: Проверенный классический матовый блюр (HazeBlurStyle, оптимальный баланс).
 * - [GLASS]: Полноценное оптическое стекло с рефракцией, бликами и фасками (Haze 2.0 Glass).
 */
enum class ScrollButtonEffect(
    val title: String,
    val subtitle: String
) {
    FLAT(
        title = "Заливка",
        subtitle = "Сплошной цвет с легкой прозрачностью (минимальная нагрузка)"
    ),
    BLUR(
        title = "Блюр",
        subtitle = "Мягкое матовое размытие фона (рекомендуется)"
    ),
    GLASS(
        title = "Стекло",
        subtitle = "Реалистичное стекло с преломлением и бликами"
    );

    val isFlat: Boolean get() = this == FLAT
    val isBlur: Boolean get() = this == BLUR
    val isGlass: Boolean get() = this == GLASS
    val requiresBlurShader: Boolean get() = this != FLAT
    val hasSubtitle: Boolean get() = subtitle.isNotBlank()

    /** Переход к следующему эффекту циклически. */
    fun next(): ScrollButtonEffect {
        val nextOrdinal = (ordinal + 1) % entries.size
        return entries[nextOrdinal]
    }

    /** Переход к предыдущему эффекту циклически. */
    fun prev(): ScrollButtonEffect {
        val prevOrdinal = if (ordinal == 0) entries.size - 1 else ordinal - 1
        return entries[prevOrdinal]
    }

    companion object {
        val DEFAULT = BLUR

        val allTitles: List<String> = entries.map { it.title }
        val allNames: List<String> = entries.map { it.name }

        fun fromNameOrNull(name: String?): ScrollButtonEffect? =
            if (name != null) entries.firstOrNull { it.name.equals(name, ignoreCase = true) } else null

        fun fromNameOrDefault(name: String?): ScrollButtonEffect {
            return fromNameOrNull(name) ?: DEFAULT
        }

        fun isValidName(name: String?): Boolean = fromNameOrNull(name) != null

        fun fromOrdinalOrDefault(ordinal: Int, default: ScrollButtonEffect = DEFAULT): ScrollButtonEffect =
            entries.getOrNull(ordinal) ?: default

        fun fromTitleOrDefault(title: String?, default: ScrollButtonEffect = DEFAULT): ScrollButtonEffect {
            if (title.isNullOrBlank()) return default
            return entries.firstOrNull { it.title.equals(title, ignoreCase = true) } ?: default
        }
    }
}
