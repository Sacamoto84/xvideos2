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

    private const val ZIP_BUFFER_SIZE = 32 * 1024

    fun zipDirectory(sourceDir: File, zipFile: File) {
        require(sourceDir.exists()) { "Source directory does not exist: ${sourceDir.absolutePath}" }
        require(sourceDir.isDirectory) { "Source is not a directory: ${sourceDir.absolutePath}" }
        zipFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
        val prefix = sourceDir.name
        ZipOutputStream(BufferedOutputStream(zipFile.outputStream(), ZIP_BUFFER_SIZE)).use { zip ->
            zip.putNextEntry(ZipEntry("$prefix/"))
            zip.closeEntry()
            sourceDir.walkTopDown().forEach { file ->
                if (file == sourceDir) return@forEach
                val rel = file.relativeTo(sourceDir).invariantSeparatorsPath.trim('/')
                if (rel.isEmpty()) return@forEach
                val entryName = "$prefix/$rel"
                if (file.isDirectory) {
                    zip.putNextEntry(ZipEntry("$entryName/"))
                    zip.closeEntry()
                } else {
                    zip.putNextEntry(ZipEntry(entryName))
                    BufferedInputStream(file.inputStream(), ZIP_BUFFER_SIZE).use { it.copyTo(zip, ZIP_BUFFER_SIZE) }
                    zip.closeEntry()
                }
            }
        }
    }

    fun unzip(zipFile: File, destDir: File) {
        require(zipFile.exists() && zipFile.isFile) { "Zip file does not exist or is not a file: ${zipFile.absolutePath}" }
        if (zipFile.length() == 0L) return
        val root = destDir.canonicalFile
        if (!root.exists()) root.mkdirs()
        ZipInputStream(BufferedInputStream(zipFile.inputStream(), ZIP_BUFFER_SIZE)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = normalizeRelativePath(entry.name)
                val target = File(root, name).canonicalFile
                requireInside(root, target)
                if (entry.isDirectory || entry.name.endsWith("/")) {
                    target.mkdirs()
                } else {
                    target.parentFile?.let { if (!it.exists()) it.mkdirs() }
                    BufferedOutputStream(target.outputStream(), ZIP_BUFFER_SIZE).use { out ->
                        zip.copyTo(out, ZIP_BUFFER_SIZE)
                    }
                }
                zip.closeEntry()
            }
        }
    }
}
