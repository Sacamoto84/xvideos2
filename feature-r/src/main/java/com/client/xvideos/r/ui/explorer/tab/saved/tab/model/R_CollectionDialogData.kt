package com.client.xvideos.r.ui.explorer.tab.saved.tab.model

import androidx.compose.runtime.Immutable

@Immutable
data class R_CollectionDialogData(
    val itemPendingAction: String?,
    val itemPendingRename: String?,
    val itemPendingDelete: String?,
    val renameValue: String,
)
