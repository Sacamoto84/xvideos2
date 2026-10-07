package com.client.xvideos.l.featured.filter

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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Менеджер пользовательских пресетов фильтрации каталога альбомов L.
 *
 * Обеспечивает сохранение, удаление, реактивное наблюдение и персистентность
 * пресетов [SavedAlbumFilter] в [SharedPreferences] в формате JSON.
 */
object AlbumFilterPresetManager {
    private const val PREFS_NAME = "l_album_filter_presets"
    private const val KEY_PRESETS = "presets_json"

    private val _presets = MutableStateFlow<List<SavedAlbumFilter>>(emptyList())
    /** Реактивный поток списка всех сохраненных пресетов фильтра. */
    val presets: StateFlow<List<SavedAlbumFilter>> = _presets.asStateFlow()

    /** Количество сохраненных пресетов. */
    val count: Int get() = _presets.value.size
    /** Флаг отсутствия сохраненных пресетов. */
    val isEmpty: Boolean get() = _presets.value.isEmpty()
    /** Флаг наличия хотя бы одного сохраненного пресета. */
    val isNotEmpty: Boolean get() = _presets.value.isNotEmpty()

    /**
     * Находит пресет по его уникальному ID.
     */
    fun getPresetById(id: String): SavedAlbumFilter? = _presets.value.find { it.id == id }

    /**
     * Находит пресет по названию (без учета регистра).
     */
    fun getPresetByName(name: String): SavedAlbumFilter? =
        _presets.value.find { it.name.equals(name.trim(), ignoreCase = true) }

    /**
     * Проверяет наличие пресета с указанным названием.
     */
    fun hasPreset(name: String): Boolean =
        _presets.value.any { it.name.equals(name.trim(), ignoreCase = true) }

    private val isInitialized = AtomicBoolean(false)

    // SupervisorJob: без него первое же исключение в корутине записи отменило
    // бы область целиком, и следующие сохранения молча не писались бы.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Чтение и записи файла настроек идут по одной. */
    private val persistMutex = Mutex()

    /**
     * Инициализирует менеджер, загружая сохраненные пресеты из [SharedPreferences].
     * Безопасен к многократным вызовам (выполняется ровно один раз).
     *
     * @param context Контекст Android приложения.
     */
    fun init(context: Context) = init(prefsOf(context))

    /**
     * Читает пресеты на IO: первое обращение к файлу настроек — это диск, а
     * зовут отсюда из композиции, с главного потока.
     */
    internal fun init(prefs: () -> SharedPreferences) {
        if (!isInitialized.compareAndSet(false, true)) return
        scope.launch {
            persistMutex.withLock { loadFromPrefs(prefs()) }
        }
    }

    private fun prefsOf(context: Context): () -> SharedPreferences {
        val appContext = context.applicationContext
        return { appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    }

    /** Ждёт, пока завершатся начатые чтение и записи. */
    @androidx.annotation.VisibleForTesting
    internal suspend fun awaitIdle() {
        scope.coroutineContext.job.children.forEach { it.join() }
    }

    private fun loadFromPrefs(prefs: SharedPreferences) {
        val raw = prefs.getString(KEY_PRESETS, null)
        if (!raw.isNullOrBlank()) {
            runCatching {
                LJson.decodeFromString<List<SavedAlbumFilter>>(raw)
            }.onSuccess { saved ->
                // Не заменой: пресет, сохранённый раньше, чем дочиталось, уже в
                // списке и остаётся главнее одноимённого с диска.
                _presets.update { current -> current + saved.filter { !current.hasName(it.name) } }
            }.onFailure {
                Timber.e(it, "Failed to load saved filter presets")
            }
        }
    }

    /**
     * Сохраняет новый или обновляет существующий пресет фильтрации.
     *
     * @param context Контекст Android приложения.
     * @param name Название пресета (если пустое, генерируется автоматически).
     * @param filter Сохраняемая конфигурация [AlbumListFilter].
     */
    fun savePreset(context: Context, name: String, filter: AlbumListFilter) = savePreset(prefsOf(context), name, filter)

    internal fun savePreset(prefs: () -> SharedPreferences, name: String, filter: AlbumListFilter) {
        _presets.update { current ->
            val cleanName = name.trim().ifBlank { defaultName(filter, current) }
            // Без учёта регистра, как и поиск по имени: иначе «Abc» и «abc»
            // сохранялись оба, а находился только первый.
            listOf(SavedAlbumFilter(name = cleanName, filter = filter)) + current.filter { !it.name.equals(cleanName, ignoreCase = true) }
        }
        persistLater(prefs)
    }

    /**
     * Пишет на диск список, каким он будет к моменту записи, а не снимок на
     * момент вызова. Корутины записи доходят до замка в произвольном порядке:
     * со снимками поздний мог лечь на диск раньше раннего, и после перезапуска
     * возвращался удалённый пресет.
     */
    private fun persistLater(prefs: () -> SharedPreferences) {
        scope.launch {
            persistMutex.withLock { persist(prefs(), _presets.value) }
        }
    }

    /**
     * Удаляет пресет фильтрации по его уникальному идентификатору [id].
     *
     * @param context Контекст Android приложения.
     * @param id Идентификатор удаляемого пресета.
     */
    fun deletePreset(context: Context, id: String) = deletePreset(prefsOf(context), id)

    internal fun deletePreset(prefs: () -> SharedPreferences, id: String) {
        _presets.update { current -> current.filter { it.id != id } }
        persistLater(prefs)
    }

    /**
     * Сбрасывает состояние менеджера в заданный список [initial] для изолированного тестирования.
     */
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

    private const val DEFAULT_NAME_SEPARATOR = " • "
    private const val FILTER_SUMMARY_SEPARATOR = " | "

    private fun PictureCountRank.formatSizeLabel(): String = when (this) {
        PictureCountRank.All -> "Any"
        PictureCountRank.C0_25 -> "0..25"
        PictureCountRank.C25_50 -> "25..50"
        PictureCountRank.C50_100 -> "50..100"
        PictureCountRank.C100_200 -> "100..200"
        PictureCountRank.C200_800 -> "200..800"
        PictureCountRank.C800_3200 -> "800..3200"
        PictureCountRank.C3200_12800 -> "3200..12800"
    }

    private fun List<SavedAlbumFilter>.hasName(name: String): Boolean =
        any { it.name.equals(name, ignoreCase = true) }

    /**
     * Генерирует читаемое имя пресета по умолчанию на основе активных параметров фильтра.
     *
     * Имя не совпадает ни с одним сохранённым: сохранение под занятым именем
     * заменяет пресет, а в имя попадает лишь часть полей фильтра — два разных
     * фильтра получали одно имя, и второй молча затирал первый.
     *
     * @param filter Объект фильтра [AlbumListFilter].
     * @return Сгенерированная строка названия.
     */
    fun generateDefaultName(filter: AlbumListFilter): String = defaultName(filter, _presets.value)

    private fun defaultName(filter: AlbumListFilter, existing: List<SavedAlbumFilter>): String {
        val parts = ArrayList<String>(6)
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
        // Номер — первый свободный, а не число пресетов плюс один: после
        // удаления одного из них такой номер совпадал с уже занятым.
        if (parts.isEmpty()) {
            return generateSequence(1) { it + 1 }.map { "Preset $it" }.first { !existing.hasName(it) }
        }
        val base = parts.joinToString(DEFAULT_NAME_SEPARATOR)
        if (!existing.hasName(base)) return base
        return generateSequence(2) { it + 1 }.map { "$base ($it)" }.first { !existing.hasName(it) }
    }

    /**
     * Форматирует краткое текстовое резюме всех условий фильтра для показа в подсказках UI.
     *
     * @param filter Объект фильтра [AlbumListFilter].
     * @return Строка со списком активных условий через разделитель ` | `.
     */
    fun formatFilterSummary(filter: AlbumListFilter): String {
        val items = ArrayList<String>(8)
        items.add("Type: ${filter.album_type.name}")
        if (filter.content_id != ContentId.All) {
            items.add("Content: ${filter.content_id.name}")
        }
        if (filter.picture_count_rank != PictureCountRank.All) {
            items.add("Size: ${filter.picture_count_rank.formatSizeLabel()}")
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
        return items.joinToString(FILTER_SUMMARY_SEPARATOR)
    }
}
