package com.client.xvideos.x.screens.ui.expandMenu

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.ButtonMoveVert
import com.client.xvideos.ui.theme.XvideosTheme

@Composable
fun X_DashboardExpandMenu(
    isFavorite: Boolean,
    onFavoriteAdd: () -> Unit,
    onFavoriteRemove: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
    onSaveToGallery: () -> Unit = {},
    isExpanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
) {
    var expanded by remember(isExpanded) { mutableStateOf(isExpanded) }
    val onOpen = remember(onExpandedChange) {
        {
            expanded = true
            onExpandedChange(true)
        }
    }
    val onDismissMenu = remember(onExpandedChange) {
        {
            expanded = false
            onExpandedChange(false)
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        ButtonMoveVert(26.dp, onOpen)

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissMenu,
            containerColor = Theme.ExpandMenu.backgroundColor,
            shadowElevation = 2.dp,
            tonalElevation = 16.dp
        ) {
            X_DashboardExpandMenuContent(
                isFavorite = isFavorite,
                onFavoriteAdd = onFavoriteAdd,
                onFavoriteRemove = onFavoriteRemove,
                onDownload = onDownload,
                onSaveToGallery = onSaveToGallery,
                onDismiss = onDismissMenu
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun X_DashboardExpandMenuPreview() {
    XvideosTheme {
        X_DashboardExpandMenu(
            isFavorite = false,
            onFavoriteAdd = {},
            onFavoriteRemove = {},
            onDownload = {}
        )
    }
}
