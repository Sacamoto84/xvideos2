package com.client.xvideos.x.feature.country

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Глобальное состояние выбранной страны.
 */
@Stable
object CountryState {
    var current: String by mutableStateOf("❓")
        private set
    var userSelectionEpoch: Int by mutableIntStateOf(0)
        private set

    fun updateCurrent(flag: String) {
        current = flag
    }

    fun onCountrySelected(flag: String) {
        current = flag
        userSelectionEpoch++
    }
}
