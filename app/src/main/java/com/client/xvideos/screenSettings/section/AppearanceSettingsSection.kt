package com.client.xvideos.screenSettings.section

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.R
import com.client.xvideos.common.settings.ScrollButtonEffect
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.atom.FloatingScrollButtons
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsCardColor
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsSectionTitle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * Экран настроек «Отображение» (Appearance).
 * Содержит интерактивное превью и переключатель режима кнопок быстрой прокрутки (Заливка, Блюр, Стекло).
 */
@Composable
internal fun AppearanceSettingsSection() {
    val effectName = Settings.scroll_buttons_effect.field.collectAsStateWithLifecycle().value
    val currentEffect = remember(effectName) { ScrollButtonEffect.fromNameOrDefault(effectName) }
    val previewHazeState = rememberHazeState()

    SettingsSectionTitle("Предпросмотр")
    ScrollButtonPreviewCard(
        hazeState = previewHazeState,
        currentEffect = currentEffect
    )

    Spacer(Modifier.height(8.dp))

    SettingsSectionTitle("Кнопки быстрой прокрутки")
    SettingsGroup {
        ScrollButtonEffect.entries.forEachIndexed { index, effect ->
            if (index > 0) {
                SettingsDivider()
            }
            SettingsListItem(
                icon = R.drawable.ic_blur_24,
                text = effect.title,
                subtitle = effect.subtitle,
                trailing = {
                    RadioButton(
                        selected = (currentEffect == effect),
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = SettingsAccentColor,
                            unselectedColor = Color(0xFF938F99)
                        )
                    )
                },
                onClick = { Settings.scroll_buttons_effect.setValue(effect.name) }
            )
        }
    }
}

/**
 * Интерактивная демонстрационная карточка:
 * моделирует цветной фон галереи с контентом под кнопками, чтобы пользователь сразу видел
 * оптическую разницу между Заливкой, Блюром и Стеклом.
 */
@Composable
private fun ScrollButtonPreviewCard(
    hazeState: HazeState,
    currentEffect: ScrollButtonEffect
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SettingsCardColor)
    ) {
        // Цветной имитационный фон галереи, помеченный как hazeSource
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF2C194D),
                            Color(0xFF880E4F),
                            Color(0xFF0D47A1),
                            Color(0xFF004D40)
                        )
                    )
                )
        ) {
            // Декоративные цветные круги для проверки преломления и размытия
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE91E63).copy(alpha = 0.85f))
            )
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .align(Alignment.BottomCenter)
                    .clip(CircleShape)
                    .background(Color(0xFFFF9800).copy(alpha = 0.85f))
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.70f))
            )
        }

        // Подпись образца
        Text(
            text = "Режим: ${currentEffect.title}",
            color = Color.White.copy(alpha = 0.9f),
            style = Theme.L.Type.caption.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            ),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )

        // Плавающие кнопки скролла в правом краю карточки
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
