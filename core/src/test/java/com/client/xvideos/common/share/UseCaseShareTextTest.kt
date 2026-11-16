package com.client.xvideos.common.share

import android.content.ContextWrapper
import org.junit.Assert.assertFalse
import org.junit.Test

class UseCaseShareTextTest {

    @Test
    fun `useCaseShareText возвращает false при пустом тексте`() {
        val dummyContext = object : ContextWrapper(null) {}
        val result = useCaseShareText(dummyContext, "")
        assertFalse(result)
    }

    @Test
    fun `useCaseShareText возвращает false при строке только из пробелов`() {
        val dummyContext = object : ContextWrapper(null) {}
        val result = useCaseShareText(dummyContext, "   ")
        assertFalse(result)
    }
}
