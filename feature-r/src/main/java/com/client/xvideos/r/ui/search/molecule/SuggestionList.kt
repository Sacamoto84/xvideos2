package com.client.xvideos.r.ui.search.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.common.search.SuggestionItem
import com.client.xvideos.r.ui.search.atom.SuggestionItem

@Composable
fun SuggestionList(
    suggestions: () -> List<SuggestionItem>,
    query: String,
    onSuggestionClick: (SuggestionItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            itemsIndexed(
                items = suggestions(),
                key = { index, item -> "${item.text}#$index" }
            ) { _, suggestion ->
                SuggestionItem(
                    suggestion = suggestion,
                    query = query,
                    onClick = { onSuggestionClick(suggestion) }
                )
            }
        }
        HorizontalDivider(
            color = Theme.R.colorBorderGray.copy(alpha = 0.5f),
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Preview
@Composable
private fun SuggestionListPreview() {
    val sample = listOf(
        SuggestionItem("sample 1", 100),
        SuggestionItem("sample 2", 200)
    )
    SuggestionList(
        suggestions = { sample },
        query = "sample",
        onSuggestionClick = {}
    )
}
