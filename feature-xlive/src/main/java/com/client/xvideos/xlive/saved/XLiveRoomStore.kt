package com.client.xvideos.xlive.saved

import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.xlive.model.XLiveSavedRoom
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Список комнат на диске — «Мои» или «История» раздела XLive.
 *
 * Запись — файл на комнату в [dirPath] (FileDB, как сохранённое X), ключ —
 * ник. Порядок — от новых к старым по [XLiveSavedRoom.savedAt].
 *
 * Все операции идут по одной и в порядке вызова на [diskDispatcher]: и файл,
 * и список в памяти меняются в одной очереди. Иначе первое чтение диска,
 * закончившись после сохранения, затирало бы новую комнату в списке.
 *
 * @param limit сколько комнат держать; лишние, самые старые, удаляются.
 * @param diskDispatcher очередь файловых операций — обязательно последовательная.
 * @param now часы для [XLiveSavedRoom.savedAt]; подменяются в тестах.
 */
class XLiveRoomStore(
    dirPath: String,
    private val scope: CoroutineScope,
    private val limit: Int = Int.MAX_VALUE,
    private val diskDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
    private val now: () -> Long = System::currentTimeMillis,
) {

    private val db = FileDB(dirPath, EXTENSION, XLiveSavedRoom.serializer())

    private val _rooms = MutableStateFlow<ImmutableList<XLiveSavedRoom>>(persistentListOf())

    /** Комнаты от новых к старым. */
    val rooms: StateFlow<ImmutableList<XLiveSavedRoom>> = _rooms.asStateFlow()

    init {
        refresh()
    }

    operator fun contains(username: String): Boolean = _rooms.value.any { it.username == username }

    /**
     * Комната [room] — первой, со временем «сейчас». Возраст, которого в [room]
     * нет, остаётся от прежней записи: история пишется при открытии комнаты,
     * до анкеты.
     */
    fun put(room: XLiveSavedRoom) {
        if (room.username.isBlank()) return
        scope.launch(diskDispatcher) {
            val current = _rooms.value
            val old = current.firstOrNull { it.username == room.username }
            val entry = room.copy(age = room.age ?: old?.age, savedAt = now())
            val next = listOf(entry) + current.filter { it.username != room.username }
            db.insert(entry.username, entry).onFailure { Timber.w("XLive: комната не сохранена — %s", it.javaClass.simpleName) }
            next.drop(limit).forEach { db.delete(it.username) }
            _rooms.value = next.take(limit).toImmutableList()
        }
    }

    fun remove(username: String) {
        scope.launch(diskDispatcher) {
            db.delete(username)
            _rooms.value = _rooms.value.filter { it.username != username }.toImmutableList()
        }
    }

    /** Список с диска заново — при создании и после восстановления резервной копии. */
    fun refresh() {
        scope.launch(diskDispatcher) {
            val loaded = db.getAllKeys().mapNotNull(db::readOrNull).sortedByDescending { it.savedAt }
            loaded.drop(limit).forEach { db.delete(it.username) }
            _rooms.value = loaded.take(limit).toImmutableList()
        }
    }

    private companion object {
        const val EXTENSION = "room"
    }
}
