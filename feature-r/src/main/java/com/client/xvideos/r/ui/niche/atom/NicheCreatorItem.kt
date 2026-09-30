package com.client.xvideos.r.ui.niche.atom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.r.model.TopCreator

/**
 * Элемент списка топовых креаторов ниши.
 */
@Composable
fun NicheCreatorItem(
    creator: TopCreator,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val handleClick = remember(creator.username, onClick) { { onClick(creator.username) } }
    NicheTopCreator(creator, onClick = handleClick, modifier = modifier)
}

@Preview
@Composable
private fun NicheCreatorItemPreview() {
    NicheCreatorItem(
        creator = TopCreator(
            creationtime = 0,
            description = "Top creator",
            followers = 100,
            gifs = 10,
            name = "Creator",
            profileImageUrl = "",
            username = "creator",
            verified = true,
            studio = false,
            views = 1000
        ),
        onClick = {}
    )
}
