package com.client.xvideos.n.ui.screens.explorer.tab.search.model

import com.client.xvideos.n.model.NTag

/**
 * Набор действий экрана поиска манги.
 */
data class NSearchActions(
    val onQueryChange: (String) -> Unit,
    val onSearch: () -> Unit,
    val onTagSuggestionClick: (NTag) -> Unit,
    val onGalleryClick: (Long) -> Unit,
    val onPageChange: (Int) -> Unit,
    val onRetry: () -> Unit
)
