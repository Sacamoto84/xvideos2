package com.client.xvideos.n.ui.screens.explorer.tab.saved.serverFavorites.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.client.xvideos.common.ui.HintTooltip

/**
 * Верхняя панель экрана серверного избранного манги.
 */
@Composable
fun NServerFavoritesTopBar(
    itemsCount: Int,
    onImportAll: () -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null,
    isImporting: Boolean = false,
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Избранное на сервере",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$itemsCount элементов на странице",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp
                )
            }

            if (onRefresh != null) {
                HintTooltip(text = "Обновить страницу") {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Обновить",
                            tint = Color.White
                        )
                    }
                }
            }

            HintTooltip(text = "Импортировать страницу в сохранённое") {
                IconButton(
                    onClick = onImportAll,
                    enabled = itemsCount > 0 && !isImporting
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudDownload,
                        contentDescription = "Импортировать в сохранённое",
                        tint = if (itemsCount > 0 && !isImporting) Color(0xFFED2553) else Color.Gray
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun NServerFavoritesTopBarPreview() {
    NServerFavoritesTopBar(
        itemsCount = 25,
        onImportAll = {},
        isImporting = false,
        topInset = 0.dp
    )
}
