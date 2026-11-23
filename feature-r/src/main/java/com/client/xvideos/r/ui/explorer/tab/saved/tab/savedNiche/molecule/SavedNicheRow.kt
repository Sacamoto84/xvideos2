package com.client.xvideos.r.ui.explorer.tab.saved.tab.savedNiche.molecule

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.r.model.Niche
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.r.ui.explorer.tab.niches.molecule.NichePreview2Content
import com.client.xvideos.r.ui.explorer.tab.saved.tab.atom.SavedItemActionsMenu
import com.client.xvideos.r.ui.niche.atom.NICHE_LEAVE_LABEL

@Composable
fun SavedNicheRow(
    item: NichesInfo,
    onClick: (NichesInfo) -> Unit,
    onDeleteClick: (NichesInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val onRowClick = remember(item, onClick) { { onClick(item) } }
    val onRowDelete = remember(item, onDeleteClick) { { onDeleteClick(item) } }
    val niche = remember(item) {
        Niche(
            id = item.id,
            name = item.name,
            gifs = item.gifs,
            subscribers = item.subscribers,
            thumbnail = item.thumbnail
        )
    }
    NichePreview2Content(
        niche = niche,
        isFollowed = true,
        onFollowClick = onRowDelete,
        onClick = onRowClick,
        modifier = modifier,
        trailingFollowAction = { onLeave ->
            SavedItemActionsMenu(
                actionIcon = Icons.AutoMirrored.Filled.ExitToApp,
                actionLabel = NICHE_LEAVE_LABEL,
                onAction = onLeave,
            )
        }
    )
}

@Preview
@Composable
private fun SavedNicheRowPreview() {
    SavedNicheRow(
        item = NichesInfo(id = "sample-niche", name = "Sample Niche", gifs = 245, subscribers = 914),
        onClick = {},
        onDeleteClick = {}
    )
}
