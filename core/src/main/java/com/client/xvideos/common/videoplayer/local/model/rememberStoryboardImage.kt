package com.client.xvideos.common.videoplayer.local.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Size
import coil3.toBitmap
import com.client.xvideos.common.coil.CoilImageLoaderFactory
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.io.File

/**
 * Картинка раскадровки ролика X целиком, без уменьшения: из неё по пикселям
 * вырезаются кадры превью при перемотке. [data] — адрес раскадровки сайта
 * (`thumbSlideBig`, ролик из сети) или файл сетки скачанного ролика
 * (`<id>.storyboard.jpg`). Грузится через общий загрузчик (DoH, кеш на диске).
 * Нет данных или картинка не загрузилась — `null`, превью тогда показывает
 * только время.
 *
 * Файл сетки в память Coil не кладётся: ролик могут удалить и скачать заново
 * под тем же id, и сетка по тому же пути будет уже другой.
 */
@Composable
fun rememberStoryboardImage(data: Any?): ImageBitmap? {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(initialValue = null, data) {
        value = null
        if (data == null || (data is String && data.isBlank())) return@produceState
        val request = ImageRequest.Builder(context)
            .data(data)
            .size(Size.ORIGINAL)
            .apply { if (data is File) memoryCachePolicy(CachePolicy.DISABLED) }
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
