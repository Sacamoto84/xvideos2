package com.client.xvideos.r.ui.ui.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.ui.ui.lazyrow123.molecule.RListLoadError

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

@Preview
@Composable
private fun MetadataLoadingContentPreview() {
    MetadataLoadingContent(errorMessage = "Ошибка загрузки", onRetry = {})
}
