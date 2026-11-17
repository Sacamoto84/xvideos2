package com.client.xvideos.n.ui.screens.explorer.tab.saved.collections.model

import androidx.compose.runtime.Immutable

/**
 * Состояние диалогов управления коллекцией (меню действий, переименование, удаление).
 */
@Immutable
data class NCollectionDialogData(
    val itemPendingAction: String? = null,
    val itemPendingRename: String? = null,
    val itemPendingDelete: String? = null,
    val renameValue: String = ""
)
