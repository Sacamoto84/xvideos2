package com.client.xvideos.n.ui.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.n.model.NTag
import java.util.Locale

/**
 * Чип тега галереи с отдельным счётчиком.
 */
@Composable
fun NGalleryTagChip(
    tag: NTag,
    onClick: (NTag) -> Unit,
    modifier: Modifier = Modifier
) {
    val count = remember(tag.count) { formatCount(tag.count) }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(if (tag.count > 0) Theme.tabLevel2 else Theme.tabLevel3)
            .clickable(role = Role.Button) { onClick(tag) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tag.name,
            style = MaterialTheme.typography.bodyMedium,
            color = Theme.Text.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .weight(1f, fill = false)
                .background(Theme.tabLevel3)
                .padding(horizontal = 8.dp, vertical = 5.dp)
        )
        if (tag.count > 0) {
            Text(
                text = count,
                style = MaterialTheme.typography.bodyMedium,
                color = Theme.Text.secondary,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
            )
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> compactCount(count / 1_000_000.0, "M")
    count >= 1_000 -> compactCount(count / 1_000.0, "k")
    else -> count.toString()
}

private fun compactCount(value: Double, suffix: String): String =
    String.format(Locale.ROOT, "%.1f", value).removeSuffix(".0") + suffix

@Preview
@Composable
private fun NGalleryTagChipPreview() {
    NGalleryTagChip(
        tag = NTag(id = 1L, type = "artist", name = "shindol", count = 15400),
        onClick = {}
    )
}
