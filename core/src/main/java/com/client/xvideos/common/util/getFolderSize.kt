package com.client.xvideos.common.util

import java.io.File

fun getFolderSize(dir: File?): Long {
    if (dir == null || !dir.exists() || !dir.isDirectory) return 0L
    val files = dir.listFiles()
    if (files.isNullOrEmpty()) return 0L
    var size = 0L
    dir.walkTopDown().forEach { file ->
        if (file.isFile) {
            size += file.length()
        }
    }
    return size
}

fun File?.folderSize(): Long = getFolderSize(this)

