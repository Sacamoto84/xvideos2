package com.client.xvideos.r.common

import com.client.xvideos.r.model.UserInfo

/**
 * Глобальный потокобезопасный in-memory кэш авторов (пользователей) для раздела R.
 *
 * Наполняется при обработке пагинационных ответов лент и страниц авторов,
 * позволяя мгновенно отображать аватары и имена без повторных сетевых запросов.
 */
object UsersRed {

    /**
     * Предел записей. Кэш наполняется каждой загруженной страницей лент и
     * раньше не вытеснял ничего — рос, пока жив процесс.
     */
    internal const val MAX_USERS = 2_000

    /**
     * Сопоставление username -> [UserInfo] в порядке обращения: при переполнении
     * уходит тот, кого дольше всех не спрашивали. Доступ — под замком на самой
     * таблице: `LinkedHashMap` с порядком обращения меняется и при чтении.
     */
    private val usersMap = object : LinkedHashMap<String, UserInfo>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, UserInfo>?): Boolean =
            size > MAX_USERS
    }

    /** Получить снимок всех кэшированных пользователей в виде списка. */
    val listAllUsers: List<UserInfo>
        get() = synchronized(usersMap) { ArrayList(usersMap.values) }

    /** Текущее количество пользователей в кэше. */
    val count: Int get() = synchronized(usersMap) { usersMap.size }

    /** Проверяет, пуст ли кэш пользователей. */
    val isEmpty: Boolean get() = count == 0

    /** Проверяет, содержит ли кэш хотя бы одного пользователя. */
    val isNotEmpty: Boolean get() = count > 0

    /** Добавить пользователя в кэш, обновляя запись по username (если username не пуст). */
    fun addUser(user: UserInfo) {
        if (user.username.isNotBlank()) {
            synchronized(usersMap) { usersMap[user.username] = user }
        }
    }

    /** Пакетно добавляет пользователей в кэш. */
    fun addUsers(users: Collection<UserInfo>) {
        for (u in users) {
            addUser(u)
        }
    }

    /** Проверить наличие пользователя в кэше по его никнейму. */
    fun containsUser(username: String): Boolean {
        if (username.isBlank()) return false
        return synchronized(usersMap) { usersMap.containsKey(username) }
    }

    /** Найти пользователя в кэше по никнейму или вернуть null. */
    fun findUser(username: String): UserInfo? {
        if (username.isBlank()) return null
        return synchronized(usersMap) { usersMap[username] }
    }

    /** Возвращает множество всех закэшированных никнеймов. */
    fun getAllUsernames(): Set<String> = synchronized(usersMap) { usersMap.keys.toSet() }

    /** Находит всех пользователей, соответствующих поисковому запросу. */
    fun findUsersMatching(query: String?): List<UserInfo> {
        if (query.isNullOrBlank()) return emptyList()
        return listAllUsers.filter { it.matches(query) }
    }

    /** Удалить пользователя из кэша. */
    fun removeUser(username: String) {
        if (username.isNotBlank()) {
            synchronized(usersMap) { usersMap.remove(username) }
        }
    }

    /** Очистить весь кэш пользователей. */
    fun clear() {
        synchronized(usersMap) { usersMap.clear() }
    }
}
