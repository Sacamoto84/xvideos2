package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf

private val style = Theme.L.Type.rowValue
private val ALBUM_TYPE_OPTIONS = persistentListOf("All", "Manga", "Pictures")
private val BUTTON_ROW_HEIGHT = 48.dp
private val BASE_SEGMENT_SHAPE = RoundedCornerShape(4.dp)

@Composable
fun AlbumListFilterAlbumType(
    start: Int,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit,
) {
    var selectedIndex by remember(start) { mutableIntStateOf(start) }
    val palette = StyleGenresTags.Palette
    val buttonColors = SegmentedButtonDefaults.colors(
        activeContainerColor = palette.selected,
        activeContentColor = palette.selectedText,
        activeBorderColor = palette.selectedBorder,
        inactiveContainerColor = palette.field,
        inactiveContentColor = palette.textSecondary,
        inactiveBorderColor = palette.border,
    )
    val activeStyle = remember(palette.selectedText) {
        style.copy(color = palette.selectedText)
    }
    val inactiveStyle = remember(palette.textSecondary) {
        style.copy(color = palette.textSecondary)
    }

    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .height(BUTTON_ROW_HEIGHT)
    ) {
        ALBUM_TYPE_OPTIONS.forEachIndexed { index, label ->
            SegmentedButton(
                modifier = Modifier.height(BUTTON_ROW_HEIGHT),
                colors = buttonColors,
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = ALBUM_TYPE_OPTIONS.size,
                    baseShape = BASE_SEGMENT_SHAPE
                ),
                onClick = {
                    selectedIndex = index
                    onChange(index)
                },
                selected = index == selectedIndex,
                label = {
                    Text(
                        label,
                        style = if (index == selectedIndex) activeStyle else inactiveStyle
                    )
                }
            )
        }
    }
}

@Preview
@Composable
fun AlbumListFilterAlbumTypePreview() {
    AlbumListFilterAlbumType(start = 0, onChange = {})
}
