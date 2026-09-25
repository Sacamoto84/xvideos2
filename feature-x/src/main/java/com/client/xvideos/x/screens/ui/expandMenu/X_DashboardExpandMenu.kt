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

private val BUTTON_VERT_SIZE = 26.dp
private val MENU_SHADOW_ELEVATION = 2.dp
private val MENU_TONAL_ELEVATION = 16.dp
private const val FAVORITE_ACTION_DELAY_MS = 50L
private const val TEXT_FAVORITE = "Избранное"
private const val TEXT_SAVE = "Сохранить"
private const val TEXT_SAVE_TO_GALLERY = "В галерею"

@Composable
fun X_DashboardExpandMenu(
    isFavorite: Boolean,
    onFavoriteAdd: () -> Unit,
    onFavoriteRemove: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
    onSaveToGallery: () -> Unit = {},
    isExpanded: Boolean = false
) {
    var expanded by remember(isExpanded) { mutableStateOf(isExpanded) }
    val onOpen = remember { { expanded = true } }
    val onDismissMenu = remember { { expanded = false } }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        ButtonMoveVert(BUTTON_VERT_SIZE, onOpen)

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissMenu,
            containerColor = Theme.ExpandMenu.backgroundColor,
            shadowElevation = MENU_SHADOW_ELEVATION,
            tonalElevation = MENU_TONAL_ELEVATION
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
                delay(FAVORITE_ACTION_DELAY_MS)
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
    val favoriteIcon = remember(isFavorite) {
        if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
    }

    val favoriteLeadingIcon: @Composable () -> Unit = remember(favoriteIcon) {
        {
            Icon(
                imageVector = favoriteIcon,
                contentDescription = TEXT_FAVORITE
            )
        }
    }
    val saveLeadingIcon: @Composable () -> Unit = remember {
        {
            Icon(
                Icons.Outlined.Save,
                contentDescription = TEXT_SAVE
            )
        }
    }
    val galleryLeadingIcon: @Composable () -> Unit = remember {
        {
            Icon(
                Icons.Outlined.SaveAlt,
                contentDescription = TEXT_SAVE_TO_GALLERY
            )
        }
    }

    val favoriteItemText: @Composable () -> Unit = remember { { Text(TEXT_FAVORITE) } }
    val saveItemText: @Composable () -> Unit = remember { { Text(TEXT_SAVE) } }
    val galleryItemText: @Composable () -> Unit = remember { { Text(TEXT_SAVE_TO_GALLERY) } }

    DropdownMenuItem(
        text = favoriteItemText,
        onClick = handleFavorite,
        leadingIcon = favoriteLeadingIcon
    )

    DropdownMenuItem(
        text = saveItemText,
        onClick = handleDownload,
        leadingIcon = saveLeadingIcon
    )

    DropdownMenuItem(
        text = galleryItemText,
        onClick = handleSaveToGallery,
        leadingIcon = galleryLeadingIcon
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
            tonalElevation = MENU_TONAL_ELEVATION,
            shadowElevation = MENU_SHADOW_ELEVATION
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
