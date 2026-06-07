package com.client.xvideos.x.screens.ui.expandMenu

import androidx.compose.foundation.layout.Box
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
    isExpanded: Boolean = false
) {

    var expanded by remember { mutableStateOf(isExpanded) }

    val size = 26.dp

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier,
        contentAlignment = Alignment.Center
    )
    {

        ButtonMoveVert (size) { expanded = true }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color(0xFFF2EDF7),
            shadowElevation = 2.dp, tonalElevation = 16.dp
        )
        {

            DropdownMenuItem(
                text = { Text("Избранное") },
                onClick = {
                    expanded = false
                    scope.launch {
                        delay(50)
                        when (isFavorite) {
                            true -> onFavoriteRemove()
                            false -> onFavoriteAdd()
                        }
                    }
                },
                leadingIcon = {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null
                    )
                }
            )

            DropdownMenuItem(
                text = { Text("Сохранить") },
                onClick = {
                    expanded = false
                    onDownload()
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Save,
                        contentDescription = null
                    )
                }
            )

        }

    }
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
fun Preview_X_DashboardExpandMenu_Favorite() {
    XvideosTheme {
        X_DashboardExpandMenu(
            isFavorite = true,
            onFavoriteAdd = {},
            onFavoriteRemove = {},
            onDownload = {},
            isExpanded = true
        )
    }
}
