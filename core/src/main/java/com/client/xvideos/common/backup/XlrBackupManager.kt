package com.client.xvideos.common.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.io.normalizeRelativePath
import com.client.xvideos.common.io.requireInside
import com.client.xvideos.common.io.writeTextAtomically
import com.client.xvideos.common.json.AppJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Менеджер создания, проверки и восстановления зашифрованных и незашифрованных бэкапов приложения.
 *
 * Архитектурные особенности:
 * - Поддерживает формат шифрования XLRB (AES-256-GCM с потоковыми блоками по 64 КБ) и legacy ZIP.
 * - Охватывает разделы X, L, R с возможностью выбора детальности (FULL/MINI).
 * - Транзакционное восстановление данных: перед заменой папок данные откладываются в скрытые
 *   папки `.xlr_old_*`, обеспечивая автоматический откат при сбоях или обрывах питания.
 * - Восстановление в двух режимах [XlrRestoreMode]: заменить папки содержимым архива или
 *   добавить его к текущему.
 */
object XlrBackupManager {
    private const val SCHEMA_VERSION = 1
    private const val MANIFEST_ENTRY = "backup.json"
    private const val L_LIKES_PATH = "L/Likes"
    private const val L_COLLECTION_PATH = "L/Collection"
    internal const val L_METADATA_FILE_NAME = "metadata.json"
    private const val L_COLLECTION_CONFIG_FILE_NAME = "collection.json"
    internal const val R_DOWNLOAD_PATH = "R/Download"

    /**
     * Префикс временной копии прежних данных на время восстановления.
     * Точка в начале — чтобы копия не выглядела как обычная папка данных,
     * если процесс убьют посреди переноса.
     */
    private const val RESTORE_ASIDE_PREFIX = ".xlr_old_"

    /** Журнал восстановления в корне данных; имя с точкой — в бэкап не попадает. */
    private const val RESTORE_JOURNAL_NAME = ".xlr_restore_journal"
    private const val RESTORE_TEMP_DIR_NAME = ".xlr_restore_tmp"

    private val sections = listOf("X", "L", "R")

    /**
     * Генерирует стандартное имя файла бэкапа с временной меткой: "xvideos-xlr-backup-YYYYMMDD-HHmmss.xlr".
     *
     * @param now Unix timestamp текущего времени.
     */
    fun defaultFileName(now: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        return "xvideos-xlr-backup-${sdf.format(Date(now))}.xlr"
    }

    /**
     * Быстро определяет тип файла бэкапа по первым байтам через [ContentResolver].
     *
     * @param context Контекст Android.
     * @param uri URI выбранного пользователем файла архива.
     * @return [XlrBackupType] (зашифрованный, zip или неподдерживаемый).
     */
    fun detectBackupType(context: Context, uri: Uri): XlrBackupType {
        return runCatching {
            val input = context.contentResolver.openInputStream(uri) ?: return XlrBackupType.UNSUPPORTED
            input.use { stream ->
                val header = ByteArray(4)
                val read = stream.read(header)
                if (read < 4) XlrBackupType.UNSUPPORTED else XlrChunkedCrypto.detectType(header)
            }
        }.getOrDefault(XlrBackupType.UNSUPPORTED)
    }

    /**
     * Сканирует локальную файловую систему и строит дерево разделов и папок с подсчетом размеров.
     *
     * @param options Настройки включения полных медиафайлов или только миниатюр.
     * @param baseDir Корневой каталог данных приложения.
     * @return Список элементов [XlrBackupItem] для отображения в дереве выбора.
     */
    suspend fun currentBackupItems(
        options: XlrBackupOptions = XlrBackupOptions(),
        baseDir: File = File(AppPath.main)
    ): List<XlrBackupItem> = withContext(Dispatchers.IO) {
        sections.flatMap { section ->
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

    /**
     * Анализирует архив бэкапа без полной распаковки на диск и строит дерево содержимого.
     *
     * @param context Контекст Android.
     * @param uri URI выбранного архива.
     * @param password Пароль для расшифровки (если архив зашифрован).
     * @return [Result] со списком элементов бэкапа [XlrBackupItem] или ошибкой (неверный пароль/повреждение).
     */
    suspend fun inspectBackup(
        context: Context,
        uri: Uri,
        password: CharArray? = null
    ): Result<List<XlrBackupItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val stats = linkedMapOf<String, MutableReport>()
            openZipInputStream(context, uri, password).use { zip ->
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

    /**
     * Рассчитывает суммарное количество файлов и байтов для подмножества выбранных путей.
     *
     * @param items Полный список элементов.
     * @param selectedPaths Набор относительных путей, отмеченных пользователем.
     */
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

    /**
     * Создает новый архив бэкапа с опциональным потоковым шифрованием XLRB.
     *
     * @param context Контекст Android.
     * @param uri URI целевого файла для сохранения.
     * @param selectedPaths Набор путей, подлежащих архивации.
     * @param options Опции фильтрации медиа (FULL/MINI).
     * @param password Пароль для шифрования (null или пустой — архив сохраняется как обычный ZIP).
     * @return [Result] с отчетом о записанных файлах и байтах.
     */
    suspend fun createBackup(
        context: Context,
        uri: Uri,
        selectedPaths: Set<String>,
        options: XlrBackupOptions = XlrBackupOptions(),
        password: CharArray? = null
    ): Result<XlrBackupReport> = withContext(Dispatchers.IO) {
        // После открытия файл по [uri] усечён и дописывается: при сбое в нём мусор.
        var outputOpened = false
        runCatching {
            val safePaths = normalizeSelectedPaths(selectedPaths)
            if (safePaths.isEmpty()) error("Select at least one folder")

            var files = 0
            var bytes = 0L
            val rawOutput = context.contentResolver.openOutputStream(uri, "wt")
                ?: error("Cannot open backup file")
            outputOpened = true

            rawOutput.use { raw ->
                val outputStream: OutputStream = if (password != null && password.isNotEmpty()) {
                    XlrEncryptedOutputStream(BufferedOutputStream(raw), password)
                } else {
                    BufferedOutputStream(raw)
                }

                ZipOutputStream(outputStream).use { zip ->
                    writeManifest(zip, safePaths, options)
                    safePaths.forEach { path ->
                        val source = File(AppPath.main, path)
                        if (source.exists()) {
                            val report = writePath(zip, source, path, options)
                            files += report.files
                            bytes += report.bytes
                        } else if (shouldIncludeBackupEntry(path, options)) {
                            zip.putNextEntry(ZipEntry("$path/"))
                            zip.closeEntry()
                        }
                    }
                }
            }

            XlrBackupReport(files = files, bytes = bytes)
        }.onFailure { error ->
            if (outputOpened) {
                Timber.e(error, "XlrBackupManager: бэкап не создан, удаляем недописанный файл")
                deletePartialBackup(context, uri)
            }
        }
    }

    /**
     * Удаляет файл бэкапа, запись которого оборвалась.
     *
     * Раньше он оставался на месте: правильное имя, обрезанное содержимое —
     * позже его легко принять за рабочий бэкап. Удалить получается не у
     * всякого провайдера документов; тогда файл остаётся, это только в лог.
     */
    private fun deletePartialBackup(context: Context, uri: Uri) {
        runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
            .onFailure { Timber.w(it, "XlrBackupManager: не удалось удалить недописанный файл бэкапа") }
    }

    /**
     * Восстанавливает данные из архива бэкапа с транзакционной гарантией безопасности.
     *
     * Распаковка выполняется сначала в изолированную временную папку `.xlr_restore_tmp`,
     * после чего текущие рабочие папки отодвигаются в `.xlr_old_*`, а новые накатываются на их место.
     * В случае сбоя выполняется автоматический откат к исходному состоянию.
     *
     * @param context Контекст Android.
     * @param uri URI файла бэкапа.
     * @param selectedPaths Список папок для восстановления.
     * @param password Пароль для расшифровки архива.
     * @param mode Заменить папки содержимым архива или добавить его к текущему.
     */
    suspend fun restoreBackup(
        context: Context,
        uri: Uri,
        selectedPaths: Set<String>,
        password: CharArray? = null,
        mode: XlrRestoreMode = XlrRestoreMode.REPLACE,
    ): Result<XlrBackupReport> = withContext(Dispatchers.IO) {
        runCatching {
            restoreArchive(File(AppPath.main), selectedPaths, mode) { openZipInputStream(context, uri, password) }
        }
    }

    /**
     * Восстановление из архива, который открывает [openZip]: проверка,
     * распаковка во временную папку, перенос. Отделено от [restoreBackup],
     * чтобы проверяться без Android — на обычном ZIP-файле.
     *
     * @param openZip Открывает архив заново: он читается дважды, при проверке и при распаковке.
     */
    internal fun restoreArchive(
        mainRoot: File,
        selectedPaths: Set<String>,
        mode: XlrRestoreMode,
        openZip: () -> ZipInputStream,
    ): XlrBackupReport {
        val safePaths = normalizeSelectedPaths(selectedPaths)
        if (safePaths.isEmpty()) error("Select at least one folder")
        // Прошлое восстановление могло оборваться: пока его след не убран,
        // новое удалило бы отодвинутые копии — последние прежние данные.
        check(recoverInterruptedRestore(mainRoot)) {
            "Не удалось завершить откат прошлого восстановления"
        }
        // Раздел восстанавливается папками, которые есть в архиве: остальные
        // его папки не трогаются — см. [XlrRestorePaths].
        val restorePaths = XlrRestorePaths.expandSections(safePaths, validateBackup(openZip))
        if (restorePaths.isEmpty()) error("В архиве нет данных для выбранных папок")
        val tempRoot = File(mainRoot, RESTORE_TEMP_DIR_NAME).apply {
            deleteRecursively()
            mkdirs()
        }
        try {
            val report = extractBackup(openZip, tempRoot, restorePaths)
            applyRestoredPaths(mainRoot, tempRoot, restorePaths, mode)
            return report
        } finally {
            tempRoot.deleteRecursively()
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
     *
     * Откат в `catch` не спасает, если процесс убит посреди переноса. На этот
     * случай ведётся журнал: какие пути тронуты и перенесено ли уже всё. По
     * нему [recoverInterruptedRestore] при следующем запуске либо возвращает
     * прежние данные, либо доводит уборку до конца.
     */
    internal fun applyRestoredPaths(
        mainRoot: File,
        tempRoot: File,
        paths: List<String>,
        mode: XlrRestoreMode = XlrRestoreMode.REPLACE,
    ) {
        val root = mainRoot.canonicalFile
        // target -> отодвинутая копия прежнего содержимого
        val movedAside = mutableListOf<Pair<File, File>>()
        val written = mutableListOf<File>()
        val journal = mutableListOf<RestoreJournalEntry>()

        try {
            paths.forEach { path ->
                val target = File(mainRoot, path)
                requireInside(root, target.canonicalFile)
                // Пути нет в распакованном — архив о нём ничего не знает. Раньше
                // такая папка становилась пустой: прежние данные пропадали.
                val restored = File(tempRoot, path)
                if (!restored.exists()) return@forEach

                // Запись в журнал — до первого изменения на диске.
                journal += RestoreJournalEntry(path = path, hadTarget = target.exists())
                writeRestoreJournal(mainRoot, committed = false, entries = journal, mode = mode)

                if (target.exists()) {
                    val aside = asideFor(target)
                    aside.deleteRecursively()
                    if (!target.renameTo(aside)) {
                        error("Cannot move aside before restore: ${target.absolutePath}")
                    }
                    movedAside += target to aside
                }

                target.parentFile?.mkdirs()
                // В список очистки — до записи, а не после: падение внутри
                // copyRecursively оставляло полузаписанную папку, которой нет
                // ни в written, ни в movedAside, и откат её не трогал.
                written += target
                if (!restored.renameTo(target)) {
                    restored.copyRecursively(target, overwrite = true)
                }
            }
            // Все пути на месте: дальше только уборка отодвинутых копий.
            writeRestoreJournal(mainRoot, committed = true, entries = journal, mode = mode)
        } catch (e: Throwable) {
            Timber.e(e, "XlrBackupManager restore failed, rolling back")
            written.forEach { it.deleteRecursively() }
            var rolledBack = true
            movedAside.forEach { (target, aside) ->
                target.deleteRecursively()
                if (!aside.renameTo(target)) {
                    rolledBack = false
                    Timber.e("Rollback incomplete, previous data left at ${aside.absolutePath}")
                }
            }
            // Если откат не удался, журнал остаётся: следующий запуск повторит.
            if (rolledBack) restoreJournalFile(mainRoot).delete()
            throw e
        }

        // Журнал уже говорит «всё перенесено»: если процесс умрёт здесь, уборку
        // доведёт recoverInterruptedRestore. Поэтому скачанное забираем именно
        // после этой точки — до неё откат обязан вернуть прежнюю копию целой.
        val finished = movedAside.map { (target, aside) -> XlrRestoreCleanup.finishPath(root, target, aside, mode) }
        if (finished.all { it }) restoreJournalFile(mainRoot).delete()
    }

    /** Запись журнала восстановления: путь и был ли он на диске до восстановления. */
    @Serializable
    internal data class RestoreJournalEntry(val path: String, val hadTarget: Boolean)

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
        val entries: List<RestoreJournalEntry>,
        val mode: XlrRestoreMode = XlrRestoreMode.REPLACE,
    )

    private fun restoreJournalFile(mainRoot: File) = File(mainRoot, RESTORE_JOURNAL_NAME)

    private fun asideFor(target: File) = File(target.parentFile, "$RESTORE_ASIDE_PREFIX${target.name}")

    internal fun writeRestoreJournal(
        mainRoot: File,
        committed: Boolean,
        entries: List<RestoreJournalEntry>,
        mode: XlrRestoreMode = XlrRestoreMode.REPLACE,
    ) {
        val journal = RestoreJournal(committed = committed, entries = entries.toList(), mode = mode)
        restoreJournalFile(mainRoot).writeTextAtomically(AppJson.encodeToString(RestoreJournal.serializer(), journal))
    }

    /** Убирает след восстановления, оборванного гибелью процесса. Вызывать при запуске приложения. */
    fun recoverInterruptedRestore(): Boolean = recoverInterruptedRestore(File(AppPath.main))

    /**
     * Убирает след восстановления, которое оборвала гибель процесса.
     *
     * Без этого прежние данные оставались в отодвинутой копии `.xlr_old_*`:
     * раздела на месте нет или он недописан, а следующее восстановление
     * удаляло копию как мусор. По журналу: если всё было перенесено — копии
     * разбираются по режиму восстановления и удаляются; если нет — тронутые
     * пути возвращаются в прежнее состояние.
     *
     * @return `true`, если незавершённого восстановления не осталось.
     */
    internal fun recoverInterruptedRestore(mainRoot: File): Boolean {
        val journalFile = restoreJournalFile(mainRoot)
        if (!journalFile.exists()) return true
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
            journalFile.delete()
            File(mainRoot, RESTORE_TEMP_DIR_NAME).deleteRecursively()
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

    @Serializable
    private data class XlrManifestData(
        val schemaVersion: Int,
        val createdAt: String,
        val sections: List<String>,
        val paths: List<String>,
        val modes: Map<String, String>,
    )

    private fun writeManifest(zip: ZipOutputStream, selectedPaths: List<String>, options: XlrBackupOptions) {
        val manifest = XlrManifestData(
            schemaVersion = SCHEMA_VERSION,
            createdAt = utcNowText(),
            sections = sections,
            paths = selectedPaths,
            modes = mapOf(
                "L" to options.lMode.name,
                "R" to options.rMode.name,
            ),
        )
        zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
        zip.write(AppJson.encodeToString(manifest).toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun writePath(
        zip: ZipOutputStream,
        source: File,
        entryRoot: String,
        options: XlrBackupOptions
    ): XlrBackupReport {
        if (!shouldIncludeBackupEntry(entryRoot, options)) {
            return XlrBackupReport(files = 0, bytes = 0L)
        }
        if (source.isFile) {
            return writeFile(zip, source, entryRoot)
        }
        return writeDirectory(zip, source, entryRoot, options)
    }

    private fun writeDirectory(
        zip: ZipOutputStream,
        source: File,
        entryRoot: String,
        options: XlrBackupOptions
    ): XlrBackupReport {
        if (!shouldIncludeBackupEntry(entryRoot, options)) {
            return XlrBackupReport(files = 0, bytes = 0L)
        }
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
            if (!shouldIncludeBackupEntry(entryName, options)) return@forEach
            if (file.isDirectory) {
                zip.putNextEntry(ZipEntry("$entryName/"))
                zip.closeEntry()
                return@forEach
            }

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

    /**
     * Проверяет архив и возвращает его пути второго уровня (`X/Favorites`):
     * по ним восстановление узнаёт, какие папки раздела в архиве есть.
     * Записи со скрытыми сегментами в список не входят — распаковка их пропускает.
     */
    private fun validateBackup(openZip: () -> ZipInputStream): Set<String> {
        val folders = sortedSetOf<String>()
        openZip().use { zip ->
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
                if (name.split('/').none { it.startsWith(".") }) {
                    childPathOrNull(name)?.let(folders::add)
                }
                zip.closeEntry()
            }
            if (!hasDataEntry) error("Backup does not contain X/L/R data")
        }
        return folders
    }

    private fun extractBackup(
        openZip: () -> ZipInputStream,
        destinationRoot: File,
        selectedPaths: List<String>,
    ): XlrBackupReport {
        var files = 0
        var bytes = 0L
        val root = destinationRoot.canonicalFile

        openZip().use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = normalizeRelativePath(entry.name)
                if (name == MANIFEST_ENTRY) {
                    zip.closeEntry()
                    continue
                }
                if (name.split('/').any { it.startsWith(".") }) {
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

    private fun openZipInputStream(context: Context, uri: Uri, password: CharArray?): ZipInputStream {
        val rawInput = context.contentResolver.openInputStream(uri)
            ?: error("Cannot open backup file")
        try {
            val buffered = BufferedInputStream(rawInput)
            buffered.mark(16)
            val header = ByteArray(4)
            val read = buffered.read(header)
            buffered.reset()
            if (read < 4) error("Backup file is empty or corrupted")

            val type = XlrChunkedCrypto.detectType(header)
            val decodedStream: InputStream = when (type) {
                XlrBackupType.ENCRYPTED_XLR -> {
                    if (password == null || password.isEmpty()) {
                        throw XlrInvalidPasswordException("Архив зашифрован. Требуется ввод пароля.")
                    }
                    XlrEncryptedInputStream(buffered, password)
                }
                XlrBackupType.LEGACY_ZIP -> {
                    buffered
                }
                XlrBackupType.UNSUPPORTED -> {
                    throw XlrCorruptedBackupException("Неподдерживаемый формат файла бэкапа")
                }
            }
            return ZipInputStream(BufferedInputStream(decodedStream))
        } catch (e: Throwable) {
            runCatching { rawInput.close() }
            throw e
        }
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

    internal fun shouldIncludeBackupEntry(entryName: String, options: XlrBackupOptions): Boolean {
        val normalized = entryName.replace('\\', '/').trim('/')
        if (normalized.split('/').any { it.startsWith(".") }) {
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

    internal fun String.isInsideBackupPath(path: String): Boolean {
        return this == path || startsWith("$path/")
    }

    internal fun normalizeSelectedPaths(paths: Set<String>): List<String> {
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
