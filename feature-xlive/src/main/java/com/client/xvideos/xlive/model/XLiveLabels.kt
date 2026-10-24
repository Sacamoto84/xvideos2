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

private const val SECONDS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60L

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
