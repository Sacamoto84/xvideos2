package com.client.xvideos.common.videoplayer.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class VideoQuality(val bitrate: Double, val resolution: String, val url: String)
data class AudioTrack(val language: String, val name: String, val groupId: String, val url: String, val isDefault: Boolean)
data class SubtitleTrack(val language: String, val name: String, val groupId: String, val url: String, val isDefault: Boolean)
data class M3U8Data(
    val videoQualities: List<VideoQuality>,
    val audioTracks: List<AudioTrack>,
    val subtitleTracks: List<SubtitleTrack>
)

class M3U8Helper {
    suspend fun fetchM3U8Data(url: String, requestHeaders: Map<String, String>? = null): M3U8Data {
        val m3u8Content = withContext(Dispatchers.IO) {
            val client = HttpClient(OkHttp) {
                defaultRequest {
                    (requestHeaders ?: redgifsRequestHeaders()).forEach { (name, value) ->
                        headers.append(name, value)
                    }
                }
            }
            try {
                client.get(url).bodyAsText()
            } finally {
                client.close()
            }
        }
        return parseM3U8Content(m3u8Content, url)
    }

    private fun redgifsRequestHeaders(): Map<String, String> {
        return mapOf(
            "Referer" to "https://www.redgifs.com/",
            "Origin" to "https://www.redgifs.com",
            HttpHeaders.UserAgent to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 YaBrowser/25.6.0.0 Safari/537.36",
            HttpHeaders.Accept to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            HttpHeaders.AcceptEncoding to "identity",
            HttpHeaders.AcceptLanguage to "ru,en;q=0.9"
        )
    }

    private fun parseM3U8Content(m3u8Content: String, baseUrl: String): M3U8Data {
        val videoQualities = mutableListOf<VideoQuality>()
        val audioTracks = mutableListOf<AudioTrack>()
        val subtitleTracks = mutableListOf<SubtitleTrack>()
        val lines = m3u8Content.lines()

        var lastQualityLine: String? = null
        for (line in lines) {
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
        return M3U8Data(videoQualities.sortedBy { it.bitrate }, audioTracks.distinctBy { it.name }, subtitleTracks.distinctBy { it.name })
    }

    private fun extractQuality(infoLine: String, urlLine: String, baseUrl: String): VideoQuality? {
        val bandwidthMatch = Regex("BANDWIDTH=(\\d+)").find(infoLine)
        val resolutionMatch = Regex("RESOLUTION=(\\d+x\\d+)").find(infoLine)
        val bitrate = bandwidthMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        val resolution = resolutionMatch?.groupValues?.get(1)?.let { formatResolution(it) } ?: return null
        val baseUri = baseUrl.substringBeforeLast("/")
        val fullUrl = if (urlLine.startsWith("http")) urlLine else "$baseUri/${urlLine.trim()}"
        return VideoQuality(bitrate, resolution, fullUrl)
    }

    private fun extractAudioTrack(infoLine: String, baseUrl: String): AudioTrack? {
        val language = Regex("LANGUAGE=\"(\\w+)\"").find(infoLine)?.groupValues?.get(1) ?: return null
        val name = Regex("NAME=\"(.*?)\"").find(infoLine)?.groupValues?.get(1) ?: "Unknown"
        val groupId = Regex("GROUP-ID=\"(.*?)\"").find(infoLine)?.groupValues?.get(1) ?: "default"
        val uri = Regex("URI=\"(.*?)\"").find(infoLine)?.groupValues?.get(1) ?: return null
        val isDefault = Regex("DEFAULT=(YES|NO)").find(infoLine)?.groupValues?.get(1)?.equals("YES", true) ?: false
        val baseUri = baseUrl.substringBeforeLast("/")
        val fullUrl = if (uri.startsWith("http")) uri else "$baseUri/${uri.trim()}"
        return AudioTrack(language, name, groupId, fullUrl, isDefault)
    }

    private fun extractSubtitleTrack(infoLine: String, baseUrl: String): SubtitleTrack? {
        val language = Regex("LANGUAGE=\"(\\w+)\"").find(infoLine)?.groupValues?.get(1) ?: return null
        val name = Regex("NAME=\"(.*?)\"").find(infoLine)?.groupValues?.get(1) ?: "Unknown"
        val groupId = Regex("GROUP-ID=\"(.*?)\"").find(infoLine)?.groupValues?.get(1) ?: "default"
        val uri = Regex("URI=\"(.*?)\"").find(infoLine)?.groupValues?.get(1) ?: return null
        val isDefault = Regex("DEFAULT=(YES|NO)").find(infoLine)?.groupValues?.get(1)?.equals("YES", true) ?: false
        val baseUri = baseUrl.substringBeforeLast("/")
        val fullUrl = if (uri.startsWith("http")) uri else "$baseUri/${uri.trim()}"
        return SubtitleTrack(language, name, groupId, fullUrl, isDefault)
    }

    private fun formatResolution(resolution: String): String {
        return resolution.split("x").getOrNull(1)?.plus("p") ?: "Unknown"
    }
}
