package com.client.xvideos.r.ui.explorer.tab.niches.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.Niche
import com.client.xvideos.r.model.NichesInfo

@Composable
fun NichePreview2(
    niches: () -> Niche,
    savedRed: () -> SavedRed,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val niche = niches()

    val isFollowed by remember(niche.id) {
        derivedStateOf {
            savedRed().niches.list.any { it.id == niche.id }
        }
    }

    val onFollowClick = remember(isFollowed, niche, savedRed) {
        {
            val red = savedRed()
            val nichesInfo = NichesInfo(
                id = niche.id,
                name = niche.name,
                subscribers = niche.subscribers,
                gifs = niche.gifs,
                thumbnail = niche.thumbnail,
            )

            if (isFollowed) red.niches.remove(nichesInfo) else red.niches.add(nichesInfo)
        }
    }

    NichePreview2Content(
        niche = niche,
        isFollowed = isFollowed,
        onFollowClick = onFollowClick,
        onClick = onClick,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun NichePreview2Preview() {
    NichePreview2Content(
        niche = Niche(
            id = "sample-niche",
            name = "Sample Niche",
            gifs = 245,
            subscribers = 914,
            thumbnail = ""
        ),
        isFollowed = false,
        onFollowClick = {},
        onClick = {}
    )
}
