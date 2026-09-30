package com.client.xvideos.r.ui.explorer.tab.niches.model

import androidx.compose.runtime.Immutable
import com.client.xvideos.r.model.Order

@Immutable
data class NichesCacheState(
    val count: Int,
    val progress: Float,
    val lastModifiedHour: Long,
)

@Immutable
data class NichesTabActions(
    val onSortTypeChange: (Order) -> Unit,
    val onUpClick: () -> Unit,
    val onNicheClick: (String) -> Unit,
    val onRefreshNichesCacheClick: () -> Unit,
)
