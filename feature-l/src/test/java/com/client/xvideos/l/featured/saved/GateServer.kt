package com.client.xvideos.l.featured.saved

import org.junit.Assert.assertTrue
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Сервер картинок на локальном порту. Запрос к `slow.jpg` висит без ответа,
 * пока тест его не отпустит: так сохранение остаётся «в полёте».
 */
internal class GateServer {
    private val socket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
    private val slowRequested = CountDownLatch(1)
    private val slowReleased = CountDownLatch(1)

    init {
        thread(isDaemon = true) {
            while (!socket.isClosed) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                thread(isDaemon = true) { runCatching { serve(client) } }
            }
        }
    }

    private fun serve(client: Socket) {
        client.use {
            val reader = it.getInputStream().bufferedReader()
            val requestLine = reader.readLine().orEmpty()
            while (!reader.readLine().isNullOrEmpty()) Unit
            if ("slow" in requestLine) {
                slowRequested.countDown()
                slowReleased.await(20, TimeUnit.SECONDS)
            }
            val body = ByteArray(64) { 1 }
            val head = "HTTP/1.1 200 OK\r\nContent-Type: image/jpeg\r\n" +
                "Content-Length: ${body.size}\r\nConnection: close\r\n\r\n"
            val output = it.getOutputStream()
            output.write(head.toByteArray())
            output.write(body)
            output.flush()
        }
    }

    fun url(name: String) = "http://127.0.0.1:${socket.localPort}/$name"
    fun awaitSlowRequested() = assertTrue("запрос slow.jpg не дошёл до сервера", slowRequested.await(5, TimeUnit.SECONDS))
    fun releaseSlow() = slowReleased.countDown()

    fun stop() {
        slowReleased.countDown()
        socket.close()
    }
}
