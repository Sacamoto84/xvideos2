package com.client.xvideos.arch

import java.io.File

/**
 * Общее для архитектурных сторожей: как найти исходники всех модулей.
 *
 * Вынесено из [ModuleBoundariesTest], когда рядом появился
 * [LayerBoundariesTest]: две копии поиска корней разъехались бы при первом же
 * переносе каталогов, и сторожа тихо перестали бы что-либо проверять.
 */
internal object ProjectSources {

    /**
     * Каталоги `com/client/xvideos` во всех модулях проекта.
     *
     * Пути внутри них не пересекаются (`common/`, `l/`, `screenRoot/`…), поэтому
     * результаты модулей просто складываются: правила формулируются в терминах
     * пакетов, а не модулей, и переживают каждый следующий вынос.
     */
    fun roots(): Sequence<File> {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle").isFile || File(dir, "settings.gradle.kts").isFile) {
                val roots = (dir.listFiles() ?: emptyArray())
                    .map { File(it, "src/main/java/com/client/xvideos") }
                    .filter { it.isDirectory }
                if (roots.isEmpty()) error("В ${dir.absolutePath} не нашлось ни одного модуля с исходниками")
                return roots.asSequence()
            }
            dir = dir.parentFile
        }
        error("Не найден корень проекта (settings.gradle) от ${File("").absolutePath}")
    }

    /** Путь через `/` независимо от платформы — чтобы записи сторожей читались одинаково. */
    fun File.invariantPath(): String = path.replace(File.separatorChar, '/')

    val IMPORT = Regex("""^\s*import\s+([\w.]+)""")

    const val PACKAGE_PREFIX = "com.client.xvideos."

    /** Первые сегменты `import com.client.xvideos.<сегмент>...` этого файла. */
    fun importedTopLevelPackages(file: File): Sequence<String> =
        file.readLines()
            .asSequence()
            .mapNotNull { IMPORT.find(it)?.groupValues?.get(1) }
            .filter { it.startsWith(PACKAGE_PREFIX) }
            .map { it.removePrefix(PACKAGE_PREFIX).substringBefore('.') }
            .distinct()
}
