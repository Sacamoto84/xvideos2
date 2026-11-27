package com.client.xvideos.n.ui.screens.gallery.molecule

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.ui.screens.gallery.atom.NGalleryTagChip

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GalleryTagRow(label: String, tags: List<NTag>, onTagClick: (NTag) -> Unit) {
    if (tags.isEmpty()) return
    GalleryMetadataRow(label) {
        for (tag in tags) {
            NGalleryTagChip(tag = tag, onClick = onTagClick, modifier = Modifier.align(Alignment.CenterVertically))
        }
    }
}

@Preview
@Composable
private fun GalleryTagRowPreview() {
    GalleryTagRow(
        label = "Теги",
        tags = listOf(NTag(id = 1, type = "tag", name = "full color", count = 5300)),
        onTagClick = {},
    )
}
