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
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.theme.Theme
import timber.log.Timber
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.lAnimationVideoUrl
import com.client.xvideos.l.model.lDownloadUrl
import com.client.xvideos.l.model.lFullScreenImageUrls
import com.client.xvideos.l.model.lImageMediaUrl
import com.client.xvideos.l.model.lPreviewImageUrl

private const val TAG_URL = "url"
private val LINK_TEXT_COLOR = Color(0xFF8AB4F8)
private val MAX_DIALOG_HEIGHT = 520.dp
private const val TITLE_INFO = "Информация"
private const val LABEL_ALBUM = "Альбом: "
private const val CONFIRM_OK = "OK"
private const val ERR_OPEN_LINK = "Не удалось открыть ссылку"

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

    val onAlbumClickAction = remember(albumId, onAlbumClick) {
        if (albumId != null && onAlbumClick != null) {
            { onAlbumClick(albumId) }
        } else {
            null
        }
    }

    val infoText = remember(item, position, total) {
        lPictureInfoText(item, position, total)
    }

    val onUrlClick: (String) -> Unit = remember(uriHandler) {
        { url ->
            try {
                uriHandler.openUri(url)
            } catch (e: Exception) {
                Timber.w(e, "LPictureInfoDialog: не удалось открыть ссылку: $url")
                SnackBar.error(ERR_OPEN_LINK)
            }
        }
    }

    LavenderDialog(
        title = TITLE_INFO,
        onDismiss = onDismiss,
        content = {
            Column(
                modifier = Modifier
                    .heightIn(max = MAX_DIALOG_HEIGHT)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(LABEL_ALBUM, color = Theme.DialogLavande.dismissTextColor, fontFamily = Theme.L.fontFamilyKarla)
                    if (albumId != null && onAlbumClickAction != null) {
                        TextButton(onClick = onAlbumClickAction) {
                            Text(albumId.toString())
                        }
                    } else {
                        Text(item.album ?: "-", color = Color.White, fontFamily = Theme.L.fontFamilyKarla)
                    }
                }

                LPictureInfoText(
                    text = infoText,
                    onUrlClick = onUrlClick
                )
            }
        },
        confirmText = CONFIRM_OK,
        onConfirm = onDismiss,
    )
}

@Composable
private fun LPictureInfoText(
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

    ClickableText(
        text = annotatedText,
        modifier = modifier,
        style = textStyle,
        onClick = { offset ->
            annotatedText
                .getStringAnnotations(TAG_URL, offset, offset)
                .firstOrNull()
                ?.item
                ?.let(onUrlClick)
        }
    )
}

private val HTTPS_URL_REGEX = Regex("""https://\S+""")

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
                color = LINK_TEXT_COLOR,
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
