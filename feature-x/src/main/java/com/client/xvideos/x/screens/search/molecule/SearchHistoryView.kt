package com.client.xvideos.x.screens.search.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.screens.search.atom.SearchHistoryItem

/**
 * Отображение локальной истории поиска до начала ввода текста.
 */
@Composable
fun SearchHistoryView(
    history: List<String>,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (history.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Введите запрос для поиска видео, моделей или каналов",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "История поиска",
                        color = Color(0xFFB0B0B0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onClearAll) {
                        Text(
                            text = "Очистить всё",
                            color = Color(0xFFFF9900),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            items(history, key = { it }) { item ->
                SearchHistoryItem(
                    query = item,
                    onSelect = onSelect,
                    onDelete = onDelete
                )
                HorizontalDivider(color = Color(0xFF262626), thickness = 0.5.dp)
            }
        }
    }
}

@Preview
@Composable
private fun SearchHistoryViewPreview() {
    SearchHistoryView(
        history = listOf("First search", "Second query"),
        onSelect = {},
        onDelete = {},
        onClearAll = {}
    )
}
