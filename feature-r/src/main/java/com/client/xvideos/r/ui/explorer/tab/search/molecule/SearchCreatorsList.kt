package com.client.xvideos.r.ui.explorer.tab.search.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.r.model.search.SearchItemCreatorsResponse
import com.client.xvideos.r.ui.explorer.tab.search.atom.SearchCreatorItem

@Composable
fun SearchCreatorsList(
    creatorsList: List<SearchItemCreatorsResponse>,
    searchText: String,
    isLoading: Boolean,
    onCreatorClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    if (creatorsList.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (searchText.isNotBlank() && !isLoading) {
                Text("Ничего не найдено", color = Color.Gray)
            } else if (searchText.isBlank()) {
                Text("Введите имя автора для поиска", color = Color.Gray)
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(creatorsList, key = { it.text.ifBlank { it.name } }) { item ->
                val handle = item.text.removePrefix("@").ifBlank { item.name }
                SearchCreatorItem(
                    item = item,
                    onClick = { onCreatorClick(handle) }
                )
            }
        }
    }
}

@Preview
@Composable
private fun SearchCreatorsListPreview() {
    SearchCreatorsList(
        creatorsList = emptyList(),
        searchText = "",
        isLoading = false,
        onCreatorClick = {}
    )
}
