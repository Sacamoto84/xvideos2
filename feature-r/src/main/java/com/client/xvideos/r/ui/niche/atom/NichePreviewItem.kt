package com.client.xvideos.r.ui.niche.atom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.r.model.Niche

/**
 * Элемент списка похожих ниш.
 */
@Composable
fun NichePreviewItem(
    item: Niche,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val handleClick = remember(item.id, onClick) { { onClick(item.id) } }
    val itemProvider = remember(item) { { item } }
    NichePreview(itemProvider, onClick = handleClick, modifier = modifier)
}

@Preview
@Composable
private fun NichePreviewItemPreview() {
    NichePreviewItem(
        item = Niche(
            id = "sample-niche",
            name = "Sample Niche",
            gifs = 100,
            subscribers = 500,
            thumbnail = ""
        ),
        onClick = {}
    )
}
