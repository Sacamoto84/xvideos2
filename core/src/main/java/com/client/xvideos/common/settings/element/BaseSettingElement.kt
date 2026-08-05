package com.client.xvideos.common.settings.element

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class BaseSettingElement<T>(
    protected val sharedPrefs: SharedPreferences,
    val name: String,
    initialValue: T
) {
    private val _field = MutableStateFlow(initialValue)
    val field: StateFlow<T> = _field.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == name) {
            _field.value = readValue()
        }
    }

    init {
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setValue(value: T) {
        writeValue(value)
        _field.value = value
    }

    fun clear() {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    protected abstract fun readValue(): T
    protected abstract fun writeValue(value: T)

    protected fun updateState(value: T) {
        _field.value = value
    }
}
