package com.client.xvideos.screenSettings.backup

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.backup.XlrBackupContentMode
import com.client.xvideos.common.backup.XlrBackupItem
import com.client.xvideos.common.backup.XlrBackupManager
import com.client.xvideos.common.backup.XlrBackupOptions
import com.client.xvideos.common.backup.XlrBackupType
import com.client.xvideos.common.backup.XlrInvalidPasswordException
import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.formatBytes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Состояние и операции бэкапа. Один на процесс и живёт вне композиции — там
 * же, где сами операции: запись и распаковка архива идут в области приложения
 * и переживают экран.
 *
 * Раньше состояние лежало в Composable, и операция его переживала:
 * - пароль бэкапа терялся при пересоздании активности под системным выбором
 *   файла, и архив писался незашифрованным, а консоль сообщала «зашифрован»;
 * - признак идущей операции восстанавливался из сохранённого состояния
 *   поднятым, сбросить его было некому — страница оставалась заперта;
 * - пароль открытого архива стирался после первого же восстановления;
 * - докачка после восстановления шла уже при разблокированном интерфейсе.
 *
 * Все поля читает и меняет главный поток.
 *
 * @param engine Работа с диском; подменяется в тестах.
 * @param scope Область приложения: операцию не обрывает уход со страницы.
 * @param mainDispatcher Поток состояния; подменяется в тестах.
 */
@Stable
@Singleton
class BackupController(
    private val engine: BackupEngine,
    private val scope: CoroutineScope,
    private val mainDispatcher: CoroutineDispatcher,
) {
    @Inject
    constructor(engine: XlrBackupEngine, @ApplicationScope scope: CoroutineScope) : this(engine, scope, Dispatchers.Main)

    /** Идёт запись или чтение архива: страница заперта до конца операции. */
    var isWorking by mutableStateOf(false)
        private set

    /**
     * Идёт докачка медиа после восстановления. Страницу не запирает — докачка
     * долгая, — но новый бэкап и восстановление не начинаются: восстановление
     * заменило бы папки под идущей докачкой, а бэкап прочитал бы их наполовину
     * записанными.
     */
    var isRecovering by mutableStateOf(false)
        private set

    /** Действия с архивом доступны: нет ни операции, ни докачки. */
    val isIdle: Boolean get() = !isWorking && !isRecovering

    val console = mutableStateListOf<String>()

    var lMode by mutableStateOf(XlrBackupContentMode.MINI)
    var rMode by mutableStateOf(XlrBackupContentMode.MINI)
    val options: XlrBackupOptions get() = XlrBackupOptions(lMode = lMode, rMode = rMode)

    var backupItems by mutableStateOf<List<XlrBackupItem>>(emptyList())
        private set
    var selectedBackupPaths by mutableStateOf<Set<String>>(emptySet())
        private set

    /** Выбор папок уже выставлен: первой загрузкой списка или самим пользователем. */
    private var backupSelectionSet = false

    var restoreUri by mutableStateOf<String?>(null)
        private set
    var restoreItems by mutableStateOf<List<XlrBackupItem>>(emptyList())
        private set
    var selectedRestorePaths by mutableStateOf<Set<String>>(emptySet())
        private set

    var showRestorePasswordDialog by mutableStateOf(false)
        private set
    var restorePasswordError by mutableStateOf<String?>(null)
        private set
    private var pendingRestoreUri: String? = null

    /** Растёт с каждым успешным восстановлением: экран по нему пересчитывает статистику файлов. */
    var restoreCount by mutableIntStateOf(0)
        private set

    private var createPassword: CharArray? = null
    private var restorePassword: CharArray? = null

    init {
        refreshBackupItems()
    }

    private fun log(message: String) {
        if (console.size >= CONSOLE_MAX_LINES) {
            console.removeAt(0)
        }
        console.add(message)
    }

    /** Строка из фонового потока — докачка пишет в консоль откуда придётся. */
    private fun logFromAnyThread(message: String) {
        scope.launch(mainDispatcher) { log(message) }
    }

    fun clearConsole() = console.clear()

    // --- Выбор папок для архива ---

    /** Перечитывает размеры папок под текущие режимы L и R. */
    fun refreshBackupItems() {
        scope.launch(mainDispatcher) { loadBackupItems() }
    }

    private suspend fun loadBackupItems() {
        val items = engine.currentItems(options)
        backupItems = items
        // Все папки отмечаются один раз, при первой загрузке. Раньше — всякий
        // раз, когда выбор пуст: «Снять всё» и смена режима возвращали отметки.
        if (!backupSelectionSet) {
            backupSelectionSet = true
            selectedBackupPaths = initialSectionSelection(items)
        }
    }

    fun selectAllBackup() = setBackupSelection(initialSectionSelection(backupItems))

    fun selectNoneBackup() = setBackupSelection(emptySet())

    fun toggleBackupPath(path: String) = setBackupSelection(toggleBackupPath(backupItems, selectedBackupPaths, path))

    private fun setBackupSelection(paths: Set<String>) {
        backupSelectionSet = true
        selectedBackupPaths = paths
    }

    // --- Создание архива ---

    /** Запоминает пароль до выбора файла: между ними экран могут пересоздать. */
    fun setCreatePassword(password: CharArray) {
        wipeCreatePassword()
        createPassword = password
    }

    private fun wipeCreatePassword() {
        createPassword?.fill(WIPED)
        createPassword = null
    }

    /**
     * Пишет архив в файл [uri], выбранный пользователем; `null` — выбор отменён.
     *
     * Без пароля архив не пишется вовсе. Диалог требует пароль всегда, поэтому
     * его отсутствие здесь означает потерю, а не выбор пользователя.
     */
    fun createBackup(uri: String?) {
        val password = createPassword
        createPassword = null
        if (uri == null || !isIdle) {
            password?.fill(WIPED)
            return
        }
        if (password == null) {
            log("Backup не создан: пароль не задан")
            SnackBar.error("Backup не создан: введите пароль ещё раз")
            return
        }
        val paths = selectedBackupPaths
        if (paths.isEmpty()) {
            password.fill(WIPED)
            SnackBar.error(MSG_SELECT_AT_LEAST_ONE_FOLDER)
            return
        }

        isWorking = true
        val options = options
        scope.launch(mainDispatcher) {
            try {
                log("Создание зашифрованного backup: L=${backupContentModeTitle(options.lMode)}, R=${backupContentModeTitle(options.rMode)}")
                engine.create(uri, paths, options, password)
                    .onSuccess { report ->
                        log("Backup создан и зашифрован: ${report.files} файлов, ${formatBytes(report.bytes)}")
                        SnackBar.success("Backup создан: ${report.files} файлов, ${formatBytes(report.bytes)}")
                        loadBackupItems()
                    }
                    .onFailure { error ->
                        log("Ошибка создания backup: ${error.message ?: error::class.java.simpleName}")
                        SnackBar.error(error.message ?: "Ошибка создания backup")
                    }
            } finally {
                password.fill(WIPED)
                isWorking = false
            }
        }
    }

    // --- Открытие архива ---

    /** Открывает архив [uri], выбранный пользователем; `null` — выбор отменён. */
    fun openArchive(uri: String?) {
        if (uri == null || !isIdle) return
        isWorking = true
        scope.launch(mainDispatcher) {
            try {
                when (engine.detectType(uri)) {
                    XlrBackupType.ENCRYPTED_XLR -> {
                        pendingRestoreUri = uri
                        restorePasswordError = null
                        showRestorePasswordDialog = true
                    }
                    XlrBackupType.LEGACY_ZIP -> {
                        log("Обнаружен незашифрованный архив (legacy ZIP)")
                        engine.inspect(uri, password = null)
                            .onSuccess { items ->
                                showArchive(uri, items, password = null)
                                SnackBar.success("Backup открыт: ${items.size} папок")
                            }
                            .onFailure { error ->
                                closeArchive()
                                SnackBar.error(error.message ?: "Ошибка чтения backup")
                            }
                    }
                    XlrBackupType.UNSUPPORTED -> {
                        closeArchive()
                        SnackBar.error("Неподдерживаемый формат файла: не является бэкапом XLR или ZIP")
                    }
                }
            } finally {
                isWorking = false
            }
        }
    }

    /** Расшифровывает архив, выбранный в [openArchive], паролем [password]. */
    fun decryptArchive(password: CharArray) {
        val uri = pendingRestoreUri
        if (uri == null || isWorking) {
            password.fill(WIPED)
            return
        }
        isWorking = true
        scope.launch(mainDispatcher) {
            try {
                engine.inspect(uri, password)
                    .onSuccess { items ->
                        showRestorePasswordDialog = false
                        restorePasswordError = null
                        pendingRestoreUri = null
                        showArchive(uri, items, password)
                        SnackBar.success("Архив успешно расшифрован: ${items.size} папок")
                    }
                    .onFailure { error ->
                        password.fill(WIPED)
                        val wrongPassword = error is XlrInvalidPasswordException || error.cause is XlrInvalidPasswordException
                        val message = if (wrongPassword) {
                            "Неверный пароль для расшифровки бэкапа"
                        } else {
                            error.message ?: "Ошибка расшифровки бэкапа"
                        }
                        restorePasswordError = message
                        SnackBar.error(message)
                    }
            } finally {
                isWorking = false
            }
        }
    }

    fun dismissRestorePassword() {
        showRestorePasswordDialog = false
        pendingRestoreUri = null
        restorePasswordError = null
    }

    private fun showArchive(uri: String, items: List<XlrBackupItem>, password: CharArray?) {
        restorePassword?.fill(WIPED)
        restorePassword = password
        restoreUri = uri
        restoreItems = items
        selectedRestorePaths = initialSectionSelection(items)
    }

    /**
     * Закрывает открытый архив и стирает его пароль. Зовётся при уходе со
     * страницы бэкапа: пароль живёт, пока архив на экране, — раньше он стирался
     * после первого же восстановления, и второе из того же архива падало.
     */
    fun closeArchive() {
        if (isWorking) return
        restorePassword?.fill(WIPED)
        restorePassword = null
        restoreUri = null
        restoreItems = emptyList()
        selectedRestorePaths = emptySet()
        dismissRestorePassword()
        wipeCreatePassword()
    }

    fun selectAllRestore() {
        selectedRestorePaths = initialSectionSelection(restoreItems)
    }

    fun selectNoneRestore() {
        selectedRestorePaths = emptySet()
    }

    fun toggleRestorePath(path: String) {
        selectedRestorePaths = toggleBackupPath(restoreItems, selectedRestorePaths, path)
    }

    // --- Восстановление ---

    /** Заменяет выбранные папки данными открытого архива и запускает докачку. */
    fun restore() {
        val uri = restoreUri
        if (uri == null) {
            SnackBar.error("Сначала выберите архив")
            return
        }
        val paths = selectedRestorePaths
        if (paths.isEmpty()) {
            SnackBar.error(MSG_SELECT_AT_LEAST_ONE_FOLDER)
            return
        }
        if (!isIdle) return

        isWorking = true
        scope.launch(mainDispatcher) {
            var restored = false
            try {
                log("Восстановление backup: ${selectionSummaryText(XlrBackupManager.reportForSelection(restoreItems, paths))}")
                engine.restore(uri, paths, restorePassword)
                    .onSuccess { report ->
                        restored = true
                        loadBackupItems()
                        restoreCount++
                        SnackBar.success("Backup восстановлен: ${report.files} файлов")
                        log("Backup восстановлен: ${report.files} файлов, ${formatBytes(report.bytes)}")
                    }
                    .onFailure { error ->
                        log("Ошибка восстановления backup: ${error.message ?: error::class.java.simpleName}")
                        SnackBar.error(error.message ?: "Ошибка восстановления backup")
                    }
            } finally {
                isRecovering = restored
                isWorking = false
            }
            if (!restored) return@launch
            try {
                engine.afterRestore(paths, ::logFromAnyThread)
            } finally {
                isRecovering = false
            }
        }
    }

    private companion object {
        const val CONSOLE_MAX_LINES = 2000
        const val WIPED = '\u0000'
    }
}

internal const val MSG_SELECT_AT_LEAST_ONE_FOLDER = "Выберите хотя бы одну папку"
