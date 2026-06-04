package com.client.xvideos.l.featured.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import timber.log.Timber
import java.io.File

fun useCaseShareFile(context: Context, file: File) {
    Timber.i("!!! useCaseShareFile: " + file.absolutePath)

    val uri = FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", file
    )

    val mimeType = when (file.extension.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "*/*"
        "webp" -> "image/webp"
        "bmp" -> "image/bmp"
        "svg" -> "image/svg+xml"
        "mp4" -> "video/mp4"
        "webm" -> "video/webm"
        "avi" -> "video/avi"
        else -> "image/*"
    }

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooserIntent = Intent.createChooser(shareIntent, "Поделиться через").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Добавляем этот флаг
    }

    context.startActivity(chooserIntent)
}


