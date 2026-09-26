package com.client.xvideos.l.repository

/**
 * Адреса источника данных Luscious.
 *
 * Лежат рядом с [Repository] — клиентом, который по ним и ходит, — а не внутри
 * `net.Luscious`. Раньше константы жили в `Luscious`, и `Repository` тянул их
 * оттуда, а `net` в ответ тянул сам `Repository`: два пакета одного слоя
 * ссылались друг на друга по кругу из-за трёх строк.
 */
object LusciousEndpoints {
    /** Конечная точка единого GraphQL API (nobatch). */
    const val API = "https://members.luscious.net/graphql/nobatch/"
    /** Базовый URL портала Luscious. */
    const val HOME = "https://members.luscious.net"
    /** Страница авторизации пользователя с формой логина. */
    const val LOGIN = "https://members.luscious.net/accounts/login/"

    /** Формирует канонический URL страницы альбома по слагу и ID. */
    fun albumUrl(slug: String, id: String): String = "$HOME/albums/${slug}_$id/"

    /** Проверяет, принадлежит ли URL к доменам Luscious. */
    fun isLusciousUrl(url: String?): Boolean =
        !url.isNullOrBlank() && (url.startsWith(HOME) || url.startsWith("https://luscious.net") || url.startsWith("https://members.luscious.net"))

    /** Проверяет, указывает ли URL на конечную точку GraphQL API. */
    fun isApiUrl(url: String?): Boolean =
        !url.isNullOrBlank() && url.startsWith(API)
}
