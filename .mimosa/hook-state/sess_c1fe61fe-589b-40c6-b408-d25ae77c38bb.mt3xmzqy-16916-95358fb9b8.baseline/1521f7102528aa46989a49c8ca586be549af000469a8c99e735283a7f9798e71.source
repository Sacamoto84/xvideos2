package com.client.xvideos.r.ui.ui.lazyrow123

import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.ref.WeakReference

/**
 * Реестр живых лент не должен расти без предела.
 *
 * Записи держатся через `WeakReference`, поэтому сам `LazyRow123Host` уходит с
 * экраном. А запись в карте не уходила: удалить её мог только `get` по тому же
 * ключу, а ключ у каждой ленты свой (`feedKey` содержит счётчик), и ключи
 * мёртвых лент никто не запрашивает. Карта росла на запись за каждый открытый
 * экран.
 *
 * Собрать настоящий `LazyRow123Host` в юнит-тесте нельзя — он тянет Compose и
 * Paging. Поэтому проверяется само правило подметания, на том же типе карты и
 * той же логике: запись с уже собранным референтом обязана исчезнуть при
 * следующей регистрации.
 */
class RFeedSessionStoreTest {

    /** Копия [RFeedSessionStore] без зависимости от `LazyRow123Host`. */
    private class Store {
        private val sessions = java.util.concurrent.ConcurrentHashMap<String, WeakReference<Any>>()

        fun register(key: String, host: Any) {
            pruneCollected()
            sessions[key] = WeakReference(host)
        }

        fun registerCollected(key: String) {
            pruneCollected()
            sessions[key] = WeakReference<Any>(null)
        }

        fun size() = sessions.size

        private fun pruneCollected() {
            val it = sessions.entries.iterator()
            while (it.hasNext()) {
                if (it.next().value.get() == null) it.remove()
            }
        }
    }

    @Test
    fun `записи с собранным референтом вычищаются при регистрации`() {
        val store = Store()
        val alive = Any()

        // Сто «закрытых» лент: у каждой свой ключ, referent уже собран.
        repeat(100) { store.registerCollected("RFeed:TOP::$it") }
        store.register("RFeed:TOP::alive", alive)

        // Осталась одна живая, остальные подметены.
        assertTrue("в реестре ${store.size()} записей", store.size() == 1)
    }

    @Test
    fun `живые записи подметание не трогает`() {
        val store = Store()
        val hosts = List(5) { Any() }

        hosts.forEachIndexed { i, h -> store.register("RFeed:TOP::$i", h) }

        assertTrue("в реестре ${store.size()} записей", store.size() == 5)
        // hosts удерживает ссылки до конца теста — иначе GC вправе их забрать.
        assertTrue(hosts.isNotEmpty())
    }
}
