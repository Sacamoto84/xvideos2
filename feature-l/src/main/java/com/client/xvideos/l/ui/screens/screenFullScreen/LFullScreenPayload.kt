package com.client.xvideos.l.ui.screens.screenFullScreen

import com.client.xvideos.l.model.PicsDetails
import java.util.LinkedHashMap
import java.util.UUID

/**
 * Передача списка картинок открывающемуся [L_FullScreenImage].
 *
 * Список не кладётся в сам Screen: сотни [PicsDetails] в Bundle дают
 * TransactionTooLarge. Раньше вместо этого была одна глобальная переменная, из-за
 * чего два быстрых открытия подряд перетирали список друг друга — второй экран
 * успевал записать свой раньше, чем первый его прочитает, и первый показывал
 * чужой альбом.
 *
 * Теперь у каждого открытия свой ключ, а хранилище ограничено несколькими
 * последними записями, чтобы список закрытого экрана не держался в памяти вечно.
 *
 * Смерть процесса хранилище не переживает — на этот случай экран показывает
 * единственную картинку, которая пришла в самом Screen.
 */
internal object LFullScreenPayload {

    private const val MAX_ENTRIES = 3

    private val store = object : LinkedHashMap<String, List<PicsDetails>>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, List<PicsDetails>>?
        ): Boolean = size > MAX_ENTRIES
    }

    @Synchronized
    fun put(items: List<PicsDetails>): String {
        val key = UUID.randomUUID().toString()
        store[key] = items
        return key
    }

    @Synchronized
    fun get(key: String): List<PicsDetails> = store[key].orEmpty()
}
