package com.client.xvideos.n.ui.screens.explorer.tab.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp

/**
 * Вкладка сохранённой манги (офлайн-библиотека).
 */
object N_SavedTab : Screen {

    private fun readResolve(): Any = N_SavedTab

    override val key: ScreenKey = "N_SavedTab"

    @Composable
    override fun Content() {
        N_SavedTabContent()
    }
}

@Composable
fun N_SavedTabContent(
    modifier: Modifier = Modifier
) {
    val topInset = getTopInsetDp()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.background)
            .padding(top = topInset),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Офлайн-библиотека",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Здесь будут отображаться сохранённые и скачанные главы манги.",
                color = Color(0xFFAAAAAA),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Preview
@Composable
private fun N_SavedTabContentPreview() {
    N_SavedTabContent()
}
