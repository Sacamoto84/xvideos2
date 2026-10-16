package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.common.util.getTopInsetDp

/** Шапка открытой коллекции: «назад» (если [onExitCollection] задан) и название. */
@Composable
fun LCollectionDetailTopBar(
    collectionName: String,
    modifier: Modifier = Modifier,
    onExitCollection: (() -> Unit)? = null
) {
    val topInset = getTopInsetDp()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.background)
            .adaptiveContentWidth()
            .padding(top = topInset)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
    }
}

@Preview
@Composable
private fun LCollectionDetailTopBarPreview() {
    LCollectionDetailTopBar(
        collectionName = "Favorites",
        onExitCollection = {}
    )
}
