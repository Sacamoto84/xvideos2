package com.client.xvideos.common.videoplayer.net

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ktor.KtorDataSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.HttpTimeout

/**
 * Единый HTTP-слой для плеера: тот же Ktor + OkHttp, что и у остального
 * приложения, вместо отдельного `DefaultHttpDataSource` со своими таймаутами
 * и своим пулом соединений.
 *
 * Клиент процессный: `HttpClient` держит пул соединений, создавать его на
 * каждый плеер — терять keep-alive между роликами.
 *
 * [USER_AGENT] подставной: он заменяет дефолтный UA ExoPlayer, потому что
 * хостинги отдают видео по браузероподобному агенту, а на UA плеера отвечают
 * ошибкой.
 */
@OptIn(UnstableApi::class)
object VideoHttpDataSource {

    const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13) ExoPlayer/media3"

    private val client: HttpClient by lazy {
        HttpClient(OkHttp) {
            // Кросс-протокольные редиректы: у плагина HttpRedirect по умолчанию
            // allowHttpsDowngrade = false, и переход https → http вернулся бы в
            // KtorDataSource как ошибка 30x. Раньше эквивалент включался явно —
            // `setAllowCrossProtocolRedirects(true)` на фабриках в ExoplayerHelper.
            install(HttpRedirect) {
                allowHttpsDowngrade = true
            }
            // Общего `requestTimeoutMillis` здесь быть не должно: у Ktor он ограничивает
            // запрос целиком, вместе с чтением тела. Для видео тело читается ровно столько,
            // сколько длится загрузка прогрессивного mp4 или длинного HLS-сегмента, и на
            // медленной сети такой таймаут оборвёт запрос посреди воспроизведения.
            // Оставляем только connect (установка соединения) и socket (пауза между байтами) —
            // они ловят реально зависшую сеть, не мешая долгой, но живой загрузке.
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 15_000
            }
        }
    }

    fun factory(headers: Map<String, String>? = null): HttpDataSource.Factory =
        KtorDataSource.Factory(httpClient = client, userAgent = USER_AGENT).apply {
            if (!headers.isNullOrEmpty()) setDefaultRequestProperties(headers)
        }
}
