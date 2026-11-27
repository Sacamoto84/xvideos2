package com.client.xvideos.arch

import com.client.xvideos.arch.ProjectSources.invariantPath
import com.client.xvideos.arch.ProjectSources.roots
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Дополнительные архитектурные стражи, закрывающие слепые зоны базовых проверок:
 * - индивидуальный контроль каждого `TextField`/`OutlinedTextField`/`BasicTextField`
 *   (чтобы один модификатор в файле не маскировал второе незащищённое поле),
 * - запрет верхнеуровневых `var`-полей,
 * - контроль прямых FQN-обращений `com.client.xvideos.*` без `import`-директив,
 * - запрет `org.json` в недоверенном I/O (пакеты `p2p` и `backup`).
 */
class ArchHardeningTest {

    @Test
    fun `каждый TextField вне форм входа L и X защищён excludedFromAutofill`() {
        val unshielded = mutableListOf<String>()

        ProjectSources.kotlinSourcesWithoutComments().forEach { (path, code) ->
            if (path in LOGIN_FORM_FILES) return@forEach

            TEXT_FIELD_CALL.findAll(code).forEach { match ->
                val callArgs = extractBalancedCall(code, match.range.last + 1)
                val hasDirectExclusion = "excludedFromAutofill(" in callArgs
                val modifierVar = MODIFIER_VAR_ASSIGN.find(callArgs)?.groupValues?.get(1)
                val hasVarExclusion = modifierVar != null &&
                    Regex("""\bval\s+${Regex.escape(modifierVar)}\b[^=\n]*=[^\n]*excludedFromAutofill\(""")
                        .containsMatchIn(code)

                if (!hasDirectExclusion && !hasVarExclusion) {
                    val lineNumber = code.substring(0, match.range.first).count { it == '\n' } + 1
                    unshielded += "$path:$lineNumber"
                }
            }
        }

        assertTrue(
            "Обнаружены поля ввода без .excludedFromAutofill() в собственном вызове:\n" +
                unshielded.joinToString("\n"),
            unshielded.isEmpty(),
        )
    }

    @Test
    fun `верхнеуровневых изменяемых var-полей нет ни в одном модуле`() {
        val violations = mutableListOf<String>()

        roots().forEach { root ->
            val rootPath = root.invariantPath().trimEnd('/') + "/"
            root.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { file ->
                    val path = file.invariantPath().removePrefix(rootPath)
                    val clean = stripCommentsAndStrings(file.readText())
                    var braceDepth = 0
                    var parenDepth = 0
                    clean.lines().forEachIndexed { idx, line ->
                        val trimmed = line.trimStart()
                        if (braceDepth == 0 && parenDepth == 0 && TOP_LEVEL_VAR.containsMatchIn(trimmed)) {
                            violations += "$path:${idx + 1}: $trimmed"
                        }
                        line.forEach { ch ->
                            when (ch) {
                                '{' -> braceDepth++
                                '}' -> if (braceDepth > 0) braceDepth--
                                '(' -> parenDepth++
                                ')' -> if (parenDepth > 0) parenDepth--
                            }
                        }
                    }
                }
        }

        assertTrue(
            "Найдены верхнеуровневые var-поля:\n" + violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }

    @Test
    fun `прямые FQN-вызовы не нарушают изоляцию разделов L N R X XLive`() {
        val featureSections = setOf("l", "n", "r", "x", "xlive")
        val violations = mutableListOf<String>()

        ProjectSources.kotlinSourcesWithoutComments().forEach { (path, code) ->
            val currentSection = path.substringBefore('/')
            code.lines().forEachIndexed { idx, line ->
                val trimmed = line.trimStart()
                if (trimmed.startsWith("package ") || trimmed.startsWith("import ")) return@forEachIndexed
                FQN_PREFIX.findAll(trimmed).forEach { m ->
                    val targetPackage = m.groupValues[1]
                    if (currentSection in featureSections && targetPackage in featureSections && targetPackage != currentSection) {
                        violations += "$path:${idx + 1} -> com.client.xvideos.$targetPackage"
                    }
                    if (currentSection == "common" && targetPackage in featureSections) {
                        violations += "$path:${idx + 1} -> com.client.xvideos.$targetPackage"
                    }
                }
            }
        }

        assertTrue(
            "Межсекционные FQN-обращения без import:\n" + violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }

    @Test
    fun `пакеты p2p и backup не используют org json`() {
        val violations = mutableListOf<String>()
        roots().forEach { root ->
            val rootPath = root.invariantPath().trimEnd('/') + "/"
            root.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { file ->
                    val rel = file.invariantPath().removePrefix(rootPath)
                    if (rel.startsWith("common/p2p/") || rel.startsWith("common/backup/")) {
                        val txt = file.readText()
                        if ("org.json" in txt || "JSONObject" in txt || "JSONArray" in txt) {
                            violations += rel
                        }
                    }
                }
        }

        assertTrue(
            "Найдено использование org.json в p2p/backup:\n" + violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }

    private fun extractBalancedCall(text: String, openParenAfterIndex: Int): String {
        var depth = 1
        var i = openParenAfterIndex
        while (i < text.length && depth > 0) {
            when (text[i]) {
                '(' -> depth++
                ')' -> depth--
            }
            i++
        }
        return text.substring(openParenAfterIndex, (i - 1).coerceAtLeast(openParenAfterIndex))
    }

    private companion object {
        val LOGIN_FORM_FILES = setOf(
            "l/ui/screens/molecule/LLoginForm.kt",
            "x/screens/login/molecule/XLoginForm.kt",
        )
        val TEXT_FIELD_CALL = Regex("""\b(?:BasicTextField|OutlinedTextField|TextField)\s*\(""")
        val MODIFIER_VAR_ASSIGN = Regex("""\bmodifier\s*=\s*([A-Za-z_][A-Za-z0-9_]*)\b(?!\.)""")
        val TOP_LEVEL_VAR = Regex("""^(?:(?:public|internal|private)\s+)?var\b""")
        val FQN_PREFIX = Regex("""\bcom\.client\.xvideos\.([a-z]+)\.""")
    }
}
