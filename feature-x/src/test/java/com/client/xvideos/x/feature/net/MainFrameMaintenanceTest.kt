package com.client.xvideos.x.feature.net

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Страница техобслуживания приходила из WebView как удачный ответ, и поиск X
 * писал «ничего не найдено» без «Повторить» (проход 210, C1). Сбой — только
 * ответ 503 самой страницы: картинка или скрипт с 503 загрузку не обрывают.
 */
class MainFrameMaintenanceTest {

    @Test
    fun `техобслуживание — только ответ 503 основной страницы`() {
        val cases = listOf(
            Triple(true, 503, true),
            Triple(false, 503, false),
            Triple(true, 500, false),
            Triple(true, 404, false),
            Triple(true, 200, false),
        )
        cases.forEach { (isForMainFrame, statusCode, expected) ->
            assertEquals(
                "основная=$isForMainFrame, код=$statusCode",
                expected,
                isMainFrameMaintenance(isForMainFrame, statusCode)
            )
        }
    }
}
