package com.client.xvideos.r.ui.explorer.tab.saved.tab.model

import androidx.compose.runtime.Immutable

@Immutable
data class R_CollectionDialogActions(
    val onDismissAction: () -> Unit,
    val onDismissRename: () -> Unit,
    val onDismissDelete: () -> Unit,
    val onRenameValueChange: (String) -> Unit,
    val onRenameAction: (String) -> Unit,
    val onShareAction: (String) -> Unit,
    val onDeleteAction: (String) -> Unit,
    val onConfirmRename: (String, String) -> Unit,
    val onConfirmDelete: (String) -> Unit,
)
