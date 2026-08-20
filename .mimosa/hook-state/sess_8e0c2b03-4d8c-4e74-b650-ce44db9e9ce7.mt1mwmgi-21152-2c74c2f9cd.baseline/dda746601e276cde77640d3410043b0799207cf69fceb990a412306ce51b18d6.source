package com.client.xvideos.arch

import com.client.xvideos.arch.ProjectSources.invariantPath
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Сторож слоёв внутри разделов (шаг 0 из docs/layer-split-plan.md).
 *
 * `ModuleBoundariesTest` рядом сторожит границы между разделами; этот —
 * направление связей внутри каждого из них:
 *
 * ```
 * model  <-  data  <-  domain  <-  ui
 * ```
 *
 * Стрелка — «кто кого имеет право импортировать». Слой не имеет права знать
 * про тот, что выше: парсер не должен тянуть Compose-экран, модель — сеть.
 *
 * Разделы называют слои по-разному ([LAYER_OF_PACKAGE]) — это исторически и
 * само по себе не ошибка, поэтому соответствие задано таблицей, а не именами
 * каталогов. Пакеты вне таблицы перечислены в [NEUTRAL]: правила на них не
 * распространяются, но появиться незаметно новый пакет не может — см. второй
 * тест.
 *
 * Тест сравнивает найденное с [BASELINE] и падает в двух случаях: появилось
 * нарушение вне списка, либо запись из списка больше не нарушается и её пора
 * вычеркнуть.
 */
class LayerBoundariesTest {

    @Test
    fun `связи между слоями совпадают с зафиксированным списком`() {
        val actual = findViolations()

        val added = actual - BASELINE
        val removed = BASELINE - actual

        val problems = buildString {
            if (added.isNotEmpty()) {
                appendLine("Новые связи снизу вверх (${added.size}):")
                added.sorted().forEach { appendLine("  + $it") }
                appendLine("Слой не должен знать про тот, что выше — перенесите общее вниз.")
            }
            if (removed.isNotEmpty()) {
                appendLine("Записи BASELINE больше не нарушаются (${removed.size}) — удалите их из теста:")
                removed.sorted().forEach { appendLine("  - $it") }
            }
        }

        assertTrue(problems, problems.isEmpty())
    }

    /**
     * Новый пакет верхнего уровня в разделе обязан получить слой — или быть
     * явно объявлен нейтральным. Иначе он просто выпадет из проверки, и
     * сторож промолчит там, где должен был ругаться.
     */
    @Test
    fun `все пакеты разделов разложены по слоям`() {
        val unclassified = mutableListOf<String>()

        for (root in ProjectSources.roots()) {
            for (section in LAYER_OF_PACKAGE.keys) {
                val sectionDir = File(root, section)
                if (!sectionDir.isDirectory) continue
                val known = LAYER_OF_PACKAGE.getValue(section).keys + NEUTRAL.getValue(section)
                (sectionDir.listFiles() ?: emptyArray())
                    .filter { it.isDirectory && it.name !in known }
                    .forEach { unclassified += "$section/${it.name}" }
            }
        }

        assertTrue(
            "Пакеты без слоя: $unclassified. Допишите их в LAYER_OF_PACKAGE или в NEUTRAL.",
            unclassified.isEmpty()
        )
    }

    private fun findViolations(): Set<String> =
        ProjectSources.roots().flatMap { root ->
            root.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .flatMap { file -> violationsIn(root, file) }
        }.toSet()

    private fun violationsIn(root: File, file: File): Sequence<String> {
        val path = file.relativeTo(root).invariantPath()
        val section = path.substringBefore('/')
        val layers = LAYER_OF_PACKAGE[section] ?: return emptySequence()

        val fromPackage = path.substringAfter('/').substringBefore('/')
        val fromRank = RANK[layers[fromPackage]] ?: return emptySequence()

        return importedPackagesOfSection(file, section)
            .mapNotNull { toPackage ->
                val toRank = RANK[layers[toPackage]] ?: return@mapNotNull null
                if (fromRank < toRank) "$path -> $toPackage" else null
            }
    }

    /** Пакеты своего же раздела, на которые ссылается файл. */
    private fun importedPackagesOfSection(file: File, section: String): Sequence<String> {
        val prefix = "${ProjectSources.PACKAGE_PREFIX}$section."
        return file.readLines()
            .asSequence()
            .mapNotNull { ProjectSources.IMPORT.find(it)?.groupValues?.get(1) }
            .filter { it.startsWith(prefix) }
            .map { it.removePrefix(prefix) }
            // Только вложенные пути: `…l.net.Luscious` — пакет net, а
            // `…l.KtorRequestHandler` — файл в корне раздела, слоя у него нет.
            .filter { it.contains('.') }
            .map { it.substringBefore('.') }
            .distinct()
    }

    private companion object {

        val RANK = mapOf("model" to 0, "data" to 1, "domain" to 2, "ui" to 3)

        /** Раздел -> пакет верхнего уровня -> слой. */
        val LAYER_OF_PACKAGE = mapOf(
            "l" to mapOf(
                "model" to "model",
                "net" to "data",
                // Не домен, хотя имя обещает обратное: `Repository` — это
                // HTTP-клиент Luscious с RAM/ROM-кешем, авторизацией и
                // антибот-кулдауном. Правил про альбомы в нём нет, только
                // openURI/deleteCache/логин. Тот же слой, что и `net`.
                "repository" to "data",
                "featured" to "domain",
                "ui" to "ui",
            ),
            "x" to mapOf(
                "model" to "model",
                "parcer" to "data",
                "search" to "data",
                "feature" to "domain",
                "screens" to "ui",
            ),
            "r" to mapOf(
                "model" to "model",
                "network" to "data",
                "common" to "domain",
                "ui" to "ui",
            ),
        )

        /** Пакеты, к слоям не относящиеся: DI собирает граф из всех слоёв сразу. */
        val NEUTRAL = mapOf(
            "l" to setOf("di"),
            "x" to emptySet<String>(),
            "r" to emptySet<String>(),
        )

        /**
         * Пусто: шаги 1–3 плана сделаны, `l` и `x` выпрямлены.
         *
         * Восемь записей `l/net -> repository` отсюда ушли не переносом кода, а
         * исправлением самого правила: `repository` был ошибочно записан в
         * `domain`, хотя это слой данных. Обращение сети к нему нарушением
         * никогда не было.
         */
        val BASELINE = emptySet<String>()
    }
}
