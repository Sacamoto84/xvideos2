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

private const val REFRESH_DELAY_MS = 1000L
private val REFRESH_SPACER_HEIGHT_8 = 8.dp
private val REFRESH_SPACER_HEIGHT_16 = 16.dp
private val REFRESH_MINI_HORIZONTAL_PADDING = 8.dp
private val REFRESH_MINI_VERTICAL_PADDING = 4.dp
private val REFRESH_INDICATOR_SIZE = 36.dp
private val REFRESH_ICON_SIZE = 34.dp
private val REFRESH_ICON_PADDING = 4.dp
private val REFRESH_TITLE_FONT_SIZE = 20.sp
private val REFRESH_BUTTON_FONT_SIZE = 18.sp
private val REFRESH_MINI_FONT_SIZE = 14.sp
private val COLOR_WHITE = Color.White
private val ICON_REFRESH = Icons.Default.Refresh
private val COLUMN_CENTER_HORIZONTAL = Alignment.CenterHorizontally
private val COLUMN_CENTER_VERTICAL = Arrangement.Center
private val ROW_CENTER_VERTICAL = Alignment.CenterVertically
private val ROW_SPACE_BETWEEN = Arrangement.SpaceBetween
private val INDICATOR_SIZE_MODIFIER = Modifier.size(REFRESH_INDICATOR_SIZE)
private val ICON_SIZE_MODIFIER = Modifier.size(REFRESH_ICON_SIZE)
private val ICON_PADDING_MODIFIER = Modifier.padding(REFRESH_ICON_PADDING)
private val SPACER_HEIGHT_8_MODIFIER = Modifier.height(REFRESH_SPACER_HEIGHT_8)
private val SPACER_HEIGHT_16_MODIFIER = Modifier.height(REFRESH_SPACER_HEIGHT_16)
private val MINI_ROW_BASE_MODIFIER = Modifier
    .padding(horizontal = REFRESH_MINI_HORIZONTAL_PADDING, vertical = REFRESH_MINI_VERTICAL_PADDING)
    .fillMaxSize()

private const val TEXT_NO_NICHES = "Отсутствует список Niches"
private const val TEXT_DOWNLOAD_LIST = "Скачать список "
private const val TEXT_OLD_NICHES_PREFIX = "Старый список Niches, возраст "
private const val TEXT_OLD_NICHES_SUFFIX = " часов"
private const val CD_REFRESH = "Refresh"

private val NICHES_MESSAGE_STYLE = TextStyle(
    fontSize = REFRESH_TITLE_FONT_SIZE,
    color = COLOR_WHITE,
    fontFamily = Theme.R.fontFamilyDMsanss
)
private val NICHES_BUTTON_STYLE = TextStyle(
    fontSize = REFRESH_BUTTON_FONT_SIZE,
    color = COLOR_WHITE,
    fontFamily = Theme.R.fontFamilyDMsanss
)
private val NICHES_MINI_STYLE = TextStyle(
    fontSize = REFRESH_MINI_FONT_SIZE,
    color = COLOR_WHITE,
    fontFamily = Theme.R.fontFamilyDMsanss
)

private val FULL_SIZE_MODIFIER = Modifier.fillMaxSize()

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
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(nichesCacheProgress) {
        if (nichesCacheProgress == 1f) {
            delay(REFRESH_DELAY_MS)
            refreshList.invoke()
        }
    }

    val buttonColors = ButtonDefaults.buttonColors(containerColor = Theme.R.colorBlue)

    Column(
        modifier = modifier
            .then(FULL_SIZE_MODIFIER)
            .background(Theme.tabLevel1),
        verticalArrangement = COLUMN_CENTER_VERTICAL,
        horizontalAlignment = COLUMN_CENTER_HORIZONTAL
    ) {
        Text(TEXT_NO_NICHES, style = NICHES_MESSAGE_STYLE)

        Spacer(SPACER_HEIGHT_8_MODIFIER)
        Button(
            onClick = onRefreshNichesCacheClick,
            colors = buttonColors
        ) {
            Text(TEXT_DOWNLOAD_LIST, style = NICHES_BUTTON_STYLE)
        }
        Spacer(SPACER_HEIGHT_16_MODIFIER)
        LinearWavyProgressIndicator(
            progress = { nichesCacheProgress },
            modifier = Modifier.graphicsLayer {
                alpha = if (nichesCacheProgress > 0f) 1f else 0f
            }
        )
    }
}

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
            delay(REFRESH_DELAY_MS)
            refreshList.invoke()
        }
    }

    Row(
        modifier = modifier
            .then(MINI_ROW_BASE_MODIFIER)
            .background(Theme.tabLevel1),
        verticalAlignment = ROW_CENTER_VERTICAL,
        horizontalArrangement = ROW_SPACE_BETWEEN
    ) {
        Text("$TEXT_OLD_NICHES_PREFIX$cacheHour$TEXT_OLD_NICHES_SUFFIX", style = NICHES_MINI_STYLE)

        Spacer(SPACER_HEIGHT_8_MODIFIER)

        Box {
            if (nichesCacheProgress == 0f) {
                IconButton(onClick = onRefreshNichesCacheClick, modifier = INDICATOR_SIZE_MODIFIER) {
                    Icon(
                        ICON_REFRESH,
                        contentDescription = CD_REFRESH,
                        tint = COLOR_WHITE,
                        modifier = ICON_SIZE_MODIFIER
                            .background(Theme.R.colorBlue, CircleShape)
                            .then(ICON_PADDING_MODIFIER)
                    )
                }
            }

            CircularWavyProgressIndicator(
                progress = { nichesCacheProgress },
                modifier = INDICATOR_SIZE_MODIFIER.graphicsLayer {
                    alpha = if (nichesCacheProgress > 0f) 1f else 0f
                }
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

@Preview(showBackground = true, backgroundColor = 0xFF282828)
@Composable
fun RefreshMiniPreview() {
    XvideosTheme {
        Column(
            modifier = Modifier
                .background(Theme.tabLevel1)
                .padding(8.dp)
        ) {
            RefreshMini(
                onRefreshNichesCacheClick = {},
                nichesCacheProgress = 0f,
                cacheHour = 2L,
            )
            Spacer(modifier = Modifier.height(16.dp))
            RefreshMini(
                onRefreshNichesCacheClick = {},
                nichesCacheProgress = 0.5f,
                cacheHour = 5L,
            )
        }
    }
}
