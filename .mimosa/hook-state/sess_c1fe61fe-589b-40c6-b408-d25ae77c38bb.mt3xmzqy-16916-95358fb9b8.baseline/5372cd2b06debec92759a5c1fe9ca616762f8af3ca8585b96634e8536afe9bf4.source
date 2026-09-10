package com.client.xvideos.common.backup

/**
 * Модели отчёта и настроек бэкапа.
 *
 * Выделено из `XlrBackupManager.kt`. Сам объект не разбирался: его приватные
 * помощники завязаны на восемь приватных констант и приватный класс, и вынос
 * их наружу расширил бы видимость кода, который пишет и удаляет данные
 * пользователя. Объявления ниже — просто модели, они переносятся как есть.
 */
data class XlrBackupReport(
    val files: Int,
    val bytes: Long
)

data class XlrBackupItem(
    val path: String,
    val title: String,
    val section: String,
    val parentPath: String?,
    val files: Int,
    val bytes: Long
)

enum class XlrBackupContentMode {
    FULL,
    MINI
}

data class XlrBackupOptions(
    val lMode: XlrBackupContentMode = XlrBackupContentMode.MINI,
    val rMode: XlrBackupContentMode = XlrBackupContentMode.MINI
)

