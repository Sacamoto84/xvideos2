package com.client.xvideos.calculator.converter

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.pow

/**
 * Движок конвертации величин, валют и систем счисления калькулятора Xiaomi.
 */
object UnitConverterEngine {

    private val symbols = DecimalFormatSymbols(Locale.US).apply { groupingSeparator = ' ' }
    private val decimalFormat = DecimalFormat("#,##0.######", symbols)

    private val currencyToUsdRates = mapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "RUB" to 92.5,
        "CNY" to 7.24,
        "GBP" to 0.79,
        "JPY" to 155.0,
        "TRY" to 34.2,
        "KZT" to 490.0,
        "BYN" to 3.28,
        "AED" to 3.67,
        "GEL" to 2.75
    )

    private val lengthToMeter = mapOf(
        "км" to 1000.0,
        "м" to 1.0,
        "дм" to 0.1,
        "см" to 0.01,
        "мм" to 0.001,
        "миля" to 1609.344,
        "ярд" to 0.9144,
        "фут" to 0.3048,
        "дюйм" to 0.0254
    )

    private val massToKg = mapOf(
        "т" to 1000.0,
        "ц" to 100.0,
        "кг" to 1.0,
        "г" to 0.001,
        "мг" to 1e-6,
        "фунт" to 0.45359237,
        "унция" to 0.028349523125
    )

    private val areaToSqMeter = mapOf(
        "км²" to 1e6,
        "га" to 10000.0,
        "сотка" to 100.0,
        "м²" to 1.0,
        "см²" to 0.0001,
        "акр" to 4046.8564224,
        "кв. фут" to 0.09290304,
        "фут²" to 0.09290304
    )

    private val volumeToLiter = mapOf(
        "м³" to 1000.0,
        "л" to 1.0,
        "дл" to 0.1,
        "мл" to 0.001,
        "галлон" to 3.785411784,
        "гал" to 3.785411784,
        "баррель" to 158.987295,
        "барр" to 158.987295,
        "пинта" to 0.473176473,
        "пинт" to 0.473176473
    )

    private val speedToMeterPerSec = mapOf(
        "м/с" to 1.0,
        "км/ч" to 1.0 / 3.6,
        "mph" to 0.44704,
        "узел" to 1852.0 / 3600.0,
        "уз" to 1852.0 / 3600.0,
        "Мах" to 340.29
    )

    private val timeToSeconds = mapOf(
        "лет" to 31536000.0,
        "нед" to 604800.0,
        "дней" to 86400.0,
        "дн" to 86400.0,
        "ч" to 3600.0,
        "мин" to 60.0,
        "с" to 1.0,
        "мс" to 0.001
    )

    private val dataToBytes = mapOf(
        "Б" to 1.0,
        "КБ" to 1024.0,
        "МБ" to 1024.0.pow(2),
        "ГБ" to 1024.0.pow(3),
        "ТБ" to 1024.0.pow(4),
        "ПБ" to 1024.0.pow(5),
        "бит" to 0.125
    )

    private val allKnownUnits: Set<String> by lazy {
        currencyToUsdRates.keys + lengthToMeter.keys + massToKg.keys +
            areaToSqMeter.keys + volumeToLiter.keys + speedToMeterPerSec.keys +
            timeToSeconds.keys + dataToBytes.keys +
            setOf("°C", "°F", "K", "DEC", "BIN", "HEX", "OCT")
    }

    internal fun normalizeUnit(unit: String): String {
        val trimmed = unit.trim()
        val last = trimmed.substringAfterLast(" ")
        val first = trimmed.substringBefore(" ")
        return when {
            allKnownUnits.contains(trimmed) -> trimmed
            allKnownUnits.contains(last) -> last
            allKnownUnits.contains(first) -> first
            else -> last.ifEmpty { trimmed }
        }
    }

    fun convert(category: ConverterCategory, value: Double, fromUnit: String, toUnit: String): Double {
        val fromKey = normalizeUnit(fromUnit)
        val toKey = normalizeUnit(toUnit)
        if (fromKey == toKey) return value

        return when (category) {
            ConverterCategory.CURRENCY -> {
                val fromRate = currencyToUsdRates[fromKey] ?: 1.0
                val toRate = currencyToUsdRates[toKey] ?: 1.0
                val inUsd = value / fromRate
                inUsd * toRate
            }
            ConverterCategory.LENGTH -> convertLinear(value, fromKey, toKey, lengthToMeter)
            ConverterCategory.MASS -> convertLinear(value, fromKey, toKey, massToKg)
            ConverterCategory.AREA -> convertLinear(value, fromKey, toKey, areaToSqMeter)
            ConverterCategory.VOLUME -> convertLinear(value, fromKey, toKey, volumeToLiter)
            ConverterCategory.TEMPERATURE -> convertTemperature(value, fromKey, toKey)
            ConverterCategory.SPEED -> convertLinear(value, fromKey, toKey, speedToMeterPerSec)
            ConverterCategory.TIME -> convertLinear(value, fromKey, toKey, timeToSeconds)
            ConverterCategory.DATA -> convertLinear(value, fromKey, toKey, dataToBytes)
            ConverterCategory.BASE -> {
                val baseRes = convertBase(value.toLong().toString(), fromKey, toKey)
                baseRes.toDoubleOrNull() ?: 0.0
            }
            ConverterCategory.FINANCE,
            ConverterCategory.DATE,
            ConverterCategory.DISCOUNT,
            ConverterCategory.BMI -> value
        }
    }

    private fun convertLinear(value: Double, from: String, to: String, table: Map<String, Double>): Double {
        val fromFactor = table[from] ?: 1.0
        val toFactor = table[to] ?: 1.0
        val baseValue = value * fromFactor
        return baseValue / toFactor
    }

    private fun convertTemperature(value: Double, from: String, to: String): Double {
        val inCelsius = when (from) {
            "°C" -> value
            "°F" -> (value - 32.0) * 5.0 / 9.0
            "K" -> value - 273.15
            else -> value
        }
        return when (to) {
            "°C" -> inCelsius
            "°F" -> inCelsius * 9.0 / 5.0 + 32.0
            "K" -> inCelsius + 273.15
            else -> inCelsius
        }
    }

    /**
     * Конвертация систем счисления (BIN, OCT, DEC, HEX).
     */
    fun convertBase(valueStr: String, fromUnit: String, toUnit: String): String {
        val fromKey = normalizeUnit(fromUnit)
        val toKey = normalizeUnit(toUnit)
        val fromRadix = getRadix(fromKey)
        val toRadix = getRadix(toKey)
        val clean = valueStr.trim().replace(" ", "").removePrefix("+")
        if (clean.isEmpty() || clean == "0") return "0"
        return runCatching {
            val isNegative = clean.startsWith("-")
            val unsigned = if (isNegative) clean.removePrefix("-") else clean
            val bigInt = BigInteger(unsigned, fromRadix)
            val result = bigInt.toString(toRadix).uppercase()
            if (isNegative && toRadix == 10) "-$result" else result
        }.getOrDefault("0")
    }

    private fun getRadix(symbol: String): Int = when (symbol.uppercase()) {
        "BIN" -> 2
        "OCT" -> 8
        "DEC" -> 10
        "HEX" -> 16
        else -> 10
    }

    fun formatResult(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        val bd = BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros()
        return decimalFormat.format(bd)
    }
}
