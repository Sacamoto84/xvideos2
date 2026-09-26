package com.client.xvideos.l.net.graphQl

/**
 * Фабрика формирования сырых GraphQL-запросов к API Luscious.
 */
object GraphQlRequest {

    /**
     * Формирует JSON GraphQL-запрос для постраничной загрузки картинок внутри альбома [albumId].
     *
     * @param albumId Идентификатор альбома.
     * @param page Номер страницы (по умолчанию 1).
     * @return JSON-строка запроса.
     */
    fun pictureListInsideAlbum(albumId: Int, page: Int = 1) : String = getPicturesJson(albumId, page)

}
