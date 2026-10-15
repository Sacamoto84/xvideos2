package com.client.xvideos.l.ui.screens.screenAlbumList

/**
 * Колонки списка альбомов по выбору на вкладке (повторный тап по вкладке L).
 *
 * Значение вне 1..4 — две колонки: столько же показывают точки на вкладке
 * (`TabBarPoints`), и сетка с ними не расходится.
 */
internal fun albumListColumns(selected: Int): Int = selected.takeIf { it in 1..4 } ?: 2
