package com.client.xvideos.l.featured.saved

import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.isLVideoFileUrl
import com.google.gson.GsonBuilder
import java.io.File

private const val L_COLLECTION_CONFIG_FILE_NAME = "collection.json"

enum class LCollectionSortOrder(val title: String) {
    RECENT("Сначала новые"),
    NAME("По названию"),
    SIZE("Больше элементов"),
}

data class LCollectionDuplicateGroup(
    val key: String,
    val items: List<PicsDetails>
)

internal data class LCollectionConfig(
    val schemaVersion: Int = 1,
    val coverFolderName: String? = null
)

/**
 * Список коллекций L: имя, превью (если найдено) и количество элементов.
 */
data class LCollectionEntity(
    val collection: String,
    val previewUrl: String?,
    val itemsCount: Int,
    val lastModifiedAt: Long,
    val duplicateCount: Int,
    val hasManualCover: Boolean
)

/**
 * Читает список коллекций из корня [collectionsRoot]. Каждая коллекция — это
 * директория первого уровня. Превью берётся из метаданных первого подходящего
 * элемента, размер — это число элементов с валидным `metadata.json`.
 */
internal fun lReadCollections(
    collectionsRoot: File,
    sortOrder: LCollectionSortOrder = LCollectionSortOrder.RECENT
): List<LCollectionEntity> {
    collectionsRoot.mkdirs()
    val collections = collectionsRoot.listFiles()
        ?.filter { it.isDirectory }
        ?.map { folder ->
            LCollectionEntity(
                collection = folder.name,
                previewUrl = lResolveCollectionPreviewUrl(folder),
                itemsCount = lResolveCollectionItemsCount(folder),
                lastModifiedAt = lResolveCollectionLastModified(folder),
                duplicateCount = lFindCollectionDuplicateFolders(folder).sumOf { it.size - 1 },
                hasManualCover = lReadCollectionConfig(folder).coverFolderName != null
            )
        }
        ?: emptyList()

    return when (sortOrder) {
        LCollectionSortOrder.RECENT -> collections.sortedByDescending { it.lastModifiedAt }
        LCollectionSortOrder.NAME -> collections.sortedBy { it.collection.lowercase() }
        LCollectionSortOrder.SIZE -> collections.sortedWith(
            compareByDescending<LCollectionEntity> { it.itemsCount }
                .thenBy { it.collection.lowercase() }
        )

    }
}

/**
 * Читает все элементы одной коллекции в виде [PicsDetails], отсортированных по
 * убыванию даты сохранения.
 */
internal fun lReadCollectionItems(collectionFolder: File): List<PicsDetails> {
    return lReadStoredCollectionItems(collectionFolder)
        .sortedByDescending { it.first.savedAt }
        .mapNotNull { (metadata, folder) -> metadata.toPicsDetails(folder) }
}

internal fun lReadStoredCollectionItems(collectionFolder: File): List<Pair<LSavedLikeMetadata, File>> {
    collectionFolder.mkdirs()
    return collectionFolder.listFiles()
        ?.filter { it.isDirectory }
        ?.mapNotNull { folder ->
            val metadata = readCollectionMetadata(File(folder, L_METADATA_FILE_NAME))
            if (metadata != null) metadata to folder else null
        }
        ?: emptyList()
}

/**
 * Находит локальный путь к превью первой коллекции. Возвращает локальный путь
 * (не URL), либо `null`, если ни в одном элементе нет валидного изображения.
 */
private fun lResolveCollectionPreviewUrl(collectionFolder: File): String? {
    val config = lReadCollectionConfig(collectionFolder)
    config.coverFolderName
        ?.takeIf { it.isNotBlank() }
        ?.let { File(collectionFolder, it) }
        ?.takeIf { it.exists() && it.isDirectory }
        ?.let { coverFolder ->
            val metadata = readCollectionMetadata(File(coverFolder, L_METADATA_FILE_NAME))
            if (metadata != null) {
                lResolveItemPreviewUrl(coverFolder, metadata)?.let { return it }
            }
        }

    val itemFolders = collectionFolder.listFiles()
        ?.filter { it.isDirectory }
        ?.sortedByDescending { it.lastModified() }
        ?: return null

    for (folder in itemFolders) {
        val metadata = readCollectionMetadata(File(folder, L_METADATA_FILE_NAME))
        if (metadata != null) {
            lResolveItemPreviewUrl(folder, metadata)?.let { return it }
        }

        // .part не фильтровать нельзя: недокачанный файл от убитого процесса
        // иначе становится обложкой коллекции.
        val fallback = folder.listFiles()
            ?.firstOrNull {
                it.isFile &&
                    it.name != L_METADATA_FILE_NAME &&
                    !it.isPartialDownload() &&
                    !it.absolutePath.isLVideoFileUrl()
            }
        if (fallback != null) {
            return fallback.absolutePath
        }
    }
    return null
}

private fun lResolveItemPreviewUrl(folder: File, metadata: LSavedLikeMetadata): String? {
    metadata.previewFiles
        ?.sortedByDescending { it.width * it.height }
        ?.forEach { preview ->
            val candidate = File(folder, preview.fileName)
            if (candidate.exists() && !candidate.absolutePath.isLVideoFileUrl()) {
                return candidate.absolutePath
            }
        }

    metadata.previewFileName?.let { previewFileName ->
        val candidate = File(folder, previewFileName)
        if (candidate.exists() && !candidate.absolutePath.isLVideoFileUrl()) {
            return candidate.absolutePath
        }
    }

    val mediaFile = File(folder, metadata.mediaFileName)
    if (mediaFile.exists() && !mediaFile.absolutePath.isLVideoFileUrl()) {
        return mediaFile.absolutePath
    }

    return null
}

private fun lResolveCollectionItemsCount(collectionFolder: File): Int {
    return collectionFolder.listFiles()
        ?.count { it.isDirectory && File(it, L_METADATA_FILE_NAME).exists() }
        ?: 0
}

private fun lResolveCollectionLastModified(collectionFolder: File): Long {
    val newestItem = collectionFolder.listFiles()
        ?.filter { it.isDirectory }
        ?.maxOfOrNull { it.lastModified() }
    return newestItem ?: collectionFolder.lastModified()
}

private val lCollectionConfigGson = GsonBuilder().setPrettyPrinting().create()

internal fun lReadCollectionConfig(collectionFolder: File): LCollectionConfig {
    val file = File(collectionFolder, L_COLLECTION_CONFIG_FILE_NAME)
    if (!file.exists()) return LCollectionConfig()
    return runCatching {
        lCollectionConfigGson.fromJson(file.readText(Charsets.UTF_8), LCollectionConfig::class.java)
            ?: LCollectionConfig()
    }.getOrDefault(LCollectionConfig())
}

internal fun lWriteCollectionConfig(collectionFolder: File, config: LCollectionConfig) {
    collectionFolder.mkdirs()
    File(collectionFolder, L_COLLECTION_CONFIG_FILE_NAME)
        .writeText(lCollectionConfigGson.toJson(config), Charsets.UTF_8)
}

internal fun lCollectionItemIdentifiers(item: PicsDetails): List<String> {
    return listOfNotNull(
        item.url_to_original,
        item.url_to_video,
        item.thumbnails?.firstOrNull { !it.url.isNullOrBlank() }?.url
    ) + (item.thumbnails?.mapNotNull { it.url } ?: emptyList())
}

internal fun lPicsDetailsIdentityKey(item: PicsDetails): String {
    return lCollectionItemIdentifiers(item)
        .firstOrNull { it.isNotBlank() }
        ?.substringBefore('?')
        ?.substringBefore('#')
        ?: "${item.album.orEmpty()}-${item.width}-${item.height}-${item.is_animated}"
}

internal fun lMetadataIdentityKey(metadata: LSavedLikeMetadata): String {
    return listOfNotNull(
        metadata.sourceOriginalUrl,
        metadata.sourceVideoUrl,
        metadata.sourceMediaUrl,
        metadata.picture.url_to_original,
        metadata.picture.url_to_video
    )
        .firstOrNull { it.isNotBlank() }
        ?.substringBefore('?')
        ?.substringBefore('#')
        ?: "${metadata.albumId.orEmpty()}-${metadata.picture.width}-${metadata.picture.height}-${metadata.picture.is_animated}"
}

internal fun lReadCollectionDuplicateGroups(collectionFolder: File): List<LCollectionDuplicateGroup> {
    return lReadStoredCollectionItems(collectionFolder)
        .groupBy { (metadata, _) -> lMetadataIdentityKey(metadata) }
        .filterValues { it.size > 1 }
        .mapNotNull { (key, items) ->
            val pics = items
                .sortedByDescending { (metadata, _) -> metadata.savedAt }
                .mapNotNull { (metadata, folder) -> metadata.toPicsDetails(folder) }
            if (pics.size > 1) LCollectionDuplicateGroup(key, pics) else null
        }
        .sortedByDescending { it.items.size }
}

internal fun lFindCollectionDuplicateFolders(collectionFolder: File): List<List<Pair<LSavedLikeMetadata, File>>> {
    return lReadStoredCollectionItems(collectionFolder)
        .groupBy { (metadata, _) -> lMetadataIdentityKey(metadata) }
        .values
        .filter { it.size > 1 }
        .map { it.sortedByDescending { (metadata, _) -> metadata.savedAt } }
}

/**
 * Ищет папку элемента коллекции по списку идентификаторов. Идентификатором
 * может быть локальный путь или один из исходных URL'ов. Сначала проверяется
 * прямое попадание идентификатора в [root] (быстрый путь), затем —
 * содержимое `metadata.json` каждой папки (медленный путь).
 */
internal fun lFindCollectionItemFolder(root: File, identifiers: List<String>): File? {
    val normalizedIdentifiers = identifiers
        .filter { it.isNotBlank() }
        .flatMap { listOf(it, it.lToFilePath()) }
        .toSet()

    normalizedIdentifiers.forEach { identifier ->
        val target = File(identifier)
        if (lIsInside(root, target)) {
            val parent = target.parentFile
            if (parent != null && File(parent, L_METADATA_FILE_NAME).exists()) return parent
        }
    }

    return root.listFiles()
        ?.filter { it.isDirectory }
        ?.firstOrNull { folder ->
            val metadata = readCollectionMetadata(File(folder, L_METADATA_FILE_NAME))
                ?: return@firstOrNull false
            val metadataIdentifiers = buildSet {
                add(File(folder, metadata.mediaFileName).absolutePath)
                metadata.previewFileName?.let { add(File(folder, it).absolutePath) }
                metadata.previewFiles?.forEach {
                    add(File(folder, it.fileName).absolutePath)
                    add(it.sourceUrl)
                }
                add(metadata.sourceMediaUrl)
                metadata.sourcePreviewUrl?.let { add(it) }
                metadata.sourceOriginalUrl?.let { add(it) }
                metadata.sourceVideoUrl?.let { add(it) }
                metadata.picture.url_to_original?.let { add(it) }
                metadata.picture.url_to_video?.let { add(it) }
                metadata.picture.thumbnails?.forEach { thumbnail ->
                    thumbnail.url?.let { add(it) }
                }
            }.flatMap { listOf(it, it.lToFilePath()) }.toSet()

            normalizedIdentifiers.any { it in metadataIdentifiers }
        }
}

/**
 * Ищет папку лайка по любому из его идентификаторов
 * (локальный путь к media/preview либо один из исходных URL).
 */
internal fun lFindLikeFolder(root: File, url: String): File? {
    val target = File(url)
    if (lIsInside(root, target)) {
        val parent = target.parentFile
        if (parent != null && File(parent, L_METADATA_FILE_NAME).exists()) return parent
    }

    return root.listFiles()
        ?.filter { it.isDirectory }
        ?.firstOrNull { folder ->
            val metadata = readLSavedLikeMetadata(File(folder, L_METADATA_FILE_NAME))
                ?: return@firstOrNull false
            val mediaPath = File(folder, metadata.mediaFileName).absolutePath
            val previewPath = metadata.previewFileName?.let { File(folder, it).absolutePath }
            val previewPaths = metadata.previewFiles
                ?.map { File(folder, it.fileName).absolutePath }
                ?: emptyList()
            url == mediaPath ||
                    url == previewPath ||
                    url in previewPaths ||
                    url == metadata.sourceMediaUrl ||
                    url == metadata.sourceOriginalUrl ||
                    url == metadata.sourceVideoUrl
        }
}
