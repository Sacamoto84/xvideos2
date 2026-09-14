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
}
