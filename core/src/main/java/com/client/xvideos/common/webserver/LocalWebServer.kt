package com.client.xvideos.common.webserver

import android.content.Context
import com.client.xvideos.common.io.normalizeRelativePath
import com.client.xvideos.common.json.AppJson
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import timber.log.Timber
import java.net.BindException
import java.util.concurrent.atomic.AtomicReference

object LocalWebServer {

    private val HTML_UTF8 = ContentType.Text.Html.withCharset(Charsets.UTF_8)
    private val cachedIndexHtml = AtomicReference<ByteArray?>(null)

    private val serverRef = AtomicReference<EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>?>(null)

    fun isRunning(): Boolean = serverRef.get() != null

    @Synchronized
    fun start(context: Context, port: Int = 8080): Result<String> {
        if (serverRef.get() != null) {
            val url = WebServerState.serverUrl.value ?: "http://127.0.0.1:$port"
            return Result.success(url)
        }

        return runCatching {
            val appContext = context.applicationContext
            val ip = NetworkIpHelper.getLocalIpAddress(appContext) ?: "127.0.0.1"
            val networkName = NetworkIpHelper.getNetworkName(appContext)

            Timber.i("LocalWebServer: запуск на $ip:$port...")

            val newServer = embeddedServer(CIO, port = port, host = "0.0.0.0") {
                install(PartialContent)
                install(ContentNegotiation) { json(AppJson) }
                install(CORS) {
                    anyHost()
                    allowHeader(HttpHeaders.ContentType)
                    allowHeader(HttpHeaders.Range)
                    allowHeader(HttpHeaders.Authorization)
                    exposeHeader(HttpHeaders.ContentRange)
                    exposeHeader(HttpHeaders.AcceptRanges)
                    exposeHeader(HttpHeaders.ContentLength)
                    allowMethod(HttpMethod.Get)
                    allowMethod(HttpMethod.Options)
                    allowMethod(HttpMethod.Head)
                }

                routing {
                    configureWebRoutes(appContext)
                    configureApiRoutes(ip, port)
                    configureMediaRoutes()
                }
            }

            newServer.start(wait = false)
            serverRef.set(newServer)

            val serverUrl = NetworkIpHelper.buildServerUrl(ip, port)
            WebServerState.updateRunning(
                running = true,
                url = serverUrl,
                ip = ip,
                port = port,
                netName = networkName
            )
            Timber.i("LocalWebServer: успешно запущен на $serverUrl")
            serverUrl
        }.onFailure { err ->
            Timber.e(err, "LocalWebServer: сбой запуска сервера на порту $port")
            val current = serverRef.getAndSet(null)
            runCatching { current?.stop(gracePeriodMillis = 100, timeoutMillis = 500) }
            val errorMessage = if (err is BindException || err.message?.contains("Address already in use", ignoreCase = true) == true) {
                "Порт $port уже занят другим приложением"
            } else {
                err.message ?: "Ошибка запуска сервера"
            }
            WebServerState.setError(errorMessage)
            WebServerState.updateRunning(false)
        }
    }

    private fun Routing.configureWebRoutes(appContext: Context) {
        get("/") {
            val html = cachedIndexHtml.get() ?: runCatching {
                appContext.assets.open("web/index.html").use { it.readBytes() }
            }.getOrNull()?.also { cachedIndexHtml.set(it) }

            if (html != null) {
                call.respondBytes(html, HTML_UTF8)
            } else {
                call.respondText(
                    "<h3>Веб-интерфейс не найден в assets/web/</h3>",
                    ContentType.Text.Html
                )
            }
        }

        get("/assets/{path...}") {
            val rawPath = call.parameters.getAll("path")?.joinToString("/") ?: ""
            val safePath = runCatching { normalizeRelativePath(rawPath) }.getOrNull()
            if (safePath.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            val bytes = runCatching {
                appContext.assets.open("web/$safePath").use { it.readBytes() }
            }.getOrNull()

            if (bytes == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            val contentType = determineAssetContentType(safePath)
            call.respondBytes(bytes, contentType)
        }
    }

    private fun determineAssetContentType(path: String): ContentType {
        return when {
            path.endsWith(".css", ignoreCase = true) -> ContentType.Text.CSS
            path.endsWith(".js", ignoreCase = true) -> ContentType.Application.JavaScript
            path.endsWith(".svg", ignoreCase = true) -> ContentType.Image.SVG
            path.endsWith(".png", ignoreCase = true) -> ContentType.Image.PNG
            path.endsWith(".jpg", ignoreCase = true) || path.endsWith(".jpeg", ignoreCase = true) -> ContentType.Image.JPEG
            path.endsWith(".json", ignoreCase = true) -> ContentType.Application.Json
            else -> ContentType.Application.OctetStream
        }
    }

    private fun Routing.configureApiRoutes(defaultIp: String, port: Int) {
        get("/api/status") {
            val currentIp = WebServerState.ipAddress.value ?: defaultIp
            val status = LocalLibraryProvider.getStatus(currentIp, port)
            call.respond(status)
        }

        get("/api/library") {
            val section = call.request.queryParameters["section"]
            val lib = LocalLibraryProvider.getLibrary(section)
            call.respond(lib)
        }

        get("/api/collections") {
            val section = call.request.queryParameters["section"]
            val cols = LocalLibraryProvider.getCollections(section)
            call.respond(cols)
        }

        get("/api/collections/{section}/{name}") {
            val section = call.parameters["section"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val items = LocalLibraryProvider.getCollectionItems(section, name)
            call.respond(items)
        }
    }

    private fun Routing.configureMediaRoutes() {
        configureStandardMediaRoutes()
        configureCollectionMediaRoutes()
    }

    private fun Routing.configureStandardMediaRoutes() {
        get("/media/{section}/{id}/video") {
            val section = call.parameters["section"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)

            val resolved = LocalLibraryProvider.resolveVideo(section, id)
            if (resolved == null) {
                call.respond(HttpStatusCode.NotFound, "Медиафайл не найден")
                return@get
            }

            val (file, downloadFileName) = resolved
            val isDownload = call.request.queryParameters["download"] == "1"
            val disposition = if (isDownload) "attachment" else "inline"
            call.response.header(HttpHeaders.ContentDisposition, "$disposition; filename=\"$downloadFileName\"")
            call.respondFile(file)
        }

        get("/media/{section}/{id}/poster") {
            val section = call.parameters["section"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)

            val file = LocalLibraryProvider.resolvePoster(section, id)
            if (file == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            call.respondFile(file)
        }

        get("/media/l/{folder}/{fileName}") {
            val folder = call.parameters["folder"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val fileName = call.parameters["fileName"] ?: return@get call.respond(HttpStatusCode.BadRequest)

            val resolved = LocalLibraryProvider.resolveLMedia(folder, fileName)
            if (resolved == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            val (file, downloadName) = resolved
            val isDownload = call.request.queryParameters["download"] == "1"
            if (isDownload) {
                call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"$downloadName\"")
            }
            call.respondFile(file)
        }
    }

    private fun Routing.configureCollectionMediaRoutes() {
        get("/media/collection/cover/{section}/{name}") {
            val section = call.parameters["section"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)

            val file = LocalLibraryProvider.resolveCollectionCover(section, name)
            if (file == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            call.respondFile(file)
        }

        get("/media/collection/l/{collection}/{itemFolder}/{fileName}") {
            val collection = call.parameters["collection"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val itemFolder = call.parameters["itemFolder"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val fileName = call.parameters["fileName"] ?: return@get call.respond(HttpStatusCode.BadRequest)

            val resolved = LocalLibraryProvider.resolveLCollectionMedia(collection, itemFolder, fileName)
            if (resolved == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            val (file, downloadName) = resolved
            val isDownload = call.request.queryParameters["download"] == "1"
            if (isDownload) {
                call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"$downloadName\"")
            }
            call.respondFile(file)
        }
    }

    @Synchronized
    fun stop() {
        val current = serverRef.getAndSet(null) ?: return
        Timber.i("LocalWebServer: остановка сервера...")
        runCatching {
            current.stop(gracePeriodMillis = 500, timeoutMillis = 1500)
        }.onFailure {
            Timber.e(it, "LocalWebServer: ошибка при остановке")
        }
        WebServerState.updateRunning(false)
        Timber.i("LocalWebServer: сервер остановлен")
    }
}
