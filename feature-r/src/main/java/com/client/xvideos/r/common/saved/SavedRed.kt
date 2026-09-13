package com.client.xvideos.r.common.saved

import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.r.model.tag.TagInfo
import com.client.xvideos.r.network.api.RedApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedRed @Inject constructor(
    val redApi: RedApi,
    @ApplicationScope val scope : CoroutineScope
) {

    /////////////////////////////////////////////////////////////////////////////////////////////
    val likes       = R_Saved_Likes(scope)
    val creators    = R_Saved_Creator(scope)
    val niches      = R_Saved_Niches(scope)
    val collections = R_Saved_Collection(scope)

    val subscriptions = R_Saved_Subscriptions(scope, redApi)

    val nichesCache = R_Saved_NichesCaches(scope, redApi)


    // Пишется из корутины на Dispatchers.IO (refreshTagList), читается с
    // главного — без @Volatile у читателя нет гарантии увидеть свежую запись.
    @Volatile
    var tagsList = listOf<TagInfo>()

    fun refreshTagList() { scope.launch(Dispatchers.IO) { tagsList =
        redApi.tags.getTags().getOrNull()?.tags ?: emptyList()
    } }

    init {
        // refreshCollectionList — синхронный обход каталога коллекций с
        // Gson-разбором каждого файла. Из конструктора Hilt-синглтона, который
        // MainActivity внедряет в onCreate, этот обход шёл на главном потоке
        // на пути запуска (проход 8, T2). X и L свою начальную загрузку уже
        // делают асинхронно; список публикуется Compose-состоянием, экраны
        // дождутся его и без блокировки старта.
        scope.launch(Dispatchers.IO) {
            collections.refreshCollectionList()
            nichesCache.refreshIfStale()
        }
    }

    /**
     * Перечитывает всё сохранённое R с диска.
     *
     * `SavedRed` — синглтон, и списки лайков, авторов, ниш и коллекций живут в
     * памяти: их читают один раз на старте, из `SplashActivity.initApp()`. Если
     * файлы под ними поменялись мимо приложения — а восстановление из бэкапа
     * делает ровно это, — в памяти остаётся прежнее. Отсюда и «в R пусто, пока
     * не перезапустишь»: X и L свои экраны перечитывают при входе, R нет.
     *
     * Блокировки (`BlockRed`) сюда не входят — они отдельный синглтон, и
     * обновляет их вызывающий.
     */
    fun refreshAll() {
        refreshTagList()
        likes.refresh()
        niches.refresh()
        creators.refresh()
        collections.refreshCollectionList()
        subscriptions.refresh()
    }

    /////////////////////////////////////////////////////////////////////////////////////////////

}



