package com.client.xvideos.l

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Сессионное состояние L-раздела. Живёт до перезапуска процесса (в памяти).
 *
 * [loginSkipped] — пользователь нажал «Пропустить» на экране логина: работаем
 * анонимно (Luscious отдаёт меньше альбомов), окно логина больше не показываем
 * до следующего запуска приложения.
 */
object LSession {
    var loginSkipped by mutableStateOf(false)
}
