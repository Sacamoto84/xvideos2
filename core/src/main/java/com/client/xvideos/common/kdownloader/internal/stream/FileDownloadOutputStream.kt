package com.client.xvideos.common.kdownloader.internal.stream

import java.io.IOException

/**
 * Интерфейс файлового потока вывода с поддержкой произвольного доступа (seek) и синхронизации с диском.
 *
 * Позволяет позиционировать указатель записи для возобновления скачивания и сбрасывать буферы
 * на физический накопитель для защиты от повреждения файлов при сбоях питания.
 */
interface FileDownloadOutputStream {
    /**
     * Записывает [len] байт из массива [b], начиная со смещения [off].
     */
    @Throws(IOException::class)
    fun write(b: ByteArray?, off: Int, len: Int)

    /**
     * Сбрасывает системные буферы памяти и принудительно синхронизирует их с физическим накопителем.
     */
    @Throws(IOException::class)
    fun flushAndSync()

    /**
     * Закрывает поток вывода и освобождает связанные дескрипторы ОС.
     */
    @Throws(IOException::class)
    fun close()

    /**
     * Перемещает указатель записи в файле на абсолютное смещение [offset].
     */
    @Throws(IOException::class, IllegalAccessException::class)
    fun seek(offset: Long)

    /**
     * Устанавливает длину целевого файла.
     */
    @Throws(IOException::class, IllegalAccessException::class)
    fun setLength(newLength: Long)
}
