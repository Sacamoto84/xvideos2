package com.client.xvideos.n.ui.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.n.model.NTag

/**
 * Горизонтальная строка подсказок поисковых тегов при автодополнении.
 */
@Composable
fun NTagSuggestionsRow(
    suggestions: List<NTag>,
    onTagClick: (NTag) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return

    val chipShape = RoundedCornerShape(16.dp)

    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.tabLevel0)
    ) {
        items(suggestions, key = { "${it.type}_${it.id}" }) { tag ->
            val displayText = when {
                tag.type.isNotEmpty() && tag.type != "tag" -> "${tag.type}:${tag.name}"
                else -> tag.name
            }

            Box(
                modifier = Modifier
                    .clip(chipShape)
                    .background(Theme.tabLevel2)
                    .border(1.dp, Color(0xFFED2553).copy(alpha = 0.5f), chipShape)
                    .clickable { onTagClick(tag) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = displayText,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (tag.count > 0) {
                        Text(
                            text = "(${formatCount(tag.count)})",
                            color = Color(0xFFAAAAAA),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1000 -> "${count / 1000}k"
    else -> count.toString()
}

@Preview
@Composable
private fun NTagSuggestionsRowPreview() {
    NTagSuggestionsRow(
        suggestions = listOf(
            NTag(id = 1, type = "parody", name = "genshin impact", count = 4500),
            NTag(id = 2, type = "artist", name = "cool_artist", count = 120),
            NTag(id = 3, type = "tag", name = "full color", count = 8900)
        ),
        onTagClick = {}
    )
}
