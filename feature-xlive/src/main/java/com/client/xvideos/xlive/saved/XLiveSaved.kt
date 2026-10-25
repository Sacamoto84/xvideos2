package com.client.xvideos.xlive.saved

import com.client.xvideos.common.AppPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Сохранённое раздела XLive на процесс: «Мои» и «История».
 *
 * Объект, а не Hilt-синглтон: экраны раздела заводят ScreenModel через
 * rememberScreenModel, и графа зависимостей у раздела нет. Хранилища
 * создаются при первом обращении — диск не читается, пока раздел не открыли.
 */
object XLiveSaved {

    /** Сколько недавно открытых комнат помнит «История». */
    const val HISTORY_LIMIT = 100

    private val scope = CoroutineScope(SupervisorJob())

    private val myStore = lazy { XLiveRoomStore(AppPath.xlive_saved, scope) }
    private val historyStore = lazy { XLiveRoomStore(AppPath.xlive_history, scope, limit = HISTORY_LIMIT) }

    /** «Мои» — комнаты, сохранённые кнопкой в шапке комнаты. */
    val my: XLiveRoomStore by myStore

    /** «История» — недавно открытые комнаты. */
    val history: XLiveRoomStore by historyStore

    /**
     * Перечитывает с диска то, что уже открыто. Восстановление резервной копии
     * меняет файлы мимо хранилищ, а они читают диск один раз, при создании.
     */
    fun refreshAll() {
        if (myStore.isInitialized()) my.refresh()
        if (historyStore.isInitialized()) history.refresh()
    }
}
