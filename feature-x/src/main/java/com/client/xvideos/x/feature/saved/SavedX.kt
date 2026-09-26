package com.client.xvideos.x.feature.saved

import androidx.compose.runtime.Stable
import com.client.xvideos.common.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Корневой фасад доступа к локальным данным раздела X (Избранное, Загрузки, История).
 *
 * Предоставляет инжектируемые хранилища:
 * - [favorites]: закладки и избранные ролики ([SavedX_Favorites]).
 * - [downloads]: сохраненные локально MP4-видео с обложками и метаданными ([SavedX_Downloads]).
 * - [history]: история просмотров с сохранением позиции воспроизведения роликов ([SavedX_History]).
 *
 * @property scope Долгоживущий CoroutineScope приложения ([ApplicationScope]) для выполнения фоновых операций записи.
 */
@Stable
@Singleton
class SavedX @Inject constructor(
    //val db: AppLDatabase,
    @ApplicationScope val scope: CoroutineScope,
    //kDownloader: KDownloader,
) {

    /** Коллекция избранных видеороликов X. */
    val favorites = SavedX_Favorites(scope)

    /** «Сохранённое» — загрузки видео в наилучшем качестве (зелёный прогресс + снекбар). */
    val downloads = SavedX_Downloads(scope)

    /** «История просмотров» с сохранением позиции воспроизведения и ротацией до 200 записей. */
    val history = SavedX_History(scope)

    //val collection = SavedL_Collection(snackBarEvent)

    //val albums = SavedL_Albums(snackBarEvent, db, scope)
    //val likes = SavedL_Likes(snackBarEvent, kDownloader)
}
