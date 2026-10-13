package com.client.xvideos.arch

import com.client.xvideos.arch.ProjectSources.invariantPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Сторож автозаполнения.
 *
 * Приложение не отдаёт системной службе автозаполнения ничего, кроме формы
 * входа L, — та рассчитана на менеджер паролей. Держится это на трёх вещах, и
 * каждая уже терялась или не была сделана:
 *
 * - запрет на корне окна `MainActivity` в проходе 193 уехал вместе с экраном
 *   замка в `LockActivity`, и поля приложения молча стали видны службе;
 * - поля в диалогах — в отдельных окнах, запрет корня на них не действует;
 * - новое поле ввода ничто не заставляло пометить.
 *
 * Тест читает исходники, а не поведение: что служба получает на самом деле,
 * проверяется на устройстве (`cmd autofill set log_level verbose`).
 */
class AutofillExclusionTest {

    @Test
    fun `каждое поле ввода закрыто от автозаполнения`() {
        val unmarked = sources()
            .filter { (path, _) -> path !in OPEN_TO_AUTOFILL }
            .mapNotNull { (path, code) ->
                val fields = FIELD_CALL.findAll(code).count()
                val marks = EXCLUSION_MARK.findAll(code).count()
                if (marks >= fields) null else "$path: полей ввода $fields, пометок $marks"
            }
            .toList()

        assertEquals(
            "Поле ввода без Modifier.excludedFromAutofill(): его текст и сам факт ввода " +
                "уйдут системной службе автозаполнения. Если полю нужен менеджер паролей — " +
                "впишите файл в OPEN_TO_AUTOFILL вместе с причиной.",
            emptyList<String>(),
            unmarked,
        )
    }

    @Test
    fun `окно автозаполнению открывает только форма входа L`() {
        val callers = sources()
            .filter { (path, code) -> ALLOW_CALL.containsMatchIn(code) && !path.endsWith("/AllowWindowAutofill.kt") }
            .map { (path, _) -> path }
            .toSortedSet()

        assertEquals(
            "Изменился список экранов, которые снимают запрет автозаполнения с окна. " +
                "Пока такой экран показан, служба автозаполнения видит все поля окна.",
            OPEN_TO_AUTOFILL,
            callers,
        )
    }

    @Test
    fun `форма входа L сама поля не закрывает`() {
        OPEN_TO_AUTOFILL.forEach { path ->
            val code = sources().first { it.first == path }.second
            assertTrue("В $path нет полей ввода — запись в OPEN_TO_AUTOFILL устарела", FIELD_CALL.containsMatchIn(code))
            assertEquals("$path: менеджер паролей не увидит помеченное поле", 0, EXCLUSION_MARK.findAll(code).count())
        }
    }

    @Test
    fun `запрет на корне окна стоит у обеих Activity`() {
        listOf("MainActivity.kt", "LockActivity.kt").forEach { name ->
            val code = sources().first { it.first == name }.second
            assertTrue(
                "$name больше не закрывает своё окно от автозаполнения (importantForAutofill у корня)",
                WINDOW_BAN in code,
            )
        }
    }

    /** Пары «путь от `com/client/xvideos` — код без комментариев». */
    private fun sources(): Sequence<Pair<String, String>> =
        ProjectSources.roots().flatMap { root ->
            root.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .map { file -> file.relativeTo(root).invariantPath() to file.codeWithoutComments() }
        }

    /**
     * Комментарии описывают прежние поля и сами вызовы — считать их нельзя.
     * Блочные убираются целиком, а не по первому символу строки: пометка,
     * закомментированная внутри такого блока, иначе сошла бы за настоящую.
     */
    private fun File.codeWithoutComments(): String =
        readText()
            .replace(BLOCK_COMMENT, "")
            .lineSequence()
            .filterNot { it.trimStart().startsWith("//") }
            .joinToString("\n")

    private companion object {
        /**
         * Поля ввода Compose. Граница слова отсекает свои обёртки вроде
         * `RSearchTextField(...)`: они зовут помеченное поле, а не являются им.
         * `InputField` — поле строки поиска Material 3; сейчас его в проекте
         * нет, но появиться оно должно сразу с пометкой.
         */
        val FIELD_CALL = Regex(
            """\b(?:(?:Basic|Outlined|BasicSecure|OutlinedSecure|Secure)?TextField|InputField)\s*\("""
        )

        // Блочный комментарий, начатый с начала строки. Внутри строки его
        // открывающая пара символов встречается и в коде — в MIME-типах со
        // звёздочкой, — и вырезать от неё до ближайшего закрытия значило бы
        // выбросить настоящий код вместе с полями и пометками.
        val BLOCK_COMMENT = Regex(
            """^[ \t]*/\*.*?\*/""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE),
        )

        val EXCLUSION_MARK = Regex("""\.excludedFromAutofill\(\)""")

        val ALLOW_CALL = Regex("""\bAllowWindowAutofill\s*\(""")

        const val WINDOW_BAN = "IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS"

        /**
         * Экраны, которым служба автозаполнения нужна.
         *
         * Форма входа L: поля объявлены как логин и пароль, сохранение завершает
         * сеанс автозаполнения — решение владельца от 13.10.2026.
         */
        val OPEN_TO_AUTOFILL = sortedSetOf("l/ui/screens/molecule/LLoginForm.kt")
    }
}
