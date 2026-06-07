package com.client.xvideos.x.screens.dashboards.bottomBar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.screens.common.bottomKeyboard.BottomListDashBoardNavigationButtons2
import com.client.xvideos.x.feature.country.ComposeCountry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Второй ряд дашборда: кнопка страны + выбор текущей страницы.
 * Объединяет в одну строку бывший `TopBarDashboard` (страна) и ряд навигации страниц.
 */
@Composable
fun DashboardControlsRow(
    isCurrentPage: Int,
    isMax: Int,
    onChange: suspend (Int) -> Unit
) {
    val job = rememberCoroutineScope()

    Row(modifier = Modifier.fillMaxWidth()) {
        ComposeCountry()
        Box(modifier = Modifier.weight(1f)) {
            BottomListDashBoardNavigationButtons2(
                value = isCurrentPage,
                onChange = { job.launch(Dispatchers.Main) { onChange.invoke(it) } },
                max = isMax,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewDashboardControlsRow() { DashboardControlsRow( isCurrentPage = 1, isMax = 10,  onChange = {}) }
