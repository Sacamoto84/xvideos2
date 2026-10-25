package com.client.xvideos.xlive.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Аккаунт сайта XLive для экранов раздела: есть ли вход и менялись ли
 * подписки. Живёт в процессе — [XLiveSession]; в тестах подменяется.
 */
interface XLiveAccount {

    /** Есть сессия сайта. */
    val signedIn: StateFlow<Boolean>

    /**
     * Счётчик изменений подписок в этом процессе: растёт после каждой подписки
     * и отписки. По нему «Подписки» перечитываются, пока они не на экране.
     */
    val followRevision: StateFlow<Int>

    /** Сайт ответил «нужен вход» на запрос с сессией: она истекла или закрыта на сайте. */
    fun onSessionLost()

    /** Подписка на комнату или отписка прошла. */
    fun onFollowChanged()

    /** Выход: на экране — сразу, сессия на сайте закрывается в фоне. */
    fun signOut()
}
