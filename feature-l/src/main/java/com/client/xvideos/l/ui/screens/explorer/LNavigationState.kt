package com.client.xvideos.l.ui.screens.explorer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LNavigationState @Inject constructor() {
    var rootTab by mutableIntStateOf(0)
    var savedTab by mutableIntStateOf(0)
}
