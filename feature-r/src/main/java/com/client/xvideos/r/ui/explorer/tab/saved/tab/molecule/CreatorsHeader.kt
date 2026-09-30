package com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.r.common.saved.SelectedCreator
import com.client.xvideos.r.ui.explorer.tab.saved.tab.atom.CreatorChip

@Composable
fun CreatorsHeader(
    listCreators: List<SelectedCreator>,
    onCreatorClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (String) -> Unit = {}
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        listCreators.forEach { creator ->
            key(creator.name) {
                val onClick = remember(creator.name, onCreatorClick) { { onCreatorClick(creator.name) } }
                val onLong = remember(creator.name, onLongClick) { { onLongClick(creator.name) } }
                CreatorChip(
                    creator = creator.name,
                    url = creator.urlProfile,
                    isSelected = creator.select,
                    onClick = onClick,
                    onLongClick = onLong
                )
            }
        }
    }
}

@Preview
@Composable
private fun CreatorsHeaderPreview() {
    CreatorsHeader(
        listCreators = listOf(
            SelectedCreator("Creator 1", true, null),
            SelectedCreator("Creator 2", false, null)
        ),
        onCreatorClick = {},
        onLongClick = {}
    )
}
