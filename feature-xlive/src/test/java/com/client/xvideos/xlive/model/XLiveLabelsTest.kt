package com.client.xvideos.xlive.model

import com.client.xvideos.common.util.formatBytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class XLiveLabelsTest {

    @Test
    fun `возраст согласуется со словом`() {
        assertEquals("18 лет", ageLabel(18))
        assertEquals("21 год", ageLabel(21))
        assertEquals("22 года", ageLabel(22))
        assertEquals("25 лет", ageLabel(25))
        assertEquals("111 лет", ageLabel(111))
        assertEquals("101 год", ageLabel(101))
    }

    @Test
    fun `пол и интересы по-русски, незнакомое как есть`() {
        assertEquals("Женщина", genderLabel("female"))
        assertEquals("Пара", genderLabel("couple"))
        assertEquals("unknown", genderLabel("unknown"))
        assertEquals("Мужчины", interestLabel("Men"))
        assertEquals("Other", interestLabel("Other"))
    }

    @Test
    fun `флаг страны из двух букв, иначе пусто`() {
        assertEquals("🇺🇸", flagEmoji("US"))
        assertEquals("🇺🇦", flagEmoji(" ua "))
        assertEquals("", flagEmoji(""))
        assertEquals("", flagEmoji("USA"))
        assertEquals("", flagEmoji("1A"))
    }

    @Test
    fun `сколько идёт эфир`() {
        assertEquals("15 мин", onAirLabel(startedAtSec = 1_000, nowSec = 1_000 + 15 * 60 + 59))
        assertEquals("2 ч 05 мин", onAirLabel(startedAtSec = 1_000, nowSec = 1_000 + 2 * 3600 + 5 * 60))
        assertEquals("0 мин", onAirLabel(startedAtSec = 1_000, nowSec = 1_000))
    }

    @Test
    fun `начало неизвестно или в будущем — null`() {
        assertNull(onAirLabel(startedAtSec = 0, nowSec = 1_000))
        assertNull(onAirLabel(startedAtSec = 2_000, nowSec = 1_000))
    }

    @Test
    fun `дата начала записи и её размер`() {
        val zone = ZoneId.of("Europe/Moscow")
        val startedAt = ZonedDateTime.of(2026, 10, 26, 21, 40, 15, 0, zone).toInstant().toEpochMilli()
        val size = 2_254_857_830L

        val label = recordDateSizeLabel(startedAt, size, zone)

        assertEquals("26 окт, 21:40 · ${formatBytes(size)}", label)
        assertEquals("ЗАПИСЬ · 26 ОКТ 21:40", recordBadgeLabel(startedAt, zone))
    }

    @Test
    fun `время записи — минуты и секунды, с часом — часы`() {
        assertEquals("00:00", recordingElapsedLabel(0))
        assertEquals("03:12", recordingElapsedLabel(192_000))
        assertEquals("1:02:05", recordingElapsedLabel(3_725_000))
        assertEquals("00:00", recordingElapsedLabel(-5))
    }
}
