package com.client.xvideos.r.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Значения [Order] уходят в параметр `order` запроса к RedGifs, и сервер
 * принимает не любые.
 *
 * Тест держит соответствие «константа — строка на проводе». Без него оно
 * проверяется только глазами на устройстве: неверное значение не даёт ни
 * ошибки, ни 404 — приходит выдача, просто отсортированная не так, как
 * выбрано в меню.
 *
 * Так уже случалось: у `TOP_WEEK` было значение `week`, хотя сервер ждёт
 * `top7`. Ленты этого не замечали — они выбирают метод по самой константе, а
 * `order` у них зашит в путь. Поиск же берёт значение отсюда.
 */
class OrderWireValuesTest {

    @Test
    fun `значения сортировок поиска совпадают с ожидаемыми сервером`() {
        assertEquals("score", Order.RELEVANT.value)
        assertEquals("top", Order.TOP.value)
        assertEquals("top7", Order.TOP_WEEK.value)
        assertEquals("top28", Order.TOP_MONTH.value)
        assertEquals("trending", Order.TRENDING.value)
        assertEquals("latest", Order.LATEST.value)
    }

    /**
     * Набор, который `/v2/gifs/search` перечисляет сам в теле ошибки
     * `400 BadOrder`. Снят с живого API 06.08.2026, таблица — в
     * `docs/redgifs-api.md`.
     *
     * Сортировки, уходящие на этот адрес, обязаны лежать внутри набора. Так
     * ушло `TOP_ALLTIME("alltime")`: значения с таким именем у RedGifs нет и не
     * было, «топ за всё время» — это `top`.
     */
    @Test
    fun `сортировки лент и поиска входят в набор, принимаемый сервером`() {
        val accepted = setOf("top", "top7", "top28", "latest", "score", "trending")
        val used = listOf(
            Order.RELEVANT, Order.TOP, Order.TOP_WEEK,
            Order.TOP_MONTH, Order.TRENDING, Order.LATEST
        )
        for (order in used) {
            assertEquals("$order", true, order.value in accepted)
        }
    }

    @Test
    fun `значения сортировок профиля не менялись`() {
        assertEquals("oldest", Order.OLDEST.value)
        assertEquals("top28", Order.TOP28.value)
    }
}
