package com.client.xvideos.x.screens.videoplayer

import androidx.collection.LruCache
import javax.inject.Inject
import javax.inject.Singleton

/** HTML с временными ссылками на видео хранится в памяти одного процесса. */
@Singleton
class XVideoPageCache internal constructor(private val maxBytes: Int) {
    @Inject constructor() : this(4 * 1024 * 1024)

    private val pages = object : LruCache<String, String>(maxBytes) {
        override fun sizeOf(key: String, value: String): Int = (key.length + value.length) * Char.SIZE_BYTES
    }

    operator fun get(url: String): String? = pages.get(url)

    fun put(url: String, html: String) {
        if ((url.length.toLong() + html.length) * Char.SIZE_BYTES <= maxBytes) {
            pages.put(url, html)
        }
    }

    fun remove(url: String) {
        pages.remove(url)
    }
}
