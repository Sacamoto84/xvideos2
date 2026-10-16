package com.client.xvideos.l.ui.screens.explorer.tab.saved.albums

import com.client.xvideos.l.model.AlbumDetails

/**
 * Сохранённые альбомы [albums] в порядке [order].
 *
 * [albums] идут от последних сохранённых к первым: так их читает FileDB (по
 * времени файла) и так их добавляет SavedL_Albums. «Больше элементов» — по
 * числу картинок альбома (гифки входят в него); при равенстве — порядок
 * сохранения.
 */
fun lSortedSavedAlbums(albums: List<AlbumDetails>, order: LSavedAlbumsOrder): List<AlbumDetails> =
    when (order) {
        LSavedAlbumsOrder.NEWEST -> albums
        LSavedAlbumsOrder.OLDEST -> albums.reversed()
        LSavedAlbumsOrder.MOST_ITEMS -> albums.sortedByDescending { it.number_of_pictures }
    }

/** Порядок по номеру из настроек; неизвестный номер — порядок по умолчанию. */
fun lSavedAlbumsOrderOf(ordinal: Int): LSavedAlbumsOrder =
    LSavedAlbumsOrder.entries.getOrElse(ordinal) { LSavedAlbumsOrder.NEWEST }
