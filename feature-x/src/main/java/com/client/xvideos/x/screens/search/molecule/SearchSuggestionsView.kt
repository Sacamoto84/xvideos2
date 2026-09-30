package com.client.xvideos.x.screens.search.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.screens.search.atom.ChannelSuggestionItem
import com.client.xvideos.x.screens.search.atom.KeywordSuggestionItem
import com.client.xvideos.x.screens.search.atom.ModelSuggestionItem
import com.client.xvideos.x.screens.search.atom.SectionHeader
import com.client.xvideos.x.search.model.Channel
import com.client.xvideos.x.search.model.Keyword
import com.client.xvideos.x.search.model.Pornstar
import com.client.xvideos.x.search.model.SearchResult

/**
 * Отображение структурированных подсказок (фразы, модели, каналы).
 */
@Composable
fun SearchSuggestionsView(
    suggestions: SearchResult,
    isLoading: Boolean,
    onSelectKeyword: (String) -> Unit,
    onSelectModel: (Pornstar) -> Unit,
    onSelectChannel: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keywords = suggestions.resolvedKeywords
    val models = suggestions.resolvedPornstars
    val channels = suggestions.resolvedChannels

    if (suggestions.isEmpty && !isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Подсказок не найдено",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // 1. Поисковые фразы
            if (keywords.isNotEmpty()) {
                item {
                    SectionHeader(title = "Категории и фразы")
                }
                items(keywords, key = { "kw_${it.name}" }) { kw ->
                    KeywordSuggestionItem(keyword = kw, onSelect = onSelectKeyword)
                    HorizontalDivider(color = Color(0xFF252525), thickness = 0.5.dp)
                }
            }

            // 2. Модели и актрисы
            if (models.isNotEmpty()) {
                item {
                    SectionHeader(title = "Модели")
                }
                items(models, key = { "md_${it.name}_${it.profilePath}" }) { star ->
                    ModelSuggestionItem(star = star, onSelect = onSelectModel)
                    HorizontalDivider(color = Color(0xFF252525), thickness = 0.5.dp)
                }
            }

            // 3. Каналы и студии
            if (channels.isNotEmpty()) {
                item {
                    SectionHeader(title = "Каналы")
                }
                items(channels, key = { "ch_${it.name}_${it.profilePath}" }) { channel ->
                    ChannelSuggestionItem(channel = channel, onSelect = onSelectChannel)
                    HorizontalDivider(color = Color(0xFF252525), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Preview
@Composable
private fun SearchSuggestionsViewPreview() {
    SearchSuggestionsView(
        suggestions = SearchResult(
            keywords = listOf(Keyword(N = "sample query", R = "98%")),
            pornstar = listOf(Pornstar.EMPTY.copy(N = "Jane Doe")),
            channel = listOf(Channel.EMPTY.copy(N = "Studio Alpha"))
        ),
        isLoading = false,
        onSelectKeyword = {},
        onSelectModel = {},
        onSelectChannel = {}
    )
}
