package com.client.xvideos.x.screens.videoplayer.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Size
import coil3.toBitmap
import com.client.xvideos.common.coil.CoilImageLoaderFactory
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * Картинка раскадровки ролика X ([url] — `thumbSlideBig`), целиком, без
 * уменьшения: из неё по пикселям вырезаются кадры превью при перемотке.
 * Грузится через общий загрузчик (DoH, кеш на диске). Нет адреса или картинка
 * не загрузилась — `null`, превью тогда показывает только время.
 */
@Composable
fun rememberStoryboardImage(url: String?): ImageBitmap? {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(initialValue = null, url) {
        value = null
        if (url.isNullOrBlank()) return@produceState
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(Size.ORIGINAL)
            .build()
        value = try {
            val result = CoilImageLoaderFactory.getImageLoader(context).execute(request)
            (result as? SuccessResult)?.image?.toBitmap()?.asImageBitmap()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // В адресе — имя сайта, поэтому в журнал только класс ошибки.
            Timber.w("X раскадровка не загрузилась: %s", e.javaClass.simpleName)
            null
        }
    }
    return image
}
