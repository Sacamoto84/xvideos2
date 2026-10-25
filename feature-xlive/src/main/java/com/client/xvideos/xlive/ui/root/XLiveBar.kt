package com.client.xvideos.xlive.ui.root

import androidx.compose.runtime.Immutable
import com.client.xvideos.xlive.model.XLiveCollection
import com.client.xvideos.xlive.model.XLiveGender
import com.client.xvideos.xlive.model.XLiveTagSort

/**
 * Выбор в нижней панели: вкладка, пол, подборка «Эфиров» и порядок «Тегов».
 */
@Immutable
data class XLiveBarState(
    val tab: XLiveTab = XLiveTab.ROOMS,
    val gender: XLiveGender = XLiveGender.ALL,
    val collection: XLiveCollection = XLiveCollection.POPULAR,
    val tagSort: XLiveTagSort = XLiveTagSort.VIEWERS,
)

/** Нажатия в нижней панели — пара к [XLiveBarState]. */
@Immutable
data class XLiveBarActions(
    val onTab: (XLiveTab) -> Unit = {},
    val onGender: (XLiveGender) -> Unit = {},
    val onCollection: (XLiveCollection) -> Unit = {},
    val onTagSort: (XLiveTagSort) -> Unit = {},
)
