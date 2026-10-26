package com.client.xvideos.xlive.model

/** Пол комнаты по-русски; незнакомое значение остаётся как есть. */
fun genderLabel(gender: String): String = when (gender) {
    "female" -> "Женщина"
    "male" -> "Мужчина"
    "couple" -> "Пара"
    "trans" -> "Транс"
    else -> gender
}

/** Кто интересует модель, по-русски; незнакомое значение остаётся как есть. */
fun interestLabel(interest: String): String = when (interest) {
    "Women" -> "Женщины"
    "Men" -> "Мужчины"
    "Couples" -> "Пары"
    "Trans" -> "Транс"
    else -> interest
}

/** Возраст с согласованным словом: «21 год», «22 года», «18 лет». */
fun ageLabel(age: Int): String {
    val lastTwo = age % 100
    val last = age % 10
    val word = when {
        lastTwo in 11..14 -> "лет"
        last == 1 -> "год"
        last in 2..4 -> "года"
        else -> "лет"
    }
    return "$age $word"
}

private const val REGIONAL_INDICATOR_A = 0x1F1E6

/** Флаг страны из кода в две латинские буквы (`US` → 🇺🇸); иначе пустая строка. */
fun flagEmoji(countryCode: String): String {
    val code = countryCode.trim().uppercase()
    if (code.length != 2 || code.any { it !in 'A'..'Z' }) return ""
    return code.map { String(Character.toChars(REGIONAL_INDICATOR_A + (it - 'A'))) }.joinToString("")
}

private const val SECONDS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60L
private const val SECONDS_PER_HOUR = 3_600L
private const val MILLIS_PER_SECOND = 1_000L

/** Сколько идёт запись или сколько она длится: «03:12», «1:02:05». */
fun recordingElapsedLabel(elapsedMs: Long): String {
    val totalSec = elapsedMs.coerceAtLeast(0L) / MILLIS_PER_SECOND
    val hours = totalSec / SECONDS_PER_HOUR
    val minutes = (totalSec % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val seconds = totalSec % SECONDS_PER_MINUTE
    val mmss = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    return if (hours > 0) "$hours:$mmss" else mmss
}

/**
 * Сколько идёт эфир: «2 ч 05 мин», «15 мин».
 *
 * @return `null`, если начало неизвестно или впереди [nowSec].
 */
fun onAirLabel(startedAtSec: Long, nowSec: Long): String? {
    if (startedAtSec <= 0L || nowSec < startedAtSec) return null
    val minutes = (nowSec - startedAtSec) / SECONDS_PER_MINUTE
    val hours = minutes / MINUTES_PER_HOUR
    val rest = minutes % MINUTES_PER_HOUR
    return if (hours > 0) "$hours ч ${rest.toString().padStart(2, '0')} мин" else "$rest мин"
}
