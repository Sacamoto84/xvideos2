package com.client.xvideos.common.zip

import com.client.xvideos.common.io.normalizeRelativePath
import com.client.xvideos.common.io.requireInside
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
                val name = normalizeRelativePath(entry.name)
                val target = File(root, name).canonicalFile
                requireInside(root, target)
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
}
