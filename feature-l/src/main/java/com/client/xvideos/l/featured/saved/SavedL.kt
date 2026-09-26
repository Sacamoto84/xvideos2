package com.client.xvideos.l.featured.saved

import androidx.compose.runtime.Stable
import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.l.net.Luscious
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Единая точка доступа и фасад над локальными хранилищами модуля Luscious.
 *
 * Объединяет менеджеры:
 * - [collection] — пользовательские коллекции изображений и видео;
 * - [albums] — сохраненные альбомы в файловой базе данных;
 * - [likes] — сохраненные понравившиеся медиа-элементы.
 *
 * @property db Локальная файловая база данных приложения.
 * @property scope Долгоживущая область корутин уровня приложения.
 */
@Stable
@Singleton
class SavedL @Inject constructor(
    val db: AppFileDatabase,
    @ApplicationScope val scope: CoroutineScope,
    luscious: Luscious
) {

    /** Менеджер сохраненных пользовательских коллекций Luscious. */
    val collection = SavedL_Collection(scope, luscious)

    /** Менеджер сохраненных альбомов в формате FileDB. */
    val albums = SavedL_Albums(db, scope)

    /** Менеджер сохраненных лайков медиа Luscious. */
    val likes = SavedL_Likes(luscious, scope)

    /**
     * Запускает фоновую процедуру восстановления и докачки поврежденных или неполных сохраненных файлов.
     *
     * @param onEvent Обратный вызов для текстовых уведомлений о ходе процесса.
     * @param onComplete Обратный вызов по завершении со сводным отчетом [LDownloadRecoveryReport].
     */
    fun recoverIncompleteSavedMedia(
        onEvent: (String) -> Unit = {},
        onComplete: (LDownloadRecoveryReport) -> Unit = {}
    ) {
        scope.launch(Dispatchers.IO) {
            val report = lRecoverIncompleteSavedMedia { message ->
                scope.launch(Dispatchers.Main) { onEvent(message) }
            }
            withContext(Dispatchers.Main) {
                likes.refresh()
                collection.refreshCollectionList()
                collection.refresh()
                onComplete(report)
            }
        }
    }
}
