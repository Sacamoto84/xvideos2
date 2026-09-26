package com.client.xvideos.l.featured.saved

import com.client.xvideos.common.io.isUnsafeItemName
import com.client.xvideos.common.io.writeTextAtomically
import com.client.xvideos.common.json.AppJson
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.isLVideoFileUrl
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File

/** Имя файла конфигурации коллекции внутри её директории. */
private const val L_COLLECTION_CONFIG_FILE_NAME = "collection.json"

/**
 * Варианты сортировки локальных пользовательских коллекций Luscious.
 *
 * @property title Человекочитаемое отображаемое название порядка сортировки.
 */
enum class LCollectionSortOrder(val title: String) {
    /** Сортировка по времени последней модификации (сначала новые). */
    RECENT("Сначала новые"),
    /** Алфавитная сортировка по имени коллекции. */
    NAME("По названию"),
    /** Сортировка по количеству элементов в коллекции (по убыванию). */
    SIZE("Больше элементов"),
}

/**
 * Группа обнаруженных дубликатов картинок/медиа внутри одной коллекции.
 *
 * @property key Идентификационный ключ дубликата (на основе URL или атрибутов медиа).
 * @property items Список повторяющихся элементов [PicsDetails].
 */
data class LCollectionDuplicateGroup(
    val key: String,
    val items: List<PicsDetails>
)

/**
 * Служебная конфигурация отдельной коллекции Luscious.
 *
 * @property schemaVersion Версия схемы конфигурации.
 * @property coverFolderName Относительное имя подпапки элемента, выбранного в качестве обложки.
 */
@Serializable
internal data class LCollectionConfig(
    val schemaVersion: Int = 1,
    val coverFolderName: String? = null
)

/**
 * Модель сущности коллекции Luscious: имя, превью, количество элементов и метаданные.
 *
 * @property collection Название коллекции (имя директории).
 * @property previewUrl Локальный путь к изображению-обложке коллекции (или null, если отсутствует).
 * @property itemsCount Количество валидных элементов (папок с `metadata.json`).
 * @property lastModifiedAt Метка времени последнего изменения коллекции или её элементов.
 * @property duplicateCount Число найденных дублирующихся элементов.
 * @property hasManualCover Флаг того, что обложка была установлена пользователем вручную.
 */
data class LCollectionEntity(
    val collection: String,
    val previewUrl: String?,
    val itemsCount: Int,
    val lastModifiedAt: Long,
    val duplicateCount: Int,
    val hasManualCover: Boolean
) {
    val name: String get() = collection
    val isEmpty: Boolean get() = itemsCount == 0
    val isNotEmpty: Boolean get() = itemsCount > 0
    val hasDuplicates: Boolean get() = duplicateCount > 0
    val hasPreview: Boolean get() = !previewUrl.isNullOrBlank()
}

/**
 * Читает список коллекций из корня [collectionsRoot]. Каждая коллекция — это
 * директория первого уровня. Превью берётся из метаданных первого подходящего
 * элемента, размер — это число элементов с валидным `metadata.json`.
 *
 * @param collectionsRoot Корневая папка с коллекциями (`AppPath.l_collection`).
 * @param sortOrder Желаемый порядок сортировки [LCollectionSortOrder].
 * @return Отсортированный список сущностей [LCollectionEntity].
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
 *
 * @param collectionFolder Директория целевой коллекции.
 * @return Список моделей [PicsDetails], восстановленных из локальных файлов метаданных.
 */
internal fun lReadCollectionItems(collectionFolder: File): List<PicsDetails> {
    return lReadStoredCollectionItems(collectionFolder)
        .sortedByDescending { it.first.savedAt }
        .mapNotNull { (metadata, folder) -> metadata.toPicsDetails(folder) }
}

/**
 * Читает пары (метаданные, директория) для всех сохраненных элементов коллекции.
 *
 * @param collectionFolder Папка коллекции.
 * @return Список пар [LSavedLikeMetadata] и [File] подпапки элемента.
 */
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
 *
 * @param collectionFolder Папка коллекции.
 * @return Абсолютный путь к файлу изображения превью или `null`.
 */
private fun lResolveCollectionPreviewUrl(collectionFolder: File): String? {
    val config = lReadCollectionConfig(collectionFolder)
    config.coverFolderName
        ?.takeIf { it.isNotBlank() && !isUnsafeItemName(it) }
        ?.let { File(collectionFolder, it) }
        ?.takeIf { it.exists() && it.isDirectory && lIsInside(collectionFolder, it) }
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
                    !it.absolutePath.isLVideoFileUrl() &&
                    it.length() > 0L
            }
        if (fallback != null) {
            return fallback.absolutePath
        }
    }
    return null
}

/**
 * Определяет локальный файл наилучшего доступного превью для сохраненного элемента.
 *
 * @param folder Папка элемента.
 * @param metadata Метаданные сохраненного элемента.
 * @return Абсолютный путь к файлу превью или медиафайлу, если превью не найдено.
 */
private fun lResolveItemPreviewUrl(folder: File, metadata: LSavedLikeMetadata): String? {
    metadata.previewFiles
        ?.sortedByDescending { it.width * it.height }
        ?.forEach { preview ->
            val candidate = File(folder, preview.fileName)
            if (candidate.exists() && candidate.length() > 0L && !candidate.absolutePath.isLVideoFileUrl()) {
                return candidate.absolutePath
            }
        }

    metadata.previewFileName?.let { previewFileName ->
        val candidate = File(folder, previewFileName)
        if (candidate.exists() && candidate.length() > 0L && !candidate.absolutePath.isLVideoFileUrl()) {
            return candidate.absolutePath
        }
    }

    val mediaFile = File(folder, metadata.mediaFileName)
    if (mediaFile.exists() && mediaFile.length() > 0L && !mediaFile.absolutePath.isLVideoFileUrl()) {
        return mediaFile.absolutePath
    }

    return null
}

/**
 * Подсчитывает число валидных элементов в коллекции (папок, содержащих непустой `metadata.json`).
 */
private fun lResolveCollectionItemsCount(collectionFolder: File): Int {
    return collectionFolder.listFiles()
        ?.count { it.isDirectory && File(it, L_METADATA_FILE_NAME).let { meta -> meta.exists() && meta.length() > 0L } }
        ?: 0
}

/**
 * Вычисляет время последнего изменения коллекции как максимум среди дат папок элементов.
 */
private fun lResolveCollectionLastModified(collectionFolder: File): Long {
    val newestItem = collectionFolder.listFiles()
        ?.filter { it.isDirectory }
        ?.maxOfOrNull { it.lastModified() }
    return newestItem ?: collectionFolder.lastModified()
}

/**
 * Считывает конфигурационный файл `collection.json` из папки коллекции [collectionFolder].
 * При отсутствии файла или ошибке разбора возвращает конфигурацию по умолчанию [LCollectionConfig].
 */
internal fun lReadCollectionConfig(collectionFolder: File): LCollectionConfig {
    val file = File(collectionFolder, L_COLLECTION_CONFIG_FILE_NAME)
    if (!file.exists()) return LCollectionConfig()
    return runCatching {
        AppJson.decodeFromString<LCollectionConfig>(file.readText(Charsets.UTF_8))
    }.getOrDefault(LCollectionConfig())
}

/**
 * Атомарно записывает конфигурационный файл `collection.json` в папку коллекции [collectionFolder].
 */
internal fun lWriteCollectionConfig(collectionFolder: File, config: LCollectionConfig) {
    collectionFolder.mkdirs()
    File(collectionFolder, L_COLLECTION_CONFIG_FILE_NAME)
        .writeTextAtomically(AppJson.encodeToString(config))
}

/**
 * Извлекает список всех возможных сетевых идентификаторов (URL оригиналов, видео, превью) из [PicsDetails].
 */
internal fun lCollectionItemIdentifiers(item: PicsDetails): List<String> {
    return listOfNotNull(
        item.url_to_original,
        item.url_to_video,
        item.thumbnails?.firstOrNull { !it.url.isNullOrBlank() }?.url
    ) + (item.thumbnails?.mapNotNull { it.url } ?: emptyList())
}

/**
 * Формирует уникальный ключ идентичности элемента [PicsDetails] на основе URL без параметров
 * либо комбинации характеристик (альбом, размеры, анимация).
 */
internal fun lPicsDetailsIdentityKey(item: PicsDetails): String {
    return lCollectionItemIdentifiers(item)
        .firstOrNull { it.isNotBlank() }
        ?.substringBefore('?')
        ?.substringBefore('#')
        ?: "${item.album.orEmpty()}-${item.width}-${item.height}-${item.is_animated}"
}

/**
 * Формирует уникальный ключ идентичности сохраненного элемента на основе метаданных [LSavedLikeMetadata].
 */
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

/**
 * Находит и группирует повторяющиеся элементы в коллекции в список [LCollectionDuplicateGroup].
 */
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

/**
 * Находит группы папок дубликатов в коллекции для последующей очистки.
 */
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
 *
 * @param root Корневая папка коллекции.
 * @param identifiers Список кандидатов-идентификаторов (URL, локальные пути).
 * @return Папка [File] найденного элемента или `null`.
 */
internal fun lFindCollectionItemFolder(root: File, identifiers: List<String>): File? {
    val normalizedIdentifiers = identifiers
        .filter { it.isNotBlank() }
        .flatMap { listOf(it, it.lToFilePath()) }
        .toSet()

    if (normalizedIdentifiers.isEmpty()) return null

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
 *
 * @param root Корневая папка лайков (`AppPath.l_likes`).
 * @param url Локальный путь или сетевой URL элемента.
 * @return Папка [File] найденного лайка или `null`.
 */
internal fun lFindLikeFolder(root: File, url: String): File? {
    val trimmed = url.trim()
    if (trimmed.isBlank()) return null

    if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
        val target = File(trimmed)
        if (lIsInside(root, target)) {
            val parent = target.parentFile
            if (parent != null && File(parent, L_METADATA_FILE_NAME).exists()) return parent
        }
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
            trimmed == mediaPath ||
                    trimmed == previewPath ||
                    trimmed in previewPaths ||
                    trimmed == metadata.sourceMediaUrl ||
                    trimmed == metadata.sourceOriginalUrl ||
                    trimmed == metadata.sourceVideoUrl ||
                    trimmed == metadata.sourcePreviewUrl ||
                    metadata.previewFiles?.any { it.sourceUrl == trimmed } == true
        }
}
