package com.client.xvideos.common.webserver

import com.client.xvideos.common.AppPath
import com.client.xvideos.common.collectionDB.CollectionName
import com.client.xvideos.common.json.AppJson
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.URLEncoder

/**
 * Резолвер медиафайлов на диске с валидацией путей и защитой от Path Traversal.
 */
object LocalMediaResolver {

    private val SAFE_ID_REGEX = Regex("^[a-zA-Z0-9_-]+$")
    private val SAFE_FILENAME_REGEX = Regex("^[a-zA-Z0-9_.-]+$")

    fun resolveVideo(section: String, id: String): Pair<File, String>? {
        val safeId = sanitizeId(id) ?: return null
        return when (section.lowercase()) {
            "x" -> {
                val dir = File(AppPath.x_cache_download)
                val file = File(dir, "$safeId.mp4")
                if (isSafeInside(file, dir) && file.exists()) {
                    Pair(file, "x_$safeId.mp4")
                } else null
            }
            "r" -> {
                val file = resolveRVideoFile(safeId) ?: return null
                Pair(file, "r_$safeId.mp4")
            }
            else -> null
        }
    }

    fun resolvePoster(section: String, id: String): File? {
        val safeId = sanitizeId(id) ?: return null
        return when (section.lowercase()) {
            "x" -> {
                val dir = File(AppPath.x_cache_download)
                val file = File(dir, "$safeId.jpg")
                if (isSafeInside(file, dir) && file.exists()) file else null
            }
            "r" -> resolveRPosterFile(safeId)
            else -> null
        }
    }

    fun resolveLMedia(folderName: String, fileName: String): Pair<File, String>? {
        val safeFolder = sanitizeFolderName(folderName) ?: return null
        val safeFile = sanitizeFileName(fileName) ?: return null

        val searchDirs = listOf(File(AppPath.l_likes), File(AppPath.l_albums))
        for (baseDir in searchDirs) {
            val folder = File(baseDir, safeFolder)
            if (isSafeInside(folder, baseDir) && folder.exists() && folder.isDirectory) {
                val targetFile = File(folder, safeFile)
                if (isSafeInside(targetFile, folder) && targetFile.exists() && targetFile.isFile) {
                    return Pair(targetFile, safeFile)
                }
            }
        }
        val colRoot = File(AppPath.l_collection)
        if (colRoot.exists() && colRoot.isDirectory) {
            return colRoot.listFiles()?.filter { it.isDirectory }?.firstNotNullOfOrNull { colDir ->
                val folder = File(colDir, safeFolder)
                if (isSafeInside(folder, colDir) && folder.exists() && folder.isDirectory) {
                    val targetFile = File(folder, safeFile)
                    if (isSafeInside(targetFile, folder) && targetFile.exists() && targetFile.isFile) {
                        Pair(targetFile, safeFile)
                    } else null
                } else null
            }
        }
        return null
    }

    fun resolveCollectionCover(section: String, collectionName: String): File? {
        val safeName = CollectionName.normalizeOrNull(collectionName) ?: return null
        return when (section.lowercase()) {
            "r" -> resolveRCollectionCover(safeName)
            "l" -> resolveLCollectionCover(safeName)
            else -> null
        }
    }

    private fun resolveRCollectionCover(collectionName: String): File? {
        val colDir = File(AppPath.r_collection, collectionName)
        if (!isSafeInside(colDir, File(AppPath.r_collection)) || !colDir.exists()) return null

        val files = colDir.listFiles()?.filter { it.isFile && it.extension == "collection" }.orEmpty()
        for (file in files) {
            val id = runCatching {
                val json = AppJson.parseToJsonElement(file.readText(Charsets.UTF_8)).jsonObject
                json["id"]?.jsonPrimitive?.contentOrNull ?: file.nameWithoutExtension
            }.getOrDefault(file.nameWithoutExtension)

            val poster = resolveRPosterFile(id)
            if (poster != null && poster.exists()) return poster
        }
        return null
    }

    private fun resolveLCollectionCover(collectionName: String): File? {
        val colDir = File(AppPath.l_collection, collectionName)
        if (!isSafeInside(colDir, File(AppPath.l_collection)) || !colDir.exists()) return null

        val configFile = File(colDir, "collection.json")
        if (configFile.exists()) {
            val coverFromConfig = findCoverFromConfig(colDir, configFile)
            if (coverFromConfig != null) return coverFromConfig
        }

        val itemDirs = colDir.listFiles()?.filter { it.isDirectory }?.sortedByDescending { it.lastModified() }.orEmpty()
        for (itemDir in itemDirs) {
            findCoverImageInFolder(itemDir)?.let { return it }
        }
        return null
    }

    private fun findCoverFromConfig(colDir: File, configFile: File): File? {
        return runCatching {
            val json = AppJson.parseToJsonElement(configFile.readText(Charsets.UTF_8)).jsonObject
            val coverFolder = json["coverFolderName"]?.jsonPrimitive?.contentOrNull
            if (!coverFolder.isNullOrBlank()) {
                val targetFolder = File(colDir, coverFolder)
                if (isSafeInside(targetFolder, colDir) && targetFolder.exists()) {
                    findCoverImageInFolder(targetFolder)
                } else null
            } else null
        }.getOrNull()
    }

    private fun findCoverImageInFolder(folder: File): File? {
        val metaFile = File(folder, "metadata.json")
        if (metaFile.exists()) {
            val metaPreview = findCoverFromMeta(folder, metaFile)
            if (metaPreview != null) return metaPreview
        }
        return folder.listFiles()?.firstOrNull {
            it.isFile && it.name != "metadata.json" && isImageExtension(it.extension) && it.length() > 0L
        }
    }

    private fun findCoverFromMeta(folder: File, metaFile: File): File? {
        return runCatching {
            val json = AppJson.parseToJsonElement(metaFile.readText(Charsets.UTF_8)).jsonObject
            val previewFileName = json["previewFileName"]?.jsonPrimitive?.contentOrNull
            if (!previewFileName.isNullOrBlank()) {
                val preview = File(folder, previewFileName)
                if (preview.exists() && preview.length() > 0L) return@runCatching preview
            }
            val mediaFileName = json["mediaFileName"]?.jsonPrimitive?.contentOrNull
            if (!mediaFileName.isNullOrBlank() && !mediaFileName.endsWith(".mp4", ignoreCase = true)) {
                val media = File(folder, mediaFileName)
                if (media.exists() && media.length() > 0L) return@runCatching media
            }
            null
        }.getOrNull()
    }

    private fun isImageExtension(ext: String): Boolean {
        return ext.equals("jpg", true) || ext.equals("png", true) ||
            ext.equals("jpeg", true) || ext.equals("webp", true)
    }

    fun resolveLCollectionMedia(collectionName: String, itemFolderName: String, fileName: String): Pair<File, String>? {
        val safeCol = CollectionName.normalizeOrNull(collectionName) ?: return null
        val safeFolder = sanitizeFolderName(itemFolderName) ?: return null
        val safeFile = sanitizeFileName(fileName) ?: return null

        val colDir = File(AppPath.l_collection, safeCol)
        if (!isSafeInside(colDir, File(AppPath.l_collection)) || !colDir.exists()) return null

        val itemDir = File(colDir, safeFolder)
        if (!isSafeInside(itemDir, colDir) || !itemDir.exists()) return null

        val targetFile = File(itemDir, safeFile)
        if (isSafeInside(targetFile, itemDir) && targetFile.exists() && targetFile.isFile) {
            return Pair(targetFile, safeFile)
        }
        return null
    }

    internal fun resolveRVideoFile(id: String): File? {
        val rDownloadDir = File(AppPath.r_cache_download)
        if (!rDownloadDir.exists()) return null

        val direct = File(rDownloadDir, "$id.mp4")
        if (isSafeInside(direct, rDownloadDir) && direct.exists() && direct.isFile) return direct

        return rDownloadDir.listFiles()?.filter { it.isDirectory }?.firstNotNullOfOrNull { userFolder ->
            val nested = File(userFolder, "$id.mp4")
            if (isSafeInside(nested, rDownloadDir) && nested.exists() && nested.isFile) nested else null
        }
    }

    internal fun resolveRPosterFile(id: String): File? {
        val rDownloadDir = File(AppPath.r_cache_download)
        if (!rDownloadDir.exists()) return null

        val direct = File(rDownloadDir, "$id.jpg")
        if (isSafeInside(direct, rDownloadDir) && direct.exists() && direct.isFile) return direct

        return rDownloadDir.listFiles()?.filter { it.isDirectory }?.firstNotNullOfOrNull { userFolder ->
            val nested = File(userFolder, "$id.jpg")
            if (isSafeInside(nested, rDownloadDir) && nested.exists() && nested.isFile) nested else null
        }
    }

    internal fun encodePathSegment(raw: String): String {
        return runCatching {
            URLEncoder.encode(raw, "UTF-8").replace("+", "%20")
        }.getOrDefault(raw)
    }

    /** Проверка, что файл строго лежит внутри базового каталога (защита от ../..) */
    internal fun isSafeInside(file: File, baseDir: File): Boolean {
        return runCatching {
            val fileCanonical = file.canonicalPath
            val baseCanonical = baseDir.canonicalPath
            fileCanonical.startsWith(baseCanonical + File.separator) || fileCanonical == baseCanonical
        }.getOrDefault(false)
    }

    private fun sanitizeId(raw: String): String? {
        val cleaned = raw.trim()
        if (cleaned.isEmpty() || !SAFE_ID_REGEX.matches(cleaned)) return null
        return cleaned
    }

    private fun isInvalidSegment(name: String): Boolean =
        name == "." || name == ".." || name.contains("..")

    private fun sanitizeFolderName(raw: String): String? {
        val cleaned = raw.trim()
        if (cleaned.isEmpty() || isInvalidSegment(cleaned) || !SAFE_FILENAME_REGEX.matches(cleaned)) {
            return null
        }
        return cleaned
    }

    private fun sanitizeFileName(raw: String): String? {
        val cleaned = raw.trim()
        if (cleaned.isEmpty() || isInvalidSegment(cleaned) || !SAFE_FILENAME_REGEX.matches(cleaned)) {
            return null
        }
        return cleaned
    }
}
