package com.client.xvideos.common.share

import android.content.Context
import android.content.Intent
import com.client.xvideos.common.snackbar.SnackBar
import timber.log.Timber

/**
 * Открывает системное меню «Поделиться» для текстового содержимого (ссылка, заголовок).
 *
 * @param context Контекст приложения или активности.
 * @param text Текст или URL для отправки.
 * @param subject Необязательная тема/заголовок для сообщений.
 * @return True, если Intent был успешно сформирован и передан системе, иначе false.
 */
fun useCaseShareText(context: Context, text: String, subject: String? = null): Boolean {
    if (text.isBlank()) {
        Timber.w("useCaseShareText -> передан пустой текст для отправки")
        return false
    }

    return runCatching {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            if (!subject.isNullOrBlank()) {
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Поделиться через").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(chooserIntent)
        true
    }.onFailure { e ->
        Timber.e(e, "useCaseShareText -> ошибка при вызове системного меню «Поделиться»")
        SnackBar.error("Не удалось открыть меню «Поделиться»")
    }.getOrDefault(false)
}
