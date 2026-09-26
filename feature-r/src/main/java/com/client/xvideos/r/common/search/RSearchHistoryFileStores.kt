package com.client.xvideos.r.common.search

import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.common.fileDB.folder.FileSearchHistoryStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Файловое хранилище истории поисковых запросов в разделе Explorer RedGifs.
 *
 * @param db База данных файловых таблиц приложения.
 * @param scope Скоп приложения для фоновой инициализации.
 */
@Singleton
class RSearchHistoryExplorerFileStore @Inject constructor(
    db: AppFileDatabase,
    @com.client.xvideos.common.di.ApplicationScope scope: CoroutineScope
) : FileSearchHistoryStore(db.rSearchHistoryExplorerTable), IDaoSearchTemplate {
    init {
        scope.launch { refresh() }
    }
}

/**
 * Файловое хранилище истории поисковых запросов в каталоге ниш RedGifs.
 *
 * @param db База данных файловых таблиц приложения.
 * @param scope Скоп приложения для фоновой инициализации.
 */
@Singleton
class RSearchHistoryNichesFileStore @Inject constructor(
    db: AppFileDatabase,
    @com.client.xvideos.common.di.ApplicationScope scope: CoroutineScope
) : FileSearchHistoryStore(db.rSearchHistoryNichesTable), IDaoSearchTemplate {
    init {
        scope.launch { refresh() }
    }
}
