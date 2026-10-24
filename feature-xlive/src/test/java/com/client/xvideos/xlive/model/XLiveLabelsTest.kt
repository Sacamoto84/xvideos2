package com.client.xvideos.xlive.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
}
