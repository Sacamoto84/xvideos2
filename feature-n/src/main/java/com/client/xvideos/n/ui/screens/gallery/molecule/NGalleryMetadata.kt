package com.client.xvideos.n.ui.screens.gallery.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NTag

private val tagLabels = mapOf(
    "parody" to "Пародии",
    "character" to "Персонажи",
    "tag" to "Теги",
    "artist" to "Художники",
    "group" to "Группы",
    "language" to "Языки",
    "category" to "Категории"
)

@Composable
internal fun NGalleryMetadata(gallery: NGallery, onTagClick: (NTag) -> Unit) {
    val tagsByType = remember(gallery.tags) { gallery.tags.groupBy { it.type.lowercase() } }
    val otherTags = remember(tagsByType) { tagsByType.filterKeys { it !in tagLabels }.values.flatten() }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        for ((type, label) in tagLabels) {
            GalleryTagRow(label, tagsByType[type].orEmpty(), onTagClick)
        }
        GalleryTagRow("Другие теги", otherTags, onTagClick)
        GalleryMetadataRow("Страниц") {
            Text(
                text = gallery.numPages.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = Theme.Text.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .background(Theme.tabLevel3, RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
        GalleryUploadDate(gallery.uploadDate)
    }
}

@Preview(widthDp = 360)
@Composable
private fun NGalleryMetadataPreview() {
    NGalleryMetadata(
        gallery = NGallery(
            numPages = 32,
            uploadDate = 1_579_132_800L,
            tags = listOf(
                NTag(id = 1, type = "tag", name = "full color", count = 5300),
                NTag(id = 2, type = "artist", name = "Aki Sato", count = 120),
            )
        ),
        onTagClick = {}
    )
}
