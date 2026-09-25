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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.l.model.enum.PictureCountRank
import kotlinx.collections.immutable.persistentListOf

private val style = Theme.L.Type.rowTitle.copy(fontWeight = FontWeight.Bold)
private val SIZE_OPTIONS = persistentListOf("Any", "0..25", "25..50", "50..100", "100..200", "200..800", "800..3200", "3200..12800")
private val DROPDOWN_SHAPE = RoundedCornerShape(6.dp)
private val DROPDOWN_WIDTH = 160.dp
private val DROPDOWN_HEIGHT = 43.dp
private val DROPDOWN_BORDER_WIDTH = 1.dp
private val DROPDOWN_HORIZONTAL_PADDING = 8.dp
private val ROW_START_PADDING = 4.dp
private const val TITLE_ALBUM_SIZE = "Album Size"

@Composable
fun AlbumListFilterSize(
    value: PictureCountRank,
    modifier: Modifier = Modifier,
    onChanged: (PictureCountRank) -> Unit,
) {
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

    val titleStyle = remember(palette.textPrimary) { style.copy(color = palette.textPrimary) }

    Row(
        modifier = modifier
            .padding(start = ROW_START_PADDING)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(TITLE_ALBUM_SIZE, style = titleStyle)

        Row(
            modifier = Modifier
                .width(DROPDOWN_WIDTH)
                .height(DROPDOWN_HEIGHT)
                .clip(DROPDOWN_SHAPE)
                .border(DROPDOWN_BORDER_WIDTH, palette.border, DROPDOWN_SHAPE)
                .background(palette.field)
                .clickable { showDialog = true }
                .padding(horizontal = DROPDOWN_HORIZONTAL_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                currentLabel,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = titleStyle
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
            title = TITLE_ALBUM_SIZE,
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

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1C)
@Composable
fun AlbumListFilterSizePreview() {
    var size by remember { mutableStateOf(PictureCountRank.C25_50) }
    AlbumListFilterSize(
        value = size,
        onChanged = { size = it }
    )
}
