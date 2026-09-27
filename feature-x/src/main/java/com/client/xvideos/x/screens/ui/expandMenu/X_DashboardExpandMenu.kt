package com.client.xvideos.x.screens.ui.expandMenu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.ButtonMoveVert
import com.client.xvideos.ui.theme.XvideosTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


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

@Composable
fun X_DashboardExpandMenuContent(
    isFavorite: Boolean,
    onFavoriteAdd: () -> Unit,
    onFavoriteRemove: () -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    onSaveToGallery: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    val handleFavorite: () -> Unit = remember(isFavorite, onDismiss, onFavoriteRemove, onFavoriteAdd, scope) {
        {
            onDismiss()
            scope.launch {
                delay(50L)
                if (isFavorite) {
                    onFavoriteRemove()
                } else {
                    onFavoriteAdd()
                }
            }
        }
    }
    val handleDownload = remember(onDismiss, onDownload) {
        {
            onDismiss()
            onDownload()
        }
    }
    val handleSaveToGallery = remember(onDismiss, onSaveToGallery) {
        {
            onDismiss()
            onSaveToGallery()
        }
    }
    val favoriteIcon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder

    val favoriteLeadingIcon: @Composable () -> Unit = remember(favoriteIcon) {
        {
            Icon(
                imageVector = favoriteIcon,
                contentDescription = "Избранное",
                tint = Color.Black
            )
        }
    }
    val saveLeadingIcon: @Composable () -> Unit = remember {
        {
            Icon(
                Icons.Outlined.Save,
                contentDescription = "Сохранить",
                tint = Color.Black
            )
        }
    }
    val galleryLeadingIcon: @Composable () -> Unit = remember {
        {
            Icon(
                Icons.Outlined.SaveAlt,
                contentDescription = "В галерею",
                tint = Color.Black
            )
        }
    }

    val favoriteItemText: @Composable () -> Unit = remember { { Text("Избранное") } }
    val saveItemText: @Composable () -> Unit = remember { { Text("Сохранить") } }
    val galleryItemText: @Composable () -> Unit = remember { { Text("В галерею") } }

    DropdownMenuItem(
        text = favoriteItemText,
        onClick = handleFavorite,
        leadingIcon = favoriteLeadingIcon,
        colors = MenuDefaults.itemColors(textColor = Color.Black)
    )

    DropdownMenuItem(
        text = saveItemText,
        onClick = handleDownload,
        leadingIcon = saveLeadingIcon,
        colors = MenuDefaults.itemColors(textColor = Color.Black)
    )

    DropdownMenuItem(
        text = galleryItemText,
        onClick = handleSaveToGallery,
        leadingIcon = galleryLeadingIcon,
        colors = MenuDefaults.itemColors(textColor = Color.Black)
    )
}

@Preview(showBackground = true)
@Composable
fun Preview_X_DashboardExpandMenu_NotFavorite() {
    XvideosTheme {
        X_DashboardExpandMenu(
            isFavorite = false,
            onFavoriteAdd = {},
            onFavoriteRemove = {},
            onDownload = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun Preview_X_DashboardExpandMenu_Content() {
    XvideosTheme {
        Surface(
            color = Color(0xFFF2EDF7),
            tonalElevation = 16.dp,
            shadowElevation = 2.dp
        ) {
            Column {
                X_DashboardExpandMenuContent(
                    isFavorite = false,
                    onFavoriteAdd = {},
                    onFavoriteRemove = {},
                    onDownload = {},
                    onDismiss = {}
                )
            }
        }
    }
}
