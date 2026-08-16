package com.client.xvideos.common.backup

import android.content.Context
import android.net.Uri
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.io.normalizeRelativePath
import com.client.xvideos.common.io.requireInside
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object XlrBackupManager {
    private const val SCHEMA_VERSION = 1
    private const val MANIFEST_ENTRY = "backup.json"
    private const val L_LIKES_PATH = "L/Likes"
    private const val L_COLLECTION_PATH = "L/Collection"
    private const val L_METADATA_FILE_NAME = "metadata.json"
    private const val L_COLLECTION_CONFIG_FILE_NAME = "collection.json"
    private const val R_DOWNLOAD_PATH = "R/Download"

    /**
     * Префикс временной копии прежних данных на время восстановления.
     * Точка в начале — чтобы копия не выглядела как обычная папка данных,
     * если процесс убьют посреди переноса.
     */
    private const val RESTORE_ASIDE_PREFIX = ".xlr_old_"

    private val sections = listOf("X", "L", "R")

    fun defaultFileName(now: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        return "xvideos-xlr-backup-${sdf.format(Date(now))}.zip"
    }

    suspend fun currentBackupItems(
        options: XlrBackupOptions = XlrBackupOptions()
    ): List<XlrBackupItem> = withContext(Dispatchers.IO) {
        sections.flatMap { section ->
            val root = File(AppPath.main, section)
            val rootReport = measurePath(root, section, options)
            val rootItem = XlrBackupItem(
                path = section,
                title = section,
                section = section,
                parentPath = null,
                files = rootReport.files,
                bytes = rootReport.bytes
            )
            val children = root.listFiles()
                ?.filter { it.isDirectory }
                ?.sortedBy { it.name.lowercase(Locale.US) }
                ?.map { child ->
                    val childPath = "$section/${child.name}"
                    val report = measurePath(child, childPath, options)
                    XlrBackupItem(
                        path = childPath,
                        title = child.name,
                        section = section,
                        parentPath = section,
                        files = report.files,
                        bytes = report.bytes
                    )
                }
                ?: emptyList()
            listOf(rootItem) + children
        }
    }

    suspend fun inspectBackup(context: Context, uri: Uri): Result<List<XlrBackupItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val stats = linkedMapOf<String, MutableReport>()
            val input = context.contentResolver.openInputStream(uri)
                ?: error("Cannot open backup file")
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = normalizeRelativePath(entry.name)
                    if (name == MANIFEST_ENTRY) {
                        zip.closeEntry()
                        continue
                    }
                    val top = name.substringBefore("/")
                    if (top !in sections) error("Backup contains unsupported entry: $name")
                    stats.getOrPut(top) { MutableReport() }

                    val child = childPathOrNull(name)
                    if (child != null) stats.getOrPut(child) { MutableReport() }

                    if (!entry.isDirectory && !name.endsWith("/")) {
                        val size = readEntrySize(zip, buffer)
                        stats.getValue(top).add(size)
                        if (child != null) stats.getValue(child).add(size)
                    }
                    zip.closeEntry()
                }
            }
            if (stats.isEmpty()) error("Backup does not contain X/L/R data")
            stats.toItems()
        }
    }

    fun reportForSelection(items: List<XlrBackupItem>, selectedPaths: Set<String>): XlrBackupReport {
        val normalized = normalizeSelectedPaths(selectedPaths)
        return items
            .filter { it.path in normalized }
            .fold(XlrBackupReport(0, 0L)) { acc, item ->
                XlrBackupReport(
                    files = acc.files + item.files,
                    bytes = acc.bytes + item.bytes
                )
            }
    }

    suspend fun createBackup(
        context: Context,
        uri: Uri,
        selectedPaths: Set<String>,
        options: XlrBackupOptions = XlrBackupOptions()
    ): Result<XlrBackupReport> = withContext(Dispatchers.IO) {
        runCatching {
            val safePaths = normalizeSelectedPaths(selectedPaths)
            if (safePaths.isEmpty()) error("Select at least one folder")

            var files = 0
            var bytes = 0L
            val output = context.contentResolver.openOutputStream(uri, "wt")
                ?: error("Cannot open backup file")

            ZipOutputStream(BufferedOutputStream(output)).use { zip ->
                zip.setLevel(ZipOutputStream.DEFLATED)
                writeManifest(zip, safePaths, options)
                safePaths.forEach { path ->
                    val source = File(AppPath.main, path)
                    if (source.exists()) {
                        val report = writePath(zip, source, path, options)
                        files += report.files
                        bytes += report.bytes
                    } else {
                        zip.putNextEntry(ZipEntry("$path/"))
                        zip.closeEntry()
                    }
                }
            }

            XlrBackupReport(files = files, bytes = bytes)
        }
    }

    suspend fun restoreBackup(
        context: Context,
        uri: Uri,
        selectedPaths: Set<String>
    ): Result<XlrBackupReport> = withContext(Dispatchers.IO) {
        runCatching {
            val safePaths = normalizeSelectedPaths(selectedPaths)
            if (safePaths.isEmpty()) error("Select at least one folder")
            validateBackup(context, uri)
            val tempRoot = File(AppPath.main, ".xlr_restore_tmp").apply {
                deleteRecursively()
                mkdirs()
            }
            try {
                val report = extractBackup(context, uri, tempRoot, safePaths)
                applyRestoredPaths(File(AppPath.main), tempRoot, safePaths)
                report
            } finally {
                tempRoot.deleteRecursively()
            }
        }
    }

    /**
     * Переносит распакованные папки поверх текущих данных.
     *
     * Раньше каждая целевая папка удалялась до переноса. Между удалением и
     * переносом данных не существовало нигде, кроме временной папки, и любой
     * сбой посреди цикла (или убийство процесса) оставлял пользователя без
     * части данных и без возможности откатиться.
     *
     * Теперь текущая папка не удаляется, а отодвигается в сторону под скрытым
     * именем. Ошибка на любом шаге возвращает всё, что успели тронуть, в
     * исходное состояние; отодвинутые копии удаляются только после того, как
     * перенесены все пути.
     */
    internal fun applyRestoredPaths(mainRoot: File, tempRoot: File, paths: List<String>) {
        val root = mainRoot.canonicalFile
        // target -> отодвинутая копия прежнего содержимого
        val movedAside = mutableListOf<Pair<File, File>>()
        val written = mutableListOf<File>()

        try {
            paths.forEach { path ->
                val target = File(mainRoot, path)
                requireInside(root, target.canonicalFile)

                if (target.exists()) {
                    val aside = File(target.parentFile, "$RESTORE_ASIDE_PREFIX${target.name}")
                    aside.deleteRecursively()
                    if (!target.renameTo(aside)) {
                        error("Cannot move aside before restore: ${target.absolutePath}")
                    }
                    movedAside += target to aside
                }

                target.parentFile?.mkdirs()
                val restored = File(tempRoot, path)
                if (restored.exists()) {
                    if (!restored.renameTo(target)) {
                        restored.copyRecursively(target, overwrite = true)
                    }
                } else {
                    target.mkdirs()
                }
                written += target
            }
        } catch (e: Throwable) {
            Timber.e(e, "XlrBackupManager restore failed, rolling back")
            written.forEach { it.deleteRecursively() }
            movedAside.forEach { (target, aside) ->
                target.deleteRecursively()
                if (!aside.renameTo(target)) {
                    Timber.e("Rollback incomplete, previous data left at ${aside.absolutePath}")
                }
            }
            throw e
        }

        movedAside.forEach { (_, aside) -> aside.deleteRecursively() }
    }

    private fun writeManifest(zip: ZipOutputStream, selectedPaths: List<String>, options: XlrBackupOptions) {
        val createdAt = utcNowText()
        val pathsJson = selectedPaths.joinToString(", ") { "\"$it\"" }
        val json = """
            {
              "schemaVersion": $SCHEMA_VERSION,
              "createdAt": "$createdAt",
              "sections": ["X", "L", "R"],
              "paths": [$pathsJson],
              "modes": {
                "L": "${options.lMode.name}",
                "R": "${options.rMode.name}"
              }
            }
        """.trimIndent()
        zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
        zip.write(json.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun writePath(
        zip: ZipOutputStream,
        source: File,
        entryRoot: String,
        options: XlrBackupOptions
    ): XlrBackupReport {
        if (source.isFile) {
            return if (shouldIncludeBackupEntry(entryRoot, options)) {
                writeFile(zip, source, entryRoot)
            } else {
                XlrBackupReport(files = 0, bytes = 0L)
            }
        }
        return writeDirectory(zip, source, entryRoot, options)
    }

    private fun writeDirectory(
        zip: ZipOutputStream,
        source: File,
        entryRoot: String,
        options: XlrBackupOptions
    ): XlrBackupReport {
        var files = 0
        var bytes = 0L
        zip.putNextEntry(ZipEntry("$entryRoot/"))
        zip.closeEntry()

        source.walkTopDown().forEach { file ->
            if (file == source) return@forEach
            val relativePath = file.relativeTo(source)
                .invariantSeparatorsPath
                .trim('/')
            if (relativePath.isBlank()) return@forEach

            val entryName = "$entryRoot/$relativePath"
            if (file.isDirectory) {
                zip.putNextEntry(ZipEntry("$entryName/"))
                zip.closeEntry()
                return@forEach
            }
            if (!shouldIncludeBackupEntry(entryName, options)) return@forEach

            val entry = ZipEntry(entryName).apply {
                time = file.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()
            }
            zip.putNextEntry(entry)
            BufferedInputStream(file.inputStream()).use { input ->
                input.copyTo(zip)
            }
            zip.closeEntry()
            files++
            bytes += file.length()
        }

        return XlrBackupReport(files = files, bytes = bytes)
    }

    private fun writeFile(zip: ZipOutputStream, source: File, entryName: String): XlrBackupReport {
        zip.putNextEntry(ZipEntry(entryName).apply {
            time = source.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()
        })
        BufferedInputStream(source.inputStream()).use { input ->
            input.copyTo(zip)
        }
        zip.closeEntry()
        return XlrBackupReport(files = 1, bytes = source.length())
    }

    private fun validateBackup(context: Context, uri: Uri) {
        val input = context.contentResolver.openInputStream(uri)
            ?: error("Cannot open backup file")
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            var hasDataEntry = false
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = normalizeRelativePath(entry.name)
                if (name == MANIFEST_ENTRY) {
                    zip.closeEntry()
                    continue
                }
                val top = name.substringBefore("/")
                if (top !in sections) {
                    error("Backup contains unsupported entry: $name")
                }
                if (name == top && !entry.isDirectory) {
                    error("Backup contains invalid section entry: $name")
                }
                hasDataEntry = true
                zip.closeEntry()
            }
            if (!hasDataEntry) error("Backup does not contain X/L/R data")
        }
    }

    private fun extractBackup(
        context: Context,
        uri: Uri,
        destinationRoot: File,
        selectedPaths: List<String>
    ): XlrBackupReport {
        var files = 0
        var bytes = 0L
        val root = destinationRoot.canonicalFile
        val input = context.contentResolver.openInputStream(uri)
            ?: error("Cannot open backup file")

        ZipInputStream(BufferedInputStream(input)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = normalizeRelativePath(entry.name)
                if (name == MANIFEST_ENTRY) {
                    zip.closeEntry()
                    continue
                }
                if (!selectedPaths.any { name == it || name.startsWith("$it/") }) {
                    zip.closeEntry()
                    continue
                }

                val target = File(root, name).canonicalFile
                requireInside(root, target)

                if (entry.isDirectory || name.endsWith("/")) {
                    target.mkdirs()
                    zip.closeEntry()
                    continue
                }

                target.parentFile?.mkdirs()
                BufferedOutputStream(target.outputStream()).use { output ->
                    zip.copyTo(output)
                }
                if (entry.time > 0L) target.setLastModified(entry.time)
                files++
                bytes += target.length()
                zip.closeEntry()
            }
        }

        return XlrBackupReport(files = files, bytes = bytes)
    }

    private fun readEntrySize(zip: ZipInputStream, buffer: ByteArray): Long {
        var size = 0L
        while (true) {
            val read = zip.read(buffer)
            if (read < 0) break
            size += read
        }
        return size
    }

    private fun measurePath(path: File, entryRoot: String, options: XlrBackupOptions): XlrBackupReport {
        if (!path.exists()) return XlrBackupReport(files = 0, bytes = 0L)
        if (path.isFile) {
            return if (shouldIncludeBackupEntry(entryRoot, options)) {
                XlrBackupReport(files = 1, bytes = path.length())
            } else {
                XlrBackupReport(files = 0, bytes = 0L)
            }
        }
        var files = 0
        var bytes = 0L
        path.walkTopDown().forEach { file ->
            if (file.isFile) {
                val relativePath = file.relativeTo(path)
                    .invariantSeparatorsPath
                    .trim('/')
                val entryName = "$entryRoot/$relativePath"
                if (!shouldIncludeBackupEntry(entryName, options)) return@forEach
                files++
                bytes += file.length()
            }
        }
        return XlrBackupReport(files = files, bytes = bytes)
    }

    private fun shouldIncludeBackupEntry(entryName: String, options: XlrBackupOptions): Boolean {
        val normalized = entryName.replace('\\', '/').trim('/')
        if (options.rMode == XlrBackupContentMode.MINI && normalized.isInsideBackupPath(R_DOWNLOAD_PATH)) {
            return normalized == R_DOWNLOAD_PATH || normalized.endsWith(".info", ignoreCase = true)
        }
        if (
            options.lMode == XlrBackupContentMode.MINI &&
            (normalized.isInsideBackupPath(L_LIKES_PATH) || normalized.isInsideBackupPath(L_COLLECTION_PATH))
        ) {
            val fileName = normalized.substringAfterLast("/")
            return normalized == L_LIKES_PATH ||
                    normalized == L_COLLECTION_PATH ||
                    fileName.equals(L_METADATA_FILE_NAME, ignoreCase = true) ||
                    fileName.equals(L_COLLECTION_CONFIG_FILE_NAME, ignoreCase = true)
        }
        return true
    }

    private fun String.isInsideBackupPath(path: String): Boolean {
        return this == path || startsWith("$path/")
    }

    private fun normalizeSelectedPaths(paths: Set<String>): List<String> {
        val sorted = paths
            .map { normalizeRelativePath(it) }
            .filter { path ->
                val top = path.substringBefore("/")
                top in sections
            }
            .distinct()
            .sorted()
        return sorted.filter { path ->
            sorted.none { parent -> parent != path && path.startsWith("$parent/") }
        }
    }

    private fun childPathOrNull(path: String): String? {
        val parts = path.split('/').filter { it.isNotBlank() }
        if (parts.size < 2) return null
        return "${parts[0]}/${parts[1]}"
    }

    private class MutableReport {
        var files: Int = 0
        var bytes: Long = 0L

        fun add(size: Long) {
            files++
            bytes += size
        }
    }

    private fun Map<String, MutableReport>.toItems(): List<XlrBackupItem> {
        return entries
            .sortedWith(compareBy<Map.Entry<String, MutableReport>> { it.key.substringBefore("/") }
                .thenBy { it.key.count { char -> char == '/' } }
                .thenBy { it.key })
            .map { (path, report) ->
                val section = path.substringBefore("/")
                XlrBackupItem(
                    path = path,
                    title = path.substringAfter("/", section),
                    section = section,
                    parentPath = path.takeIf { it.contains("/") }?.substringBefore("/"),
                    files = report.files,
                    bytes = report.bytes
                )
            }
    }

    private fun utcNowText(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
}
