package com.client.xvideos.r.common.search

import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.common.fileDB.folder.FileSearchHistoryStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RSearchHistoryExplorerFileStore @Inject constructor(
    db: AppFileDatabase,
    @com.client.xvideos.common.di.ApplicationScope scope: CoroutineScope
) : FileSearchHistoryStore(db.rSearchHistoryExplorerTable), IDaoSearchTemplate {
    init {
        scope.launch { refresh() }
    }
}

@Singleton
class RSearchHistoryNichesFileStore @Inject constructor(
    db: AppFileDatabase,
    @com.client.xvideos.common.di.ApplicationScope scope: CoroutineScope
) : FileSearchHistoryStore(db.rSearchHistoryNichesTable), IDaoSearchTemplate {
    init {
        scope.launch { refresh() }
    }
}
