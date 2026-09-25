package com.client.xvideos.x.screens.dashboards.bottomBar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.x.feature.country.ComposeCountry
import com.client.xvideos.x.screens.common.bottomKeyboard.BottomListDashBoardNavigationButtons2
import kotlinx.coroutines.launch

private val CONTROLS_ROW_BASE_MODIFIER = Modifier.fillMaxWidth()
private val ROW_VERTICAL_ALIGNMENT = Alignment.CenterVertically

/**
 * Второй ряд дашборда: кнопка страны + выбор текущей страницы.
 * Объединяет в одну строку бывший `TopBarDashboard` (страна) и ряд навигации страниц.
 */
@Composable
fun DashboardControlsRow(
    isCurrentPage: Int,
    isMax: Int,
    onChange: suspend (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val handleChange: (Int) -> Unit = remember(scope, onChange) {
        { page -> scope.launch { onChange(page) } }
    }

    Row(
        modifier = modifier.then(CONTROLS_ROW_BASE_MODIFIER),
        verticalAlignment = ROW_VERTICAL_ALIGNMENT
    ) {
        ComposeCountry()
        Box(modifier = Modifier.weight(1f)) {
            BottomListDashBoardNavigationButtons2(
                value = isCurrentPage,
                onChange = handleChange,
                max = isMax,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewDashboardControlsRow() {
    DashboardControlsRow(isCurrentPage = 1, isMax = 10, onChange = {})
}
