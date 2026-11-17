package com.client.xvideos.n.ui.screens.explorer.tab.saved.collections.model

/**
 * Набор действий управления коллекциями манги.
 */
data class NCollectionActions(
    val onCollectionClick: (String) -> Unit,
    val onCollectionLongClick: (String) -> Unit,
    val onRenameCollection: (String) -> Unit,
    val onDeleteCollection: (String) -> Unit,
    val onCreateClick: () -> Unit,
    val onBackFromCollection: () -> Unit
)
