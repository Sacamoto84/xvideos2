package com.client.xvideos.screenSettings.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.settings.ScrollButtonEffect
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.atom.FloatingScrollButtons
import com.client.xvideos.screenSettings.components.SettingsCardColor
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private val PREVIEW_GRADIENT = Brush.linearGradient(
    colors = listOf(
        Color(0xFF2C194D),
        Color(0xFF880E4F),
        Color(0xFF0D47A1),
        Color(0xFF004D40)
    )
)

private val PREVIEW_CARD_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .padding(horizontal = 16.dp)
    .height(180.dp)
    .clip(RoundedCornerShape(24.dp))
    .background(SettingsCardColor)

/**
 * Интерактивная демонстрационная карточка для предпросмотра стиля кнопок прокрутки.
 */
@Composable
internal fun ScrollButtonPreviewCard(
    hazeState: HazeState,
    currentEffect: ScrollButtonEffect,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.then(PREVIEW_CARD_BASE_MODIFIER)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PREVIEW_GRADIENT)
                .hazeSource(hazeState)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(110.dp)
                    .padding(start = 16.dp, top = 16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE91E63).copy(alpha = 0.85f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF9800).copy(alpha = 0.85f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(120.dp)
                    .padding(end = 24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.70f))
            )
        }

        val previewLabelStyle = remember(Theme.L.Type.caption) {
            Theme.L.Type.caption.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        }
        val previewLabelText = remember(currentEffect) {
            "Режим: ${currentEffect.title}"
        }

        Text(
            text = previewLabelText,
            color = Color.White.copy(alpha = 0.9f),
            style = previewLabelStyle,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )

        FloatingScrollButtons(
            showScrollToTop = true,
            showScrollToBottom = true,
            hazeState = hazeState,
            onScrollToTop = {},
            onScrollToBottom = {},
            effect = currentEffect,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
        )
    }
}

@Preview
@Composable
private fun ScrollButtonPreviewCardPreview() {
    ScrollButtonPreviewCard(
        hazeState = rememberHazeState(),
        currentEffect = ScrollButtonEffect.entries.first(),
    )
}
