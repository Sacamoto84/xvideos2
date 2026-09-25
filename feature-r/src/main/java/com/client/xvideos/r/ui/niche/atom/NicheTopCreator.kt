package com.client.xvideos.r.ui.niche.atom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.r.model.TopCreator
import com.client.xvideos.ui.theme.XvideosTheme

private val TOP_CREATOR_SHAPE = RoundedCornerShape(8.dp)
private val TOP_CREATOR_SIZE = 96.dp
private val HORIZONTAL_PADDING = 2.dp
private const val INNER_SHADOW_RADIUS = 3f
private const val INNER_SHADOW_SPREAD = 0f

private val FULL_SIZE_MODIFIER = Modifier.fillMaxSize()
private val TOP_CREATOR_BASE_MODIFIER = Modifier
    .padding(horizontal = HORIZONTAL_PADDING)
    .size(TOP_CREATOR_SIZE)
    .clip(TOP_CREATOR_SHAPE)

private val SHADOW_BOX_MODIFIER = Modifier
    .size(TOP_CREATOR_SIZE)
    .innerShadow(
        shape = TOP_CREATOR_SHAPE,
        block = {
            radius = INNER_SHADOW_RADIUS
            spread = INNER_SHADOW_SPREAD
        }
    )

@Composable
fun NicheTopCreator(
    creator: TopCreator,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val boxModifier = if (modifier == Modifier) {
        TOP_CREATOR_BASE_MODIFIER
    } else {
        modifier.then(TOP_CREATOR_BASE_MODIFIER)
    }.clickable(onClick = onClick)

    Box(
        modifier = boxModifier
    ) {
        UrlImage(
            creator.profileImageUrl,
            modifier = FULL_SIZE_MODIFIER
        )

        Box(modifier = SHADOW_BOX_MODIFIER)
    }
}

@Preview
@Composable
private fun NicheTopCreatorPreview() {
    XvideosTheme {
        NicheTopCreator(
            creator = TopCreator(
                creationtime = 0,
                description = "description",
                followers = 1,
                gifs = 1,
                name = "name",
                profileImageUrl = "",
                username = "username",
                verified = false,
                studio = false,
                views = 1
            ),
            onClick = {}
        )
    }
}
