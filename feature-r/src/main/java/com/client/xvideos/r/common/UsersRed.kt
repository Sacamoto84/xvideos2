package com.client.xvideos.r.common

import com.client.xvideos.r.model.UserInfo
import java.util.concurrent.ConcurrentHashMap

/**
 * Глобальный потокобезопасный in-memory кэш авторов (пользователей) для раздела RedGifs.
 *
 * Наполняется при обработке пагинационных ответов лент и страниц авторов,
 * позволяя мгновенно отображать аватары и имена без повторных сетевых запросов.
 */
object UsersRed {

    /** Внутренняя хэш-таблица сопоставления username -> [UserInfo]. */
    private val usersMap = ConcurrentHashMap<String, UserInfo>()

    /** Получить снимок всех кэшированных пользователей в виде списка. */
    val listAllUsers: List<UserInfo>
        get() = if (usersMap.isEmpty()) emptyList() else ArrayList(usersMap.values)

    /** Текущее количество пользователей в кэше. */
    val count: Int get() = usersMap.size

    /** Проверяет, пуст ли кэш пользователей. */
    val isEmpty: Boolean get() = usersMap.isEmpty()

    /** Проверяет, содержит ли кэш хотя бы одного пользователя. */
    val isNotEmpty: Boolean get() = !usersMap.isEmpty()

    /** Добавить пользователя в кэш, обновляя запись по username (если username не пуст). */
    fun addUser(user: UserInfo) {
        if (user.username.isNotBlank()) {
            usersMap[user.username] = user
        }
    }

    /** Проверить наличие пользователя в кэше по его никнейму. */
    fun containsUser(username: String): Boolean {
        if (username.isBlank()) return false
        return usersMap.containsKey(username)
    }

    /** Найти пользователя в кэше по никнейму или вернуть null. */
    fun findUser(username: String): UserInfo? {
        if (username.isBlank()) return null
        return usersMap[username]
    }

    /** Удалить пользователя из кэша. */
    fun removeUser(username: String) {
        if (username.isNotBlank()) {
            usersMap.remove(username)
        }
    }

    /** Очистить весь кэш пользователей. */
    fun clear() {
        usersMap.clear()
    }
}
