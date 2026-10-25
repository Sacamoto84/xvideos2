package com.client.xvideos.xlive.saved

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.io.File

/**
 * Хранилище во временной папке [dir]: файловые операции идут прямо в потоке
 * вызова, поэтому после `put` и `remove` и список, и диск уже готовы.
 */
fun testRoomStore(dir: File, limit: Int = Int.MAX_VALUE, now: () -> Long = { 0L }) =
    XLiveRoomStore(dir.path, CoroutineScope(Job()), limit, Dispatchers.Unconfined, now)
