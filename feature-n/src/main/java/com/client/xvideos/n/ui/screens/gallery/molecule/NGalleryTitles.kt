package com.client.xvideos.n.ui.screens.gallery.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.n.model.NGallery

private val titleNotes = Regex("""\[[^\[\]]*]""")

@Composable
internal fun NGalleryTitles(gallery: NGallery) {
    val title = gallery.englishTitle.ifBlank { gallery.title }.ifBlank { gallery.japaneseTitle }
    val displayTitle = remember(title, gallery.title) {
        buildAnnotatedString {
            append(title)
            if (title != gallery.title) {
                titleNotes.findAll(title).forEach { note ->
                    addStyle(SpanStyle(color = Theme.Text.secondary), note.range.first, note.range.last + 1)
                }
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title.isNotBlank()) {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Theme.Text.primary,
                modifier = Modifier.semantics { heading() }
            )
        }
        if (gallery.japaneseTitle.isNotBlank() && gallery.japaneseTitle != title) {
            Text(
                text = gallery.japaneseTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Theme.Text.secondary
            )
        }
        Text(
            text = "#${gallery.id}",
            style = MaterialTheme.typography.labelLarge,
            color = Theme.Text.secondary
        )
    }
}

@Preview
@Composable
private fun NGalleryTitlesPreview() {
    NGalleryTitles(gallery = NGallery(id = 123456L, title = "Sample Gallery"))
}
