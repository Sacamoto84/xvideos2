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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.l.model.enum.ContentId
import kotlinx.collections.immutable.persistentListOf

private val style = Theme.L.Type.rowValue
private val CONTENT_TYPE_OPTIONS = persistentListOf("All", "Hentai", "NErotic", "RPeople")

@Composable
fun AlbumListFilterContentType(onStart: ContentId, onChange: (ContentId) -> Unit) {

    var selectedIndex by remember(onStart) { mutableIntStateOf(
        when (onStart) {
            ContentId.All -> 0
            ContentId.Hentai -> 1
            ContentId.NonErotic -> 2
            ContentId.RealPeople -> 3
        }
    ) }

    val palette = StyleGenresTags.Palette

    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().height(48.dp)) {
        CONTENT_TYPE_OPTIONS.forEachIndexed { index, label ->
            SegmentedButton(
                modifier = Modifier.height(48.dp),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = palette.selected,
                    activeContentColor = palette.selectedText,
                    activeBorderColor = palette.selectedBorder,
                    inactiveContainerColor = palette.field,
                    inactiveContentColor = palette.textSecondary,
                    inactiveBorderColor = palette.border,
                ),

                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = CONTENT_TYPE_OPTIONS.size,
                    baseShape = RoundedCornerShape(4.dp)
                ),
                onClick = {
                    selectedIndex = index
                    val a = when (index) {
                        0 -> ContentId.All
                        1 -> ContentId.Hentai
                        2 -> ContentId.NonErotic
                        3 -> ContentId.RealPeople
                        else -> ContentId.All
                    }
                    onChange(a)
                },
                selected = index == selectedIndex,
                label = {
                    Text(
                        label,
                        style = style.copy(
                            color = if (index == selectedIndex) palette.selectedText else palette.textSecondary
                        )
                    )
                }
            )
        }
    }

}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1C)
@Composable
fun AlbumListFilterContentTypePreview() {
    var contentId by remember { mutableStateOf(ContentId.Hentai) }
    AlbumListFilterContentType(
        onStart = contentId,
        onChange = { contentId = it }
    )
}
