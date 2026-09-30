package com.client.xvideos.x.screens.ui.expandMenu

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.ui.theme.XvideosTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
private fun X_DashboardExpandMenuContentPreview() {
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
