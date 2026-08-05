package com.client.xvideos.l.featured.saved

import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.l.net.Luscious
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedL @Inject constructor(
    val db: AppFileDatabase,
    @ApplicationScope val scope: CoroutineScope,
    luscious: Luscious
) {

    val collection = SavedL_Collection(scope, luscious)

    val albums = SavedL_Albums(db, scope)
    val likes = SavedL_Likes(luscious, scope)

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
