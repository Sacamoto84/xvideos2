package com.client.xvideos.n.ui.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.n.model.NSortType
import com.client.xvideos.n.ui.molecule.NSortMenuButton
import kotlinx.collections.immutable.toImmutableList

/**
 * Верхняя панель ленты манги с названием и кнопкой открытия случайной работы.
 */
@Composable
fun NFeedTopBar(
    onRandomClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedSort: NSortType,
    onSortSelected: (NSortType) -> Unit,
    topInset: Dp = 0.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.tabLevel0)
    ) {
        if (topInset > 0.dp) {
            Spacer(Modifier.height(topInset))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Манга",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            NSortMenuButton(
                options = remember { NSortType.entries.map { it.title }.toImmutableList() },
                selectedIndex = selectedSort.ordinal,
                onSelectIndex = { onSortSelected(NSortType.entries[it]) },
                contentDescription = "Сортировка"
            )

            IconButton(onClick = onRandomClick) {
                Icon(
                    imageVector = Icons.Outlined.Shuffle,
                    contentDescription = "Случайная манга",
                    tint = Color(0xFFED2553)
                )
            }
        }
    }
}

@Preview
@Composable
private fun NFeedTopBarPreview() {
    NFeedTopBar(
        selectedSort = NSortType.RECENT,
        onSortSelected = {},
        onRandomClick = {},
        topInset = 24.dp
    )
}
