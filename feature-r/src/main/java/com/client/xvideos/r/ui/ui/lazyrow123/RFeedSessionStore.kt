package com.client.xvideos.r.ui.ui.lazyrow123

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

object RFeedSessionStore {
    private val sessions = ConcurrentHashMap<String, WeakReference<LazyRow123Host>>()

    fun register(host: LazyRow123Host) {
        sessions[host.feedKey] = WeakReference(host)
    }

    fun get(feedKey: String): LazyRow123Host? {
        val host = sessions[feedKey]?.get()
        if (host == null) {
            sessions.remove(feedKey)
        }
        return host
    }
}
