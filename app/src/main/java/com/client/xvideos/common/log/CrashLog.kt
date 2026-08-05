package com.client.xvideos.common.log

import android.os.Build
import com.client.xvideos.BuildConfig
import com.client.xvideos.common.AppPath
import timber.log.Timber
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Локальный журнал ошибок для релизной сборки.
 *
 * В релизе `Timber` не сажает ни одного дерева, поэтому все `Timber.e` — пустышки,
 * а крэш-репортинга в проекте нет. О падении у пользователя узнать было неоткуда:
 * дефект, из-за которого падало избранное X, нашли только потому, что стектрейс
 * пришёл из отладочной сборки.
 *
 * Здесь сознательно **нет сети**. Приложение ничего не отправляет наружу:
 * `allowBackup=false`, учётные данные в шифрованном хранилище, аналитики нет.
 * Журнал лежит во внутренней памяти и покидает устройство только тогда, когда
 * пользователь сам нажмёт «Поделиться» в настройках.
 *
 * Что попадает в файл: сообщения уровня `ERROR` и полные стектрейсы
 * необработанных исключений. Уровни ниже не пишутся — и чтобы файл не разбухал,
 * и потому что в отладочных сообщениях этого проекта встречаются URL контента.
 */
object CrashLog {

    /** Потолок размера файла: старое обрезается с головы, свежее остаётся в хвосте. */
    private const val MAX_BYTES = 256 * 1024

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    val file: File get() = File(AppPath.main, "crash.log")

    /** Размер журнала в байтах, `0` — если файла нет. */
    fun sizeBytes(): Long = runCatching { file.takeIf { it.exists() }?.length() ?: 0L }.getOrDefault(0L)

    fun clear() {
        runCatching { file.delete() }
    }

    /**
     * Ставит обработчик необработанных исключений.
     *
     * Прежний обработчик вызывается следом — система обязана показать «приложение
     * остановлено» и завершить процесс как обычно. Мы только дописываем запись в
     * журнал и уходим с дороги.
     */
    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                appendEntry(
                    target = file,
                    header = crashHeader(thread.name),
                    body = throwable.stackTraceToStringCompat(),
                    maxBytes = MAX_BYTES
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** Дерево для релиза: записывает только `ERROR`. */
    fun releaseTree(): Timber.Tree = ReleaseErrorTree(file)

    private fun crashHeader(threadName: String): String = buildString {
        append("FATAL ")
        append(timeFormat.format(Date()))
        append(" v")
        append(BuildConfig.VERSION_NAME)
        append(" поток=")
        append(threadName)
        append(" ")
        append(Build.MANUFACTURER)
        append(" ")
        append(Build.MODEL)
        append(" Android ")
        append(Build.VERSION.SDK_INT)
    }

    private class ReleaseErrorTree(private val target: File) : Timber.Tree() {

        private val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        override fun isLoggable(tag: String?, priority: Int): Boolean =
            priority >= android.util.Log.ERROR

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            runCatching {
                appendEntry(
                    target = target,
                    header = "ERROR ${format.format(Date())} ${tag.orEmpty()}",
                    body = if (t == null) message else "$message\n${t.stackTraceToStringCompat()}",
                    maxBytes = MAX_BYTES
                )
            }
        }
    }
}

/**
 * Дописывает запись в конец журнала и держит размер в пределах [maxBytes],
 * обрезая файл **с головы**: свежие записи ценнее старых.
 *
 * Вынесено из [CrashLog] отдельной функцией, потому что это единственная часть
 * с нетривиальным поведением — её покрывают тесты.
 */
internal fun appendEntry(target: File, header: String, body: String, maxBytes: Int) {
    target.parentFile?.mkdirs()

    val entry = buildString {
        append(header)
        append('\n')
        append(body.trimEnd())
        append("\n\n")
    }

    target.appendText(entry)
    trimToLimit(target, maxBytes)
}

/**
 * Если файл перерос [maxBytes], оставляет последние [maxBytes] байт, начиная с
 * границы строки, — иначе журнал открывался бы с середины стектрейса.
 */
internal fun trimToLimit(target: File, maxBytes: Int) {
    if (!target.exists() || target.length() <= maxBytes) return

    val text = target.readText()
    val cut = text.length - maxBytes
    val fromLineStart = text.indexOf('\n', cut).let { if (it < 0) cut else it + 1 }
    target.writeText(text.substring(fromLineStart))
}

/** `Throwable.stackTraceToString()` без зависимости от версии stdlib на устройстве. */
private fun Throwable.stackTraceToStringCompat(): String {
    val writer = StringWriter()
    PrintWriter(writer).use { printStackTrace(it) }
    return writer.toString()
}
