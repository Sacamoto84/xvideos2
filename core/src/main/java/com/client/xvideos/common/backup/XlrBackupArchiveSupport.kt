package com.client.xvideos.common.backup

import android.content.Context
import android.net.Uri
import com.client.xvideos.common.io.normalizeRelativePath
import com.client.xvideos.common.io.requireInside
import com.client.xvideos.common.io.writeTextAtomically
import com.client.xvideos.common.json.AppJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.CRC32
import java.util.zip.CheckedInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal object XlrBackupPathsConfig {
    const val SCHEMA_VERSION = 1
    const val MANIFEST_ENTRY = "backup.json"
    private const val L_LIKES_PATH = "L/Likes"
    private const val L_COLLECTION_PATH = "L/Collection"
    const val L_METADATA_FILE_NAME = "metadata.json"
    private const val L_COLLECTION_CONFIG_FILE_NAME = "collection.json"
    const val R_DOWNLOAD_PATH = "R/Download"

    val sections = listOf("X", "L", "R", XLiveLegacyFolder.CURRENT, "N")

    fun shouldIncludeBackupEntry(entryName: String, options: XlrBackupOptions): Boolean {
        val normalized = entryName.replace('\\', '/').trim('/')
        if (normalized.split('/').any { it.startsWith(".") }) {
            return false
        }
        // Остаток оборванной загрузки: недокачанный ролик нельзя ни открыть, ни
        // докачать на другом устройстве, а восстановление такие файлы отбрасывает.
        if (XlrRestoreCleanup.isDisposableBackupEntry(normalized)) {
            return false
        }
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

    fun String.isInsideBackupPath(path: String): Boolean {
        return this == path || startsWith("$path/")
    }

    fun normalizeSelectedPaths(paths: Set<String>): List<String> {
        val sorted = paths
            .map { normalizeRelativePath(it) }
            .filter { path ->
                val top = path.substringBefore("/")
                top in sections && !path.split('/').any { it.startsWith(".") }
            }
            .distinct()
            .sorted()
        return sorted.filter { path ->
            sorted.none { parent -> parent != path && path.startsWith("$parent/") }
        }
    }

    fun childPathOrNull(path: String): String? {
        val parts = path.split('/').filter { it.isNotBlank() }
        if (parts.size < 2) return null
        return "${parts[0]}/${parts[1]}"
    }
}

/**
 * Записано на сейчас. Байты растут по ходу копирования, а не по концу
 * файла: запись XLive в гигабайт иначе стоит на одном проценте минуту.
 */
internal class XlrBackupProgress(private val onProgress: (XlrBackupReport) -> Unit) {
    var report = XlrBackupReport(files = 0, bytes = 0L)
        private set

    fun addBytes(count: Int) = publish(report.copy(bytes = report.bytes + count))

    fun fileDone() = publish(report.copy(files = report.files + 1))

    private fun publish(next: XlrBackupReport) {
        report = next
        onProgress(next)
    }
}

internal object XlrBackupArchiveWriter {
    @Serializable
    private data class XlrManifestData(
        val schemaVersion: Int,
        val createdAt: String,
        val sections: List<String>,
        val paths: List<String>,
        val modes: Map<String, String>,
    )

    fun writeManifest(zip: ZipOutputStream, selectedPaths: List<String>, options: XlrBackupOptions) {
        val manifest = XlrManifestData(
            schemaVersion = XlrBackupPathsConfig.SCHEMA_VERSION,
            createdAt = utcNowText(),
            sections = XlrBackupPathsConfig.sections,
            paths = selectedPaths,
            modes = mapOf(
                "L" to options.lMode.name,
                "R" to options.rMode.name,
            ),
        )
        zip.putNextEntry(ZipEntry(XlrBackupPathsConfig.MANIFEST_ENTRY))
        zip.write(AppJson.encodeToString(manifest).toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    fun writePath(
        zip: ZipOutputStream,
        source: File,
        entryRoot: String,
        options: XlrBackupOptions,
        progress: XlrBackupProgress,
        checkActive: () -> Unit,
    ) {
        checkActive()
        if (!XlrBackupPathsConfig.shouldIncludeBackupEntry(entryRoot, options)) return
        if (source.isFile) {
            writeFile(zip, source, entryRoot, progress, checkActive)
        } else {
            writeDirectory(zip, source, entryRoot, options, progress, checkActive)
        }
    }

    private fun writeDirectory(
        zip: ZipOutputStream,
        source: File,
        entryRoot: String,
        options: XlrBackupOptions,
        progress: XlrBackupProgress,
        checkActive: () -> Unit,
    ) {
        zip.putNextEntry(ZipEntry("$entryRoot/"))
        zip.closeEntry()

        source.walkTopDown().forEach { file ->
            checkActive()
            if (file == source) return@forEach
            val relativePath = file.relativeTo(source)
                .invariantSeparatorsPath
                .trim('/')
            if (relativePath.isBlank()) return@forEach

            val entryName = "$entryRoot/$relativePath"
            if (!XlrBackupPathsConfig.shouldIncludeBackupEntry(entryName, options)) return@forEach
            if (file.isDirectory) {
                zip.putNextEntry(ZipEntry("$entryName/"))
                zip.closeEntry()
                return@forEach
            }

            writeFile(zip, file, entryName, progress, checkActive)
        }
    }

    private fun writeFile(
        zip: ZipOutputStream,
        source: File,
        entryName: String,
        progress: XlrBackupProgress,
        checkActive: () -> Unit,
    ) {
        zip.putNextEntry(ZipEntry(entryName).apply {
            time = source.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()
        })
        source.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read = input.read(buffer)
            while (read >= 0) {
                checkActive()
                zip.write(buffer, 0, read)
                progress.addBytes(read)
                read = input.read(buffer)
            }
        }
        zip.closeEntry()
        progress.fileDone()
    }

    private fun utcNowText(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
}

internal object XlrBackupArchiveReader {
    private class MutableReport {
        var files: Int = 0
        var bytes: Long = 0L

        fun add(size: Long) {
            files++
            bytes += size
        }
    }

    fun detectBackupType(context: Context, uri: Uri): XlrBackupType {
        return runCatching {
            val input = context.contentResolver.openInputStream(uri) ?: return XlrBackupType.UNSUPPORTED
            input.use { stream ->
                val header = ByteArray(4)
                val read = readUpToNBytes(stream, header)
                if (read < 4) XlrBackupType.UNSUPPORTED else XlrChunkedCrypto.detectType(header)
            }
        }.getOrDefault(XlrBackupType.UNSUPPORTED)
    }

    fun buildCurrentBackupItems(options: XlrBackupOptions, baseDir: File): List<XlrBackupItem> {
        return XlrBackupPathsConfig.sections.flatMap { section ->
            val root = File(baseDir, section)
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
                ?.filter { it.isDirectory && !it.name.startsWith(".") }
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

    fun inspectArchiveItems(
        context: Context,
        uri: Uri,
        password: CharArray?,
        checkActive: () -> Unit,
    ): List<XlrBackupItem> {
        val stats = linkedMapOf<String, MutableReport>()
        ZipInputStream(BufferedInputStream(openBackupInputStream(context, uri, password))).use { zip ->
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = normalizeRelativePath(entry.name)
                if (name == XlrBackupPathsConfig.MANIFEST_ENTRY) {
                    zip.closeEntry()
                    continue
                }
                val top = name.substringBefore("/")
                if (top !in XlrBackupPathsConfig.sections) error("Backup contains unsupported entry: $name")
                stats.getOrPut(top) { MutableReport() }

                val child = XlrBackupPathsConfig.childPathOrNull(name)
                if (child != null) stats.getOrPut(child) { MutableReport() }

                if (!entry.isDirectory && !name.endsWith("/") && !entry.name.endsWith('\\')) {
                    // Чтение гигабайтного архива длится десятки секунд: отмена
                    // из диалога пароля прерывает его, а не ждёт конца.
                    val size = readEntrySize(zip, buffer, checkActive)
                    stats.getValue(top).add(size)
                    if (child != null) stats.getValue(child).add(size)
                }
                zip.closeEntry()
            }
        }
        if (stats.isEmpty()) error("Backup does not contain X/L/R data")
        return stats.toItems()
    }

    fun openBackupInputStream(context: Context, uri: Uri, password: CharArray?): InputStream {
        val rawInput = context.contentResolver.openInputStream(uri)
            ?: error("Cannot open backup file")
        try {
            val buffered = BufferedInputStream(rawInput)
            buffered.mark(16)
            val header = ByteArray(4)
            val read = readUpToNBytes(buffered, header)
            buffered.reset()
            if (read < 4) error("Backup file is empty or corrupted")

            val type = XlrChunkedCrypto.detectType(header)
            return when (type) {
                XlrBackupType.ENCRYPTED_XLR -> {
                    if (password == null || password.isEmpty()) {
                        throw XlrInvalidPasswordException("Архив зашифрован. Требуется ввод пароля.")
                    }
                    XlrEncryptedInputStream(buffered, password)
                }
                XlrBackupType.LEGACY_ZIP -> buffered
                XlrBackupType.UNSUPPORTED -> {
                    throw XlrCorruptedBackupException("Неподдерживаемый формат файла бэкапа")
                }
            }
        } catch (e: Throwable) {
            runCatching { rawInput.close() }
            throw e
        }
    }

    /**
     * Проверяет архив и возвращает его пути второго уровня (`X/Favorites`):
     * по ним восстановление узнаёт, какие папки раздела в архиве есть.
     * Записи со скрытыми сегментами в список не входят — распаковка их пропускает.
     */
    fun validateBackup(zip: ZipFile, checkActive: () -> Unit): Set<String> {
        val folders = sortedSetOf<String>()
        for (entry in zip.entries()) {
            checkActive()
            val name = normalizeRelativePath(entry.name)
            if (name == XlrBackupPathsConfig.MANIFEST_ENTRY) continue
            val top = name.substringBefore("/")
            check(top in XlrBackupPathsConfig.sections) { "Backup contains unsupported entry: $name" }
            check(name != top || entry.isDirectory) { "Backup contains invalid section entry: $name" }
            if (name.split('/').none { it.startsWith(".") }) {
                XlrBackupPathsConfig.childPathOrNull(name)?.let(folders::add)
            }
        }
        return folders
    }

    fun extractBackup(
        zip: ZipFile,
        destinationRoot: File,
        selectedPaths: List<String>,
        checkActive: () -> Unit,
    ): XlrBackupReport {
        var files = 0
        var bytes = 0L
        val root = destinationRoot.canonicalFile

        // Если в архиве есть обе структуры, запись из новой папки имеет приоритет.
        val entries = zip.entries().asSequence()
            .map { it to normalizeRelativePath(it.name) }
            .sortedBy { (_, name) ->
                with(XlrBackupPathsConfig) { !name.isInsideBackupPath(XLiveLegacyFolder.LEGACY) }
            }
        for ((entry, name) in entries) {
            checkActive()
            val delta = extractArchiveEntry(zip, entry, name, root, selectedPaths, checkActive)
            files += delta.first
            bytes += delta.second
        }

        return XlrBackupReport(files = files, bytes = bytes)
    }

    private fun extractArchiveEntry(
        zip: ZipFile,
        entry: ZipEntry,
        name: String,
        root: File,
        selectedPaths: List<String>,
        checkActive: () -> Unit,
    ): Pair<Int, Long> {
        if (name.split('/').any { it.startsWith(".") }) return 0 to 0L
        if (selectedPaths.none { with(XlrBackupPathsConfig) { name.isInsideBackupPath(it) } }) return 0 to 0L
        val restoreName = XLiveLegacyFolder.restorePath(name) ?: return 0 to 0L
        val target = File(root, restoreName).canonicalFile
        requireInside(root, target)

        if (entry.isDirectory || entry.name.endsWith('/') || entry.name.endsWith('\\')) {
            check(target.isDirectory || target.mkdirs()) { "Не удалось создать папку восстановления" }
            return 0 to 0L
        }

        target.parentFile?.mkdirs()
        var addedFile = 0
        val previousSize = if (target.isFile) {
            target.length()
        } else {
            addedFile = 1
            0L
        }
        val crc = CRC32()
        val size = CheckedInputStream(zip.getInputStream(entry), crc).use { input ->
            BufferedOutputStream(target.outputStream()).use { output -> input.copyArchiveTo(output, checkActive) }
        }
        check(size == entry.size && crc.value == entry.crc) { "Повреждён файл в архиве: $name" }
        if (entry.time > 0L) target.setLastModified(entry.time)
        return addedFile to (size - previousSize)
    }

    private fun readUpToNBytes(input: InputStream, buffer: ByteArray): Int {
        var total = 0
        while (total < buffer.size) {
            val read = input.read(buffer, total, buffer.size - total)
            if (read <= 0) break
            total += read
        }
        return total
    }

    private fun readEntrySize(zip: ZipInputStream, buffer: ByteArray, ensureActive: () -> Unit): Long {
        var size = 0L
        while (true) {
            ensureActive()
            val read = zip.read(buffer)
            if (read < 0) break
            size += read
        }
        return size
    }

    private fun measurePath(path: File, entryRoot: String, options: XlrBackupOptions): XlrBackupReport {
        if (!path.exists()) return XlrBackupReport(files = 0, bytes = 0L)
        if (path.isFile) {
            return if (XlrBackupPathsConfig.shouldIncludeBackupEntry(entryRoot, options)) {
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
                if (!XlrBackupPathsConfig.shouldIncludeBackupEntry(entryName, options)) return@forEach
                files++
                bytes += file.length()
            }
        }
        return XlrBackupReport(files = files, bytes = bytes)
    }

    private fun Map<String, MutableReport>.toItems(): List<XlrBackupItem> {
        return entries
            .sortedWith(
                compareBy<Map.Entry<String, MutableReport>> { it.key.substringBefore("/") }
                    .thenBy { it.key.count { char -> char == '/' } }
                    .thenBy { it.key }
            )
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
}

internal object XlrBackupRestoreJournal {
    /**
     * Префикс временной копии прежних данных на время восстановления.
     * Точка в начале — чтобы копия не выглядела как обычная папка данных,
     * если процесс убьют посреди переноса.
     */
    private const val RESTORE_ASIDE_PREFIX = ".xlr_old_"

    /** Журнал восстановления в корне данных; имя с точкой — в бэкап не попадает. */
    private const val RESTORE_JOURNAL_NAME = ".xlr_restore_journal"
    const val RESTORE_TEMP_DIR_NAME = ".xlr_restore_tmp"

    /**
     * Журнал восстановления.
     *
     * @property committed `false` — пути ещё переносятся, при сбое нужен откат;
     * `true` — всё перенесено, осталось удалить отодвинутые копии.
     * @property mode Режим восстановления: от него зависит, что уборка делает с
     * прежней копией. В журналах, записанных до появления режима, поля нет —
     * тогда восстановление умело только заменять.
     */
    @Serializable
    private data class RestoreJournal(
        val committed: Boolean,
        val entries: List<XlrBackupManager.RestoreJournalEntry>,
        val mode: XlrRestoreMode = XlrRestoreMode.REPLACE,
    )

    fun restoreJournalFile(mainRoot: File): File = File(mainRoot, RESTORE_JOURNAL_NAME)

    fun asideFor(target: File): File = File(target.parentFile, "$RESTORE_ASIDE_PREFIX${target.name}")

    fun writeRestoreJournal(
        mainRoot: File,
        committed: Boolean,
        entries: List<XlrBackupManager.RestoreJournalEntry>,
        mode: XlrRestoreMode = XlrRestoreMode.REPLACE,
    ) {
        val journal = RestoreJournal(committed = committed, entries = entries.toList(), mode = mode)
        restoreJournalFile(mainRoot).writeTextAtomically(AppJson.encodeToString(RestoreJournal.serializer(), journal))
    }

    fun hasRestoreJournal(mainRoot: File): Boolean = restoreJournalFile(mainRoot).exists()

    fun recoverInterruptedRestoreBeforeStartup(mainRoot: File): Boolean {
        if (!restoreJournalFile(mainRoot).exists()) return true
        return recoverInterruptedRestore(mainRoot, cleanTemp = false)
    }

    fun recoverInterruptedRestore(mainRoot: File, cleanTemp: Boolean = true): Boolean {
        val journalFile = restoreJournalFile(mainRoot)
        val tempRoot = File(mainRoot, RESTORE_TEMP_DIR_NAME)
        if (!journalFile.exists()) return !cleanTemp || tempRoot.deleteRecursively()
        val journal = runCatching {
            AppJson.decodeFromString(RestoreJournal.serializer(), journalFile.readText(Charsets.UTF_8))
        }.getOrElse {
            Timber.e(it, "XlrBackupManager: журнал восстановления не читается")
            return false
        }

        Timber.w("XlrBackupManager: найдено оборванное восстановление, committed=${journal.committed}")
        val root = mainRoot.canonicalFile
        var clean = true
        journal.entries.asReversed().forEach { entry ->
            val target = File(mainRoot, entry.path)
            if (runCatching { requireInside(root, target.canonicalFile) }.isFailure) {
                Timber.w("XlrBackupManager: путь из журнала вне корня, пропущен")
                return@forEach
            }
            val aside = asideFor(target)
            if (journal.committed) {
                if (!XlrRestoreCleanup.finishPath(root, target, aside, journal.mode)) clean = false
            } else if (!rollBackEntry(target, aside, entry.hadTarget)) {
                clean = false
            }
        }
        if (clean) {
            val tempCleaned = !cleanTemp || tempRoot.deleteRecursively()
            return journalFile.delete() && tempCleaned
        }
        return clean
    }

    /** Возвращает один путь в состояние до восстановления. */
    private fun rollBackEntry(target: File, aside: File, hadTarget: Boolean): Boolean = when {
        aside.exists() -> {
            target.deleteRecursively()
            aside.renameTo(target).also { renamed ->
                if (!renamed) Timber.e("Rollback incomplete, previous data left at ${aside.absolutePath}")
            }
        }
        // Пути до восстановления не было: то, что успели положить, убираем.
        !hadTarget -> {
            target.deleteRecursively()
            true
        }
        // Отодвинуть не успели — прежние данные на месте.
        else -> true
    }
}
