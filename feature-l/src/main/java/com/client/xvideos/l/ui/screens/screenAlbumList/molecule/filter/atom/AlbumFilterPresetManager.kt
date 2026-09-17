package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import android.content.Context
import android.content.SharedPreferences
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.SavedAlbumFilter
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId
import com.client.xvideos.l.model.enum.PictureCountRank
import com.client.xvideos.l.net.json.LJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

object AlbumFilterPresetManager {
    private const val PREFS_NAME = "l_album_filter_presets"
    private const val KEY_PRESETS = "presets_json"

    private val _presets = MutableStateFlow<List<SavedAlbumFilter>>(emptyList())
    val presets: StateFlow<List<SavedAlbumFilter>> = _presets.asStateFlow()

    private val isInitialized = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val persistMutex = Mutex()

    fun init(context: Context) {
        if (!isInitialized.compareAndSet(false, true)) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromPrefs(prefs)
    }

    private fun loadFromPrefs(prefs: SharedPreferences) {
        val raw = prefs.getString(KEY_PRESETS, null)
        if (!raw.isNullOrBlank()) {
            runCatching {
                LJson.decodeFromString<List<SavedAlbumFilter>>(raw)
            }.onSuccess {
                _presets.value = it
            }.onFailure {
                Timber.e(it, "Failed to load saved filter presets")
            }
        }
    }

    fun savePreset(context: Context, name: String, filter: AlbumListFilter) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cleanName = name.trim().ifBlank { generateDefaultName(filter) }
        val newPreset = SavedAlbumFilter(name = cleanName, filter = filter)
        val snapshot = _presets.updateAndGet { current ->
            listOf(newPreset) + current.filter { it.name != cleanName }
        }
        scope.launch {
            persistMutex.withLock {
                persist(prefs, snapshot)
            }
        }
    }

    fun deletePreset(context: Context, id: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val snapshot = _presets.updateAndGet { current ->
            current.filter { it.id != id }
        }
        scope.launch {
            persistMutex.withLock {
                persist(prefs, snapshot)
            }
        }
    }

    @androidx.annotation.VisibleForTesting
    fun resetForTesting(initial: List<SavedAlbumFilter> = emptyList()) {
        _presets.value = initial
        isInitialized.set(false)
    }

    private fun persist(prefs: SharedPreferences, list: List<SavedAlbumFilter>) {
        runCatching {
            val json = LJson.encodeToString(list)
            prefs.edit().putString(KEY_PRESETS, json).apply()
        }.onFailure {
            Timber.e(it, "Failed to persist saved filter presets")
        }
    }

    fun generateDefaultName(filter: AlbumListFilter): String {
        val parts = mutableListOf<String>()
        if (filter.searchQuery.isNotBlank()) {
            parts.add(filter.searchQuery.take(20))
        }
        if (filter.album_type != AlbumType.All) {
            parts.add(filter.album_type.name)
        }
        if (filter.content_id != ContentId.All) {
            parts.add(filter.content_id.name)
        }
        if (filter.genresPlus.isNotEmpty()) {
            parts.add("+${filter.genresPlus.size} genres")
        }
        if (filter.tagPlus.isNotEmpty()) {
            parts.add("+${filter.tagPlus.size} tags")
        }
        if (filter.selection == "animated") {
            parts.add("Animated")
        }
        return if (parts.isEmpty()) "Preset ${_presets.value.size + 1}" else parts.joinToString(" • ")
    }

    fun formatFilterSummary(filter: AlbumListFilter): String {
        val items = mutableListOf<String>()
        items.add("Type: ${filter.album_type.name}")
        if (filter.content_id != ContentId.All) {
            items.add("Content: ${filter.content_id.name}")
        }
        if (filter.picture_count_rank != PictureCountRank.All) {
            val sizeLabel = when (filter.picture_count_rank) {
                PictureCountRank.All -> "Any"
                PictureCountRank.C0_25 -> "0..25"
                PictureCountRank.C25_50 -> "25..50"
                PictureCountRank.C50_100 -> "50..100"
                PictureCountRank.C100_200 -> "100..200"
                PictureCountRank.C200_800 -> "200..800"
                PictureCountRank.C800_3200 -> "800..3200"
                PictureCountRank.C3200_12800 -> "3200..12800"
            }
            items.add("Size: $sizeLabel")
        }
        if (filter.genresPlus.isNotEmpty()) {
            items.add("+${filter.genresPlus.joinToString { it.title }}")
        }
        if (filter.genresMinus.isNotEmpty()) {
            items.add("NOT ${filter.genresMinus.joinToString { it.title }}")
        }
        if (filter.tagPlus.isNotEmpty()) {
            items.add("+tags: ${filter.tagPlus.joinToString()}")
        }
        if (filter.tagMinus.isNotEmpty()) {
            items.add("-tags: ${filter.tagMinus.joinToString()}")
        }
        if (filter.selection == "animated") {
            items.add("Animated")
        }
        return items.joinToString(" | ")
    }
}
