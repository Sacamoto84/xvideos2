package com.client.xvideos.n.ui.screens.gallery.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GalleryMetadataRow(label: String, content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = Theme.Text.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        content()
    }
}

@Preview
@Composable
private fun GalleryMetadataRowPreview() {
    GalleryMetadataRow("Страниц") {
        Text("32", color = Theme.Text.primary)
    }
}
