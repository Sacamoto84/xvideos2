package com.client.xvideos.l.ui.screens.explorer.tab.saved

internal const val L_SAVED_TAB_COUNT = 5

internal fun savedTabPage(page: Int): Int = page.coerceIn(0, L_SAVED_TAB_COUNT - 1)
