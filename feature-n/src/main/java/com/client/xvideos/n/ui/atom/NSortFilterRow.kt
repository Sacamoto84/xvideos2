package com.client.xvideos.n.ui.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.common.ui.choice.ChoiceRow
import com.client.xvideos.common.ui.choice.ChoiceRowStyle
import com.client.xvideos.n.model.NSortType
import kotlinx.collections.immutable.persistentListOf

private val SORT_OPTIONS = persistentListOf(
    NSortType.POPULAR_ALL_TIME,
    NSortType.POPULAR_MONTH,
    NSortType.POPULAR_WEEK,
    NSortType.POPULAR_TODAY
)

private val SORT_OPTION_TITLES = persistentListOf(
    NSortType.POPULAR_ALL_TIME.title,
    NSortType.POPULAR_MONTH.title,
    NSortType.POPULAR_WEEK.title,
    NSortType.POPULAR_TODAY.title
)

/**
 * Строка переключения фильтра популярности (Все время, Месяц, Неделя, Сегодня).
 */
@Composable
fun NSortFilterRow(
    selectedSort: NSortType,
    onSortSelected: (NSortType) -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp
) {
    val selectedIndex = SORT_OPTIONS.indexOf(selectedSort).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.tabLevel0)
    ) {
        if (topInset > 0.dp) {
            Spacer(Modifier.height(topInset))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            ChoiceRow(
                options = SORT_OPTION_TITLES,
                selectedIndex = selectedIndex,
                onSelectIndex = { index ->
                    SORT_OPTIONS.getOrNull(index)?.let(onSortSelected)
                },
                style = ChoiceRowStyle.TABS,
                accent = Color(0xFFED2553),
                modifier = Modifier.adaptiveContentWidth()
            )
        }
    }
}

@Preview
@Composable
private fun NSortFilterRowPreview() {
    NSortFilterRow(
        selectedSort = NSortType.POPULAR_ALL_TIME,
        onSortSelected = {},
        topInset = 24.dp
    )
}
