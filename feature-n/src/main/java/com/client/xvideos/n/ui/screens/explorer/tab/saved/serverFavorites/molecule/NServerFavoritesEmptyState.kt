package com.client.xvideos.n.ui.screens.explorer.tab.saved.serverFavorites.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Заглушка пустого состояния серверного избранного.
 */
@Composable
fun NServerFavoritesEmptyState(
    message: String = "На сервере пока нет добавленных закладок",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color(0xFFAAAAAA),
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
private fun NServerFavoritesEmptyStatePreview() {
    NServerFavoritesEmptyState()
}
