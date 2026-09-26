package com.client.xvideos.l.featured.saved

import com.client.xvideos.common.io.writeTextAtomically
import com.client.xvideos.common.json.AppJson
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.extractAnchorId
import com.client.xvideos.l.model.Thumbnails
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import timber.log.Timber
import java.io.File

/**
 * Метаданные одного скачанного локального файла превью.
 *
 * @property fileName Относительное имя файла превью внутри каталога элемента.
 * @property sourceUrl Исходный сетевой адрес, с которого было загружено превью.
 * @property width Ширина превью в пикселях.
 * @property height Высота превью в пикселях.
 * @property size Маркер исходного размера превью ("small", "large_thumbnail" и т.д.).
 */
@Serializable
data class LSavedLikePreview(
    val fileName: String,
    val sourceUrl: String,
    val width: Int,
    val height: Int,
    val size: String?
) {
    val hasSize: Boolean get() = !size.isNullOrBlank()
    val area: Int get() = width * height
    val hasValidDimensions: Boolean get() = width > 0 && height > 0
}

/**
 * Полный набор сериализуемых метаданных сохраненного элемента Luscious (`metadata.json`).
 *
 * Используется одинаково как для сохранения в «Лайки», так и для пользовательских «Коллекций».
 *
 * @property schemaVersion Версия структуры метаданных.
 * @property savedAt Метка времени сохранения на диск (в миллисекундах UTC).
 * @property site Идентификатор источника ("luscious").
 * @property folderName Имя директории элемента на диске.
 * @property mediaFileName Имя основного медиафайла ("media.jpg", "media.mp4").
 * @property previewFileName Имя файла превью по умолчанию (устаревший формат версий без previewFiles).
 * @property previewFiles Список доступных локальных превью разного разрешения.
 * @property sourceMediaUrl Исходный сетевой адрес медиафайла.
 * @property sourcePreviewUrl Исходный сетевой адрес превью по умолчанию.
 * @property sourceOriginalUrl Исходный URL оригинального медиа высокого разрешения.
 * @property sourceVideoUrl Исходный URL видеофайла (если элемент анимированный).
 * @property albumId Идентификатор родительского альбома Luscious.
 * @property albumTitle Название родительского альбома.
 * @property albumDescription Описание родительского альбома.
 * @property albumUrl Полный URL страницы альбома на сайте.
 * @property albumDownloadUrl Полный URL для скачивания архива альбома на сайте.
 * @property albumDetails Детальные метаданные родительского альбома [AlbumDetails].
 * @property pictureId Идентификатор картинки в системе Luscious.
 * @property pictureUrl URL страницы картинки.
 * @property picture Исходный объект [PicsDetails] с метаданными изображения.
 */
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
    val pictureId: String? = null,
    val pictureUrl: String? = null,
    val picture: PicsDetails = PicsDetails()
) {
    val hasAlbum: Boolean get() = !albumId.isNullOrBlank()
    val hasPictureId: Boolean get() = !pictureId.isNullOrBlank()
    val hasMediaFile: Boolean get() = mediaFileName.isNotBlank()
    val isAnimated: Boolean get() = !sourceVideoUrl.isNullOrBlank() || picture.is_animated
    val previewsCount: Int get() = previewFiles?.size ?: 0
}

/**
 * Считывает и десериализует [LSavedLikeMetadata] из указанного файла [file].
 *
 * @param file Файл `metadata.json`.
 * @return Разобранный объект [LSavedLikeMetadata] либо `null` при повреждении или отсутствии файла.
 */
fun readLSavedLikeMetadata(file: File): LSavedLikeMetadata? {
    if (!file.exists() || file.length() == 0L) {
        Timber.w("L saved metadata missing or empty, skip folder: ${file.parentFile?.absolutePath ?: file.absolutePath}")
        return null
    }

    val text = file.readText(Charsets.UTF_8)
    if (text.isBlank()) {
        Timber.w("L saved metadata blank, skip: ${file.absolutePath}")
        return null
    }

    return try {
        AppJson.decodeFromString<LSavedLikeMetadata>(text)
    } catch (e: Exception) {
        Timber.e(e, "read L like metadata error: ${file.absolutePath}")
        null
    }
}

/**
 * Атомарно сохраняет объект метаданных [metadata] в JSON-файл [file].
 *
 * @param file Целевой файл `metadata.json`.
 * @param metadata Экземпляр метаданных для сохранения.
 */
fun writeLSavedLikeMetadata(file: File, metadata: LSavedLikeMetadata) {
    file.parentFile?.mkdirs()
    file.writeTextAtomically(AppJson.encodeToString(metadata))
}

/**
 * Восстанавливает полноценную модель [PicsDetails] для показа в UI на основе
 * локально сохраненных файлов в каталоге [folder] и метаданных.
 *
 * Подменяет сетевые URL на абсолютные локальные пути к сохраненным файлам.
 *
 * @param folder Каталог элемента на диске.
 * @return Восстановленный объект [PicsDetails] либо `null`, если файлы отсутствуют.
 */
fun LSavedLikeMetadata.toPicsDetails(folder: File): PicsDetails? {
    val mediaFile = File(folder, mediaFileName)
        .takeIf { it.exists() && it.length() > 0L }

    val savedPreviews = previewFiles
        ?.mapNotNull { preview ->
            File(folder, preview.fileName)
                .takeIf { it.exists() && it.length() > 0L }
                ?.let { preview to it }
        }
        ?: emptyList()

    val oldPreviewFile = previewFileName
        ?.let { File(folder, it) }
        ?.takeIf { it.exists() && it.length() > 0L }

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

    val effectiveId = pictureId?.takeIf { it.isNotBlank() }
        ?: picture.id?.takeIf { it.isNotBlank() }
        ?: picture.extractAnchorId()

    return picture.copy(
        id = effectiveId,
        url = pictureUrl ?: picture.url,
        url_to_original = displayMediaFile.absolutePath,
        url_to_video = if (picture.is_animated) mediaFile?.absolutePath else null,
        album = albumId ?: picture.album,
        thumbnails = thumbnails
    )
}

/** Псевдоним для чтения метаданных элемента коллекции [readLSavedLikeMetadata]. */
fun readCollectionMetadata(file: File): LSavedLikeMetadata? {
    return readLSavedLikeMetadata(file)
}

/** Псевдоним для записи метаданных элемента коллекции [writeLSavedLikeMetadata]. */
fun writeCollectionMetadata(file: File, metadata: LSavedLikeMetadata) {
    writeLSavedLikeMetadata(file, metadata)
}
