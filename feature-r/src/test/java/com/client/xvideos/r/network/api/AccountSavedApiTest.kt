package com.client.xvideos.r.network.api

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountSavedApiTest {
    @Test
    fun `подписка на автора передаёт полный контекст профиля`() {
        val expected = Json.parseToJsonElement("""{"source":"profile","source_id":null,"position":1}""")

        assertEquals(expected, AccountMembership.AUTHOR.requestBody(enabled = true))
    }

    @Test
    fun `отписка от автора отправляется без тела запроса`() {
        assertNull(AccountMembership.AUTHOR.requestBody(enabled = false))
    }
}
