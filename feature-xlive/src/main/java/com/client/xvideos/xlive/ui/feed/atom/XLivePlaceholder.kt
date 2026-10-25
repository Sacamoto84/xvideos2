package com.client.xvideos.xlive.ui.feed.atom

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.client.xvideos.xlive.ui.XLiveColors

/** Тихая подпись по центру: пустая лента или вкладка, которой ещё нет. */
@Composable
fun XLivePlaceholder(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = XLiveColors.Label,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun XLivePlaceholderPreview() {
    XLivePlaceholder(text = "Сейчас нет эфиров по этому отбору")
}
