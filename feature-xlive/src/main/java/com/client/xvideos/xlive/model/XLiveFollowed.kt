package com.client.xvideos.xlive.model

/**
 * Подписки аккаунта на сайте.
 *
 * @property online в эфире — в порядке сайта.
 * @property offline не в эфире.
 */
data class XLiveFollowed(
    val online: List<XLiveRoomItem> = emptyList(),
    val offline: List<XLiveRoomItem> = emptyList(),
)

/**
 * Счётчик подписок: [total] всего, [online] из них в эфире.
 */
data class XLiveFollowedCount(val online: Int, val total: Int)
