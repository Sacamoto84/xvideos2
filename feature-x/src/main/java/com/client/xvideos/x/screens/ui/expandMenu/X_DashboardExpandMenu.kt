package com.client.xvideos.x.screens.ui.expandMenu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
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

@Composable
fun X_DashboardExpandMenu(
    isFavorite: Boolean,
    onFavoriteAdd: () -> Unit,
    onFavoriteRemove: () -> Unit,
    onDownload: () -> Unit,
    onSaveToGallery: () -> Unit = {},
    isExpanded: Boolean = false
) {

    var expanded by remember(isExpanded) { mutableStateOf(isExpanded) }
    val onOpen = remember { { expanded = true } }
    val onDismissMenu = remember { { expanded = false } }

    val size = 26.dp

    Box(
        modifier = Modifier,
        contentAlignment = Alignment.Center
    )
    {

        ButtonMoveVert(size, onOpen)

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissMenu,
            containerColor = Theme.ExpandMenu.backgroundColor,
            shadowElevation = 2.dp, tonalElevation = 16.dp
        )
        {
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
                delay(50)
                when (isFavorite) {
                    true -> onFavoriteRemove()
                    false -> onFavoriteAdd()
                }
            }.let {}
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

    DropdownMenuItem(
        text = { Text("Избранное") },
        onClick = handleFavorite,
        leadingIcon = {
            Icon(
                imageVector = favoriteIcon,
                contentDescription = null
            )
        }
    )

    DropdownMenuItem(
        text = { Text("Сохранить") },
        onClick = handleDownload,
        leadingIcon = {
            Icon(
                Icons.Outlined.Save,
                contentDescription = null
            )
        }
    )

    DropdownMenuItem(
        text = { Text("В галерею") },
        onClick = handleSaveToGallery,
        leadingIcon = {
            Icon(
                Icons.Outlined.SaveAlt,
                contentDescription = null
            )
        }
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
