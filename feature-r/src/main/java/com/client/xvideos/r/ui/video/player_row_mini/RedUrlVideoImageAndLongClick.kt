package com.client.xvideos.r.ui.video.player_row_mini

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
import com.client.xvideos.r.ui.video.player_row_mini.atom.Red_Video_Lite_Row2
import java.io.File

private const val ASPECT_RATIO_9_16 = 1080f / 1920f
private val BASE_BOX_MODIFIER = Modifier.fillMaxSize().aspectRatio(ASPECT_RATIO_9_16)
private val VIDEO_CONTAINER_BASE_MODIFIER = Modifier.fillMaxSize()
private val VIDEO_ENTER_TRANSITION = fadeIn(animationSpec = tween(100))
private val VIDEO_EXIT_TRANSITION = fadeOut(animationSpec = tween(200))
private val POSTER_ENTER_TRANSITION = fadeIn(animationSpec = tween(100))
private val POSTER_EXIT_TRANSITION = fadeOut(animationSpec = tween(100))
private val INDEX_BOX_ALIGNMENT = Alignment.TopStart
private val BOX_CENTER_ALIGNMENT = Alignment.Center
private val COLOR_GRAY = Color.Gray
private val COLOR_LIGHT_GRAY = Color.LightGray
private val INDEX_TEXT_SIZE = 14.sp

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
    var isVideo by remember(item.id) { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(isVideo) { onVideo(isVideo) }
    LaunchedEffect(item.id, play) { isVideo = play }

    var poster by remember(item.id) { mutableStateOf(true) }
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

    val handleDoubleClick = remember(context, onDoubleClick) {
        {
            vibrateWithPatternAndAmplitude(context = context)
            onDoubleClick()
        }
    }
    val handleLongClick = remember(context, onLongClick) {
        {
            vibrateWithPatternAndAmplitude(context = context)
            onLongClick()
        }
    }
    val handleClick = remember(haptic) {
        {
            isVideo = !isVideo
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        }
    }
    val handleVideoClick = remember {
        { isVideo = !isVideo }
    }
    val handleVideoLongClick = remember(onFullScreen) {
        { onFullScreen() }
    }
    val handlePosterChange = remember {
        { isShowing: Boolean -> poster = isShowing }
    }

    val indexText = remember(index) { index.toString() }
    val indexTextStyle = remember {
        androidx.compose.ui.text.TextStyle(
            color = COLOR_GRAY,
            fontFamily = Theme.R.fontFamilyDMsanss,
            fontSize = INDEX_TEXT_SIZE
        )
    }

    val clickableModifier = BASE_BOX_MODIFIER.combinedClickable(
        indication = null,
        interactionSource = interactionSource,
        onDoubleClick = handleDoubleClick,
        onLongClick = handleLongClick,
        onClick = handleClick
    )
    val rootModifier = if (modifier == Modifier) clickableModifier else clickableModifier.then(modifier)

    Box(
        modifier = rootModifier,
        contentAlignment = BOX_CENTER_ALIGNMENT
    ) {
        AnimatedVisibility(
            visible = isVideo || preload,
            enter = VIDEO_ENTER_TRANSITION,
            exit = VIDEO_EXIT_TRANSITION
        ) {
            Box(
                modifier = VIDEO_CONTAINER_BASE_MODIFIER.alpha(if (isVideo) 1f else 0f),
                contentAlignment = BOX_CENTER_ALIGNMENT
            ) {
                Red_Video_Lite_Row2(
                    url = videoUri,
                    play = shouldPlayVideo,
                    onClick = handleVideoClick,
                    onLongClick = handleVideoLongClick,
                    poster = handlePosterChange
                )
            }
        }

        AnimatedVisibility(
            visible = poster || !isVideo,
            enter = POSTER_ENTER_TRANSITION,
            exit = POSTER_EXIT_TRANSITION
        ) {
            Box {
                UrlImage(
                    url = imageUrl,
                    contentScale = ContentScale.Fit,
                    modifier = VIDEO_CONTAINER_BASE_MODIFIER.alpha(if (isVideo) 0.8f else 1.0f)
                )
                if (isVideo) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(BOX_CENTER_ALIGNMENT),
                        color = COLOR_LIGHT_GRAY
                    )
                }
            }
        }

        Box(modifier = Modifier.align(INDEX_BOX_ALIGNMENT)) {
            Text(
                text = indexText,
                style = indexTextStyle
            )
        }
    }
}
