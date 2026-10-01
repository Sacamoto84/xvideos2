package com.client.xvideos.l.ui.screens.explorer.model

internal enum class LCollectionDialogBackAction {
    DISMISS_NEW_COLLECTION,
    DISMISS_COLLECTION_PICKER,
    NONE
}

internal fun resolveLCollectionDialogBackAction(
    visibleDialogCreateNew: Boolean,
    visibleDialog: Boolean
): LCollectionDialogBackAction = when {
    visibleDialogCreateNew -> LCollectionDialogBackAction.DISMISS_NEW_COLLECTION
    visibleDialog -> LCollectionDialogBackAction.DISMISS_COLLECTION_PICKER
    else -> LCollectionDialogBackAction.NONE
}
