package com.client.xvideos.common.webserver

import android.os.Build
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.json.AppJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.io.File

@Serializable
data class WebMediaItem(
    val id: String,
    val section: String,
    val title: String,
    val subtitle: String = "",
    val duration: String = "",
    val sizeBytes: Long = 0L,
    val dateModified: Long = 0L,
    val hasVideo: Boolean = false,
    val hasPoster: Boolean = false,
    val videoUrl: String = "",
    val posterUrl: String = "",
    val downloadUrl: String = "",
    val mimeType: String = "video/mp4",
    val tags: List<String> = emptyList()
)

@Serializable
data class WebLibraryResponse(
    val items: List<WebMediaItem>,
    val totalCount: Int,
    val sections: List<String> = listOf("ALL", "X", "R", "L")
)

@Serializable
data class WebStatusResponse(
    val serverName: String = "Xvideos Media Streamer",
    val version: String = "1.0",
    val ip: String,
    val port: Int,
    val totalItems: Int,
    val totalSizeBytes: Long,
    val deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}"
)

object LocalLibraryProvider {

    private val SAFE_ID_REGEX = Regex("^[a-zA-Z0-9_-]+$")
    private val SAFE_FILENAME_REGEX = Regex("^[a-zA-Z0-9_.-]+$")

    /**
     * Возвращает список всех сохранённых медиафайлов (X, R, L) с опциональной фильтрацией по разделу.
     */
    fun getLibrary(sectionFilter: String? = null): WebLibraryResponse {
        val allItems = mutableListOf<WebMediaItem>()
        val filter = sectionFilter?.uppercase()?.trim()

        if (filter.isNullOrBlank() || filter == "ALL" || filter == "X") {
            allItems.addAll(loadXItems())
        }
        if (filter.isNullOrBlank() || filter == "ALL" || filter == "R") {
            allItems.addAll(loadRItems())
        }
        if (filter.isNullOrBlank() || filter == "ALL" || filter == "L") {
            allItems.addAll(loadLItems())
        }

        allItems.sortByDescending { it.dateModified }
        return WebLibraryResponse(
            items = allItems,
            totalCount = allItems.size
        )
    }

    /**
     * Статистика сервера: общее количество медиафайлов и суммарный размер.
     */
    fun getStatus(ip: String, port: Int): WebStatusResponse {
        val lib = getLibrary()
        val totalBytes = lib.items.sumOf { it.sizeBytes }
        return WebStatusResponse(
            ip = ip,
            port = port,
            totalItems = lib.totalCount,
            totalSizeBytes = totalBytes
        )
    }

    // --- Загрузка X ---
    private fun loadXItems(): List<WebMediaItem> {
        val dir = File(AppPath.x_cache_download)
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        val files = dir.listFiles().orEmpty()
        val mp4Files = files.filter { it.isFile && it.extension.equals("mp4", ignoreCase = true) && it.length() > 0L }
            .associateBy { it.nameWithoutExtension }

        val infoFiles = files.filter { it.isFile && it.extension.equals("info", ignoreCase = true) && it.length() > 0L }

        return infoFiles.mapNotNull { infoFile ->
            parseXInfoFile(infoFile, dir, mp4Files)
        }
    }

    private fun parseXInfoFile(infoFile: File, dir: File, mp4Files: Map<String, File>): WebMediaItem? {
        return runCatching {
            val idStr = infoFile.nameWithoutExtension
            val mp4File = mp4Files[idStr]
            val jpgFile = File(dir, "$idStr.jpg").takeIf { it.exists() && it.length() > 0L }

            val json = AppJson.parseToJsonElement(infoFile.readText(Charsets.UTF_8)).jsonObject
            val title = json["title"]?.jsonPrimitive?.contentOrNull ?: "X Video #$idStr"
            val duration = json["duration"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val channel = json["channel"]?.jsonPrimitive?.contentOrNull.orEmpty()

            val hasVideo = mp4File != null
            val hasPoster = jpgFile != null
            val sizeBytes = (mp4File?.length() ?: 0L) + (jpgFile?.length() ?: 0L)

            WebMediaItem(
                id = idStr,
                section = "X",
                title = title,
                subtitle = channel,
                duration = duration,
                sizeBytes = sizeBytes,
                dateModified = mp4File?.lastModified() ?: infoFile.lastModified(),
                hasVideo = hasVideo,
                hasPoster = hasPoster,
                videoUrl = if (hasVideo) "/media/x/$idStr/video" else "",
                posterUrl = if (hasPoster) "/media/x/$idStr/poster" else "",
                downloadUrl = if (hasVideo) "/media/x/$idStr/video?download=1" else "",
                mimeType = "video/mp4"
            )
        }.onFailure {
            Timber.w(it, "LocalLibraryProvider: ошибка разбора X .info файла ${infoFile.name}")
        }.getOrNull()
    }

    // --- Загрузка R ---
    private fun loadRItems(): List<WebMediaItem> {
        val result = mutableListOf<WebMediaItem>()
        val rDownloadDir = File(AppPath.r_cache_download)
        val rLikesDir = File(AppPath.r_likes)

        if (rDownloadDir.exists() && rDownloadDir.isDirectory) {
            result.addAll(loadRFromDownloads(rDownloadDir))
        }
        if (rLikesDir.exists() && rLikesDir.isDirectory) {
            result.addAll(loadRFromLikes(rLikesDir, result.map { it.id }.toSet()))
        }
        return result
    }

    private fun loadRFromDownloads(rDownloadDir: File): List<WebMediaItem> {
        val mp4s = rDownloadDir.walkTopDown().maxDepth(2)
            .filter { it.isFile && it.extension.equals("mp4", ignoreCase = true) && it.length() > 0L }

        return mp4s.mapNotNull { mp4File ->
            val idStr = mp4File.nameWithoutExtension
            val parent = mp4File.parentFile ?: rDownloadDir
            val jpgFile = File(parent, "$idStr.jpg").takeIf { it.exists() && it.length() > 0L }
            val infoFile = File(parent, "$idStr.info").takeIf { it.exists() && it.length() > 0L }

            var title = "Red Clip #$idStr"
            var author = parent.name.takeIf { it != rDownloadDir.name }.orEmpty()
            var tags = emptyList<String>()

            if (infoFile != null) {
                runCatching {
                    val json = AppJson.parseToJsonElement(infoFile.readText(Charsets.UTF_8)).jsonObject
                    json["description"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let { title = it }
                    json["userName"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let { author = it }
                    tags = json["tags"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                }
            }

            WebMediaItem(
                id = idStr,
                section = "R",
                title = title,
                subtitle = if (author.isNotBlank()) "@$author" else "",
                sizeBytes = mp4File.length() + (jpgFile?.length() ?: 0L),
                dateModified = mp4File.lastModified(),
                hasVideo = true,
                hasPoster = jpgFile != null,
                videoUrl = "/media/r/$idStr/video",
                posterUrl = if (jpgFile != null) "/media/r/$idStr/poster" else "",
                downloadUrl = "/media/r/$idStr/video?download=1",
                mimeType = "video/mp4",
                tags = tags
            )
        }.toList()
    }

    private fun loadRFromLikes(rLikesDir: File, existingIds: Set<String>): List<WebMediaItem> {
        val result = mutableListOf<WebMediaItem>()
        val likesFiles = rLikesDir.listFiles()?.filter { it.isFile && it.extension == "likes" }.orEmpty()

        for (likesFile in likesFiles) {
            val idStr = likesFile.nameWithoutExtension
            if (existingIds.contains(idStr)) continue

            runCatching {
                val json = AppJson.parseToJsonElement(likesFile.readText(Charsets.UTF_8)).jsonObject
                val title = json["description"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: "Red #$idStr"
                val author = json["userName"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val tags = json["tags"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

                val mp4File = resolveRVideoFile(idStr)
                val jpgFile = resolveRPosterFile(idStr)
                if (mp4File != null) {
                    result.add(
                        WebMediaItem(
                            id = idStr,
                            section = "R",
                            title = title,
                            subtitle = if (author.isNotBlank()) "@$author" else "",
                            sizeBytes = mp4File.length() + (jpgFile?.length() ?: 0L),
                            dateModified = mp4File.lastModified(),
                            hasVideo = true,
                            hasPoster = jpgFile != null,
                            videoUrl = "/media/r/$idStr/video",
                            posterUrl = if (jpgFile != null) "/media/r/$idStr/poster" else "",
                            downloadUrl = "/media/r/$idStr/video?download=1",
                            mimeType = "video/mp4",
                            tags = tags
                        )
                    )
                }
            }
        }
        return result
    }

    // --- Загрузка L ---
    private fun loadLItems(): List<WebMediaItem> {
        val result = mutableListOf<WebMediaItem>()
        val lDirs = listOf(File(AppPath.l_likes), File(AppPath.l_albums), File(AppPath.l_collection))

        for (baseDir in lDirs) {
            if (!baseDir.exists() || !baseDir.isDirectory) continue
            val folders = baseDir.listFiles()?.filter { it.isDirectory }.orEmpty()

            for (folder in folders) {
                parseLFolder(folder)?.let { result.add(it) }
            }
        }
        return result
    }

    private fun parseLFolder(folder: File): WebMediaItem? {
        val metadataFile = File(folder, "metadata.json")
        if (!metadataFile.exists() || metadataFile.length() == 0L) return null

        return runCatching {
            val json = AppJson.parseToJsonElement(metadataFile.readText(Charsets.UTF_8)).jsonObject
            val mediaFileName = json["mediaFileName"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val previewFileName = json["previewFileName"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val albumTitle = json["albumTitle"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: folder.name

            val mediaFile = if (mediaFileName.isNotBlank()) File(folder, mediaFileName) else null
            val previewFile = if (previewFileName.isNotBlank()) File(folder, previewFileName) else null

            val isVideo = mediaFile?.extension?.equals("mp4", ignoreCase = true) == true
            val hasMedia = mediaFile != null && mediaFile.exists() && mediaFile.length() > 0L
            val hasPoster = previewFile != null && previewFile.exists() && previewFile.length() > 0L

            val folderName = folder.name
            val safeMediaName = mediaFile?.name.orEmpty()
            val safePreviewName = previewFile?.name.orEmpty()
            val sizeBytes = folder.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L

            WebMediaItem(
                id = folderName,
                section = "L",
                title = albumTitle,
                subtitle = if (isVideo) "Видео" else "Галерея / Изображение",
                sizeBytes = sizeBytes,
                dateModified = folder.lastModified(),
                hasVideo = hasMedia && isVideo,
                hasPoster = hasPoster,
                videoUrl = if (hasMedia && isVideo) "/media/l/$folderName/$safeMediaName" else "",
                posterUrl = if (hasPoster) "/media/l/$folderName/$safePreviewName" else "",
                downloadUrl = if (hasMedia) "/media/l/$folderName/$safeMediaName?download=1" else "",
                mimeType = if (isVideo) "video/mp4" else "image/jpeg"
            )
        }.onFailure {
            Timber.w(it, "LocalLibraryProvider: ошибка разбора L папки ${folder.name}")
        }.getOrNull()
    }

    // --- Безопасный резолв файлов с защитой от Path Traversal ---

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
        val safeFolder = sanitizeId(folderName) ?: return null
        val safeFile = sanitizeFileName(fileName) ?: return null

        val searchDirs = listOf(File(AppPath.l_likes), File(AppPath.l_albums), File(AppPath.l_collection))
        for (baseDir in searchDirs) {
            val folder = File(baseDir, safeFolder)
            if (isSafeInside(folder, baseDir) && folder.exists() && folder.isDirectory) {
                val targetFile = File(folder, safeFile)
                if (isSafeInside(targetFile, folder) && targetFile.exists() && targetFile.isFile) {
                    return Pair(targetFile, safeFile)
                }
            }
        }
        return null
    }

    private fun resolveRVideoFile(id: String): File? {
        val rDownloadDir = File(AppPath.r_cache_download)
        if (!rDownloadDir.exists()) return null

        val direct = File(rDownloadDir, "$id.mp4")
        if (isSafeInside(direct, rDownloadDir) && direct.exists() && direct.isFile) return direct

        rDownloadDir.listFiles()?.filter { it.isDirectory }?.forEach { userFolder ->
            val nested = File(userFolder, "$id.mp4")
            if (isSafeInside(nested, rDownloadDir) && nested.exists() && nested.isFile) return nested
        }
        return null
    }

    private fun resolveRPosterFile(id: String): File? {
        val rDownloadDir = File(AppPath.r_cache_download)
        if (!rDownloadDir.exists()) return null

        val direct = File(rDownloadDir, "$id.jpg")
        if (isSafeInside(direct, rDownloadDir) && direct.exists() && direct.isFile) return direct

        rDownloadDir.listFiles()?.filter { it.isDirectory }?.forEach { userFolder ->
            val nested = File(userFolder, "$id.jpg")
            if (isSafeInside(nested, rDownloadDir) && nested.exists() && nested.isFile) return nested
        }
        return null
    }

    /** Проверка, что файл строго лежит внутри базового каталога (защита от ../..) */
    private fun isSafeInside(file: File, baseDir: File): Boolean {
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

    private fun sanitizeFileName(raw: String): String? {
        val cleaned = raw.trim()
        if (cleaned.isEmpty() || cleaned.contains("..") || !SAFE_FILENAME_REGEX.matches(cleaned)) return null
        return cleaned
    }
}
