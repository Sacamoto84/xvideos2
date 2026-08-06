package com.client.xvideos.arch

import com.client.xvideos.arch.ProjectSources.invariantPath
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Сторож границ будущих модулей (шаг 0 из docs/module-split-plan.md).
 *
 * Правила целевого графа:
 *   - `common` (будущий `:core`) не знает про разделы `l`, `r`, `x`;
 *   - разделы `l`, `r`, `x` не знают друг о друге;
 *   - ни один раздел не знает про пакеты уровня приложения ([APP_LEVEL]) —
 *     точка сборки зависит от разделов, а не наоборот.
 *
 * Третье правило появилось на шаге 5. Оно ловит соблазн «поднять наверх» то,
 * что зовут из разделов: после распила `:feature-*` не сможет зависеть от
 * `:app` физически, и такая связь просто не соберётся.
 *
 * Тема (`ui`), `R` и `BuildConfig` под правила не попадают: на шаге 6 они
 * уезжают в `:core` либо генерируются на модуль.
 *
 * Тест сравнивает найденные нарушения с зафиксированным списком [BASELINE].
 * Он падает в двух случаях:
 *   - появилось нарушение вне списка — новую связь между слоями завели зря;
 *   - запись из списка больше не нарушается — работа сделана, строку надо
 *     вычеркнуть.
 */
class ModuleBoundariesTest {

    @Test
    fun `импорты между слоями совпадают с зафиксированным списком`() {
        val actual = findViolations()

        val added = actual - BASELINE
        val removed = BASELINE - actual

        val problems = buildString {
            if (added.isNotEmpty()) {
                appendLine("Новые запрещённые импорты (${added.size}):")
                added.sorted().forEach { appendLine("  + $it") }
                appendLine("Слои не должны обрастать новыми связями — перенесите общий код в common.")
            }
            if (removed.isNotEmpty()) {
                appendLine("Записи BASELINE больше не нарушаются (${removed.size}) — удалите их из теста:")
                removed.sorted().forEach { appendLine("  - $it") }
            }
        }

        assertTrue(problems, problems.isEmpty())
    }

    /**
     * Страховка от тихой деградации: после каждого выноса модуля исходники
     * оказываются в новом месте, и сторож, который их не нашёл, зеленеет просто
     * потому, что ничего не прочитал.
     */
    @Test
    fun `сторож видит исходники всех модулей`() {
        val scanned = sourceRoots()
            .flatMap { (it.listFiles() ?: emptyArray()).asSequence() }
            .filter { it.isDirectory }
            .map { it.name }
            .toSet()

        val expected = setOf("common", "l", "r", "x", "screenRoot", "screenSettings", "p2p", "ui")
        val missing = expected - scanned
        assertTrue("Не просканированы пакеты $missing; найдено: $scanned", missing.isEmpty())
    }

    private fun findViolations(): Set<String> =
        sourceRoots().flatMap { root ->
            root.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .flatMap { file ->
                    val path = file.relativeTo(root).invariantPath()
                    val forbidden = FORBIDDEN[sectionOf(path)] ?: return@flatMap emptySequence()
                    ProjectSources.importedTopLevelPackages(file)
                        .filter { it in forbidden }
                        .map { to -> "$path -> $to" }
                }
        }.toSet()

    /** Раздел, которому принадлежит файл: первый сегмент пути внутри `com/client/xvideos`. */
    private fun sectionOf(relativePath: String): String = relativePath.substringBefore('/')

    private fun sourceRoots(): Sequence<File> = ProjectSources.roots()

    private companion object {

        /** Пакеты уровня приложения: будущий `:app`, знать про них разделам нельзя. */
        val APP_LEVEL = setOf("screenRoot", "screenSettings", "p2p")

        /** Кто кого не имеет права импортировать. */
        val FORBIDDEN = mapOf(
            "common" to setOf("l", "r", "x") + APP_LEVEL,
            "l" to setOf("r", "x") + APP_LEVEL,
            "r" to setOf("l", "x") + APP_LEVEL,
            "x" to setOf("l", "r") + APP_LEVEL,
        )

        /**
         * Пусто — и должно таким остаться.
         *
         * Правила проверяются на всех модулях сразу, поэтому сторож продолжает
         * работать и для разделов, которые ещё не вынесены: он падает раньше,
         * чем Gradle, и говорит понятнее.
         */
        val BASELINE = emptySet<String>()
    }
}
