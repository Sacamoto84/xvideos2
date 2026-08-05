package com.client.xvideos.common.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import timber.log.Timber
import java.io.File

/**
 * Отдаёт [file] системному share-меню через FileProvider.
 *
 * Тип интента намеренно оставлен максимально широким: сюда приходят и картинки,
 * и гифки, и видео, а более узкий MIME отсеял бы часть приложений из списка
 * «Поделиться».
 */
fun useCaseShareFile(context: Context, file: File) {
    Timber.i("!!! useCaseShareFile: " + file.absolutePath)

    val uri = FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", file
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooserIntent = Intent.createChooser(shareIntent, "Поделиться через").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    context.startActivity(chooserIntent)
}
