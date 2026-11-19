package com.client.xvideos.n.ui.screens.explorer.tab.list

import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.repository.CloudflareBlockedException

/**
 * Состояние экрана списка или ленты манги.
 */
sealed interface NGalleryListUiState {
    data object Loading : NGalleryListUiState
    data class Success(val items: List<NGalleryPreview>) : NGalleryListUiState
    data class Error(val message: String) : NGalleryListUiState
    data class CloudflareRequired(val challenge: CloudflareBlockedException) : NGalleryListUiState
}
