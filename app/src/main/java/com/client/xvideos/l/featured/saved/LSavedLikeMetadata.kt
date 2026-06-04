package com.client.xvideos.l.featured.saved

import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.Thumbnails
import com.google.gson.GsonBuilder
import timber.log.Timber
import java.io.File

data class LSavedLikePreview(
    val fileName: String,
    val sourceUrl: String,
    val width: Int,
    val height: Int,
    val size: String?
)

data class LSavedLikeMetadata(
    val schemaVersion: Int = 1,
    val savedAt: Long = System.currentTimeMillis(),
    val site: String = "luscious",
    val folderName: String,
    val mediaFileName: String,
    val previewFileName: String?,
    val previewFiles: List<LSavedLikePreview>?,
    val sourceMediaUrl: String,
    val sourcePreviewUrl: String?,
    val sourceOriginalUrl: String?,
    val sourceVideoUrl: String?,
    val albumId: String?,
    val albumTitle: String?,
    val albumDescription: String?,
    val albumUrl: String?,
    val albumDownloadUrl: String?,
    val albumDetails: AlbumDetails?,
    val picture: PicsDetails
)

private val lSavedLikeGson = GsonBuilder().setPrettyPrinting().create()

fun readLSavedLikeMetadata(file: File): LSavedLikeMetadata? {
    if (!file.exists()) {
        Timber.w("L saved metadata missing, skip folder: ${file.parentFile?.absolutePath ?: file.absolutePath}")
        return null
    }

    return try {
        lSavedLikeGson.fromJson(file.readText(Charsets.UTF_8), LSavedLikeMetadata::class.java)
    } catch (e: Exception) {
        Timber.e(e, "!!! read L like metadata error: ${file.absolutePath}")
        null
    }
}

fun writeLSavedLikeMetadata(file: File, metadata: LSavedLikeMetadata) {
    file.parentFile?.mkdirs()
    file.writeText(lSavedLikeGson.toJson(metadata), Charsets.UTF_8)
}

fun LSavedLikeMetadata.toPicsDetails(folder: File): PicsDetails? {
    val mediaFile = File(folder, mediaFileName)
        .takeIf { it.exists() }

    val savedPreviews = previewFiles
        ?.mapNotNull { preview ->
            File(folder, preview.fileName)
                .takeIf { it.exists() }
                ?.let { preview to it }
        }
        ?: emptyList()

    val oldPreviewFile = previewFileName
        ?.let { File(folder, it) }
        ?.takeIf { it.exists() }

    val largestPreviewFile = savedPreviews
        .maxByOrNull { (preview, _) -> preview.width * preview.height }
        ?.second
        ?: oldPreviewFile

    val displayMediaFile = mediaFile ?: largestPreviewFile ?: return null

    val thumbnails = when {
        savedPreviews.isNotEmpty() -> savedPreviews.map { (preview, file) ->
            Thumbnails(
                width = preview.width,
                height = preview.height,
                size = preview.size,
                url = file.absolutePath
            )
        }
        oldPreviewFile != null -> listOf("large_thumbnail", "small", "xMax").map { size ->
            Thumbnails(
                width = picture.width,
                height = picture.height,
                size = size,
                url = oldPreviewFile.absolutePath
            )
        }
        else -> {
        picture.thumbnails
        }
    }

    return picture.copy(
        url_to_original = displayMediaFile.absolutePath,
        url_to_video = if (picture.is_animated) mediaFile?.absolutePath else null,
        album = albumId ?: picture.album,
        thumbnails = thumbnails
    )
}

fun readCollectionMetadata(file: File): LSavedLikeMetadata? {
    return readLSavedLikeMetadata(file)
}

fun writeCollectionMetadata(file: File, metadata: LSavedLikeMetadata) {
    writeLSavedLikeMetadata(file, metadata)
}
