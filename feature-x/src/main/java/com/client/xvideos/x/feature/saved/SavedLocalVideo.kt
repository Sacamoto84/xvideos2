package com.client.xvideos.x.feature.saved

import androidx.compose.runtime.Immutable

/**
 * Скачанный ролик X для карточки: [fileUrl] — `file://`-URI `<id>.mp4` (превью
 * из файла и плеер скачанного ролика), [posterPath] — скачанная обложка
 * `<id>.jpg` или `null`, если её нет.
 */
@Immutable
data class SavedLocalVideo(val fileUrl: String, val posterPath: String?)
