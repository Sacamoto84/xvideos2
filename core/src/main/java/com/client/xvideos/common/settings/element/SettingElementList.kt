package com.client.xvideos.common.settings.element

import android.content.SharedPreferences
import androidx.core.content.edit
import com.client.xvideos.common.json.AppJsonCompact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

class SettingElementList<T>(
    private val sharedPrefs: SharedPreferences,
    val name: String,
    private val default: List<T> = emptyList(),
    private val serializer: KSerializer<T>,
    private val json: Json = AppJsonCompact
) {
    companion object {
        inline operator fun <reified T> invoke(
            sharedPrefs: SharedPreferences,
            name: String,
            default: List<T> = emptyList(),
            json: Json = AppJsonCompact
        ): SettingElementList<T> = SettingElementList(sharedPrefs, name, default, serializer<T>(), json)
    }

    private val listSerializer = ListSerializer(serializer)
    private val _field = MutableStateFlow(load())
    val field: StateFlow<List<T>> = _field.asStateFlow()

    private fun load(): List<T> {
        val raw = sharedPrefs.getString(name, null) ?: return default
        return try {
            json.decodeFromString(listSerializer, raw)
        } catch (e: Exception) {
            default
        }
    }

    fun setValue(value: List<T>) {
        sharedPrefs.edit { putString(name, json.encodeToString(listSerializer, value)) }
        _field.value = value
    }

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == name) {
            _field.value = load()
        }
    }

    init {
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun clear() {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}

