package com.client.xvideos.common.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.client.xvideos.common.gallery.GalleryTarget
import com.client.xvideos.common.snackbar.SnackBar
import timber.log.Timber
import java.io.File

/**
 * Отдаёт [file] системному share-меню через FileProvider.
 *
 * Безопасно предоставляет права на чтение через ClipData и флаг FLAG_GRANT_READ_URI_PERMISSION,
 * исключая падения из-за ограничений безопасности Android 10+ и утечку абсолютных путей в логи.
 */
fun useCaseShareFile(context: Context, file: File): Boolean {
    if (!file.exists() || file.length() == 0L) {
        Timber.w("useCaseShareFile -> файл не найден или пуст: ${file.name}")
        SnackBar.error("Файл повреждён или отсутствует")
        return false
    }

    return runCatching {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val mimeType = GalleryTarget.mimeTypeFor(file.name)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Поделиться через").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(chooserIntent)
        true
    }.onFailure { e ->
        Timber.e(e, "useCaseShareFile -> ошибка при отправке файла: ${file.name}")
        SnackBar.error("Не удалось открыть меню «Поделиться»")
    }.getOrDefault(false)
}
