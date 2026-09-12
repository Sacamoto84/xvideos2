package com.client.xvideos.l.featured.saved

import com.client.xvideos.common.io.writeTextAtomically
import com.client.xvideos.common.json.AppJson
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.Thumbnails
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import timber.log.Timber
import java.io.File

@Serializable
data class LSavedLikePreview(
    val fileName: String,
    val sourceUrl: String,
    val width: Int,
    val height: Int,
    val size: String?
)

@Serializable
data class LSavedLikeMetadata(
    val schemaVersion: Int = 1,
    val savedAt: Long = System.currentTimeMillis(),
    val site: String = "luscious",
    val folderName: String = "",
    val mediaFileName: String = "",
    val previewFileName: String? = null,
    val previewFiles: List<LSavedLikePreview>? = null,
    val sourceMediaUrl: String = "",
    val sourcePreviewUrl: String? = null,
    val sourceOriginalUrl: String? = null,
    val sourceVideoUrl: String? = null,
    val albumId: String? = null,
    val albumTitle: String? = null,
    val albumDescription: String? = null,
    val albumUrl: String? = null,
    val albumDownloadUrl: String? = null,
    val albumDetails: AlbumDetails? = null,
    val picture: PicsDetails = PicsDetails()
)

fun readLSavedLikeMetadata(file: File): LSavedLikeMetadata? {
    if (!file.exists()) {
        Timber.w("L saved metadata missing, skip folder: ${file.parentFile?.absolutePath ?: file.absolutePath}")
        return null
    }

    return try {
        AppJson.decodeFromString<LSavedLikeMetadata>(file.readText(Charsets.UTF_8))
    } catch (e: Exception) {
        Timber.e(e, "!!! read L like metadata error: ${file.absolutePath}")
        null
    }
}

fun writeLSavedLikeMetadata(file: File, metadata: LSavedLikeMetadata) {
    file.parentFile?.mkdirs()
    file.writeTextAtomically(AppJson.encodeToString(metadata))
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
