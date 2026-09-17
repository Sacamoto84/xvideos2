package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.client.xvideos.l.model.enum.PictureCountRank
import kotlinx.collections.immutable.persistentListOf

private val style = Theme.L.Type.rowTitle.copy(fontWeight = FontWeight.Bold)
private val SIZE_OPTIONS = persistentListOf("Any", "0..25", "25..50", "50..100", "100..200", "200..800", "800..3200", "3200..12800")

@Composable
fun AlbumListFilterSize(value: PictureCountRank, onChanged: (PictureCountRank) -> Unit) {

    var showDialog by remember { mutableStateOf(false) }
    val palette = StyleGenresTags.Palette

    val currentLabel = when (value) {
        PictureCountRank.All -> "Any"
        PictureCountRank.C0_25 -> "0..25"
        PictureCountRank.C25_50 -> "25..50"
        PictureCountRank.C50_100 -> "50..100"
        PictureCountRank.C100_200 -> "100..200"
        PictureCountRank.C200_800 -> "200..800"
        PictureCountRank.C800_3200 -> "800..3200"
        PictureCountRank.C3200_12800 -> "3200..12800"
    }

    Row(
        modifier = Modifier
            .padding(start = 4.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Album Size", style = style.copy(color = palette.textPrimary))

        Row(
            modifier = Modifier
                .width(160.dp)
                .height(43.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, palette.border, RoundedCornerShape(6.dp))
                .background(palette.field)
                .clickable { showDialog = true }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                currentLabel,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = Theme.L.Type.rowTitle.copy(color = palette.textPrimary, fontWeight = FontWeight.Bold)
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = palette.textSecondary
            )
        }
    }

    if (showDialog) {
        AlbumFilterSelectDialog(
            title = "Album Size",
            items = SIZE_OPTIONS,
            selectedItem = currentLabel,
            itemTitle = { it },
            onDismiss = { showDialog = false },
            onSelect = { item ->
                val selected = when (item) {
                    "Any" -> PictureCountRank.All
                    "0..25" -> PictureCountRank.C0_25
                    "25..50" -> PictureCountRank.C25_50
                    "50..100" -> PictureCountRank.C50_100
                    "100..200" -> PictureCountRank.C100_200
                    "200..800" -> PictureCountRank.C200_800
                    "800..3200" -> PictureCountRank.C800_3200
                    "3200..12800" -> PictureCountRank.C3200_12800
                    else -> PictureCountRank.All
                }
                onChanged(selected)
                showDialog = false
            }
        )
    }
}
