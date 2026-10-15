package com.client.xvideos.l.ui.screens.screenAlbumList

/** Список альбомов в одну колонку неудобен: меньше двух колонок не бывает. */
internal const val ALBUM_LIST_MIN_COLUMNS = 2

/**
 * Колонки списка альбомов по выбору на вкладке (повторный тап по вкладке L).
 *
 * Значение вне 2..4 — две колонки: и сохранённая раньше одна колонка, и
 * мусор в настройках. Это же число показывают точки на вкладке, и сетка с ними
 * не расходится.
 */
internal fun albumListColumns(selected: Int): Int =
    selected.takeIf { it in ALBUM_LIST_MIN_COLUMNS..4 } ?: ALBUM_LIST_MIN_COLUMNS
