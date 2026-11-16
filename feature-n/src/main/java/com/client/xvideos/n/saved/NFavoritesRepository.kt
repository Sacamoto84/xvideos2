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
        return db.contains(id.toString())
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
     * Принудительно перечитывает закладки с диска (например, после восстановления бэкапа).
     */
    fun refresh() {
        if (AppPath.isInitialized) {
            db.refresh()
        }
    }
}
