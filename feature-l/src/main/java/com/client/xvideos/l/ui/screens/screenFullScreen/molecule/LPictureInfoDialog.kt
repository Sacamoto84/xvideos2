package com.client.xvideos.l.ui.screens.screenFullScreen.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.lAnimationVideoUrl
import com.client.xvideos.l.model.lDownloadUrl
import com.client.xvideos.l.model.lFullScreenImageUrls
import com.client.xvideos.l.model.lImageMediaUrl
import com.client.xvideos.l.model.lPreviewImageUrl
import com.client.xvideos.l.ui.screens.screenFullScreen.atom.LPictureInfoText
import timber.log.Timber

/**
 * Диалог «Информация» о картинке и сборка его текста.
 */
@Composable
fun LPictureInfoDialog(
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
                Timber.w("LPictureInfoDialog: не удалось открыть ссылку: ${e.javaClass.simpleName}")
                SnackBar.error("Не удалось открыть ссылку")
            }
        }
    }

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
                    Text("Альбом: ", color = Theme.DialogLavande.dismissTextColor, fontFamily = Theme.L.fontFamilyKarla)
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
        confirmText = "OK",
        onConfirm = onDismiss,
    )
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

@Preview
@Composable
private fun LPictureInfoDialogPreview() {
    LPictureInfoDialog(
        item = PicsDetails(),
        position = 0,
        total = 1,
        onDismiss = {}
    )
}
