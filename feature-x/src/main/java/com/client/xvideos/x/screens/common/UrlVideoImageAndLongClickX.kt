package com.client.xvideos.x.screens.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.urlVideoImage.UrlVideoLite
import com.client.xvideos.common.vibrate.vibrateWithPatternAndAmplitude
import com.client.xvideos.ui.theme.XvideosTheme
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.parcer.parserVideoPreviewFromImageUrl
import kotlinx.collections.immutable.persistentListOf
import timber.log.Timber

private const val NULL_STRING = "null"
private val VIDEO_BOX_BASE_MODIFIER = Modifier.fillMaxSize()
private val POSTER_IMAGE_MODIFIER = Modifier.fillMaxWidth()
private val EMPTY_FALLBACK_URLS = persistentListOf<String>()
private val HAPTIC_FEEDBACK_CONFIRM = HapticFeedbackType.Confirm

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UrlVideoImageAndLongClickX(
    item: ItemsX,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit,
    onDoubleClick: () -> Unit,
    overlay: @Composable () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var isVideo by remember(item.id) { mutableStateOf(false) }

    val previewVideoUrl = remember(item.previewImage, item.previewVideo) {
        parserVideoPreviewFromImageUrl(item.previewImage)
            ?: item.previewVideo.takeIf { it.isNotBlank() && !it.equals(NULL_STRING, ignoreCase = true) }
    }
    val fallbackUrls = remember(item.previewVideo) {
        if (item.previewVideo.isNotBlank() && !item.previewVideo.equals(NULL_STRING, ignoreCase = true)) {
            persistentListOf(item.previewVideo)
        } else {
            EMPTY_FALLBACK_URLS
        }
    }

    val handleDoubleClick: () -> Unit = remember(context, onDoubleClick) {
        {
            vibrateWithPatternAndAmplitude(context = context)
            onDoubleClick()
        }
    }

    val handleLongClick: () -> Unit = remember(context, onLongClick) {
        {
            vibrateWithPatternAndAmplitude(context = context)
            onLongClick()
        }
    }

    val handleClick: () -> Unit = remember(item, previewVideoUrl, haptic) {
        {
            val nextIsVideo = !isVideo
            isVideo = nextIsVideo
            if (nextIsVideo) {
                Timber.d(
                    """
                    !!! X preview item click
                    id: ${item.id}
                    title: ${item.title}
                    href: ${item.href}
                    poster: ${item.previewImage}
                    parsed preview video: $previewVideoUrl
                    saved preview video: ${item.previewVideo}
                    """.trimIndent()
                )
            }
            haptic.performHapticFeedback(HAPTIC_FEEDBACK_CONFIRM)
        }
    }

    val handleVideoClick: () -> Unit = remember(haptic) {
        {
            isVideo = !isVideo
            haptic.performHapticFeedback(HAPTIC_FEEDBACK_CONFIRM)
        }
    }

    val baseModifier = if (modifier == Modifier) VIDEO_BOX_BASE_MODIFIER else modifier.then(VIDEO_BOX_BASE_MODIFIER)
    Box(
        modifier = baseModifier
            .combinedClickable(
                onDoubleClick = handleDoubleClick,
                onLongClick = handleLongClick,
                onClick = handleClick
            )
    ) {
        if (isVideo) {
            UrlVideoLite(
                url = previewVideoUrl.orEmpty(),
                posterUrl = item.previewImage,
                modifier = VIDEO_BOX_BASE_MODIFIER,
                fallbackUrls = fallbackUrls,
                onClick = handleVideoClick
            )
        } else {
            UrlImage(item.previewImage, modifier = POSTER_IMAGE_MODIFIER)
            overlay()
        }
    }
}

@Preview
@Composable
private fun UrlVideoImageAndLongClickXPreview() {
    XvideosTheme {
        UrlVideoImageAndLongClickX(
            item = ItemsX(
                id = 1L,
                title = "Preview video",
                duration = "10:00",
                views = "100K",
                channel = "Channel",
                previewImage = "",
                href = "/video",
                nameProfile = "Profile",
                linkProfile = "/profile",
            ),
            onLongClick = {},
            onDoubleClick = {}
        )
    }
}
