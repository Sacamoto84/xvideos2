package com.client.xvideos.r.common.saved

import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.r.network.api.RedApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Главный фасад сохраненного пользовательского контента в модуле RedGifs.
 *
 * Инкапсулирует доступ к локальным хранилищам:
 * - Лайков [likes] ([R_Saved_Likes]);
 * - Избранных авторов [creators] ([R_Saved_Creator]);
 * - Избранных ниш [niches] ([R_Saved_Niches]);
 * - Пользовательских коллекций [collections] ([R_Saved_Collection]);
 * - Подписок на авторов [subscriptions] ([R_Saved_Subscriptions]);
 * - Локального кэша полного списка ниш [nichesCache] ([R_Saved_NichesCaches]).
 *
 * Синглтон: инициализирует фоновую загрузку коллекций и проверку свежести кэша ниш при старте приложения.
 *
 * @property redApi Сетевой API RedGifs.
 * @property scope Корутин-скоп уровня приложения.
 */
@Singleton
class SavedRed @Inject constructor(
    val redApi: RedApi,
    @ApplicationScope val scope : CoroutineScope
) {

    /////////////////////////////////////////////////////////////////////////////////////////////
    /** Хранилище лайков гифок. */
    val likes       = R_Saved_Likes(scope)
    /** Хранилище сохраненных создателей. */
    val creators    = R_Saved_Creator(scope)
    /** Хранилище сохраненных ниш. */
    val niches      = R_Saved_Niches(scope)
    /** Хранилище именованных коллекций пользователя. */
    val collections = R_Saved_Collection(scope)

    /** Менеджер подписок на авторов и агрегации их контента. */
    val subscriptions = R_Saved_Subscriptions(scope, redApi)

    /** Дисковый кэш каталога ниш с периодическим обновлением. */
    val nichesCache = R_Saved_NichesCaches(scope, redApi)

    /**
     * Общее суммарное количество сохраненных элементов во всех разделах
     * (лайки + авторы + ниши + коллекции).
     */
    val totalSavedCount: Int
        get() = likes.list.size + creators.list.size + niches.list.size + collections.collectionList.size



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
        likes.refresh()
        niches.refresh()
        creators.refresh()
        collections.refreshCollectionList()
        subscriptions.refresh()
    }

    /////////////////////////////////////////////////////////////////////////////////////////////

}
