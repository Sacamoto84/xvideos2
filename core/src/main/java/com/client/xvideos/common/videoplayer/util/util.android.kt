package com.client.xvideos.common.videoplayer.util

import android.util.Base64
import com.client.xvideos.common.videoplayer.host.DrmConfig
import java.nio.charset.StandardCharsets
import java.util.Locale

fun formatMinSec(value: Int): String {
    val safeValue = value.coerceAtLeast(0)
    if (safeValue == 0) {
        return "00:00"
    }
    // Calculate hours, minutes, and seconds
    val hours = safeValue / 3600
    val minutes = (safeValue % 3600) / 60
    val seconds = safeValue % 60

    // Format the output string
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

internal object VideoUtils {
    private fun encodeBase64(input: String): String {
        val bytes = input.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun createDrmJson(drmConfig: DrmConfig): ByteArray {
        val key = encodeBase64(drmConfig.key)
        val keyId = encodeBase64(drmConfig.keyId)
        val json = """{"keys":[{"kty":"oct","k":"$key","kid":"$keyId"}],"type":"temporary"}"""
        return json.toByteArray(StandardCharsets.UTF_8)
    }
}
