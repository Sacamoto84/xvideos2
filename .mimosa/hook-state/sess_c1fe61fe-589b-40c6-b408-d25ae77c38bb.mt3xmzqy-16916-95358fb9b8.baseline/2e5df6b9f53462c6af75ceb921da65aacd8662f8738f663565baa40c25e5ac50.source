package com.client.xvideos.common.gallery

import android.os.Environment

/**
 * Куда в общей галерее кладётся файл.
 *
 * Вынесено из [GallerySaver] отдельно, чтобы решение «видео или картинка»
 * считалось по одному имени файла и покрывалось обычными JUnit-тестами без
 * Robolectric.
 *
 * Тип определяется по расширению, а не через `MimeTypeMap`: таблица MIME —
 * это вызов Android-стаба, недоступный в юнит-тесте, да и ответ на вопрос
 * «в Movies или в Pictures» нужен ровно бинарный.
 *
 * [relativePath] всё же зависит от `Environment.DIRECTORY_*` — это не
 * константы времени компиляции, поэтому в юнит-тестах они `null`, и там
 * проверяется только форма пути, а не имена базовых папок.
 */
internal object GalleryTarget {

    /** Имя папки внутри `Movies/` и `Pictures/`. */
    const val DIR = "xvideos_download"

    private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "avi", "mkv", "mov", "m4v", "3gp")

    fun isVideo(fileName: String): Boolean = extensionOf(fileName) in VIDEO_EXTENSIONS

    /**
     * `RELATIVE_PATH` для MediaStore.
     *
     * Завершающий слэш обязателен: MediaStore хранит значение именно так, и без
     * него запрос на существование файла не сматчится.
     */
    fun relativePath(fileName: String): String {
        val base = if (isVideo(fileName)) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
        return "$base/$DIR/"
    }

    fun extensionOf(fileName: String): String = fileName.substringAfterLast('.', "").lowercase()
}
