package com.client.xvideos.r.ui.explorer.tab.niches

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.ui.theme.XvideosTheme
import kotlinx.coroutines.delay

/**
 * Заглушки списка ниш: предложение скачать список и подсказка, что он устарел.
 *
 * Выделено из `R_ScreenNichesTab.kt` (было 526 строк). Тела функций не менялись
 * — перенос дословный.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Refresh(
    onRefreshNichesCacheClick: () -> Unit,
    nichesCacheProgress: Float,
    refreshList: () -> Unit = {},
) {
    LaunchedEffect(nichesCacheProgress) {
        if (nichesCacheProgress == 1f) {
            delay(1000)
            refreshList.invoke()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.tabLevel1),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Отсутствует список Niches", style = styleTest)

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onRefreshNichesCacheClick,
            colors = ButtonDefaults.buttonColors(containerColor = Theme.R.colorBlue)
        ) {
            Text("Скачать список ", style = styleTest.copy(fontSize = 18.sp))
        }
        Spacer(Modifier.height(16.dp))
        LinearWavyProgressIndicator(
            progress = { nichesCacheProgress },
            Modifier.graphicsLayer(
                alpha = if (nichesCacheProgress > 0f) 1f else 0f
            )
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RefreshMini(
    onRefreshNichesCacheClick: () -> Unit,
    nichesCacheProgress: Float= 0f,
    refreshList: () -> Unit = {},
    cacheHour : Long = 1L
) {
    LaunchedEffect(nichesCacheProgress) {
        if (nichesCacheProgress == 1f) {
            delay(1000)
            refreshList.invoke()
        }
    }

    Row(
        modifier = Modifier.padding(horizontal = 8.dp).padding(vertical = 4.dp).fillMaxSize().background(Theme.tabLevel1), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    )
    {

        Text("Старый список Niches, возраст $cacheHour часов", style = styleTest.copy(fontSize = 14.sp))

        Spacer(Modifier.height(8.dp))

        Box() {

            if (nichesCacheProgress == 0f) {
                IconButton(onClick = onRefreshNichesCacheClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp).background(
                            Theme.R.colorBlue,
                            CircleShape
                        ).padding(4.dp)
                    )
                }
            }



            CircularWavyProgressIndicator(
                progress = { nichesCacheProgress },
                Modifier.size(36.dp)

                    .graphicsLayer(
                        alpha = if (nichesCacheProgress > 0f) 1f else 0f
                    )
            )

        }

    }
}

@Preview(showBackground = true, backgroundColor = 0xFF282828)
@Composable
fun RefreshPreview() {
    XvideosTheme {
        Column(
            modifier = Modifier
                .background(Theme.tabLevel1)
                .padding(8.dp)
        ) {
            Refresh(
                onRefreshNichesCacheClick = {},
                nichesCacheProgress = 0f,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Refresh(
                onRefreshNichesCacheClick = {},
                nichesCacheProgress = 0.45f,
            )
        }
    }
}

private val styleTest = TextStyle(
    fontSize = 20.sp,
    color = Color.White,
    fontFamily = Theme.R.fontFamilyDMsanss
)
