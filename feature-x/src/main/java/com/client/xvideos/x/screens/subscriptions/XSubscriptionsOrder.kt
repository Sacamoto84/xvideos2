package com.client.xvideos.x.screens.subscriptions

/**
 * Порядок списка подписок X (каналы, модели); [title] — пункт меню сортировки.
 * Номер в списке хранится в настройках — новые варианты добавлять в конец.
 */
enum class XSubscriptionsOrder(val title: String) {
    NEWEST("Сначала новые"),
    OLDEST("Сначала старые"),
    NAME("По имени"),
    VIDEOS("По числу видео"),
}
