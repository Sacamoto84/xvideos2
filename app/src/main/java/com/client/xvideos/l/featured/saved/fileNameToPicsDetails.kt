package com.client.xvideos.l.featured.saved

import com.client.xvideos.common.AppPath
import com.client.xvideos.l.model.PicsDetails
import java.io.File

fun fileNameToPicsDetails(file: File, folder : String): PicsDetails? {

    val name = file.nameWithoutExtension// убираем .jpg / .png и т.п.

    val parts = name.split("_", limit = 5)

    if (parts.size < 4) return null

    val width = parts[0].toIntOrNull() ?: return null
    val height = parts[1].toIntOrNull() ?: return null
    val is_animated = parts[2].toBooleanStrictOrNull() ?: false
    val album = parts.getOrNull(3) ?: "null"

    val path = File(folder, file.name).absolutePath

    return PicsDetails(
        height = height,
        width = width,
        is_animated = is_animated, // тут надо решать самому, инфы в имени нет
        url_to_original = path,
        url_to_video = null,
        album = album,
        thumbnails = emptyList()
    )
}
