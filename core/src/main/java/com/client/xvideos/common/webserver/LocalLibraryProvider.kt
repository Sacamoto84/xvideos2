package com.client.xvideos.common.webserver

import android.os.Build
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.collectionDB.CollectionName
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

private val DEFAULT_SECTIONS = listOf("ALL", "X", "R", "L", "COLLECTIONS")

@Serializable
data class WebLibraryResponse(
    val items: List<WebMediaItem>,
    val totalCount: Int,
    val sections: List<String> = DEFAULT_SECTIONS
)

@Serializable
data class WebCollection(
    val id: String,
    val name: String,
    val section: String,
    val itemCount: Int,
    val coverUrl: String = "",
    val dateModified: Long = 0L
)

@Serializable
data class WebCollectionsResponse(
    val collections: List<WebCollection>,
    val totalCount: Int
)

@Serializable
data class WebStatusResponse(
    val serverName: String = "Xvideos Media Streamer",
    val version: String = "1.0",
    val ip: String,
    val port: Int,
    val totalItems: Int,
    val totalSizeBytes: Long,
    val deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    val totalCollections: Int = 0
)

object LocalLibraryProvider {

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
     * Возвращает список всех коллекций пользователя (R и L).
     */
    fun getCollections(sectionFilter: String? = null): WebCollectionsResponse {
        val result = mutableListOf<WebCollection>()
        val filter = sectionFilter?.uppercase()?.trim()

        if (filter.isNullOrBlank() || filter == "ALL" || filter == "R") {
            result.addAll(loadRCollections())
        }
        if (filter.isNullOrBlank() || filter == "ALL" || filter == "L") {
            result.addAll(loadLCollections())
        }

        result.sortByDescending { it.dateModified }
        return WebCollectionsResponse(
            collections = result,
            totalCount = result.size
        )
    }

    /**
     * Возвращает элементы конкретной коллекции.
     */
    fun getCollectionItems(section: String, collectionName: String): WebLibraryResponse {
        val safeName = CollectionName.normalizeOrNull(collectionName)
            ?: return WebLibraryResponse(emptyList(), 0)

        val items = when (section.lowercase()) {
            "r" -> loadRCollectionItems(safeName)
            "l" -> loadLCollectionItems(safeName)
            else -> emptyList()
        }

        return WebLibraryResponse(
            items = items,
            totalCount = items.size,
            sections = listOf(section.uppercase())
        )
    }

    /**
     * Статистика сервера: общее количество медиафайлов, размер и число коллекций.
     */
    fun getStatus(ip: String, port: Int): WebStatusResponse {
        val lib = getLibrary()
        val cols = getCollections()
        val totalBytes = lib.items.sumOf { it.sizeBytes }
        return WebStatusResponse(
            ip = ip,
            port = port,
            totalItems = lib.totalCount,
            totalSizeBytes = totalBytes,
            totalCollections = cols.totalCount
        )
    }

    // --- Коллекции R ---
    private fun loadRCollections(): List<WebCollection> {
        val root = File(AppPath.r_collection)
        if (!root.exists() || !root.isDirectory) return emptyList()

        val dirs = root.listFiles()?.filter { it.isDirectory }.orEmpty()
        return dirs.mapNotNull { dir ->
            val name = CollectionName.normalizeOrNull(dir.name) ?: return@mapNotNull null
            val items = dir.listFiles()?.filter { it.isFile && it.extension == "collection" }.orEmpty()
            val newestDate = items.maxOfOrNull { it.lastModified() } ?: dir.lastModified()
            val coverUrl = if (items.isNotEmpty()) "/media/collection/cover/r/${encodePathSegment(name)}" else ""

            WebCollection(
                id = "R_$name",
                name = name,
                section = "R",
                itemCount = items.size,
                coverUrl = coverUrl,
                dateModified = newestDate
            )
        }
    }

    private fun loadRCollectionItems(collectionName: String): List<WebMediaItem> {
        val colDir = File(AppPath.r_collection, collectionName)
        if (!isSafeInside(colDir, File(AppPath.r_collection)) || !colDir.exists()) return emptyList()

        val colFiles = colDir.listFiles()?.filter { it.isFile && it.extension == "collection" }.orEmpty()
        return colFiles.mapNotNull { file ->
            parseRCollectionFile(file)
        }.sortedByDescending { it.dateModified }
    }

    private data class RResolvedUrls(
        val videoUrl: String,
        val posterUrl: String,
        val downloadUrl: String,
        val hasVideo: Boolean,
        val hasPoster: Boolean
    )

    private fun resolveRMediaUrls(
        id: String,
        mp4File: File?,
        jpgFile: File?,
        json: kotlinx.serialization.json.JsonObject
    ): RResolvedUrls {
        val urlsObj = json["urls"]?.jsonObject
        val remoteMp4 = urlsObj?.get("mp4Url")?.jsonPrimitive?.contentOrNull
        val remoteGif = urlsObj?.get("gifUrl")?.jsonPrimitive?.contentOrNull
        val remoteJpg = urlsObj?.get("jpgUrl")?.jsonPrimitive?.contentOrNull

        val hasLocalVideo = mp4File != null && mp4File.exists() && mp4File.length() > 0L
        val hasLocalPoster = jpgFile != null && jpgFile.exists() && jpgFile.length() > 0L

        val videoUrl = when {
            hasLocalVideo -> "/media/r/$id/video"
            !remoteMp4.isNullOrBlank() -> remoteMp4
            !remoteGif.isNullOrBlank() -> remoteGif
            else -> ""
        }
        val posterUrl = when {
            hasLocalPoster -> "/media/r/$id/poster"
            !remoteJpg.isNullOrBlank() -> remoteJpg
            else -> ""
        }
        val downloadUrl = if (hasLocalVideo) "/media/r/$id/video?download=1" else videoUrl
        return RResolvedUrls(
            videoUrl = videoUrl,
            posterUrl = posterUrl,
            downloadUrl = downloadUrl,
            hasVideo = videoUrl.isNotBlank(),
            hasPoster = posterUrl.isNotBlank()
        )
    }

    private fun parseRCollectionFile(file: File): WebMediaItem? {
        return runCatching {
            val text = file.readText(Charsets.UTF_8)
            if (text.isBlank()) return null
            val json = AppJson.parseToJsonElement(text).jsonObject
            val id = json["id"]?.jsonPrimitive?.contentOrNull ?: file.nameWithoutExtension
            val description = json["description"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val userName = json["userName"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val tags = json["tags"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

            val mp4File = resolveRVideoFile(id)
            val jpgFile = resolveRPosterFile(id)
            val urls = resolveRMediaUrls(id, mp4File, jpgFile, json)

            WebMediaItem(
                id = id,
                section = "R",
                title = description.ifBlank { "Red Clip #$id" },
                subtitle = if (userName.isNotBlank()) "@$userName" else "",
                sizeBytes = (mp4File?.length() ?: 0L) + (jpgFile?.length() ?: 0L),
                dateModified = mp4File?.lastModified() ?: file.lastModified(),
                hasVideo = urls.hasVideo,
                hasPoster = urls.hasPoster,
                videoUrl = urls.videoUrl,
                posterUrl = urls.posterUrl,
                downloadUrl = urls.downloadUrl,
                mimeType = "video/mp4",
                tags = tags
            )
        }.onFailure {
            Timber.w(it, "LocalLibraryProvider: ошибка парсинга элемента R коллекции ${file.name}")
        }.getOrNull()
    }

    // --- Коллекции L ---
    private fun loadLCollections(): List<WebCollection> {
        val root = File(AppPath.l_collection)
        if (!root.exists() || !root.isDirectory) return emptyList()

        val dirs = root.listFiles()?.filter { it.isDirectory }.orEmpty()
        return dirs.mapNotNull { dir ->
            val name = CollectionName.normalizeOrNull(dir.name) ?: return@mapNotNull null
            val itemDirs = dir.listFiles()?.filter { itemDir ->
                itemDir.isDirectory && File(itemDir, "metadata.json").let { it.exists() && it.length() > 0L }
            }.orEmpty()
            val newestDate = itemDirs.maxOfOrNull { it.lastModified() } ?: dir.lastModified()
            val coverUrl = if (itemDirs.isNotEmpty()) "/media/collection/cover/l/${encodePathSegment(name)}" else ""

            WebCollection(
                id = "L_$name",
                name = name,
                section = "L",
                itemCount = itemDirs.size,
                coverUrl = coverUrl,
                dateModified = newestDate
            )
        }
    }

    private fun loadLCollectionItems(collectionName: String): List<WebMediaItem> {
        val colDir = File(AppPath.l_collection, collectionName)
        if (!isSafeInside(colDir, File(AppPath.l_collection)) || !colDir.exists()) return emptyList()

        val itemDirs = colDir.listFiles()?.filter { it.isDirectory }.orEmpty()
        return itemDirs.mapNotNull { itemDir ->
            parseLFolder(itemDir, collectionName)
        }.sortedByDescending { it.dateModified }
    }

    // --- Загрузка X ---
    private fun loadXItems(): List<WebMediaItem> {
        val dir = File(AppPath.x_cache_download)
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        val files = dir.listFiles() ?: return emptyList()
        val mp4Files = HashMap<String, File>()
        val infoFiles = ArrayList<File>()
        for (f in files) {
            if (!f.isFile || f.length() == 0L) continue
            val ext = f.extension
            if (ext.equals("mp4", ignoreCase = true)) {
                mp4Files[f.nameWithoutExtension] = f
            } else if (ext.equals("info", ignoreCase = true)) {
                infoFiles.add(f)
            }
        }

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
        val likesFiles = rLikesDir.listFiles() ?: return emptyList()

        for (likesFile in likesFiles) {
            if (!likesFile.isFile || likesFile.extension != "likes") continue
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
        loadFoldersInDir(File(AppPath.l_likes), result)
        loadFoldersInDir(File(AppPath.l_albums), result)
        loadLCollectionsItems(result)
        return result
    }

    private fun loadFoldersInDir(dir: File, result: MutableList<WebMediaItem>, collectionName: String? = null) {
        if (!dir.exists() || !dir.isDirectory) return
        val folders = dir.listFiles() ?: return
        for (folder in folders) {
            if (folder.isDirectory) {
                parseLFolder(folder, collectionName)?.let { result.add(it) }
            }
        }
    }

    private fun loadLCollectionsItems(result: MutableList<WebMediaItem>) {
        val colRoot = File(AppPath.l_collection)
        if (!colRoot.exists() || !colRoot.isDirectory) return
        val colDirs = colRoot.listFiles() ?: return
        for (colDir in colDirs) {
            if (colDir.isDirectory) {
                loadFoldersInDir(colDir, result, colDir.name)
            }
        }
    }

    private data class LResolvedUrls(
        val videoUrl: String,
        val posterUrl: String,
        val downloadUrl: String,
        val hasVideo: Boolean,
        val hasPoster: Boolean
    )

    private fun resolveLFolderUrls(
        folderName: String,
        mediaFileName: String,
        previewFileName: String,
        hasMedia: Boolean,
        isVideo: Boolean,
        hasPoster: Boolean,
        collectionName: String?
    ): LResolvedUrls {
        val safeFolderName = encodePathSegment(folderName)
        val basePath = if (collectionName != null) {
            "/media/collection/l/${encodePathSegment(collectionName)}/$safeFolderName"
        } else {
            "/media/l/$safeFolderName"
        }

        val safeMedia = encodePathSegment(mediaFileName)
        val safePreview = encodePathSegment(previewFileName)
        val videoUrl = if (hasMedia && isVideo) "$basePath/$safeMedia" else ""
        val posterUrl = if (hasPoster) "$basePath/$safePreview" else ""
        val downloadUrl = if (hasMedia) "$basePath/$safeMedia?download=1" else ""

        return LResolvedUrls(
            videoUrl = videoUrl,
            posterUrl = posterUrl,
            downloadUrl = downloadUrl,
            hasVideo = hasMedia && isVideo,
            hasPoster = hasPoster
        )
    }

    private fun parseLFolder(folder: File, collectionName: String? = null): WebMediaItem? {
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

            val urls = resolveLFolderUrls(
                folderName = folder.name,
                mediaFileName = mediaFile?.name.orEmpty(),
                previewFileName = previewFile?.name.orEmpty(),
                hasMedia = hasMedia,
                isVideo = isVideo,
                hasPoster = hasPoster,
                collectionName = collectionName
            )

            var sizeBytes = 0L
            val files = folder.listFiles()
            if (files != null) {
                for (f in files) {
                    if (f.isFile) sizeBytes += f.length()
                }
            }
            val id = if (collectionName != null) "${collectionName}_${folder.name}" else folder.name

            WebMediaItem(
                id = id,
                section = "L",
                title = albumTitle,
                subtitle = if (isVideo) "Видео" else "Галерея / Изображение",
                sizeBytes = sizeBytes,
                dateModified = folder.lastModified(),
                hasVideo = urls.hasVideo,
                hasPoster = urls.hasPoster,
                videoUrl = urls.videoUrl,
                posterUrl = urls.posterUrl,
                downloadUrl = urls.downloadUrl,
                mimeType = if (isVideo) "video/mp4" else "image/jpeg"
            )
        }.onFailure {
            Timber.w(it, "LocalLibraryProvider: ошибка разбора L папки ${folder.name}")
        }.getOrNull()
    }

    // --- Безопасный резолв файлов (делегируется LocalMediaResolver) ---

    fun resolveVideo(section: String, id: String): Pair<File, String>? =
        LocalMediaResolver.resolveVideo(section, id)

    fun resolvePoster(section: String, id: String): File? =
        LocalMediaResolver.resolvePoster(section, id)

    fun resolveLMedia(folderName: String, fileName: String): Pair<File, String>? =
        LocalMediaResolver.resolveLMedia(folderName, fileName)

    fun resolveCollectionCover(section: String, collectionName: String): File? =
        LocalMediaResolver.resolveCollectionCover(section, collectionName)

    fun resolveLCollectionMedia(collectionName: String, itemFolderName: String, fileName: String): Pair<File, String>? =
        LocalMediaResolver.resolveLCollectionMedia(collectionName, itemFolderName, fileName)

    private fun resolveRVideoFile(id: String): File? =
        LocalMediaResolver.resolveRVideoFile(id)

    private fun resolveRPosterFile(id: String): File? =
        LocalMediaResolver.resolveRPosterFile(id)

    private fun encodePathSegment(raw: String): String =
        LocalMediaResolver.encodePathSegment(raw)

    private fun isSafeInside(file: File, baseDir: File): Boolean =
        LocalMediaResolver.isSafeInside(file, baseDir)
}
