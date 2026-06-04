package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

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
import androidx.compose.ui.unit.dp
import com.client.xvideos.l.theme.ThemeL
import com.client.xvideos.l.model.enum.ContentId

private val style = ThemeL.Type.rowValue

@Composable
fun AlbumListFilterContentType(onStart: ContentId, onChange: (ContentId) -> Unit) {

    var selectedIndex by remember { mutableIntStateOf(
        when (onStart) {
            ContentId.All -> 0
            ContentId.Hentai -> 1
            ContentId.NonErotic -> 2
            ContentId.RealPeople -> 3
        }
    ) }

    val options = listOf("All", "Hentai", "NErotic", "RPeople")
    val palette = StyleGenresTags.Palette

    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().height(48.dp)) {
        options.forEachIndexed { index, label ->
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
                    count = options.size,
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
