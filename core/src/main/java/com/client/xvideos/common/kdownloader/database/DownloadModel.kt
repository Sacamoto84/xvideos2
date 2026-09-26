package com.client.xvideos.common.kdownloader.database

data class DownloadModel(
    var id: Int = 0,
    var url: String = "",
    var eTag: String = "",
    var dirPath: String = "",
    var fileName: String = "",
    var totalBytes: Long = 0,
    var downloadedBytes: Long = 0,
    var lastModifiedAt: Long = 0
) {
    val isValid: Boolean get() = id > 0 && url.isNotBlank()
    val hasTotalBytes: Boolean get() = totalBytes > 0L
    val remainingBytes: Long get() = if (totalBytes > downloadedBytes) totalBytes - downloadedBytes else 0L
    val progressFraction: Float
        get() = if (totalBytes > 0L) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    companion object {
        const val ID = "id"
        const val URL = "url"
        const val ETAG = "etag"
        const val DIR_PATH = "dir_path"
        const val FILE_NAME = "file_name"
        const val TOTAL_BYTES = "total_bytes"
        const val DOWNLOADED_BYTES = "downloaded_bytes"
        const val LAST_MODIFIED_AT = "last_modified_at"
    }
}
