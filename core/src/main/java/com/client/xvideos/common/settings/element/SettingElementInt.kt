package com.client.xvideos.common.settings.element

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingElementInt(private val sharedPrefs: SharedPreferences, val name: String, val default: Int = 0) {
    private val _galleryCheckbox = MutableStateFlow(sharedPrefs.getInt(name, default))
    val field: StateFlow<Int> = _galleryCheckbox.asStateFlow()

    fun setValue(value: Int) {
        sharedPrefs.edit { putInt(name, value) }
        _galleryCheckbox.value = value
        println("!!! setValue $value _galleryCheckbox ${ _galleryCheckbox.value} ")
    }

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == name) { _galleryCheckbox.value = sharedPrefs.getInt(key, default)  }
    }

    init { sharedPrefs.registerOnSharedPreferenceChangeListener(listener) }

    fun clear() { sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener) }

}
