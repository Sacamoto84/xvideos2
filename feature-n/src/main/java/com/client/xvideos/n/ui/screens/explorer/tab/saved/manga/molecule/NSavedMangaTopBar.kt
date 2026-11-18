package com.client.xvideos.n.ui.screens.explorer.tab.saved.manga.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.n.ui.molecule.NSortMenuButton
import com.client.xvideos.n.ui.screens.explorer.tab.saved.manga.model.NSavedMangaOrder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

private val ORDER_OPTIONS: ImmutableList<String> = NSavedMangaOrder.entries.map { it.title }.toPersistentList()

/**
 * Верхняя панель сохранённой манги с количеством элементов и выпадающим меню сортировки.
 */
@Composable
fun NSavedMangaTopBar(
    itemsCount: Int,
    order: NSavedMangaOrder,
    onOrderChange: (NSavedMangaOrder) -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.tabLevel1)
    ) {
        if (topInset > 0.dp) {
            Spacer(Modifier.height(topInset))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Сохранённая манга",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$itemsCount сохранённых работ",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp
                )
            }

            if (itemsCount > 0) {
                NSortMenuButton(
                    options = ORDER_OPTIONS,
                    selectedIndex = order.ordinal,
                    onSelectIndex = { index -> onOrderChange(NSavedMangaOrder.entries[index]) },
                    contentDescription = "Сортировка сохранённой манги"
                )
            }
        }
    }
}

@Preview
@Composable
private fun NSavedMangaTopBarPreview() {
    NSavedMangaTopBar(
        itemsCount = 12,
        order = NSavedMangaOrder.NEWEST,
        onOrderChange = {},
        topInset = 0.dp
    )
}
