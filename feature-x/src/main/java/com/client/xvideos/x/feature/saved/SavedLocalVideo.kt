package com.client.xvideos.x.feature.saved

import androidx.compose.runtime.Immutable

/**
 * Скачанный ролик X для карточки: [fileUrl] — `file://`-URI `<id>.mp4` (превью
 * из файла и плеер скачанного ролика), [posterPath] — скачанная обложка
 * `<id>.jpg` или `null`, если её нет. [sizeBytes] — размер видеофайла для
 * карточки «Сохранённого»; `0`, если неизвестен.
 */
@Immutable
data class SavedLocalVideo(
    val fileUrl: String,
    val posterPath: String?,
    val sizeBytes: Long = 0L,
)
