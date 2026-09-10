package com.client.xvideos.common.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Журнал ошибок релизной сборки. Проверяется то, ради чего он вообще есть:
 * запись переживает несколько падений подряд и не растёт бесконечно, а при
 * обрезке остаются **свежие** записи, а не старые.
 */
class CrashLogTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun logFile(): File = File(temp.root, "logs/crash.log")

    @Test
    fun `запись создаёт файл и каталог`() {
        val file = logFile()

        appendEntry(file, header = "FATAL 1", body = "boom", maxBytes = 1024)

        assertTrue(file.exists())
        val text = file.readText()
        assertTrue(text.contains("FATAL 1"))
        assertTrue(text.contains("boom"))
    }

    @Test
    fun `записи дописываются, а не затирают друг друга`() {
        val file = logFile()

        appendEntry(file, "FATAL 1", "первое", maxBytes = 1024)
        appendEntry(file, "FATAL 2", "второе", maxBytes = 1024)

        val text = file.readText()
        assertTrue(text.contains("первое"))
        assertTrue(text.contains("второе"))
        assertTrue("порядок обязан быть хронологическим",
            text.indexOf("первое") < text.indexOf("второе"))
    }

    @Test
    fun `файл не растёт выше потолка`() {
        val file = logFile()
        val big = "x".repeat(500)

        repeat(20) { i -> appendEntry(file, "FATAL $i", big, maxBytes = 2048) }

        assertTrue("размер ${file.length()} должен быть в пределах потолка",
            file.length() <= 2048)
    }

    @Test
    fun `при обрезке остаётся свежее, а не старое`() {
        val file = logFile()
        val filler = "y".repeat(400)

        appendEntry(file, "FATAL самое старое", filler, maxBytes = 1024)
        repeat(5) { appendEntry(file, "FATAL промежуточное", filler, maxBytes = 1024) }
        appendEntry(file, "FATAL самое свежее", "хвост", maxBytes = 1024)

        val text = file.readText()
        assertTrue("свежая запись обязана уцелеть", text.contains("самое свежее"))
        assertTrue("старая запись обязана быть вытеснена", !text.contains("самое старое"))
    }

    @Test
    fun `обрезка режет по границе строки`() {
        val file = logFile()
        file.parentFile?.mkdirs()
        file.writeText((1..200).joinToString("\n") { "строка $it" })

        trimToLimit(file, maxBytes = 200)

        val text = file.readText()
        // Первая строка не должна оказаться обрубком вроде "ока 173".
        val firstLine = text.lineSequence().first()
        assertTrue("первая строка «$firstLine» обрублена", firstLine.startsWith("строка "))
    }

    @Test
    fun `файл меньше потолка не трогается`() {
        val file = logFile()
        file.parentFile?.mkdirs()
        file.writeText("короткая запись")

        trimToLimit(file, maxBytes = 1024)

        assertEquals("короткая запись", file.readText())
    }
}
