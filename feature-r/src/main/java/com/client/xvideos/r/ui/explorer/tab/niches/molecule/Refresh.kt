package com.client.xvideos.r.ui.explorer.tab.niches.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Refresh(
    onRefreshNichesCacheClick: () -> Unit,
    nichesCacheProgress: Float,
    refreshList: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(nichesCacheProgress) {
        if (nichesCacheProgress == 1f) {
            delay(1000L)
            refreshList.invoke()
        }
    }

    val buttonColors = ButtonDefaults.buttonColors(containerColor = Theme.R.colorBlue)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.tabLevel1),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Отсутствует список Niches",
            fontSize = 20.sp,
            color = Color.White,
            fontFamily = Theme.R.fontFamilyDMsanss
        )

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onRefreshNichesCacheClick,
            colors = buttonColors
        ) {
            Text(
                text = "Скачать список ",
                fontSize = 18.sp,
                color = Color.White,
                fontFamily = Theme.R.fontFamilyDMsanss
            )
        }
        Spacer(Modifier.height(16.dp))
        LinearWavyProgressIndicator(
            progress = { nichesCacheProgress },
            modifier = Modifier.graphicsLayer {
                alpha = if (nichesCacheProgress > 0f) 1f else 0f
            }
        )
    }
}

@Preview
@Composable
private fun RefreshPreview() {
    Refresh(
        onRefreshNichesCacheClick = {},
        nichesCacheProgress = 0.5f
    )
}
