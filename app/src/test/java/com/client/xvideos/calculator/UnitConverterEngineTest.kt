package com.client.xvideos.calculator

import com.client.xvideos.calculator.converter.ConverterCategory
import com.client.xvideos.calculator.converter.UnitConverterEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterEngineTest {

    @Test
    fun `конвертация длины км в м и обратно`() {
        val inMeters = UnitConverterEngine.convert(ConverterCategory.LENGTH, 5.0, "км", "м")
        assertEquals(5000.0, inMeters, 0.001)

        val inKm = UnitConverterEngine.convert(ConverterCategory.LENGTH, 2500.0, "м", "км")
        assertEquals(2.5, inKm, 0.001)
    }

    @Test
    fun `конвертация температуры цельсий в фаренгейт и кельвин`() {
        val fahrenheit = UnitConverterEngine.convert(ConverterCategory.TEMPERATURE, 0.0, "°C", "°F")
        assertEquals(32.0, fahrenheit, 0.001)

        val kelvin = UnitConverterEngine.convert(ConverterCategory.TEMPERATURE, 100.0, "°C", "K")
        assertEquals(373.15, kelvin, 0.001)

        val fromFahrenheit = UnitConverterEngine.convert(ConverterCategory.TEMPERATURE, 212.0, "°F", "°C")
        assertEquals(100.0, fromFahrenheit, 0.001)
    }

    @Test
    fun `конвертация массы кг в граммы и тонны`() {
        val grams = UnitConverterEngine.convert(ConverterCategory.MASS, 2.5, "кг", "г")
        assertEquals(2500.0, grams, 0.001)

        val tons = UnitConverterEngine.convert(ConverterCategory.MASS, 5000.0, "кг", "т")
        assertEquals(5.0, tons, 0.001)
    }

    @Test
    fun `конвертация данных мегабайты в килобайты и байты`() {
        val kb = UnitConverterEngine.convert(ConverterCategory.DATA, 1.0, "МБ", "КБ")
        assertEquals(1024.0, kb, 0.001)

        val bytes = UnitConverterEngine.convert(ConverterCategory.DATA, 1.0, "КБ", "Б")
        assertEquals(1024.0, bytes, 0.001)

        val kbFromFull = UnitConverterEngine.convert(ConverterCategory.DATA, 1.0, "Мегабайт МБ", "Килобайт КБ")
        assertEquals(1024.0, kbFromFull, 0.001)
    }

    @Test
    fun `конвертация валюты рассчитывается корректно`() {
        val usdToRub = UnitConverterEngine.convert(ConverterCategory.CURRENCY, 10.0, "USD", "RUB")
        assertEquals(925.0, usdToRub, 0.01)

        val sameCurrency = UnitConverterEngine.convert(ConverterCategory.CURRENCY, 100.0, "RUB", "RUB")
        assertEquals(100.0, sameCurrency, 0.001)
    }

    @Test
    fun `конвертация скорости кмч в мс`() {
        val ms = UnitConverterEngine.convert(ConverterCategory.SPEED, 36.0, "км/ч", "м/с")
        assertEquals(10.0, ms, 0.001)
    }

    @Test
    fun `конвертация времени минуты в секунды`() {
        val seconds = UnitConverterEngine.convert(ConverterCategory.TIME, 5.0, "мин", "с")
        assertEquals(300.0, seconds, 0.001)
    }

    @Test
    fun `конвертация систем счисления BIN DEC HEX OCT`() {
        val hex = UnitConverterEngine.convertBase("255", "DEC", "HEX")
        assertEquals("FF", hex)

        val dec = UnitConverterEngine.convertBase("1010", "BIN", "DEC")
        assertEquals("10", dec)

        val oct = UnitConverterEngine.convertBase("64", "DEC", "OCT")
        assertEquals("100", oct)
    }
}
