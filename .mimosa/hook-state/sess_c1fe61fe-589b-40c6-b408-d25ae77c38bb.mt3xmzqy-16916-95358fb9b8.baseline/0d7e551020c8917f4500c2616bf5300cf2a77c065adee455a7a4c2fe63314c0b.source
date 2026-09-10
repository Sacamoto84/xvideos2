package com.client.xvideos.l.ui.screens.screenFullScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.lAnimationVideoUrl
import com.client.xvideos.l.model.lDownloadUrl
import com.client.xvideos.l.model.lFullScreenImageUrls
import com.client.xvideos.l.model.lImageMediaUrl
import com.client.xvideos.l.model.lPreviewImageUrl

private const val TAG_URL = "url"

/**
 * Диалог «Информация» о картинке и сборка его текста.
 *
 * Выделено из `L_FullScreenImage.kt` (было 800 строк). Тела функций не менялись
 * — перенос дословный.
 */
@Composable
internal fun LPictureInfoDialog(
    item: PicsDetails,
    position: Int,
    total: Int,
    onDismiss: () -> Unit,
    onAlbumClick: ((Long) -> Unit)? = null
) {
    val albumId = item.album?.toLongOrNull()
    val uriHandler = LocalUriHandler.current

    LavenderDialog(
        title = "Информация",
        onDismiss = onDismiss,
        content = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Альбом: ", color = Color.DarkGray, fontFamily = Theme.L.fontFamilyKarla)
                    if (albumId != null && onAlbumClick != null) {
                        TextButton(onClick = { onAlbumClick(albumId) }) {
                            Text(albumId.toString())
                        }
                    } else {
                        Text(item.album ?: "-", color = Color.DarkGray, fontFamily = Theme.L.fontFamilyKarla)
                    }
                }

                LPictureInfoText(
                    text = lPictureInfoText(item, position, total),
                    onUrlClick = { url -> uriHandler.openUri(url) }
                )
            }
        },
        confirmText = "OK",
        onConfirm = onDismiss,
    )
}

@Composable
private fun LPictureInfoText(
    text: String,
    onUrlClick: (String) -> Unit
) {
    val annotatedText = remember(text) { text.withClickableHttpsLinks() }

    ClickableText(
        text = annotatedText,
        style = TextStyle(
            color = Color.DarkGray,
            fontFamily = Theme.L.fontFamilyKarla
        ),
        onClick = { offset ->
            annotatedText
                .getStringAnnotations(TAG_URL, offset, offset)
                .firstOrNull()
                ?.item
                ?.let(onUrlClick)
        }
    )
}

private fun String.withClickableHttpsLinks() = buildAnnotatedString {
    val urlRegex = Regex("""https://\S+""")
    var lastIndex = 0

    urlRegex.findAll(this@withClickableHttpsLinks).forEach { match ->
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

private fun lPictureInfoText(
    item: PicsDetails,
    position: Int,
    total: Int
): String = buildString {
    appendLine("Позиция: ${position + 1} / $total")
    appendLine("Размер: ${item.width} x ${item.height}")
    appendLine("Анимация: ${item.is_animated}")
    appendLine()

    appendLine("Используемая картинка:")
    appendLine(item.lImageMediaUrl() ?: "-")
    appendLine()

    appendLine("URL для скачивания:")
    appendLine(item.lDownloadUrl() ?: "-")
    appendLine()

    appendLine("URL видео:")
    appendLine(item.lAnimationVideoUrl() ?: item.url_to_video ?: "-")
    appendLine()

    appendLine("url_to_original:")
    appendLine(item.url_to_original ?: "-")
    appendLine()

    appendLine("url_to_video raw:")
    appendLine(item.url_to_video ?: "-")
    appendLine()

    appendLine("FullScreen candidates (${item.lFullScreenImageUrls().size}):")
    item.lFullScreenImageUrls().forEachIndexed { index, url ->
        appendLine("${index + 1}. $url")
    }
    appendLine()

    appendLine("Preview large_thumbnail:")
    appendLine(item.lPreviewImageUrl("large_thumbnail").ifBlank { "-" })
    appendLine()

    appendLine("Preview small:")
    appendLine(item.lPreviewImageUrl("small").ifBlank { "-" })
    appendLine()

    appendLine("Preview xMax:")
    appendLine(item.lPreviewImageUrl("xMax").ifBlank { "-" })
    appendLine()

    val thumbnails = item.thumbnails.orEmpty()
    appendLine("Thumbnails (${thumbnails.size}):")
    thumbnails.forEachIndexed { index, thumbnail ->
        appendLine("${index + 1}. size=${thumbnail.size ?: "-"} ${thumbnail.width}x${thumbnail.height}")
        appendLine(thumbnail.url ?: "-")
    }
}
