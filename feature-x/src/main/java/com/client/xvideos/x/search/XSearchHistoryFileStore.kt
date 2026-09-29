package com.client.xvideos.x.search

import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.common.fileDB.folder.FileSearchHistoryStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Файловое хранилище истории поисковых запросов в разделе X.
 *
 * Сохраняет до 30 последних уникальных поисковых фраз на устройстве.
 *
 * @param db База данных файловых таблиц приложения.
 * @param scope Скоп приложения для фоновой инициализации.
 */
@Singleton
class XSearchHistoryFileStore @Inject constructor(
    db: AppFileDatabase,
    @com.client.xvideos.common.di.ApplicationScope scope: CoroutineScope
) : FileSearchHistoryStore(db.xSearchHistoryTable) {
    init {
        scope.launch { refresh() }
    }
}
