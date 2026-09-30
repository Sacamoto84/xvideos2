package com.client.xvideos.r.ui.root

internal enum class RedRootDialogBackAction {
    DISMISS_NEW_COLLECTION,
    DISMISS_COLLECTION_PICKER,
    DISMISS_BLOCK,
    NONE
}

internal fun resolveRedRootDialogBackAction(
    visibleDialogCreateNew: Boolean,
    visibleDialog: Boolean,
    blockVisibleDialog: Boolean
): RedRootDialogBackAction = when {
    visibleDialogCreateNew -> RedRootDialogBackAction.DISMISS_NEW_COLLECTION
    visibleDialog -> RedRootDialogBackAction.DISMISS_COLLECTION_PICKER
    blockVisibleDialog -> RedRootDialogBackAction.DISMISS_BLOCK
    else -> RedRootDialogBackAction.NONE
}
