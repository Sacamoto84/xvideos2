package com.client.xvideos.common.ui.choice

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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** [ChoiceRowStyle.MATERIAL]: сегментные кнопки Material3, как было до дизайн-системы. */
@Composable
internal fun ChoiceRowMaterial(
    options: ImmutableList<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonColors = SegmentedButtonDefaults.colors(activeContainerColor = Color(0xFF938F99))
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, label ->
            key(label) {
                val onClick = remember(index, onSelectIndex) { { onSelectIndex(index) } }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    onClick = onClick,
                    selected = index == selectedIndex,
                    label = { Text(label) },
                    colors = buttonColors
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun ChoiceRowMaterialPreview() {
    ChoiceRowMaterial(
        options = persistentListOf("All", "Image", "Gif"),
        selectedIndex = 0,
        onSelectIndex = {}
    )
}
