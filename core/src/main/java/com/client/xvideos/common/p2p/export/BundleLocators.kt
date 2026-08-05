package com.client.xvideos.common.p2p.export

import java.io.File

/** Результат локации: корень store, файлы бандла, файл-метаданные. */
data class LocatedBundle(
    val storeRoot: File,
    val files: List<File>,
    val metadataFile: File,
)

/** X: `<storeRoot>/<id>.mp4|.jpg|.info` (плоско). Обязательны mp4 и info. */
object XBundleLocator {
    fun locate(storeRoot: File, id: Long): LocatedBundle? {
        val mp4 = File(storeRoot, "$id.mp4")
        val info = File(storeRoot, "$id.info")
        if (!mp4.exists() || !info.exists()) return null
        val jpg = File(storeRoot, "$id.jpg")
        val files = buildList {
            add(mp4); add(info); if (jpg.exists()) add(jpg)
        }
        return LocatedBundle(storeRoot, files, info)
    }
}

/** R: `<storeRoot>/<userName>/<id>.mp4|.jpg|.info`. Обязательны mp4 и info. */
object RBundleLocator {
    fun locate(storeRoot: File, userName: String, id: String): LocatedBundle? {
        val dir = File(storeRoot, userName)
        val mp4 = File(dir, "$id.mp4")
        val info = File(dir, "$id.info")
        if (!mp4.exists() || !info.exists()) return null
        val jpg = File(dir, "$id.jpg")
        val files = buildList {
            add(mp4); add(info); if (jpg.exists()) add(jpg)
        }
        return LocatedBundle(storeRoot, files, info)
    }
}

/** L: папка item с `metadata.json` + media/preview. storeRoot = родитель папки. */
object LBundleLocator {
    const val METADATA = "metadata.json"

    fun locate(itemFolder: File): LocatedBundle? {
        if (!itemFolder.isDirectory) return null
        val metadata = File(itemFolder, METADATA)
        if (!metadata.exists()) return null
        val parent = itemFolder.parentFile ?: return null
        val files = itemFolder.listFiles()?.filter { it.isFile }?.sortedBy { it.name }.orEmpty()
        if (files.isEmpty()) return null
        return LocatedBundle(storeRoot = parent, files = files, metadataFile = metadata)
    }
}
