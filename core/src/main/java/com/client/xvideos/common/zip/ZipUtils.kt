package com.client.xvideos.common.zip

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Маленькая zip-утилита для P2P-передачи папок (коллекции L).
 * [zipDirectory] кладёт содержимое [sourceDir] в архив с префиксом
 * `sourceDir.name` (имя папки едет внутри архива). [unzip] распаковывает с
 * защитой от zip-slip. Чистые функции на File — тестируются на JVM.
 */
object ZipUtils {

    fun zipDirectory(sourceDir: File, zipFile: File) {
        zipFile.parentFile?.mkdirs()
        val prefix = sourceDir.name
        ZipOutputStream(BufferedOutputStream(zipFile.outputStream())).use { zip ->
            zip.putNextEntry(ZipEntry("$prefix/"))
            zip.closeEntry()
            sourceDir.walkTopDown().forEach { file ->
                if (file == sourceDir) return@forEach
                val rel = file.relativeTo(sourceDir).invariantSeparatorsPath.trim('/')
                if (rel.isBlank()) return@forEach
                val entryName = "$prefix/$rel"
                if (file.isDirectory) {
                    zip.putNextEntry(ZipEntry("$entryName/"))
                    zip.closeEntry()
                } else {
                    zip.putNextEntry(ZipEntry(entryName))
                    BufferedInputStream(file.inputStream()).use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }

    fun unzip(zipFile: File, destDir: File) {
        val root = destDir.canonicalFile
        root.mkdirs()
        ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = normalizeEntryName(entry.name)
                val target = File(root, name).canonicalFile
                ensureInside(root, target)
                if (entry.isDirectory || entry.name.endsWith("/")) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    BufferedOutputStream(target.outputStream()).use { out -> zip.copyTo(out) }
                }
                zip.closeEntry()
            }
        }
    }

    private fun normalizeEntryName(raw: String): String {
        val name = raw.replace('\\', '/').trim('/')
        require(name.isNotBlank()) { "Empty zip entry name" }
        require(!name.startsWith("/") && !name.contains(':')) { "Unsafe zip entry: $raw" }
        val parts = name.split('/').filter { it.isNotBlank() }
        require(parts.none { it == "." || it == ".." }) { "Unsafe zip entry: $raw" }
        return parts.joinToString("/")
    }

    private fun ensureInside(root: File, target: File) {
        val rootPath = root.absolutePath
        val targetPath = target.absolutePath
        require(targetPath == rootPath || targetPath.startsWith(rootPath + File.separator)) {
            "Zip entry escapes target dir: $targetPath"
        }
    }
}
