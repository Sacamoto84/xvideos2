package com.client.xvideos.l.ui.screens.explorer.tab.saved.likes.molecule

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun LikesFilterSegmentedRow(
    options: ImmutableList<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    topInset: Dp,
    modifier: Modifier = Modifier
) {
    val buttonColors = SegmentedButtonDefaults.colors(
        activeContainerColor = Color(0xFF938F99)
    )

    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .padding(top = topInset)
            .padding(horizontal = 4.dp)
    ) {
        options.forEachIndexed { index, label ->
            key(label) {
                val onClick = remember(index, onSelectIndex) { { onSelectIndex(index) } }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size
                    ),
                    onClick = onClick,
                    selected = index == selectedIndex,
                    label = { Text(label) },
                    colors = buttonColors
                )
            }
        }
    }
}

@Preview
@Composable
private fun LikesFilterSegmentedRowPreview() {
    LikesFilterSegmentedRow(
        options = persistentListOf("All", "Image", "Gif"),
        selectedIndex = 0,
        onSelectIndex = {},
        topInset = 24.dp
    )
}
