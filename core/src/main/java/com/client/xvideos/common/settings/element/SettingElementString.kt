package com.client.xvideos.common.settings.element

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingElementString(private val sharedPrefs: SharedPreferences, val name: String, val default: String = "") {
    private val _field = MutableStateFlow(sharedPrefs.getString(name, default)!!)
    val field: StateFlow<String> = _field.asStateFlow()

    fun setValue(value: String) {
        sharedPrefs.edit { putString(name, value) }
        _field.value = value
    }

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == name) { _field.value = sharedPrefs.getString(key, default)!!  }
    }

    init { sharedPrefs.registerOnSharedPreferenceChangeListener(listener) }
    fun clear() { sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
}
