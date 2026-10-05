package com.client.xvideos.screenSettings.backup

import androidx.compose.runtime.mutableStateListOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Строки консоли бэкапа следуют за её содержимым.
 *
 * Консоль — изменяемый список, а он сравнивается по ссылке: строки для показа
 * были запомнены по такому ключу и не пересчитывались. Найдено на устройстве:
 * после создания архива счётчик показывал «1 строк», а список — «Пока пусто».
 */
class BackupConsoleLinesTest {

    @Test
    fun `пустая консоль показывает заглушку`() {
        assertEquals(listOf("Пока пусто"), backupConsoleLines(mutableStateListOf()).value)
    }

    @Test
    fun `строка, добавленная в уже показанную консоль, появляется`() {
        val console = mutableStateListOf<String>()
        val visible = backupConsoleLines(console)
        assertEquals(listOf("Пока пусто"), visible.value)

        console.add("Backup создан и зашифрован")

        assertEquals(listOf("Backup создан и зашифрован"), visible.value)
    }

    @Test
    fun `многострочная запись показывается построчно`() {
        val console = mutableStateListOf("Итог:\nвосстановлено 2\nошибок 0")

        assertEquals(listOf("Итог:", "восстановлено 2", "ошибок 0"), backupConsoleLines(console).value)
    }

    @Test
    fun `после очистки снова заглушка`() {
        val console = mutableStateListOf("Backup создан")
        val visible = backupConsoleLines(console)
        assertEquals(listOf("Backup создан"), visible.value)

        console.clear()

        assertEquals(listOf("Пока пусто"), visible.value)
    }
}
