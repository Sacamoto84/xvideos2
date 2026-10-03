package com.client.xvideos.x.screens.actresses.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.ui.atom.TopLoadingBar
import com.client.xvideos.x.model.ActressesIndexItem
import com.client.xvideos.x.model.ActressesIndexUiState
import com.client.xvideos.x.screens.actresses.atom.LoadMoreErrorFooter

@Composable
fun ActressesIndexGrid(
    uiState: ActressesIndexUiState,
    gridState: LazyGridState,
    onActressClick: (ActressesIndexItem) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onRetryLoadMore: () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
        ) {
            if (uiState.isLoadingInitial) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFDE2600))
                    }
                }
            } else if (uiState.error != null) {
                item(span = { GridItemSpan(2) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error,
                            color = Color(0xFFCCCCCC),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDE2600))
                        ) {
                            Text("Повторить", color = Color.White)
                        }
                    }
                }
            } else if (uiState.isEmpty) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "По выбранным фильтрам ничего не найдено",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(
                    items = uiState.items,
                    key = { "${it.slug}_${it.rankText}" }
                ) { actress ->
                    ActressCard(
                        item = actress,
                        onClick = { onActressClick(actress) },
                    )
                }

                if (uiState.isLoadingMore) {
                    item(span = { GridItemSpan(2) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFDE2600),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                if (uiState.loadMoreError != null) {
                    item(span = { GridItemSpan(2) }) {
                        LoadMoreErrorFooter(message = uiState.loadMoreError, onRetry = onRetryLoadMore)
                    }
                }

                if (uiState.isEndReached && uiState.items.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = "Все модели каталога загружены",
                            color = Color(0xFF666666),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
        if (uiState.isLoadingInitial || uiState.isLoadingMore) {
            TopLoadingBar(modifier = Modifier.align(Alignment.TopCenter))
        }
    }
}

@Preview
@Composable
private fun ActressesIndexGridPreview() {
    ActressesIndexGrid(
        uiState = ActressesIndexUiState(),
        gridState = rememberLazyGridState(),
        onActressClick = {},
        onRetry = {}
    )
}
