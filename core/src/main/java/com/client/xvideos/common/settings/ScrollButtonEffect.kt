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

    companion object {
        fun fromNameOrDefault(name: String?): ScrollButtonEffect {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: BLUR
        }
    }
}
