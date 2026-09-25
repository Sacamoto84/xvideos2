package com.client.xvideos.common.util

import java.io.File

fun getFolderSize(dir: File): Long {
    if (!dir.exists() || !dir.isDirectory) return 0L
    var size = 0L
    dir.walkTopDown().forEach { file ->
        if (file.isFile) {
            size += file.length()
        }
    }
    return size
}
