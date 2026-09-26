package com.client.xvideos.common.kdownloader.internal.stream

import java.io.*

/**
 * Реализация [FileDownloadOutputStream] на базе [RandomAccessFile] и [BufferedOutputStream].
 *
 * Сочетает производительность буферизованной записи с возможностью прямого позиционирования
 * [RandomAccessFile.seek] и аппаратной синхронизации [FileDescriptor.sync].
 */
class FileDownloadRandomAccessFile private constructor(file: File) : FileDownloadOutputStream {
    private val out: BufferedOutputStream
    private val fd: FileDescriptor
    private val randomAccess: RandomAccessFile

    init {
        randomAccess = RandomAccessFile(file, "rw")
        fd = randomAccess.fd
        out = BufferedOutputStream(FileOutputStream(randomAccess.fd))
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray?, off: Int, len: Int) {
        out.write(b, off, len)
    }

    @Throws(IOException::class)
    override fun flushAndSync() {
        out.flush()
        fd.sync()
    }

    @Throws(IOException::class)
    override fun close() {
        out.close()
        randomAccess.close()
    }

    @Throws(IOException::class)
    override fun seek(offset: Long) {
        randomAccess.seek(offset)
    }

    @Throws(IOException::class)
    override fun setLength(newLength: Long) {
        randomAccess.setLength(newLength)
    }

    companion object {
        /**
         * Фабричный метод создания потока произвольного доступа к файлу.
         */
        @Throws(IOException::class)
        fun create(file: File): FileDownloadOutputStream {
            return FileDownloadRandomAccessFile(file)
        }
    }
}
