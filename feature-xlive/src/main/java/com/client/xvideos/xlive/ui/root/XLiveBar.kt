package com.client.xvideos.xlive.ui.root

import androidx.compose.runtime.Immutable
import com.client.xvideos.xlive.model.XLiveCollection
import com.client.xvideos.xlive.model.XLiveGender
import com.client.xvideos.xlive.model.XLiveTagSort
import com.client.xvideos.xlive.ui.saved.XLiveSavedTab

/**
 * Выбор в нижней панели: вкладка, пол, подборка «Эфиров», порядок «Тегов» и
 * подвкладка «Избранного».
 */
@Immutable
data class XLiveBarState(
    val tab: XLiveTab = XLiveTab.ROOMS,
    val gender: XLiveGender = XLiveGender.ALL,
    val collection: XLiveCollection = XLiveCollection.POPULAR,
    val tagSort: XLiveTagSort = XLiveTagSort.VIEWERS,
    val savedTab: XLiveSavedTab = XLiveSavedTab.MY,
)

/** Нажатия в нижней панели — пара к [XLiveBarState]. */
@Immutable
data class XLiveBarActions(
    val onTab: (XLiveTab) -> Unit = {},
    val onGender: (XLiveGender) -> Unit = {},
    val onCollection: (XLiveCollection) -> Unit = {},
    val onTagSort: (XLiveTagSort) -> Unit = {},
    val onSavedTab: (XLiveSavedTab) -> Unit = {},
)
