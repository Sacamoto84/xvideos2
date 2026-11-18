package com.client.xvideos.n.saved

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NGalleryPreview
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий локально сохранённого (избранного) манги nhentai на базе FileDB.
 */
@Singleton
class NFavoritesRepository @Inject constructor() {

    private val dbPath: String by lazy {
        File(AppPath.n_favorites).apply { mkdirs() }.absolutePath
    }

    private val db: FileDB<NGalleryPreview> by lazy {
        FileDB(dbPath, "manga", NGalleryPreview.serializer()).apply {
            refresh()
        }
    }

    val favorites: SnapshotStateList<NGalleryPreview>
        get() = db.list

    fun isFavorite(id: Long): Boolean {
        if (!AppPath.isInitialized) return false
        return favorites.any { it.id == id } || db.contains(id.toString())
    }

    fun toggleFavorite(gallery: NGallery): Boolean {
        val idStr = gallery.id.toString()
        return if (db.contains(idStr)) {
            db.delete(idStr)
            db.refresh()
            false
        } else {
            val preview = NGalleryPreview(
                id = gallery.id,
                mediaId = gallery.mediaId,
                title = gallery.title,
                thumbnailUrl = gallery.thumbnailUrl,
                numPages = gallery.numPages,
                language = gallery.languages.firstOrNull()?.name.orEmpty()
            )
            db.insert(idStr, preview)
            db.refresh()
            true
        }
    }

    fun removeFavorite(id: Long) {
        db.delete(id.toString())
        db.refresh()
    }

    /**
     * Импортирует одну галерею в локальное избранное.
     * Возвращает true, если элемент добавлен (ранее отсутствовал).
     */
    fun importFavorite(preview: NGalleryPreview): Boolean {
        val idStr = preview.id.toString()
        if (db.contains(idStr)) return false
        val inserted = db.insert(idStr, preview)
        if (inserted.isSuccess) {
            db.refresh()
            return true
        }
        return false
    }

    /**
     * Пакетно импортирует список галерей в локальное избранное.
     * Возвращает количество успешно добавленных новых элементов.
     */
    fun importFavorites(galleries: List<NGalleryPreview>): Int {
        var addedCount = 0
        galleries.forEach { preview ->
            val idStr = preview.id.toString()
            if (!db.contains(idStr)) {
                if (db.insert(idStr, preview).isSuccess) {
                    addedCount++
                }
            }
        }
        if (addedCount > 0) {
            db.refresh()
        }
        return addedCount
    }

    /**
     * Принудительно перечитывает закладки с диска (например, после восстановления бэкапа).
     */
    fun refresh() {
        if (AppPath.isInitialized) {
            db.refresh()
        }
    }
}
