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
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.urlVideoImage.UrlVideoLite
import com.client.xvideos.common.vibrate.vibrateWithPatternAndAmplitude
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.parcer.parserVideoPreviewFromImageUrl
import timber.log.Timber

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UrlVideoImageAndLongClickX(
    item: ItemsX,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit,
    onDoubleClick: () -> Unit,
    overlay: @Composable () -> Unit= {}) {

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var isVideo by remember { mutableStateOf(false) }
    val previewVideoUrl = remember(item.previewImage, item.previewVideo) {
        parserVideoPreviewFromImageUrl(item.previewImage)
            .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            ?: item.previewVideo
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onDoubleClick = {
                    vibrateWithPatternAndAmplitude(context = context)
                    onDoubleClick.invoke()
                },

                onLongClick = {
                    //haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    vibrateWithPatternAndAmplitude(context = context)
                    onLongClick.invoke()
                },
                onClick = {
                    val nextIsVideo = !isVideo
                    isVideo = nextIsVideo
                    if (nextIsVideo) {
                        Timber.i(
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
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
            )
            .then(modifier)

    ) {

        if (isVideo) {
            //Показ видео
            UrlVideoLite(
                url = previewVideoUrl,
                posterUrl = item.previewImage,
                modifier = Modifier.fillMaxSize(),
                fallbackUrls = listOf(item.previewVideo),
                onClick = {
                    isVideo = !isVideo
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
            )
        } else {
            //Показ картинки
            UrlImage(item.previewImage, modifier = Modifier.fillMaxWidth())
            overlay.invoke()
        }

    }

}
