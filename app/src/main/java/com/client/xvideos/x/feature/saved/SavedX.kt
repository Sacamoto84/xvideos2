package com.client.xvideos.x.feature.saved

import com.client.xvideos.common.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedX @Inject constructor(
    //val db: AppLDatabase,
    @ApplicationScope val scope: CoroutineScope,
    //kDownloader: KDownloader,
) {

    val favorites = SavedX_Favorites(scope)

    //val collection = SavedL_Collection(snackBarEvent)

    //val albums = SavedL_Albums(snackBarEvent, db, scope)
    //val likes = SavedL_Likes(snackBarEvent, kDownloader)
}

