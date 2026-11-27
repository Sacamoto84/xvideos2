package com.client.xvideos.screenSettings.components

import androidx.compose.runtime.Immutable
import com.client.xvideos.common.AppPath
import kotlinx.collections.immutable.persistentListOf
import java.io.File

@Immutable
internal data class StorageStat(
    val key: String,
    val title: String,
    val sizeBytes: Long = 0L,
    val fileCount: Int = 0,
    val folders: List<StorageFolderStat> = emptyList()
)

/** Папка первого уровня раздела; `name == null` — файлы прямо в папке раздела. */
@Immutable
internal data class StorageFolderStat(
    val name: String?,
    val sizeBytes: Long,
    val fileCount: Int
)

@Immutable
internal data class FolderSnapshot(
    val sizeBytes: Long,
    val fileCount: Int
)

internal val EmptyStorageStats: List<StorageStat> = persistentListOf(
    StorageStat(key = "X", title = "X"),
    StorageStat(key = "L", title = "L"),
    StorageStat(key = "R", title = "R"),
    StorageStat(key = "XLive", title = "XLive")
)

/**
 * Размер разделов в корне хранилища [root] и их папок первого уровня;
 * XLive — свой раздел, с записями эфиров.
 */
internal fun loadStorageStats(root: File = File(AppPath.main)): List<StorageStat> {
    return listOf("X", "L", "R", "XLive").map { key ->
        val folders = File(root, key).collectFolders()
        StorageStat(
            key = key,
            title = key,
            sizeBytes = folders.sumOf { it.sizeBytes },
            fileCount = folders.sumOf { it.fileCount },
            folders = folders
        )
    }
}

/** Папки первого уровня, большие первыми; файлы прямо в разделе — последней строкой. */
private fun File.collectFolders(): List<StorageFolderStat> {
    val children = listFiles() ?: return emptyList()
    val folders = children
        .filter { it.isDirectory }
        .map { dir ->
            val snapshot = dir.collectSnapshot()
            StorageFolderStat(name = dir.name, sizeBytes = snapshot.sizeBytes, fileCount = snapshot.fileCount)
        }
        .sortedWith(compareByDescending<StorageFolderStat> { it.sizeBytes }.thenBy { it.name })
    val rootFiles = children.filter { it.isFile }
    if (rootFiles.isEmpty()) return folders
    return folders + StorageFolderStat(
        name = null,
        sizeBytes = rootFiles.sumOf { it.length() },
        fileCount = rootFiles.size
    )
}

private fun File.collectSnapshot(): FolderSnapshot {
    if (!exists()) return FolderSnapshot(sizeBytes = 0L, fileCount = 0)

    var sizeBytes = 0L
    var fileCount = 0
    walkTopDown().forEach { file ->
        if (file.isFile) {
            sizeBytes += file.length()
            fileCount += 1
        }
    }
    return FolderSnapshot(sizeBytes = sizeBytes, fileCount = fileCount)
}
