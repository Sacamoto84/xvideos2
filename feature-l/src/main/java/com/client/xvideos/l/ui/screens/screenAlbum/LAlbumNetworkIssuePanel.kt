package com.client.xvideos.l.ui.screens.screenAlbum

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.l.net.AlbumPicsDetails
import com.client.xvideos.l.ui.screens.screenAlbum.molecule.LAlbumNetworkIssuePanelContent

@Composable
internal fun LAlbumNetworkIssuePanel(
    albumPicsDetails: AlbumPicsDetails?,
    onRetryFailedPages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (albumPicsDetails == null) return
    val protectionState by albumPicsDetails.protectionUiState.collectAsStateWithLifecycle()
    LAlbumNetworkIssuePanelContent(
        failedPages = albumPicsDetails.failedPages.toList(),
        protectionState = protectionState,
        isRetryingFailedPages = albumPicsDetails.isRetryingFailedPages,
        onRetryFailedPages = onRetryFailedPages,
        modifier = modifier
    )
}
