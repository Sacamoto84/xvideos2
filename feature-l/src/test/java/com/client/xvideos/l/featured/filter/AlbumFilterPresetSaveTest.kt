package com.client.xvideos.l.featured.filter

import android.content.SharedPreferences
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.FilterGenre
import com.client.xvideos.l.model.SavedAlbumFilter
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.net.json.LJson
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** Сохранение пресетов фильтра: имена, порядок записи на диск, чтение при старте. */
class AlbumFilterPresetSaveTest {

    /** Настройки в памяти. Чтение можно придержать, чтобы поймать то, что его ждёт. */
    private class MemoryPrefs(initial: String? = null) : SharedPreferences {
        @Volatile
        var stored: String? = initial
            private set

        /** Пока защёлка закрыта, чтение висит — как первое обращение к файлу настроек. */
        var readGate: CountDownLatch? = null

        override fun getString(key: String?, defValue: String?): String? {
            readGate?.await(5, TimeUnit.SECONDS)
            return stored ?: defValue
        }

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private var pending: String? = null

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                pending = value
                return this
            }

            override fun apply() {
                stored = pending
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun putStringSet(key: String?, values: Set<String>?) = unsupported()
            override fun putInt(key: String?, value: Int) = unsupported()
            override fun putLong(key: String?, value: Long) = unsupported()
            override fun putFloat(key: String?, value: Float) = unsupported()
            override fun putBoolean(key: String?, value: Boolean) = unsupported()
            override fun remove(key: String?) = unsupported()
            override fun clear() = unsupported()
        }

        override fun getAll(): MutableMap<String, *> = unsupported()
        override fun contains(key: String?): Boolean = stored != null
        override fun getStringSet(key: String?, defValues: Set<String>?) = unsupported()
        override fun getInt(key: String?, defValue: Int) = unsupported()
        override fun getLong(key: String?, defValue: Long) = unsupported()
        override fun getFloat(key: String?, defValue: Float) = unsupported()
        override fun getBoolean(key: String?, defValue: Boolean) = unsupported()
        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = unsupported()

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = unsupported()

        private fun unsupported(): Nothing = throw UnsupportedOperationException()

        fun storedPresets(): List<SavedAlbumFilter> =
            stored?.let { LJson.decodeFromString<List<SavedAlbumFilter>>(it) }.orEmpty()
    }

    private val prefs = MemoryPrefs()

    private fun genres(vararg ids: String) = ids.map { FilterGenre(id = it, title = "Genre $it") }

    private fun names() = AlbumFilterPresetManager.presets.value.map { it.name }

    private fun awaitIdle() = runBlocking { AlbumFilterPresetManager.awaitIdle() }

    @Before
    fun setUp() {
        AlbumFilterPresetManager.resetForTesting()
    }

    @After
    fun tearDown() {
        awaitIdle()
        AlbumFilterPresetManager.resetForTesting()
    }

    // --- Имена ---

    @Test
    fun `два разных фильтра с одинаковым авто-именем сохраняются оба`() {
        val first = AlbumListFilter(album_type = AlbumType.All, genresPlus = genres("1", "2"))
        val second = AlbumListFilter(album_type = AlbumType.All, genresPlus = genres("3", "4"))

        AlbumFilterPresetManager.savePreset({ prefs }, "", first)
        AlbumFilterPresetManager.savePreset({ prefs }, "", second)

        assertEquals(listOf("+2 genres (2)", "+2 genres"), names())
        assertEquals(listOf(second, first), AlbumFilterPresetManager.presets.value.map { it.filter })
    }

    @Test
    fun `предложенное имя не совпадает с именем существующего пресета`() {
        AlbumFilterPresetManager.resetForTesting(listOf(SavedAlbumFilter(name = "+2 Genres")))
        val filter = AlbumListFilter(album_type = AlbumType.All, genresPlus = genres("3", "4"))

        assertEquals("+2 genres (2)", AlbumFilterPresetManager.generateDefaultName(filter))
    }

    @Test
    fun `номерное имя не занимает номер существующего пресета`() {
        // Был «Preset 1» и «Preset 2», первый удалили: по числу пресетов вышло бы снова «Preset 2».
        AlbumFilterPresetManager.resetForTesting(listOf(SavedAlbumFilter(name = "Preset 2")))
        val plain = AlbumListFilter(album_type = AlbumType.All)

        assertEquals("Preset 1", AlbumFilterPresetManager.generateDefaultName(plain))
    }

    @Test
    fun `имя, отличающееся только регистром, заменяет пресет, а не добавляет второй`() {
        val plain = AlbumListFilter(album_type = AlbumType.All)
        AlbumFilterPresetManager.savePreset({ prefs }, "Abc", plain)

        AlbumFilterPresetManager.savePreset({ prefs }, "abc", plain.copy(searchQuery = "new"))

        assertEquals(listOf("abc"), names())
        assertEquals("new", AlbumFilterPresetManager.getPresetByName("ABC")?.filter?.searchQuery)
    }

    // --- Запись на диск ---

    @Test
    fun `после серии сохранений и удалений на диске лежит то же, что в памяти`() {
        val plain = AlbumListFilter(album_type = AlbumType.All)
        repeat(30) { AlbumFilterPresetManager.savePreset({ prefs }, "preset $it", plain) }
        val toDelete = AlbumFilterPresetManager.presets.value.filterIndexed { index, _ -> index % 3 == 0 }
        toDelete.forEach { AlbumFilterPresetManager.deletePreset({ prefs }, it.id) }

        awaitIdle()

        assertEquals(20, AlbumFilterPresetManager.count)
        assertEquals(AlbumFilterPresetManager.presets.value, prefs.storedPresets())
    }

    // --- Чтение при старте ---

    @Test
    fun `init не ждёт чтения настроек на вызывающем потоке`() {
        val saved = listOf(SavedAlbumFilter(name = "saved"))
        val slow = MemoryPrefs(LJson.encodeToString(saved)).apply { readGate = CountDownLatch(1) }

        val caller = thread { AlbumFilterPresetManager.init { slow } }
        caller.join(2_000)
        val returnedBeforeRead = !caller.isAlive
        slow.readGate?.countDown()
        caller.join(5_000)
        awaitIdle()

        assertTrue("init держал вызывающий поток, пока читались настройки", returnedBeforeRead)
        assertEquals(listOf("saved"), names())
    }

    @Test
    fun `пресет, сохранённый до конца чтения, не теряется`() {
        val saved = listOf(SavedAlbumFilter(name = "saved"))
        val slow = MemoryPrefs(LJson.encodeToString(saved)).apply { readGate = CountDownLatch(1) }
        AlbumFilterPresetManager.init { slow }

        AlbumFilterPresetManager.savePreset({ slow }, "fresh", AlbumListFilter(album_type = AlbumType.All))
        slow.readGate?.countDown()
        awaitIdle()

        assertEquals(listOf("fresh", "saved"), names())
        assertEquals(listOf("fresh", "saved"), slow.storedPresets().map { it.name })
        assertNotEquals(names().first(), names().last())
    }
}
