package com.client.xvideos.common.videoplayer.util

import com.client.xvideos.common.net.doh.AppDns
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URI

data class VideoQuality(val bitrate: Double, val resolution: String, val url: String)
data class AudioTrack(val language: String, val name: String, val groupId: String, val url: String, val isDefault: Boolean)
data class SubtitleTrack(val language: String, val name: String, val groupId: String, val url: String, val isDefault: Boolean)
data class M3U8Data(
    val videoQualities: List<VideoQuality>,
    val audioTracks: List<AudioTrack>,
    val subtitleTracks: List<SubtitleTrack>
)

private val BANDWIDTH_REGEX = Regex("BANDWIDTH=(\\d+)")
private val RESOLUTION_REGEX = Regex("RESOLUTION=(\\d+x\\d+)")
private val LANGUAGE_REGEX = Regex("LANGUAGE=\"(\\w+)\"")
private val NAME_REGEX = Regex("NAME=\"(.*?)\"")
private val GROUP_ID_REGEX = Regex("GROUP-ID=\"(.*?)\"")
private val URI_REGEX = Regex("URI=\"(.*?)\"")
private val DEFAULT_REGEX = Regex("DEFAULT=(YES|NO)")

private val REDGIFS_REQUEST_HEADERS = mapOf(
    "Referer" to "https://www.redgifs.com/",
    "Origin" to "https://www.redgifs.com",
    HttpHeaders.UserAgent to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 YaBrowser/25.6.0.0 Safari/537.36",
    HttpHeaders.Accept to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
    HttpHeaders.AcceptEncoding to "identity",
    HttpHeaders.AcceptLanguage to "ru,en;q=0.9"
)

private val EMPTY_M3U8_DATA = M3U8Data(emptyList(), emptyList(), emptyList())

private val sharedM3U8Client: HttpClient by lazy {
    HttpClient(OkHttp) {
        engine {
            config {
                dns(AppDns)
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
    }
}

class M3U8Helper {
    suspend fun fetchM3U8Data(url: String, requestHeaders: Map<String, String>? = null): M3U8Data {
        val m3u8Content = withContext(Dispatchers.IO) {
            try {
                val response = sharedM3U8Client.get(url) {
                    (requestHeaders ?: REDGIFS_REQUEST_HEADERS).forEach { (name, value) ->
                        headers.append(name, value)
                    }
                }
                if (!response.status.isSuccess()) {
                    Timber.w("fetchM3U8Data: HTTP error ${response.status.value} for $url")
                    ""
                } else {
                    response.bodyAsText()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "fetchM3U8Data failed for $url")
                ""
            }
        }
        return parseM3U8Content(m3u8Content, url)
    }

    internal fun parseM3U8Content(m3u8Content: String, baseUrl: String): M3U8Data {
        if (m3u8Content.isBlank()) {
            return EMPTY_M3U8_DATA
        }
        val videoQualities = mutableListOf<VideoQuality>()
        val audioTracks = mutableListOf<AudioTrack>()
        val subtitleTracks = mutableListOf<SubtitleTrack>()

        var lastQualityLine: String? = null
        for (line in m3u8Content.lineSequence()) {
            when {
                line.startsWith("#EXT-X-STREAM-INF") -> lastQualityLine = line
                lastQualityLine != null && !line.startsWith("#") -> {
                    extractQuality(lastQualityLine, line, baseUrl)?.let { videoQualities.add(it) }
                    lastQualityLine = null
                }
                line.startsWith("#EXT-X-MEDIA") && line.contains("TYPE=AUDIO") ->
                    extractAudioTrack(line, baseUrl)?.let { audioTracks.add(it) }
                line.startsWith("#EXT-X-MEDIA") && line.contains("TYPE=SUBTITLES") ->
                    extractSubtitleTrack(line, baseUrl)?.let { subtitleTracks.add(it) }
            }
        }
        return M3U8Data(
            videoQualities.sortedBy { it.bitrate },
            audioTracks.distinctBy { it.name },
            subtitleTracks.distinctBy { it.name }
        )
    }

    private fun resolveUrl(baseUrl: String, relativeOrAbsolute: String): String {
        val trimmed = relativeOrAbsolute.trim()
        return try {
            URI(baseUrl).resolve(trimmed).toString()
        } catch (_: Exception) {
            val baseUri = baseUrl.substringBeforeLast("/")
            if (trimmed.startsWith("http")) trimmed else "$baseUri/$trimmed"
        }
    }

    private fun extractQuality(infoLine: String, urlLine: String, baseUrl: String): VideoQuality? {
        val bitrate = BANDWIDTH_REGEX.find(infoLine)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        val resolution = RESOLUTION_REGEX.find(infoLine)?.groupValues?.get(1)?.let { formatResolution(it) } ?: return null
        val fullUrl = resolveUrl(baseUrl, urlLine)
        return VideoQuality(bitrate, resolution, fullUrl)
    }

    private fun extractAudioTrack(infoLine: String, baseUrl: String): AudioTrack? {
        val language = LANGUAGE_REGEX.find(infoLine)?.groupValues?.get(1) ?: return null
        val name = NAME_REGEX.find(infoLine)?.groupValues?.get(1) ?: "Unknown"
        val groupId = GROUP_ID_REGEX.find(infoLine)?.groupValues?.get(1) ?: "default"
        val uri = URI_REGEX.find(infoLine)?.groupValues?.get(1) ?: return null
        val isDefault = DEFAULT_REGEX.find(infoLine)?.groupValues?.get(1)?.equals("YES", true) ?: false
        val fullUrl = resolveUrl(baseUrl, uri)
        return AudioTrack(language, name, groupId, fullUrl, isDefault)
    }

    private fun extractSubtitleTrack(infoLine: String, baseUrl: String): SubtitleTrack? {
        val language = LANGUAGE_REGEX.find(infoLine)?.groupValues?.get(1) ?: return null
        val name = NAME_REGEX.find(infoLine)?.groupValues?.get(1) ?: "Unknown"
        val groupId = GROUP_ID_REGEX.find(infoLine)?.groupValues?.get(1) ?: "default"
        val uri = URI_REGEX.find(infoLine)?.groupValues?.get(1) ?: return null
        val isDefault = DEFAULT_REGEX.find(infoLine)?.groupValues?.get(1)?.equals("YES", true) ?: false
        val fullUrl = resolveUrl(baseUrl, uri)
        return SubtitleTrack(language, name, groupId, fullUrl, isDefault)
    }

    private fun formatResolution(resolution: String): String {
        val height = resolution.substringAfter('x', "")
        return if (height.isNotEmpty()) "${height}p" else "Unknown"
    }
}
