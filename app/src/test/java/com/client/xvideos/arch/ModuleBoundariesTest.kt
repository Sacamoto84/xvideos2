package com.client.xvideos.arch

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Сторож границ будущих модулей (шаг 0 из docs/module-split-plan.md).
 *
 * Правила целевого графа:
 *   - `common` (будущий `:core`) не знает про разделы `l`, `r`, `x`;
 *   - разделы `l`, `r`, `x` не знают друг о друге.
 *
 * Всё, что должно знать про все разделы сразу (`screenRoot`, `MainActivity`,
 * агрегирующие экраны), лежит выше и под правила не попадает.
 *
 * Тест сравнивает найденные нарушения с зафиксированным списком [BASELINE].
 * Он падает в двух случаях:
 *   - появилось нарушение вне списка — новую связь между слоями завели зря;
 *   - запись из списка больше не нарушается — шаг плана сделан, строку надо
 *     вычеркнуть.
 *
 * Пустой [BASELINE] означает, что шаги 1–5 плана завершены.
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

    private fun findViolations(): Set<String> {
        val root = sourceRoot()
        return root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                val from = sectionOf(file.relativeTo(root).invariantPath()) ?: return@flatMap emptySequence()
                val path = file.relativeTo(root).invariantPath()
                importedSections(file)
                    .filter { it != from && it in FORBIDDEN.getValue(from) }
                    .map { to -> "$path -> $to" }
            }
            .toSet()
    }

    /** Раздел, которому принадлежит файл: первый сегмент пути внутри `com/client/xvideos`. */
    private fun sectionOf(relativePath: String): String? =
        relativePath.substringBefore('/').takeIf { it in SECTIONS }

    /** Разделы, на которые ссылаются `import com.client.xvideos.<раздел>...` этого файла. */
    private fun importedSections(file: File): Sequence<String> =
        file.readLines()
            .asSequence()
            .mapNotNull { IMPORT.find(it)?.groupValues?.get(1) }
            .filter { it.startsWith(PACKAGE_PREFIX) }
            .map { it.removePrefix(PACKAGE_PREFIX).substringBefore('.') }
            .filter { it in SECTIONS }
            .distinct()

    private fun File.invariantPath(): String = path.replace(File.separatorChar, '/')

    /** Каталог `com/client/xvideos` в main-исходниках, найденный от рабочей директории вверх. */
    private fun sourceRoot(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            for (prefix in listOf("src/main/java", "app/src/main/java")) {
                val candidate = File(dir, "$prefix/com/client/xvideos")
                if (candidate.isDirectory) return candidate
            }
            dir = dir.parentFile
        }
        error("Не найден каталог исходников com/client/xvideos от ${File("").absolutePath}")
    }

    private companion object {

        const val PACKAGE_PREFIX = "com.client.xvideos."

        val IMPORT = Regex("""^\s*import\s+([\w.]+)""")

        val SECTIONS = setOf("common", "l", "r", "x")

        /** Кто кого не имеет права импортировать. */
        val FORBIDDEN = mapOf(
            "common" to setOf("l", "r", "x"),
            "l" to setOf("r", "x"),
            "r" to setOf("l", "x"),
            "x" to setOf("l", "r"),
        )

        /**
         * Текущие нарушения на срезе 8dbcce9. Это чек-лист шагов 1–5 плана:
         * строку вычёркивают вместе с переносом кода.
         */
        val BASELINE = setOf(
            // Шаг 1. Общие виджеты уезжают в common/ui.
            "l/ui/screens/explorer/L_ScreenExplorer.kt -> r",
            "l/ui/screens/explorer/tab/saved/ScreenSaved.kt -> r",
            "l/ui/screens/explorer/tab/saved/likes/L_ScreenSavedLikesTab.kt -> r",
            "l/ui/screens/screenAlbumList/atom/AlbumListPageSelector.kt -> x",
            "x/screens/dashboards/ScreenXDashBoards.kt -> r",

            // Шаг 1 + шаг 3: виджеты профиля и атомы плеера.
            "l/ui/element/lazyRowPictureDetails/L_LazyRowPictureDetails.kt -> r",

            // Шаг 2. ThumbnailsSize переезжает из l/model в common.
            "common/settings/Settings.kt -> l",
            "common/settings/ui/components/ThumbnailSizeSelector.kt -> l",
            "common/settings/ui/section/LSettingsSection.kt -> l",

            // Шаг 3. StaticPlayer и VideoPlayerWithMenuContent — в common/videoplayer.
            "common/videoplayer/ui/ComposeVideoPlayer.kt -> r",
            "common/urlVideoImage/UrlVideoLite.kt -> r",
            "l/ui/screens/screenFullScreen/L_FullScreenImage.kt -> r",

            // Шаг 4. UrlVideoImageAndLongClickX переезжает из common в x.
            "common/urlVideoImage/UrlVideoImageAndLongClickX.kt -> x",

            // Шаг 5. Агрегирующие экраны и части P2P уезжают наверх, в :app.
            "common/collectionDB/ui/DialogCollection.kt -> r",
            "common/p2p/P2pReceiveManager.kt -> l",
            "common/p2p/P2pReceiveManager.kt -> r",
            "common/p2p/P2pSendSource.kt -> l",
            "common/p2p/export/Exporters.kt -> l",
            "common/p2p/imports/RLikesBundleImporter.kt -> r",
            "common/p2p/ui/ScreenP2pSend.kt -> l",
            "common/settings/ui/AppSettingsScreen.kt -> l",
            "common/settings/ui/AppSettingsScreen.kt -> r",
            "common/settings/ui/DownloadRecoveryText.kt -> l",
            "common/settings/ui/DownloadRecoveryText.kt -> r",
            "common/settings/ui/backup/BackupSettingsSection.kt -> l",
            "common/settings/ui/backup/BackupSettingsSection.kt -> r",
            "common/settings/ui/section/RSettingsSection.kt -> r",
        )
    }
}
