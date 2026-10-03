package com.client.xvideos.common.ui.atom

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Полоса загрузки сверху списка: красная, бегущая, 2dp.
 *
 * Общая для X / R / L — экран показывает её, пока грузит данные,
 * и сам ставит её наверх (`Modifier.align(Alignment.TopCenter)` или слот `topBar`).
 */
@Composable
fun TopLoadingBar(modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp),
        color = Color.Red
    )
}

@Preview
@Composable
private fun TopLoadingBarPreview() {
    TopLoadingBar()
}
