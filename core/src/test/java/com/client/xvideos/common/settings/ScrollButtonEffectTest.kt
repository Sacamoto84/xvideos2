package com.client.xvideos.common.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollButtonEffectTest {

    @Test
    fun fromNameOrDefault_returnsCorrectEnumForValidNames() {
        assertEquals(ScrollButtonEffect.FLAT, ScrollButtonEffect.fromNameOrDefault("FLAT"))
        assertEquals(ScrollButtonEffect.BLUR, ScrollButtonEffect.fromNameOrDefault("BLUR"))
        assertEquals(ScrollButtonEffect.GLASS, ScrollButtonEffect.fromNameOrDefault("GLASS"))
    }

    @Test
    fun fromNameOrDefault_caseInsensitive() {
        assertEquals(ScrollButtonEffect.FLAT, ScrollButtonEffect.fromNameOrDefault("flat"))
        assertEquals(ScrollButtonEffect.BLUR, ScrollButtonEffect.fromNameOrDefault("bLuR"))
        assertEquals(ScrollButtonEffect.GLASS, ScrollButtonEffect.fromNameOrDefault("Glass"))
    }

    @Test
    fun fromNameOrDefault_returnsBlurForNullOrInvalid() {
        assertEquals(ScrollButtonEffect.BLUR, ScrollButtonEffect.fromNameOrDefault(null))
        assertEquals(ScrollButtonEffect.BLUR, ScrollButtonEffect.fromNameOrDefault(""))
        assertEquals(ScrollButtonEffect.BLUR, ScrollButtonEffect.fromNameOrDefault("UNKNOWN"))
    }

    @Test
    fun allEffectsHaveNonEmptyTitlesAndSubtitles() {
        for (effect in ScrollButtonEffect.entries) {
            assert(effect.title.isNotBlank()) { "Title for $effect must not be blank" }
            assert(effect.subtitle.isNotBlank()) { "Subtitle for $effect must not be blank" }
        }
    }
}
