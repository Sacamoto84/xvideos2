package com.client.xvideos.common.kdownloader

data class DownloaderConfig(
    var databaseEnabled: Boolean = false,
    var connectTimeOut: Int = Constants.DEFAULT_CONNECT_TIMEOUT_IN_MILLS,
    var readTimeOut: Int = Constants.DEFAULT_READ_TIMEOUT_IN_MILLS
) {
    val isDefaultTimeouts: Boolean
        get() = connectTimeOut == Constants.DEFAULT_CONNECT_TIMEOUT_IN_MILLS &&
            readTimeOut == Constants.DEFAULT_READ_TIMEOUT_IN_MILLS

    companion object {
        val DEFAULT = DownloaderConfig()
    }
}
