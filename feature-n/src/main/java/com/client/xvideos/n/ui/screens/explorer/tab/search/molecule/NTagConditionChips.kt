package com.client.xvideos.n.ui.screens.explorer.tab.search.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.model.NTagCondition
import com.client.xvideos.n.ui.screens.explorer.tab.search.atom.NFilterChip

@Composable
internal fun NTagConditionChips(
    tags: List<NTagCondition>,
    onRemove: (NTagCondition) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tags.isEmpty()) return
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(tags, key = { it.tag.id }) { condition ->
            val sign = if (condition.excluded) "−" else "+"
            NFilterChip("$sign ${condition.tag.name} ×", isSelected = true, onClick = { onRemove(condition) })
        }
    }
}

@Preview(name = "Выбранные теги", widthDp = 360, showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun NTagConditionChipsPreview() {
    NTagConditionChips(
        tags = listOf(
            NTagCondition(NTag(id = 1, type = "tag", name = "full color"), excluded = false),
            NTagCondition(NTag(id = 2, type = "tag", name = "monochrome"), excluded = true)
        ),
        onRemove = {}
    )
}
