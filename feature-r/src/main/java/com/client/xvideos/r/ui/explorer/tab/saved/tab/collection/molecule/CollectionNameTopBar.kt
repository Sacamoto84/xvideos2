package com.client.xvideos.r.ui.explorer.tab.saved.tab.collection.molecule

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.ui.atom.SavedTabHeader

/** Шапка открытой коллекции — та же, что у вкладок сохранённого, со стрелкой «назад». */
@Composable
fun CollectionNameTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SavedTabHeader(
        title = title,
        modifier = modifier,
        navigation = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White
                )
            }
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun CollectionNameTopBarPreview() {
    CollectionNameTopBar(
        title = "Favorites",
        onBackClick = {}
    )
}
