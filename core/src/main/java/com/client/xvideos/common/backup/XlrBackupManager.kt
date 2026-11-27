package com.client.xvideos.common.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.io.requireInside
import com.client.xvideos.common.util.runCatchingCancellable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Менеджер создания, проверки и восстановления зашифрованных и незашифрованных бэкапов приложения.
 *
 * Архитектурные особенности:
 * - Поддерживает формат шифрования XLRB (AES-256-GCM с потоковыми блоками по 64 КБ) и legacy ZIP.
 * - Охватывает разделы X, L, R, XLive; у L и R — выбор детальности (FULL/MINI).
 * - Транзакционное восстановление данных: перед заменой папок данные откладываются в скрытые
 *   папки `.xlr_old_*`, обеспечивая автоматический откат при сбоях или обрывах питания.
 * - Восстановление в двух режимах [XlrRestoreMode]: заменить папки содержимым архива или
 *   добавить его к текущему.
 */
object XlrBackupManager {
    internal const val L_METADATA_FILE_NAME = XlrBackupPathsConfig.L_METADATA_FILE_NAME
    internal const val R_DOWNLOAD_PATH = XlrBackupPathsConfig.R_DOWNLOAD_PATH

    /** Запись журнала восстановления: путь и был ли он на диске до восстановления. */
    @Serializable
    internal data class RestoreJournalEntry(val path: String, val hadTarget: Boolean)

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
    fun detectBackupType(context: Context, uri: Uri): XlrBackupType =
        XlrBackupArchiveReader.detectBackupType(context, uri)

    /**
     * Сканирует локальную файловую систему и строит дерево разделов и папок с подсчетом размеров.
     *
     * @param options Настройки включения полных медиафайлов или только миниатюр.
     * @param baseDir Корневой каталог данных приложения.
     * @return Список элементов [XlrBackupItem] для отображения в дереве выбора.
     */
    suspend fun currentBackupItems(
        options: XlrBackupOptions = XlrBackupOptions(),
        baseDir: File = File(AppPath.main),
    ): List<XlrBackupItem> = withContext(Dispatchers.IO) {
        XlrBackupArchiveReader.buildCurrentBackupItems(options, baseDir)
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
        password: CharArray? = null,
    ): Result<List<XlrBackupItem>> = withContext(Dispatchers.IO) {
        val inspectContext = currentCoroutineContext()
        runCatchingCancellable {
            XlrBackupArchiveReader.inspectArchiveItems(context, uri, password) {
                inspectContext.ensureActive()
            }
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
     * @param onProgress Записано на сейчас; зовётся по ходу копирования каждого файла, с потока записи.
     * @return [Result] с отчетом о записанных файлах и байтах.
     */
    suspend fun createBackup(
        context: Context,
        uri: Uri,
        selectedPaths: Set<String>,
        options: XlrBackupOptions = XlrBackupOptions(),
        password: CharArray? = null,
        onProgress: (XlrBackupReport) -> Unit = {},
    ): Result<XlrBackupReport> = withContext(Dispatchers.IO) {
        // После открытия файл по [uri] усечён и дописывается: при сбое или отмене в нём мусор.
        var outputOpened = false
        val backupContext = currentCoroutineContext()
        val checkActive = { backupContext.ensureActive() }
        runCatchingCancellable {
            try {
                val safePaths = normalizeSelectedPaths(selectedPaths)
                if (safePaths.isEmpty()) error("Select at least one folder")

                val progress = XlrBackupProgress(onProgress)
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
                        XlrBackupArchiveWriter.writeManifest(zip, safePaths, options)
                        safePaths.forEach { path ->
                            checkActive()
                            val source = File(AppPath.main, path)
                            if (source.exists()) {
                                XlrBackupArchiveWriter.writePath(zip, source, path, options, progress, checkActive)
                            } else if (shouldIncludeBackupEntry(path, options)) {
                                zip.putNextEntry(ZipEntry("$path/"))
                                zip.closeEntry()
                            }
                        }
                    }
                }

                progress.report
            } catch (cancel: CancellationException) {
                if (outputOpened) {
                    Timber.i("XlrBackupManager: создание бэкапа отменено, удаляем недописанный файл")
                    deletePartialBackup(context, uri)
                }
                throw cancel
            }
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
    ): Result<XlrRestoreResult> = withContext(Dispatchers.IO) {
        val restoreContext = currentCoroutineContext()
        runCatchingCancellable {
            restoreArchive(File(AppPath.main), selectedPaths, mode, { restoreContext.ensureActive() }) {
                XlrBackupArchiveReader.openBackupInputStream(context, uri, password)
            }
        }
    }

    /**
     * Читает поток до конца, проверяет центральный каталог ZIP и распаковывает
     * выбранные данные. До этого библиотека не меняется. Один снимок архива
     * используется и для проверки, и для распаковки.
     */
    @Synchronized
    internal fun restoreArchive(
        mainRoot: File,
        selectedPaths: Set<String>,
        mode: XlrRestoreMode,
        checkActive: () -> Unit = {},
        openArchive: () -> InputStream,
    ): XlrRestoreResult {
        val safePaths = normalizeSelectedPaths(selectedPaths)
        if (safePaths.isEmpty()) error("Select at least one folder")
        // Прошлое восстановление могло оборваться: пока его след не убран,
        // новое удалило бы отодвинутые копии — последние прежние данные.
        check(recoverInterruptedRestore(mainRoot)) {
            "Не удалось завершить откат прошлого восстановления"
        }
        val tempRoot = File(mainRoot, XlrBackupRestoreJournal.RESTORE_TEMP_DIR_NAME).apply {
            if (exists()) deleteRecursively()
            check(isDirectory || mkdirs()) { "Не удалось создать временную папку восстановления" }
        }
        try {
            val archive = File(tempRoot, "backup.zip")
            openArchive().use { input ->
                archive.outputStream().use { output -> input.copyArchiveTo(output, checkActive) }
            }
            val unpacked = File(tempRoot, "files")
            val (paths, report) = ZipFile(archive).use { zip ->
                val sourcePaths = XlrRestorePaths.expandSections(
                    safePaths,
                    XlrBackupArchiveReader.validateBackup(zip, checkActive)
                )
                check(sourcePaths.isNotEmpty()) { "В архиве нет данных для выбранных папок" }
                val report = XlrBackupArchiveReader.extractBackup(zip, unpacked, sourcePaths, checkActive)
                val paths = normalizeSelectedPaths(XLiveLegacyFolder.restoreTargets(sourcePaths).toSet())
                    .filter { File(unpacked, it).exists() }
                check(paths.isNotEmpty()) { "В архиве нет данных для выбранных папок" }
                paths to report
            }
            check(archive.delete()) { "Не удалось удалить временную копию архива" }
            checkActive()
            applyRestoredPaths(mainRoot, unpacked, paths, mode)
            return XlrRestoreResult(report, paths.toSet())
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    /**
     * Переносит распакованные папки поверх текущих данных.
     *
     * Откат возвращает прежнее состояние при ошибке на любом шаге;
     * журнал [XlrBackupRestoreJournal] фиксирует границу коммита для восстановления после краша.
     */
    internal fun applyRestoredPaths(
        mainRoot: File,
        tempRoot: File,
        paths: List<String>,
        mode: XlrRestoreMode = XlrRestoreMode.REPLACE,
    ) {
        val root = mainRoot.canonicalFile
        val movedAside = mutableListOf<Pair<File, File>>()
        val written = mutableListOf<File>()
        val journal = mutableListOf<RestoreJournalEntry>()

        try {
            paths.forEach { path ->
                val target = File(mainRoot, path)
                requireInside(root, target.canonicalFile)
                val restored = File(tempRoot, path)
                if (!restored.exists()) return@forEach

                // Запись в журнал — до первого изменения на диске.
                journal += RestoreJournalEntry(path = path, hadTarget = target.exists())
                writeRestoreJournal(mainRoot, committed = false, entries = journal, mode = mode)

                if (target.exists()) {
                    val aside = XlrBackupRestoreJournal.asideFor(target)
                    aside.deleteRecursively()
                    if (!target.renameTo(aside)) {
                        error("Cannot move aside before restore: ${target.absolutePath}")
                    }
                    movedAside += target to aside
                }

                target.parentFile?.mkdirs()
                written += target
                if (!restored.renameTo(target)) {
                    restored.copyRecursively(target, overwrite = true)
                }
            }
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
            if (rolledBack) XlrBackupRestoreJournal.restoreJournalFile(mainRoot).delete()
            throw e
        }

        val finished = movedAside.map { (target, aside) -> XlrRestoreCleanup.finishPath(root, target, aside, mode) }
        if (finished.all { it }) XlrBackupRestoreJournal.restoreJournalFile(mainRoot).delete()
    }

    internal fun writeRestoreJournal(
        mainRoot: File,
        committed: Boolean,
        entries: List<RestoreJournalEntry>,
        mode: XlrRestoreMode = XlrRestoreMode.REPLACE,
    ) = XlrBackupRestoreJournal.writeRestoreJournal(mainRoot, committed, entries, mode)

    /** `true`, если на диске оставлен журнал незавершённого восстановления. */
    fun hasRestoreJournal(mainRoot: File): Boolean = XlrBackupRestoreJournal.hasRestoreJournal(mainRoot)

    /**
     * Быстрый синхронный откат/завершение журнала восстановления до `AppPath.init`
     * без тяжёлой рекурсивной очистки `.xlr_restore_tmp`.
     */
    fun recoverInterruptedRestoreBeforeStartup(mainRoot: File): Boolean =
        XlrBackupRestoreJournal.recoverInterruptedRestoreBeforeStartup(mainRoot)

    /** Убирает след восстановления, оборванного гибелью процесса. Вызывать при запуске приложения. */
    fun recoverInterruptedRestore(): Boolean = recoverInterruptedRestore(File(AppPath.main))

    @Synchronized
    internal fun recoverInterruptedRestore(mainRoot: File, cleanTemp: Boolean = true): Boolean =
        XlrBackupRestoreJournal.recoverInterruptedRestore(mainRoot, cleanTemp)

    internal fun shouldIncludeBackupEntry(entryName: String, options: XlrBackupOptions): Boolean =
        XlrBackupPathsConfig.shouldIncludeBackupEntry(entryName, options)

    internal fun String.isInsideBackupPath(path: String): Boolean =
        with(XlrBackupPathsConfig) { this@isInsideBackupPath.isInsideBackupPath(path) }

    internal fun normalizeSelectedPaths(paths: Set<String>): List<String> =
        XlrBackupPathsConfig.normalizeSelectedPaths(paths)
}
