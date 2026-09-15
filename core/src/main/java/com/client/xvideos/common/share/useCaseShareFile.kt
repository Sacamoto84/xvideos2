package com.client.xvideos.common.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.client.xvideos.common.snackbar.SnackBar
import timber.log.Timber
import java.io.File

/**
 * Отдаёт [file] системному share-меню через FileProvider.
 *
 * Тип интента намеренно оставлен максимально широким: сюда приходят и картинки,
 * и гифки, и видео, а более узкий MIME отсеял бы часть приложений из списка
 * «Поделиться».
 */
fun useCaseShareFile(context: Context, file: File): Boolean {
    Timber.i("!!! useCaseShareFile: ${file.absolutePath}")

    if (!file.exists() || file.length() == 0L) {
        Timber.w("useCaseShareFile -> файл не найден или пуст: ${file.path}")
        SnackBar.error("Файл повреждён или отсутствует")
        return false
    }

    return runCatching {
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
        true
    }.onFailure { e ->
        Timber.e(e, "useCaseShareFile -> ошибка при отправке файла: ${file.path}")
        SnackBar.error("Не удалось открыть меню «Поделиться»")
    }.getOrDefault(false)
}
