package com.client.xvideos.common.settings.element

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.reflect.Type


/**
 * ```kotlin
 * // Использование:
 * val stringListSetting = SettingElementList<String>(
 *     sharedPrefs,
 *     "my_strings",
 *     emptyList(),
 *     typeToken = object : TypeToken<List<String>>() {}.type
 * )
 *
 * val customObjectSetting = SettingElementList<MyDataClass>(
 *     sharedPrefs,
 *     "my_objects",
 *     typeToken = object : TypeToken<List<MyDataClass>>() {}.type
 * )
 * ```
 */
class SettingElementList<T>(
    private val sharedPrefs: SharedPreferences,
    val name: String,
    private val default: List<T> = emptyList(),
    private val gson: Gson = Gson(),
    private val typeToken: Type
) {
    private val _field = MutableStateFlow(load())
    val field: StateFlow<List<T>> = _field.asStateFlow()

    private fun load(): List<T> {
        val json = sharedPrefs.getString(name, null) ?: return default
        return try {
            gson.fromJson(json, typeToken)
        } catch (e: Exception) {
            default
        }
    }

    fun setValue(value: List<T>) {
        sharedPrefs.edit { putString(name, gson.toJson(value)) }
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

