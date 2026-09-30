package com.client.xvideos.r.ui.explorer.tab.niches.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
fun RefreshMini(
    onRefreshNichesCacheClick: () -> Unit,
    nichesCacheProgress: Float = 0f,
    refreshList: () -> Unit = {},
    cacheHour: Long = 1L,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(nichesCacheProgress) {
        if (nichesCacheProgress == 1f) {
            delay(1000L)
            refreshList.invoke()
        }
    }

    Row(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxSize()
            .background(Theme.tabLevel1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Старый список Niches, возраст $cacheHour часов",
            fontSize = 14.sp,
            color = Color.White,
            fontFamily = Theme.R.fontFamilyDMsanss
        )

        Spacer(Modifier.height(8.dp))

        Box {
            if (nichesCacheProgress == 0f) {
                IconButton(onClick = onRefreshNichesCacheClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Theme.R.colorBlue, CircleShape)
                            .padding(4.dp)
                    )
                }
            }

            CircularWavyProgressIndicator(
                progress = { nichesCacheProgress },
                modifier = Modifier.size(36.dp).graphicsLayer {
                    alpha = if (nichesCacheProgress > 0f) 1f else 0f
                }
            )
        }
    }
}

@Preview
@Composable
private fun RefreshMiniPreview() {
    RefreshMini(onRefreshNichesCacheClick = {})
}
