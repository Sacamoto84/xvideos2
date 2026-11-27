package com.client.xvideos.common.coil

import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.AnimatedVectorDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.asDrawable
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberConstraintsSizeResolver
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Scale
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.util.fileNameForLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import timber.log.Timber
import java.io.File


@Suppress("LongMethod", "CyclomaticComplexMethod", "UiComposable")
@Composable
fun UrlImage(

    url: String,
    modifier: Modifier = Modifier,
    urlGif : String?= null, //url для gif файла
    contentScale: ContentScale = ContentScale.Crop,
    loadIndicator: Boolean = true,

    albumName: String = "",

    isAnimated: Boolean = false,
    onSuccess: () -> Unit = {},
    onFailure: () -> Unit = {},
    autoPlay: Boolean = false,
    sizeButton: Dp = 32.dp,
    sizeButtonIcon: Dp = 20.dp,
    rotate: Boolean = false,                 //Поворот изображения

    isVisible: Boolean = true,

    //new

    isVisibleLoadingIndicator: Boolean = true, //Показ индикатора прогресса после индикатора загрузки
    sizeLoadingIndicator: Dp = 32.dp, //Показ индикатора прогресса после индикатора загрузки


    isVisibleProgressText: Boolean = false,    //Показ текста скачанных данных

    isFullScreen: Boolean = false, //Режим полного экрана с поддержкой поворота

    backgroung : Color = Color.Transparent,

) {


    //if (isAnimated) return

//    SideEffect {
//        Timber.i("!!! UrlImageGifsCoil")
//    }

    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val resources = LocalResources.current

    // В режиме превью возвращаем заглушку, чтобы избежать инициализации OkHttp/Conscrypt,
    // которая вызывает NoClassDefFoundError в LayoutLib.
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier.background(Color.Gray.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(sizeButtonIcon)
            )
        }
        return
    }

    val effectiveUrl = url.ifBlank { urlGif.orEmpty() }

    // Пустая строка — не адрес. Coil трактует её как локальный файл и падает с
    // `filePath == null`, а сообщение уезжало пользователю в текст ошибки.
    if (effectiveUrl.isBlank()) {
        Box(
            modifier = modifier.background(backgroung),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(sizeButtonIcon)
            )
        }
        return
    }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    var isPlaying by remember(effectiveUrl, autoPlay) { mutableStateOf(autoPlay) }

    val rawProgress by remember(effectiveUrl) {
        derivedStateOf {
            CoilProgressManager.progressMap[effectiveUrl] ?: CoilProgressItem(effectiveUrl, 0L, 0L, false)
        }
    }

    val needProgress = isVisibleProgressText || isAnimated
    val progress by produceState(CoilProgressItem(effectiveUrl, 0L, 0L, false), effectiveUrl, needProgress) {
        if (!needProgress) return@produceState
        snapshotFlow { rawProgress }
            .conflate()
            .collect {
                value = it
                delay(200)
            }
    }

    val bytes = progress.bytes
    val total = progress.total

    val dataSource = remember(effectiveUrl, albumName) {
        if (effectiveUrl.startsWith("https://", ignoreCase = true) ||
            effectiveUrl.startsWith("http://", ignoreCase = true)
        ) {
            effectiveUrl.toUri()
        } else {
            val cleanUrl = if (effectiveUrl.startsWith("file://", ignoreCase = true)) {
                effectiveUrl.substring(7)
            } else {
                effectiveUrl
            }
            val fileName = cleanUrl.substringAfterLast('/').substringBefore('?')
            val localFile = File(cleanUrl)
            if (localFile.isAbsolute || cleanUrl.contains('/') || cleanUrl.contains('\\')) {
                localFile
            } else if (albumName.isEmpty()) {
                File(cleanUrl)
            } else {
                when (albumName) {
                    "l_likes" -> File(AppPath.l_likes, fileName)
                    else -> File(cleanUrl)
                }
            }
        }
    }

    // Один глобальный ImageLoader на всё приложение (обновляется после recreate/clearCache)
    val loaderVersion by CoilImageLoaderFactory.version.collectAsStateWithLifecycle()
    val imageLoader = remember(appContext, loaderVersion) { CoilImageLoaderFactory.getImageLoader(appContext) }

    val sizeResolver = rememberConstraintsSizeResolver()

    val imageRequest = remember(appContext, dataSource, rotate, isFullScreen, sizeResolver) {
        ImageRequest.Builder(appContext)
            .data(dataSource)
            .scale(Scale.FIT)
            .apply {
                if (!isFullScreen) {
                    size(sizeResolver)
                    precision(Precision.INEXACT)
                }
            }
            .build()
    }

    var state by remember(imageRequest, imageLoader) {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }
    var hasLoaded by remember(effectiveUrl) { mutableStateOf(false) }
    val shouldLoad = isVisible || hasLoaded

    val currentOnSuccess by rememberUpdatedState(onSuccess)
    val currentOnFailure by rememberUpdatedState(onFailure)

    LaunchedEffect(state) {
        when (state) {
            is AsyncImagePainter.State.Success -> {
                hasLoaded = true
                currentOnSuccess()
            }
            is AsyncImagePainter.State.Error -> currentOnFailure()
            else -> Unit
        }
    }

    // Управление воспроизведением анимации + остановка при уходе элемента
    DisposableEffect(state, isPlaying, isAnimated, resources) {
        val drawable = if (isAnimated && state is AsyncImagePainter.State.Success) {
            (state as AsyncImagePainter.State.Success).result.image.asDrawable(resources)
        } else {
            null
        }
        if (drawable is AnimatedImageDrawable || drawable is AnimatedVectorDrawable || drawable is Animatable) {
            val anim = drawable as Animatable
            if (isPlaying) {
                anim.start()
            } else {
                anim.stop()
            }
        }
        onDispose {
            if (drawable is Animatable && drawable.isRunning) {
                drawable.stop()
            }
        }
    }

    Box(
        modifier = Modifier
            .then(modifier)
            .then(
                if (isFullScreen) {
                    Modifier.onSizeChanged { containerSize = it }
                } else
                    Modifier
            ),
        contentAlignment = Alignment.Center
    )
    {
        if (shouldLoad) {
            AsyncImage(
                onState = { st ->
                    state = st
                },
                model = imageRequest,
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = contentScale,

//            placeholder = forwardingPainter(
//                painter = painterResource(R.drawable.placeholder),
//                colorFilter = ColorFilter(Color.Red),
//                alpha = 0.5f,
//            ),
                modifier = Modifier
                    .background(backgroung)
                    .then(if (!isFullScreen) Modifier.then(sizeResolver) else Modifier)
                    .then(
                        if (isFullScreen && rotate) {
                            val scale = if (containerSize.width > 0 && containerSize.height > 0) {
                                if (containerSize.height > containerSize.width) {
                                    containerSize.height.toFloat() / containerSize.width
                                } else {
                                    containerSize.width.toFloat() / containerSize.height
                                }
                            } else {
                                1f
                            }
                            Modifier.graphicsLayer(
                                rotationZ = 90f,
                                scaleX = scale,
                                scaleY = scale
                            )
                        } else {
                            Modifier
                        }
                    )
                    .fillMaxSize()
                    .then(
                        if (isAnimated) {
                            Modifier.clickable { isPlaying = !isPlaying }
                        } else Modifier
                    )
            )

            when (state) {
                is AsyncImagePainter.State.Empty -> { }

                is AsyncImagePainter.State.Loading -> {

                    if (isVisibleLoadingIndicator) {

                        Box( modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center )
                        {
                            if (isAnimated) {
                                // Показываем прогресс в процентах если известен общий размер
                                if (total > 0 && bytes > 0) {
                                    val progress = (bytes.toFloat() / total.toFloat())
                                    CircularProgressIndicator( progress = { progress }, modifier = Modifier.size(sizeLoadingIndicator) )
                                } else {
                                    CircularProgressIndicator( modifier = Modifier.size(sizeLoadingIndicator), color = Color.Gray )
                                }
                            } else {
                                CircularProgressIndicator( modifier = Modifier.size(sizeLoadingIndicator), color = Color.Gray )
                            }
                        }

                    }

                }

                is AsyncImagePainter.State.Success -> { }

                is AsyncImagePainter.State.Error -> {
                    // Текст исключения был виден пользователю прямо в карточке
                    // («Ошибка загрузки: filePath == null»). Подробность нужна в
                    // журнале, на экране достаточно значка. В журнал идут имя файла
                    // и класс ошибки: в адресе и в тексте сетевой ошибки — имя сайта.
                    val error = (state as AsyncImagePainter.State.Error).result.throwable
                    LaunchedEffect(url, error) {
                        Timber.w("!!! UrlImage load failed %s: %s", url.fileNameForLog(), error.javaClass.simpleName)
                    }
                    Box( modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(sizeButtonIcon)
                        )
                    }
                }
            }


            // Кнопка управления анимацией
            if (isAnimated) {
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .align(Alignment.BottomStart)
                        .size(sizeButton)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
                        Icon( Icons.Default.Animation, contentDescription = null, tint = Color.White, modifier = Modifier.size(sizeButtonIcon) )
                    } else {
                        Icon( if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black,  modifier = Modifier.size(sizeButtonIcon) )
                        Icon( if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(sizeButtonIcon).offset((-0.5).dp, (-0.5).dp) )
                    }
                }
            }

            // Прогресс загрузки
            if (isVisibleProgressText) { Box(modifier = Modifier.align(Alignment.BottomEnd)) { if (bytes > 1000) { ProgressText( bytesRead = bytes,  totalBytes = total ) } } }
        } else {
            // Элемент за пределами активной зоны и ещё не загружен:
            // отображаем базовый фон без запуска или удержания сетевого запроса
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroung)
            )
        }

    }


}
