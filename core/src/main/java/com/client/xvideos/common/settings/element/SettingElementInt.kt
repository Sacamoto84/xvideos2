package com.client.xvideos.common.settings.element

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingElementInt(private val sharedPrefs: SharedPreferences, val name: String, val default: Int = 0) {
    private val _field = MutableStateFlow(sharedPrefs.getInt(name, default))
    val field: StateFlow<Int> = _field.asStateFlow()

    fun setValue(value: Int) {
        if (_field.value == value && sharedPrefs.contains(name)) return
        sharedPrefs.edit { putInt(name, value) }
        _field.value = value
    }

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == name) { _field.value = sharedPrefs.getInt(key, default) }
    }

    init { sharedPrefs.registerOnSharedPreferenceChangeListener(listener) }

    fun clear() { sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
}
