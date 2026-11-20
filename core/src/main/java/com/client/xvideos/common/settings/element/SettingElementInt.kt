package com.client.xvideos.common.settings.element

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingElementInt(
    private val sharedPrefs: SharedPreferences,
    val name: String,
    val default: Int = 0,
    private val normalize: (Int) -> Int = { it },
) {
    private fun readValue() = normalize(sharedPrefs.getInt(name, default))

    private val _field = MutableStateFlow(readValue())
    val field: StateFlow<Int> = _field.asStateFlow()

    fun setValue(value: Int) {
        val normalized = normalize(value)
        if (_field.value == normalized && sharedPrefs.contains(name)) return
        sharedPrefs.edit { putInt(name, normalized) }
        _field.value = normalized
    }

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == name) { _field.value = readValue() }
    }

    init { sharedPrefs.registerOnSharedPreferenceChangeListener(listener) }

    fun clear() { sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
}
