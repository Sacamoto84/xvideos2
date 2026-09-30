package com.client.xvideos.x.screens.tags.atom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Заголовок выдачи по выбранному тегу.
 */
@Composable
fun TagsHeader(
    tag: String,
    title0: String,
    title1: String,
    topCutout: Dp,
    modifier: Modifier = Modifier,
) {
    val hasTitle0 = remember(title0) { title0.isNotBlank() }
    val hasTitle1 = remember(title1) { title1.isNotBlank() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = topCutout + 8.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 8.dp
            )
    ) {
        Text(
            text = tag,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (hasTitle0 || hasTitle1) {
            Row {
                if (hasTitle0) {
                    Text(
                        text = "$title0 ",
                        color = Color(0xFFB0B0B0),
                        fontSize = 12.sp,
                    )
                }
                if (hasTitle1) {
                    Text(
                        text = title1,
                        color = Color(0xFF787878),
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun TagsHeaderPreview() {
    TagsHeader(
        tag = "vr",
        title0 = "Virtual Reality Videos",
        title1 = "12,450 results",
        topCutout = 24.dp
    )
}
