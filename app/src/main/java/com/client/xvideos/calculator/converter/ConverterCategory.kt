package com.client.xvideos.calculator.converter

/**
 * Категории конвертера Xiaomi MIUI / HyperOS.
 * Содержит 14 категорий, в точности соответствующих актуальной сетке Xiaomi:
 * Валюта, Длина, Масса, Площадь, Время, Финансы, Данные, Дата, Скидка, Объем,
 * Система счисления, Скорость, Температура, ИМТ.
 */
enum class ConverterCategory(
    val title: String,
    val defaultUnits: List<String> = emptyList(),
    val isTool: Boolean = false
) {
    CURRENCY(
        title = "Валюта",
        defaultUnits = listOf("USD", "EUR", "RUB", "CNY", "GBP", "JPY", "TRY", "KZT", "BYN", "AED", "GEL")
    ),
    LENGTH(
        title = "Длина",
        defaultUnits = listOf(
            "Километр км",
            "Метр м",
            "Дециметр дм",
            "Сантиметр см",
            "Миллиметр мм",
            "Миля миля",
            "Ярд ярд",
            "Фут фут",
            "Дюйм дюйм"
        )
    ),
    MASS(
        title = "Масса",
        defaultUnits = listOf(
            "Килограмм кг",
            "Грамм г",
            "Миллиграмм мг",
            "Тонна т",
            "Центнер ц",
            "Фунт фунт",
            "Унция унция"
        )
    ),
    AREA(
        title = "Площадь",
        defaultUnits = listOf(
            "Кв. метр м²",
            "Кв. километр км²",
            "Гектар га",
            "Сотка сотка",
            "Кв. сантиметр см²",
            "Акр акр",
            "Кв. фут фут²"
        )
    ),
    TIME(
        title = "Время",
        defaultUnits = listOf(
            "Часов ч",
            "Минут мин",
            "Секунд с",
            "Миллисекунд мс",
            "Дней дн",
            "Недель нед",
            "Лет лет"
        )
    ),
    FINANCE(
        title = "Финансы",
        isTool = true
    ),
    DATA(
        title = "Данные",
        defaultUnits = listOf(
            "Мегабайт МБ",
            "Килобайт КБ",
            "Гигабайт ГБ",
            "Байт Б",
            "Терабайт ТБ",
            "Петабайт ПБ",
            "Бит бит"
        )
    ),
    DATE(
        title = "Дата",
        isTool = true
    ),
    DISCOUNT(
        title = "Скидка",
        isTool = true
    ),
    VOLUME(
        title = "Объем",
        defaultUnits = listOf(
            "Литр л",
            "Миллилитр мл",
            "Куб. метр м³",
            "Децилитр дл",
            "Галлон гал",
            "Баррель барр",
            "Пинта пинт"
        )
    ),
    BASE(
        title = "Система счисления",
        defaultUnits = listOf(
            "Десятичная DEC",
            "Двоичная BIN",
            "Шестнадцатеричная HEX",
            "Восьмеричная OCT"
        )
    ),
    SPEED(
        title = "Скорость",
        defaultUnits = listOf(
            "Км/ч км/ч",
            "Метр/с м/с",
            "Миля/ч mph",
            "Узел уз",
            "Мах Мах"
        )
    ),
    TEMPERATURE(
        title = "Температура",
        defaultUnits = listOf(
            "Градус Цельсия °C",
            "Градус Фаренгейта °F",
            "Кельвин K"
        )
    ),
    BMI(
        title = "ИМТ",
        isTool = true
    )
}
