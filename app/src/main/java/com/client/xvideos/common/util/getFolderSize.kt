package com.client.xvideos.common.util

import java.io.File

fun getFolderSize(dir: File): Long {
    var size = 0L
    dir.listFiles()?.forEach { file ->
        size += if (file.isFile) {
            file.length()
        } else {
            getFolderSize(file)
        }
    }
    return size
}