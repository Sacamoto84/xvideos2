package com.client.xvideos.common.webserver

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import timber.log.Timber

object QrCodeGenerator {

    /**
     * Генерирует QR-код в виде чистой матрицы [BitMatrix] (без зависимостей от Android SDK).
     */
    fun generateMatrix(
        content: String,
        sizePx: Int = 512,
        margin: Int = 1
    ): BitMatrix? {
        if (content.isBlank()) return null
        return runCatching {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to margin
            )
            QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        }.onFailure {
            Timber.e(it, "QrCodeGenerator: ошибка генерации QR-кода")
        }.getOrNull()
    }

    /**
     * Генерирует QR-код в виде Android [Bitmap].
     * @param content Содержимое (например, URL)
     * @param sizePx Размер стороны в пикселях
     * @param darkColor Цвет модулей QR-кода
     * @param lightColor Цвет фона
     */
    fun generateBitmap(
        content: String,
        sizePx: Int = 512,
        darkColor: Int = Color.WHITE,
        lightColor: Int = Color.TRANSPARENT,
        margin: Int = 1
    ): Bitmap? {
        val bitMatrix = generateMatrix(content, sizePx, margin) ?: return null
        return runCatching {
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) darkColor else lightColor
                }
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        }.onFailure {
            Timber.e(it, "QrCodeGenerator: ошибка создания Bitmap")
        }.getOrNull()
    }

    /**
     * Генерирует QR-код в виде Compose [ImageBitmap] для прямого использования в Image().
     */
    fun generateImageBitmap(
        content: String,
        sizePx: Int = 512,
        darkColor: Int = Color.WHITE,
        lightColor: Int = Color.TRANSPARENT,
        margin: Int = 1
    ): ImageBitmap? {
        return generateBitmap(content, sizePx, darkColor, lightColor, margin)?.asImageBitmap()
    }
}
