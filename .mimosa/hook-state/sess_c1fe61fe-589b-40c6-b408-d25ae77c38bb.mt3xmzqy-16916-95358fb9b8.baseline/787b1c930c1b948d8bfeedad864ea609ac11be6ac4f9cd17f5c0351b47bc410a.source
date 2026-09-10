package com.client.xvideos.r.ui.ui.lazyrow123

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * Реестр живых лент: полноэкранный просмотр находит по `feedKey` тот же
 * `LazyRow123Host`, из которого его открыли.
 *
 * Записи держатся через [WeakReference], поэтому сам хост уходит с экраном. А
 * вот запись в карте не уходила: удалить её мог только [get] по тому же ключу,
 * а ключ у каждой ленты свой (`feedKey` содержит счётчик `AtomicInteger`), и
 * ключи мёртвых лент никто не запрашивает. Карта росла на запись за каждый
 * открытый экран и не уменьшалась никогда.
 *
 * Чинится подметанием в [register]: там же, где карта пополняется, из неё
 * вычищаются записи с уже собранным референтом. Обход дешёвый — живых лент
 * единицы, а `register` вызывается только при создании экрана.
 */
object RFeedSessionStore {
    private val sessions = ConcurrentHashMap<String, WeakReference<LazyRow123Host>>()

    fun register(host: LazyRow123Host) {
        pruneCollected()
        sessions[host.feedKey] = WeakReference(host)
    }

    fun get(feedKey: String): LazyRow123Host? {
        val host = sessions[feedKey]?.get()
        if (host == null) {
            sessions.remove(feedKey)
        }
        return host
    }

    /** Число записей в реестре. Для тестов. */
    internal fun size(): Int = sessions.size

    private fun pruneCollected() {
        val iterator = sessions.entries.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().value.get() == null) iterator.remove()
        }
    }
}
