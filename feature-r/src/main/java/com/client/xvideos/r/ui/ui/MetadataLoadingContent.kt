package com.client.xvideos.r.ui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.ui.ui.lazyrow123.molecule.RListLoadError

internal sealed interface MetadataLoadState<out T> {
    data object Loading : MetadataLoadState<Nothing>
    data class Ready<T>(val value: T) : MetadataLoadState<T>
    data class Failed(val message: String) : MetadataLoadState<Nothing>
}

@Composable
internal fun MetadataLoadingContent(errorMessage: String?, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Theme.background),
        contentAlignment = Alignment.Center,
    ) {
        if (errorMessage == null) {
            CircularProgressIndicator()
        } else {
            RListLoadError(message = errorMessage, onRetry = onRetry)
        }
    }
}
