package com.client.xvideos.common.settings.ui.backup

/** Какой из двух режимов раздела Backup сейчас показан: создание архива или восстановление. */
internal enum class BackupFlowScreen(val title: String) {
    CREATE("Создать"),
    RESTORE("Восстановить")
}
