package com.client.xvideos.r.common.video.player_row_mini

import com.client.xvideos.common.theme.Theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.vibrate.vibrateWithPatternAndAmplitude
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.common.video.player_row_mini.atom.Red_Video_Lite_Row2
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RedUrlVideoImageAndLongClick(
    item: GifsInfo,                      //Текущий элемент
    index: Int,                          //Индекс элемента, отображается в режиме картинка
    modifier: Modifier = Modifier,

    //--- Свойства ---
    isNetConnected: Boolean,             // Состояние сети
    isVisibleView: Boolean = true,       // Показать количество просмотров
    isVisibleDuration: Boolean = true,   // Показать продолжительность видео

    play: Boolean = false,                //Запуск видео или картинка, управление из вне

    //-= Колбеки =-

    //--- Нажатия на кнопки ---
    onFullScreen: () -> Unit = {},         //Нажатие на кнопку FullScreen
    onLongClick: () -> Unit = {},
    onDoubleClick: () -> Unit = {},

    preload: Boolean = false,

    onVideo: (Boolean) -> Unit = {},       //true - видео, false - картинка

    downloadRed: () -> DownloadRed

) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var isVideo by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(isVideo) { onVideo(isVideo) }
    LaunchedEffect(item.id, play) { isVideo = play }

    var poster by remember { mutableStateOf(true) }
    val shouldPlayVideo = isVideo && (play || !preload)

    // Сбрасываем состояние видео при смене ID
    val videoUri = remember(item.id, item.userName, isNetConnected) {
        if (downloadRed().downloader.findVideoInDownload(item.id, item.userName)) {
            "${AppPath.r_cache_download}/${item.userName}/${item.id}.mp4"
        } else {
            if (isNetConnected)
                "https://api.redgifs.com/v2/gifs/${item.id.lowercase()}/hd.m3u8"
            else
                "android.resource://${context.packageName}/raw/q"
        }
    }

    val imageUrl = remember(item.id, item.userName) {
        val imagePath = "${AppPath.r_cache_download}/${item.userName}/${item.id}.jpg"
        if (File(imagePath).exists()) {
            imagePath
        } else {
            item.urls.poster ?: item.urls.thumbnail
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(1080f / 1920)
            .combinedClickable(
                indication = null,
                interactionSource = interactionSource,
                onDoubleClick = {
                    vibrateWithPatternAndAmplitude(context = context)
                    onDoubleClick()
                },
                onLongClick = {
                    vibrateWithPatternAndAmplitude(context = context)
                    onLongClick()
                },
                onClick = {
                    isVideo = !isVideo
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
            )
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isVideo || preload,
            enter = fadeIn(animationSpec = tween(100)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isVideo) 1f else 0f),
                contentAlignment = Alignment.Center
            ) {
                Red_Video_Lite_Row2(
                    url = videoUri,
                    play = shouldPlayVideo,
                    onClick = { isVideo = !isVideo },
                    onLongClick = { onFullScreen() },
                    poster = { poster = it }
                )
            }
        }

        AnimatedVisibility(
            visible = poster || !isVideo,
            enter = fadeIn(animationSpec = tween(100)),
            exit = fadeOut(animationSpec = tween(100))
        ) {
            Box {
                UrlImage(
                    url = imageUrl,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (isVideo) 0.8f else 1.0f)
                )
                if (isVideo) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.LightGray
                    )
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.TopStart)) {
            Text(
                text = index.toString(),
                color = Color.Gray,
                fontFamily = Theme.R.fontFamilyDMsanss,
                fontSize = 14.sp
            )
        }
    }
}
