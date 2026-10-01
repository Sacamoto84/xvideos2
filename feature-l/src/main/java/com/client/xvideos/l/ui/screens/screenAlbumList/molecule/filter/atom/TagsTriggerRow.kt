package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.style.StyleGenresTags

@Composable
fun TagsTriggerRow(
    selectorText: String,
    totalSelected: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = StyleGenresTags.Palette
    val titleStyle = remember(Theme.L.Type.rowTitle, palette.textPrimary) {
        Theme.L.Type.rowTitle.copy(
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary
        )
    }
    val normalValueStyle = remember(Theme.L.Type.rowTitle, palette.textPrimary) {
        Theme.L.Type.rowTitle.copy(
            color = palette.textPrimary,
            fontWeight = FontWeight.Bold
        )
    }
    val selectedValueStyle = remember(Theme.L.Type.rowTitle, palette.selectedText) {
        Theme.L.Type.rowTitle.copy(
            color = palette.selectedText,
            fontWeight = FontWeight.Bold
        )
    }
    val dropdownBoxModifier = remember(palette.border, palette.field) {
        Modifier
            .widthIn(min = 160.dp, max = 220.dp)
            .height(43.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, palette.border, RoundedCornerShape(6.dp))
            .background(palette.field)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Tags",
            style = titleStyle
        )

        Row(
            modifier = dropdownBoxModifier
                .clickable { onClick() }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = selectorText,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = if (totalSelected == 0) normalValueStyle else selectedValueStyle
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = palette.textSecondary
            )
        }
    }
}

@Preview
@Composable
private fun TagsTriggerRowPreview() {
    TagsTriggerRow(
        selectorText = "3 selected",
        totalSelected = 3,
        onClick = {}
    )
}
