package com.client.xvideos.x.screens.rubric

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/** Экран категории переживает сериализацию стека навигатора с тем же ключом. */
class ScreenXRubricSerializationTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> roundTrip(value: T): T {
        val bytes = ByteArrayOutputStream().use { baos ->
            ObjectOutputStream(baos).use { oos -> oos.writeObject(value) }
            baos.toByteArray()
        }
        return ByteArrayInputStream(bytes).use { bais ->
            ObjectInputStream(bais).use { ois -> ois.readObject() as T }
        }
    }

    @Test
    fun `ScreenXRubric сериализуется с ключом по id`() {
        val restored = roundTrip(ScreenXRubric(12, "Cat_B", "Категория Б"))

        assertEquals(12, restored.id)
        assertEquals("Cat_B", restored.slug)
        assertEquals("Категория Б", restored.title)
        assertEquals("ScreenXRubric:12", restored.key)
    }
}
