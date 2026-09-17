package com.client.xvideos.r.network.http

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Тестирование построения маршрутов RedApi и эскейпинга параметров.
 */
class RedApiRouteTest {

    @Test
    fun `route builds search gifs url with all parameters`() {
        val route = Route(
            method = "GET",
            path = "/v2/gifs/search?query={search_text}&order={order}&count={count}&page={page}&type={type}",
            "search_text" to "dance video",
            "order" to "trending",
            "count" to 50,
            "page" to 2,
            "type" to "g",
        )

        assertEquals(
            "https://api.redgifs.com/v2/gifs/search?query=dance%20video&order=trending&count=50&page=2&type=g",
            route.url
        )
    }

    @Test
    fun `route builds user search with multiple tags`() {
        val tags = listOf("4k", "pov", "vr")
        val route = Route(
            method = "GET",
            path = "/v2/users/{username}/search?order={order}&page={page}&count={count}&tags={tags}",
            "username" to "creator_one",
            "page" to 1,
            "count" to 100,
            "order" to "latest",
            "tags" to tags.joinToString(",")
        )

        assertEquals(
            "https://api.redgifs.com/v2/users/creator_one/search?order=latest&page=1&count=100&tags=4k%2Cpov%2Cvr",
            route.url
        )
    }

    @Test
    fun `route correctly escapes special characters in username`() {
        val route = Route(
            method = "GET",
            path = "/v1/users/{username}",
            "username" to "user&name=fake",
        )

        assertEquals("https://api.redgifs.com/v1/users/user%26name%3Dfake", route.url)
    }

    @Test
    fun `route handles empty string parameter safely`() {
        val route = Route(
            method = "GET",
            path = "/v2/niches/search?query={text}",
            "text" to "",
        )

        assertEquals("https://api.redgifs.com/v2/niches/search?query=", route.url)
    }
}
