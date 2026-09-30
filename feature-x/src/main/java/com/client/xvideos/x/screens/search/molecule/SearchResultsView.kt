package com.client.xvideos.x.screens.search.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.dashboards.molecule.DashboardsPaginatedListContent
import kotlinx.collections.immutable.toImmutableList

/**
 * Отображение результатов поиска видеороликов.
 */
@Composable
fun SearchResultsView(
    items: List<ItemsX>,
    isLoading: Boolean,
    isError: Boolean,
    onRetry: () -> Unit,
    isFavorite: (Long) -> Boolean,
    onFavoriteAdd: (ItemsX) -> Unit,
    onFavoriteRemove: (ItemsX) -> Unit,
    onDownload: (ItemsX) -> Unit,
    onSaveToGallery: (ItemsX) -> Unit,
    openVideoPlayer: (ItemsX) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isError -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Не удалось загрузить результаты поиска",
                        color = Color.White,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9900))
                    ) {
                        Text("Повторить", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
            items.isEmpty() && !isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "По данному запросу ничего не найдено",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
            else -> {
                DashboardsPaginatedListContent(
                    items = items.toImmutableList(),
                    isFavorite = isFavorite,
                    onFavoriteAdd = onFavoriteAdd,
                    onFavoriteRemove = onFavoriteRemove,
                    onDownload = onDownload,
                    openVideoPlayer = openVideoPlayer,
                    onSaveToGallery = onSaveToGallery,
                    contentPadding = PaddingValues(top = 2.dp, bottom = 4.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun SearchResultsViewPreview() {
    SearchResultsView(
        items = emptyList(),
        isLoading = false,
        isError = false,
        onRetry = {},
        isFavorite = { false },
        onFavoriteAdd = {},
        onFavoriteRemove = {},
        onDownload = {},
        onSaveToGallery = {},
        openVideoPlayer = {}
    )
}
