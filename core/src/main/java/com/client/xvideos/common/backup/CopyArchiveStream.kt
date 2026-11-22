package com.client.xvideos.common.backup

import java.io.InputStream
import java.io.OutputStream

internal fun InputStream.copyArchiveTo(output: OutputStream, checkActive: () -> Unit): Long {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var bytes = 0L
    while (true) {
        checkActive()
        val read = read(buffer)
        if (read < 0) return bytes
        output.write(buffer, 0, read)
        bytes += read
    }
}
