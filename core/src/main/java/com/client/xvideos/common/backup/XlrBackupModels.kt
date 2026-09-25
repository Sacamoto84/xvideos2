package com.client.xvideos.common.backup

import androidx.compose.runtime.Immutable

/**
 * Модели отчёта и настроек бэкапа.
 *
 * Выделено из `XlrBackupManager.kt`. Сам объект не разбирался: его приватные
 * помощники завязаны на восемь приватных констант и приватный класс, и вынос
 * их наружу расширил бы видимость кода, который пишет и удаляет данные
 * пользователя. Объявления ниже — просто модели, они переносятся как есть.
 */
@Immutable
data class XlrBackupReport(
    val files: Int = 0,
    val bytes: Long = 0L
) {
    val isEmpty: Boolean get() = files == 0 && bytes == 0L
    val isNotEmpty: Boolean get() = !isEmpty

    companion object {
        val EMPTY = XlrBackupReport(0, 0L)
    }
}

@Immutable
data class XlrBackupItem(
    val path: String,
    val title: String,
    val section: String,
    val parentPath: String? = null,
    val files: Int = 0,
    val bytes: Long = 0L
) {
    val isEmpty: Boolean get() = files == 0 && bytes == 0L
    val isNotEmpty: Boolean get() = !isEmpty
}

enum class XlrBackupContentMode {
    FULL,
    MINI
}

@Immutable
data class XlrBackupOptions(
    val lMode: XlrBackupContentMode = XlrBackupContentMode.MINI,
    val rMode: XlrBackupContentMode = XlrBackupContentMode.MINI
) {
    val isFullBackup: Boolean get() = lMode == XlrBackupContentMode.FULL && rMode == XlrBackupContentMode.FULL
    val isMiniBackup: Boolean get() = lMode == XlrBackupContentMode.MINI && rMode == XlrBackupContentMode.MINI

    companion object {
        val DEFAULT = XlrBackupOptions()
        val FULL = XlrBackupOptions(XlrBackupContentMode.FULL, XlrBackupContentMode.FULL)
        val MINI = XlrBackupOptions(XlrBackupContentMode.MINI, XlrBackupContentMode.MINI)
    }
}
