package com.client.xvideos.r.common

import com.client.xvideos.r.model.UserInfo
import java.util.concurrent.ConcurrentHashMap

object UsersRed {

    private val usersMap = ConcurrentHashMap<String, UserInfo>()

    /** Получить "снимок" всех пользователей. Быстрое чтение. */
    val listAllUsers: List<UserInfo>
        get() = if (usersMap.isEmpty()) emptyList() else ArrayList(usersMap.values)

    val count: Int get() = usersMap.size
    val isEmpty: Boolean get() = usersMap.isEmpty()
    val isNotEmpty: Boolean get() = !usersMap.isEmpty()

    /** Добавить пользователя, исключая дубликаты по username. */
    fun addUser(user: UserInfo) {
        if (user.username.isNotBlank()) {
            usersMap[user.username] = user
        }
    }

    /** Проверить наличие пользователя по username. */
    fun containsUser(username: String): Boolean {
        if (username.isBlank()) return false
        return usersMap.containsKey(username)
    }

    /** Найти пользователя по username. */
    fun findUser(username: String): UserInfo? {
        if (username.isBlank()) return null
        return usersMap[username]
    }

    /** Удалить пользователя. */
    fun removeUser(username: String) {
        if (username.isNotBlank()) {
            usersMap.remove(username)
        }
    }

    /** Очистить всех пользователей. */
    fun clear() {
        usersMap.clear()
    }
}
