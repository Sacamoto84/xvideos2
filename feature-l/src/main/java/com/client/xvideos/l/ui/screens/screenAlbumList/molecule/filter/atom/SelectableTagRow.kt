package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.ui.screens.screenAlbumList.filter.style.StyleGenresTags

@Composable
fun SelectableTagRow(
    item: String,
    count: Int?,
    onAddPlus: () -> Unit,
    onAddMinus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = StyleGenresTags.Palette
    val chipShape = RoundedCornerShape(6.dp)
    val titleStyle = remember(palette.textPrimary) {
        Theme.L.Type.rowTitle.copy(
            color = palette.textPrimary,
            fontWeight = FontWeight.Bold
        )
    }
    val countStyle = remember(palette.textSecondary) {
        Theme.L.Type.rowTitle.copy(
            color = palette.textSecondary,
            fontWeight = FontWeight.Bold
        )
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Include $item",
                tint = palette.selectedBorder,
                modifier = Modifier
                    .padding(vertical = 2.dp, horizontal = 4.dp)
                    .size(36.dp)
                    .clip(chipShape)
                    .border(1.dp, palette.selectedBorder, chipShape)
                    .background(palette.field)
                    .clickable { onAddPlus() }
                    .padding(6.dp)
            )

            Spacer(Modifier.width(4.dp))

            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Exclude $item",
                tint = palette.excludedBorder,
                modifier = Modifier
                    .padding(vertical = 2.dp, horizontal = 4.dp)
                    .size(36.dp)
                    .clip(chipShape)
                    .border(1.dp, palette.excludedBorder, chipShape)
                    .background(palette.field)
                    .clickable { onAddMinus() }
                    .padding(6.dp)
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = item,
                color = palette.textPrimary,
                style = titleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (count != null && count >= 0) {
            Text(
                text = count.toString(),
                color = palette.textSecondary,
                style = countStyle,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}

@Preview
@Composable
private fun SelectableTagRowPreview() {
    SelectableTagRow(
        item = "Cosplay",
        count = 450,
        onAddPlus = {},
        onAddMinus = {}
    )
}
