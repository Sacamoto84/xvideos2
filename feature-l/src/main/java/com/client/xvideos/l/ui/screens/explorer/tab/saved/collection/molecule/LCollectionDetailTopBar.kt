package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.molecule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp

@Composable
fun LCollectionDetailTopBar(
    collectionName: String,
    searchQuery: String,
    searchVisible: Boolean,
    onSearchChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    modifier: Modifier = Modifier,
    onExitCollection: (() -> Unit)? = null
) {
    val topInset = getTopInsetDp()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.background)
            .padding(top = topInset)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onExitCollection != null) {
                IconButton(onClick = onExitCollection) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = Theme.L.primaryColor
                    )
                }
            }
            Text(
                collectionName,
                modifier = Modifier.weight(1f),
                color = Theme.L.primaryColor,
                fontSize = 18.sp,
                fontFamily = Theme.L.fontFamilyPopinsRegular
            )
            IconButton(onClick = onToggleSearch) {
                Icon(
                    imageVector = if (searchVisible) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (searchVisible) "Закрыть поиск" else "Поиск в коллекции",
                    tint = Theme.L.primaryColor
                )
            }
        }

        AnimatedVisibility(searchVisible) {
            val searchTextStyle = remember { Theme.L.Type.body.copy(color = Theme.L.textColor) }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Поиск в коллекции") },
                textStyle = searchTextStyle
            )
        }
    }
}

@Preview
@Composable
private fun LCollectionDetailTopBarPreview() {
    LCollectionDetailTopBar(
        collectionName = "Favorites",
        searchQuery = "",
        searchVisible = false,
        onSearchChange = {},
        onToggleSearch = {},
        onExitCollection = {}
    )
}
