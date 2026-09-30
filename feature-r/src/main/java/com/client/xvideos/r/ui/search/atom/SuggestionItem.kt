package com.client.xvideos.r.ui.search.atom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.toPrettyCount2
import com.client.xvideos.r.common.search.SuggestionItem

@Composable
fun SuggestionItem(
    suggestion: SuggestionItem,
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val annotatedString = remember(suggestion.text, query) {
        buildAnnotatedString {
            val text = suggestion.text
            val startIndex = text.indexOf(query, ignoreCase = true)
            if (startIndex != -1 && query.isNotEmpty()) {
                append(text.substring(0, startIndex))
                withStyle(style = SpanStyle(color = Theme.R.colorYellow)) {
                    append(text.substring(startIndex, startIndex + query.length))
                }
                append(text.substring(startIndex + query.length))
            } else {
                append(text)
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .padding(horizontal = 12.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = annotatedString,
            fontFamily = Theme.R.fontFamilyDMsanss,
            fontSize = 18.sp,
            color = Color.White,
            maxLines = 1
        )
        Text(
            text = suggestion.count.toPrettyCount2(),
            fontFamily = Theme.R.fontFamilyDMsanss,
            fontSize = 16.sp,
            color = Color.Gray,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF212121)
@Composable
private fun SuggestionItemPreview() {
    val suggestion = SuggestionItem("big sample", 123456)
    SuggestionItem(
        suggestion = suggestion,
        query = "big",
        onClick = {}
    )
}
