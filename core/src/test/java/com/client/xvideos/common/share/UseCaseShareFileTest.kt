package com.client.xvideos.common.share

import android.content.ContextWrapper
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class UseCaseShareFileTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `useCaseShareFile возвращает false и не падает если файл не существует`() {
        val dummyContext = object : ContextWrapper(null) {}
        val nonExistentFile = File(tmp.root, "not_found.mp4")

        val result = useCaseShareFile(dummyContext, nonExistentFile)

        assertFalse(result)
    }

    @Test
    fun `useCaseShareFile возвращает false и не падает если файл пустой 0 байт`() {
        val dummyContext = object : ContextWrapper(null) {}
        val emptyFile = File(tmp.root, "empty.mp4").apply { createNewFile() }

        val result = useCaseShareFile(dummyContext, emptyFile)

        assertFalse(result)
    }
}
