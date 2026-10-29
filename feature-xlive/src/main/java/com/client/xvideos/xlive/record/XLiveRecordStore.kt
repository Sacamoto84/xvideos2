package com.client.xvideos.xlive.record

import com.client.xvideos.common.json.AppJson
import com.client.xvideos.common.json.decodeFromStringOrNull
import com.client.xvideos.xlive.model.XLiveRecord
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * Записи эфиров на диске: папка на запись, `<dirPath>/<id>/`, в ней
 * [RECORD_JSON], снимок, видео и, пока запись не склеена, сырые дорожки.
 *
 * Все операции идут по одной и в порядке вызова на [diskDispatcher] — как у
 * хранилища «Моих»: файл и список в памяти меняются в одной очереди.
 *
 * @param diskDispatcher очередь файловых операций — обязательно последовательная.
 */
class XLiveRecordStore(
    private val dirPath: String,
    private val scope: CoroutineScope,
    private val diskDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
) {

    private val _records = MutableStateFlow<ImmutableList<XLiveRecord>>(persistentListOf())

    /** Записи от новых к старым. */
    val records: StateFlow<ImmutableList<XLiveRecord>> = _records.asStateFlow()

    init {
        refresh()
    }

    /** Список после всех операций, поставленных до вызова, — в том числе первого чтения диска. */
    suspend fun snapshot(): List<XLiveRecord> = withContext(diskDispatcher) { _records.value }

    /** Папка записи [id], есть она или нет. */
    fun dirOf(id: Long): File = File(dirPath, id.toString())

    /** Новая запись или замена прежней с тем же id. */
    fun put(record: XLiveRecord) {
        scope.launch(diskDispatcher) {
            if (write(record)) publish(_records.value.filter { it.id != record.id } + record)
        }
    }

    /** Изменить запись [id], если она есть. */
    fun update(id: Long, change: (XLiveRecord) -> XLiveRecord) {
        scope.launch(diskDispatcher) {
            val old = _records.value.firstOrNull { it.id == id } ?: return@launch
            val new = change(old)
            if (write(new)) publish(_records.value.map { if (it.id == id) new else it })
        }
    }

    /** Удалить запись [id] вместе с её папкой. */
    fun remove(id: Long) {
        scope.launch(diskDispatcher) {
            if (!dirOf(id).deleteRecursively()) Timber.w("XLive: папка записи удалена не целиком")
            publish(_records.value.filter { it.id != id })
        }
    }

    /** Список с диска заново — при создании и после восстановления резервной копии. */
    fun refresh() {
        scope.launch(diskDispatcher) {
            val dirs = File(dirPath).listFiles { file -> file.isDirectory }.orEmpty()
            publish(dirs.mapNotNull { read(File(it, RECORD_JSON)) })
        }
    }

    private fun publish(list: List<XLiveRecord>) {
        _records.value = list.sortedByDescending { it.id }.toImmutableList()
    }

    private fun read(file: File): XLiveRecord? {
        if (!file.isFile) return null
        val text = try {
            file.readText()
        } catch (e: IOException) {
            Timber.w("XLive: запись не прочитана — %s", e.javaClass.simpleName)
            return null
        }
        return AppJson.decodeFromStringOrNull<XLiveRecord>(text)
    }

    /** Пишет во временный файл и переименовывает: оборванная запись не портит прежний JSON. */
    private fun write(record: XLiveRecord): Boolean {
        val dir = dirOf(record.id).apply { mkdirs() }
        val temp = File(dir, "$RECORD_JSON$TEMP_SUFFIX")
        return try {
            temp.writeText(AppJson.encodeToString(XLiveRecord.serializer(), record))
            val target = File(dir, RECORD_JSON)
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            true
        } catch (e: IOException) {
            Timber.w("XLive: запись не сохранена — %s", e.javaClass.simpleName)
            false
        }
    }

    companion object {
        const val RECORD_JSON = "record.json"
        const val SNAPSHOT = "snapshot.jpg"

        /**
         * Кадр начала из видео. Версия способа — в имени: пересобранный кадр
         * получает новый путь, и карточка не показывает прежний из кэша картинок.
         */
        const val FRAME = "frame-$XLIVE_FRAME_VERSION.jpg"

        /** Начало имени любого файла кадра — прежние версии удаляются. */
        const val FRAME_PREFIX = "frame"
        const val VIDEO = "video.mp4"
        const val VIDEO_RAW = "video.part"
        const val AUDIO_RAW = "audio.part"
        const val MUX_TEMP = "video.mp4.part"
        const val STORYBOARD = ".storyboard.jpg"
        private const val TEMP_SUFFIX = ".tmp"

        /** Имя записи [record] в общей галерее: `xlive_<ник>_<id>.mp4`, в нике — только безопасные знаки. */
        fun galleryFileName(record: XLiveRecord): String {
            val cleanUser = record.username.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            return "xlive_${cleanUser}_${record.id}.mp4"
        }
    }
}
