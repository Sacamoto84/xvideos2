package com.client.xvideos.arch

import com.client.xvideos.arch.ProjectSources.invariantPath
import com.client.xvideos.arch.ProjectSources.roots
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Архитектурный сторож правил `GEMINI.md` (Atomic UI & Code Review Rules).
 *
 * Проверяет все 7 модулей проекта:
 * 1. Вне `Screen`-файлов один файл содержит не более одной non-Preview Composable-функции,
 *    и её имя совпадает с именем файла (с точностью до регистра первой буквы для `remember*`).
 * 2. В `Screen`-файлах (наследниках Voyager `Screen`) из `@Composable` допускаются только
 *    `override fun Content()`, `<Name>Content()` и `@Preview`-функции.
 * 3. Все Composable-компоненты в папках `atom/` и `molecule/` имеют `@Preview`.
 * 4. `@Preview`-функции оформляются легковесно — без обёрток `XvideosTheme` и `SettingsPreview`.
 */
class AtomicUiArchTest {

    @Test
    fun `один файл содержит не более одной non-Preview Composable-функции с именем файла`() {
        val parsed = parsedFiles()
        assertTrue("Сторож не нашёл исходники", parsed.size >= 50)

        val violations = mutableListOf<String>()
        parsed.filterNot { it.isScreenFile }.forEach { info ->
            val topLevelComposables = info.composables.filter { !it.isPreview && it.braceDepth == 0 }
            when {
                topLevelComposables.size > 1 -> {
                    val names = topLevelComposables.joinToString(", ") { it.name }
                    violations += "${info.relativePath}: несколько Composable-функций в одном файле [$names]"
                }
                topLevelComposables.size == 1 -> {
                    val name = topLevelComposables.single().name
                    val expectedUpper = info.stem
                    val expectedLower = info.stem.replaceFirstChar { it.lowercaseChar() }
                    if (name != expectedUpper && name != expectedLower) {
                        violations += "${info.relativePath}: функция '$name' не совпадает с именем файла '${info.stem}.kt'"
                    }
                }
            }
        }

        assertTrue(
            "Нарушение правила 'Один компонент — один файл':\n" + violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }

    @Test
    fun `файлы экранов Screen не содержат вспомогательных Composable-функций`() {
        val violations = mutableListOf<String>()
        parsedFiles().filter { it.isScreenFile }.forEach { info ->
            info.composables.filterNot { it.isPreview }.forEach { fn ->
                if (fn.name != "Content" && !fn.name.endsWith("Content")) {
                    violations += "${info.relativePath}: запрещённая Composable '${fn.name}' внутри Screen-файла"
                }
            }
        }

        assertTrue(
            "Нарушение правила запрета монолитных экранов GEMINI.md:\n" + violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }

    @Test
    fun `компоненты в atom и molecule содержат Preview и не используют тяжёлые обёртки`() {
        val missingPreview = mutableListOf<String>()
        val heavyPreview = mutableListOf<String>()

        parsedFiles().forEach { info ->
            val hasMainComposable = info.composables.any { !it.isPreview }
            val hasPreview = info.composables.any { it.isPreview }
            if (info.isAtomOrMolecule && hasMainComposable && !hasPreview) {
                missingPreview += info.relativePath
            }
            if (hasPreview && (info.cleanCode.contains("XvideosTheme") || info.cleanCode.contains("SettingsPreview"))) {
                heavyPreview += info.relativePath
            }
        }

        assertTrue(
            "Отсутствует @Preview в atom/molecule:\n" + missingPreview.joinToString("\n"),
            missingPreview.isEmpty(),
        )
        assertTrue(
            "Обнаружены тяжёлые обёртки тем/настроек в @Preview:\n" + heavyPreview.joinToString("\n"),
            heavyPreview.isEmpty(),
        )
    }

    private data class ComposableDecl(val name: String, val isPreview: Boolean, val braceDepth: Int)

    private data class FileInfo(
        val relativePath: String,
        val stem: String,
        val cleanCode: String,
        val isScreenFile: Boolean,
        val isAtomOrMolecule: Boolean,
        val composables: List<ComposableDecl>,
    )

    private fun parsedFiles(): List<FileInfo> = roots().flatMap { root ->
        val rootPath = root.invariantPath().trimEnd('/') + "/"
        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .map { file -> parseFile(rootPath, file) }
    }.toList()

    private fun parseFile(rootPath: String, file: File): FileInfo {
        val relativePath = file.invariantPath().removePrefix(rootPath)
        val cleanCode = stripCommentsAndStrings(file.readText())
        val isScreenFile = SCREEN_CLASS_REGEX.containsMatchIn(cleanCode)
        val isAtomOrMolecule = "/atom/" in "/$relativePath" || "/molecule/" in "/$relativePath"

        val composables = mutableListOf<ComposableDecl>()
        val lines = cleanCode.lines()
        var braceDepth = 0

        lines.forEachIndexed { index, line ->
            val funMatch = FUN_DECL_REGEX.find(line)
            if (funMatch != null && braceDepth <= 1) {
                val lookback = lines.subList((index - 8).coerceAtLeast(0), index + 1).joinToString("\n")
                if (lookback.contains("@Composable")) {
                    val isPreview = lookback.contains("@Preview") ||
                        lookback.contains("@androidx.compose.ui.tooling.preview.Preview")
                    composables += ComposableDecl(
                        name = funMatch.groupValues[1],
                        isPreview = isPreview,
                        braceDepth = braceDepth,
                    )
                }
            }
            line.forEach { ch ->
                when (ch) {
                    '{' -> braceDepth++
                    '}' -> if (braceDepth > 0) braceDepth--
                }
            }
        }

        return FileInfo(
            relativePath = relativePath,
            stem = file.nameWithoutExtension,
            cleanCode = cleanCode,
            isScreenFile = isScreenFile,
            isAtomOrMolecule = isAtomOrMolecule,
            composables = composables,
        )
    }

    private companion object {
        val SCREEN_CLASS_REGEX = Regex("""\b(?:class|object)\s+[A-Za-z0-9_]+\b[^{]*[: ,]\s*Screen\b""")
        val FUN_DECL_REGEX = Regex("""\bfun\s+(?:<[^>]+>\s+)?(?:[A-Za-z0-9_?.<>]+\.)?([A-Za-z0-9_]+)\s*\(""")
    }
}
