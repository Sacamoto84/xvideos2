package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.molecule

import androidx.compose.runtime.Immutable

@Immutable
data class CollectionDialogData(
    val itemPendingAction: String? = null,
    val itemPendingRename: String? = null,
    val itemPendingDelete: String? = null,
    val renameValue: String = "",
)
