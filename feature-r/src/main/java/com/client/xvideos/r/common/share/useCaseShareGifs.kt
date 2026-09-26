package com.client.xvideos.r.common.share

import android.content.Context
import android.widget.Toast
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.io.isUnsafeItemName
import com.client.xvideos.common.io.requireInside
import com.client.xvideos.common.share.useCaseShareFile
import com.client.xvideos.r.model.GifsInfo
import timber.log.Timber
import java.io.File

/**
 * Отправляет локальный скачанный видеофайл медиаэлемента [item] в системный диалог «Поделиться».
 *
 * Выполняет проверки безопасности пути и существования файла в `r_cache_download/<userName>/<id>.mp4`.
 *
 * @param context Контекст Android для вызова системного интента.
 * @param item Передаваемый медиаэлемент.
 */
fun useCaseShareGifs(context: Context, item: GifsInfo) {
    if (isUnsafeItemName(item.userName) || isUnsafeItemName(item.id)) {
        Timber.w("shareGifs -> Отклонён небезопасный путь: userName=${item.userName}, id=${item.id}")
        Toast.makeText(context, "Недопустимое имя файла", Toast.LENGTH_SHORT).show()
        return
    }

    val baseDir = File(AppPath.r_cache_download)
    val userDir = File(baseDir, item.userName)
    val file = File(userDir, "${item.id}.mp4")

    try {
        requireInside(baseDir, userDir)
        requireInside(userDir, file)
    } catch (e: Exception) {
        Timber.w(e, "shareGifs -> Файл за пределами каталога кэша")
        Toast.makeText(context, "Недопустимый путь файла", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        if (file.exists() && file.length() > 0L) {
            useCaseShareFile(context, file)
        } else {
            Toast.makeText(context, "Файл не найден или пуст: ${file.path}", Toast.LENGTH_SHORT).show()
            Timber.w("shareGifs -> Файл не существует или пуст: ${file.path}")
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка при попытке поделиться файлом", Toast.LENGTH_SHORT).show()
        Timber.e(e, "shareGifs -> Ошибка при работе с файлом: ${file.path}")
    }
}
