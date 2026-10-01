package com.client.xvideos.l.ui.screens.screenFullScreen.atom

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.Theme

private const val TAG_URL = "url"
private val HTTPS_URL_REGEX = Regex("""https://\S+""")

@Composable
fun LPictureInfoText(
    text: String,
    onUrlClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val annotatedText = remember(text) { text.withClickableHttpsLinks() }
    val textStyle = remember(Theme.DialogLavande.bodyColor) {
        TextStyle(
            color = Theme.DialogLavande.bodyColor,
            fontFamily = Theme.L.fontFamilyKarla
        )
    }

    val onAnnotatedClick: (Int) -> Unit = remember(annotatedText, onUrlClick) {
        { offset ->
            annotatedText
                .getStringAnnotations(TAG_URL, offset, offset)
                .firstOrNull()
                ?.item
                ?.let(onUrlClick)
        }
    }

    ClickableText(
        text = annotatedText,
        modifier = modifier,
        style = textStyle,
        onClick = onAnnotatedClick
    )
}

private fun String.withClickableHttpsLinks() = buildAnnotatedString {
    var lastIndex = 0

    HTTPS_URL_REGEX.findAll(this@withClickableHttpsLinks).forEach { match ->
        val rawUrl = match.value
        val url = rawUrl.trimEnd('.', ',', ';', ')', ']', '}')
        val start = match.range.first
        val end = start + url.length

        append(this@withClickableHttpsLinks.substring(lastIndex, start))

        val annotatedStart = length
        append(url)
        addStringAnnotation(TAG_URL, url, annotatedStart, annotatedStart + url.length)
        addStyle(
            SpanStyle(
                color = Color(0xFF8AB4F8),
                textDecoration = TextDecoration.Underline
            ),
            annotatedStart,
            annotatedStart + url.length
        )

        append(rawUrl.substring(url.length))
        lastIndex = match.range.last + 1
        if (end < start) lastIndex = match.range.last + 1
    }

    if (lastIndex < this@withClickableHttpsLinks.length) {
        append(this@withClickableHttpsLinks.substring(lastIndex))
    }
}

@Preview
@Composable
private fun LPictureInfoTextPreview() {
    LPictureInfoText(
        text = "Тестовая ссылка: https://example.com/test",
        onUrlClick = {}
    )
}
