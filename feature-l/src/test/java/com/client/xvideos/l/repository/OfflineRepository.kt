package com.client.xvideos.l.repository

import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode

/**
 * [Repository] без сети: любой запрос получает 404 от подставного движка,
 * без повторов и пауз.
 *
 * Для тестов, которым Repository нужен лишь как зависимость (например, для
 * `Luscious`). Настоящий ходил на сайт из `Luscious.init`: пока сервер
 * отвечал ошибкой, это было незаметно, а когда ответ пришёл из кэша,
 * фоновая корутина полезла в `Dispatchers.Main` и уронила соседний тест.
 */
fun offlineRepository(fileDb: AppFileDatabase): Repository =
    Repository(fileDb, engineFactory = { MockEngine { respond("", HttpStatusCode.NotFound) } })
