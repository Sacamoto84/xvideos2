package com.client.xvideos.x.screens.rubric.molecule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.x.screens.common.KeepHorizontalScroll
import com.client.xvideos.x.screens.common.atom.FilterButton
import com.client.xvideos.x.screens.rubric.atom.XRubricOptionRow
import com.client.xvideos.x.screens.rubric.model.XRubricFilterGroup
import com.client.xvideos.x.screens.rubric.model.XRubricFilterOption
import com.client.xvideos.x.screens.rubric.model.XRubricFilters

/**
 * Фильтры выдачи категории: четыре выпадающие кнопки в ряд (на кнопке —
 * текущее значение группы) и под ними список вариантов раскрытой группы.
 */
@Composable
fun XRubricFilterBar(
    filters: XRubricFilters,
    openGroup: XRubricFilterGroup?,
    onToggleGroup: (XRubricFilterGroup) -> Unit,
    onSelect: (XRubricFilterOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Ряд стоит в пейджере страниц: длинный свайп по нему не листает страницу.
                .nestedScroll(KeepHorizontalScroll)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            XRubricFilterGroup.entries.forEach { group ->
                FilterButton(
                    title = group.selectedIn(filters).title,
                    isExpanded = openGroup == group,
                    onClick = { onToggleGroup(group) },
                )
            }
        }

        AnimatedVisibility(
            visible = openGroup != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            if (openGroup != null) {
                val selected = openGroup.selectedIn(filters)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A1A1E))
                ) {
                    openGroup.options.forEach { option ->
                        XRubricOptionRow(
                            title = option.title,
                            selected = option == selected,
                            onClick = { onSelect(option) },
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun XRubricFilterBarPreview() {
    XRubricFilterBar(
        filters = XRubricFilters(),
        openGroup = XRubricFilterGroup.SORT,
        onToggleGroup = {},
        onSelect = {},
    )
}
