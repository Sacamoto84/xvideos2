package com.client.xvideos.screenSettings.section

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.components.SettingsSectionTitle
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
 * Экран настроек «Отображение» (Appearance).
 * Содержит интерактивное превью и переключатель режима кнопок быстрой прокрутки (Заливка, Блюр, Стекло).
 */
@Composable
internal fun AppearanceSettingsSection(
    modifier: Modifier = Modifier,
) {
    val effectName by Settings.scroll_buttons_effect.field.collectAsStateWithLifecycle()
    val currentEffect = remember(effectName) { ScrollButtonEffect.fromNameOrDefault(effectName) }
    val previewHazeState = rememberHazeState()

    val onSelectEffect: (ScrollButtonEffect) -> Unit = remember {
        { effect -> Settings.scroll_buttons_effect.setValue(effect.name) }
    }

    Column(modifier = if (modifier == Modifier) Modifier.fillMaxWidth() else modifier.fillMaxWidth()) {
        SettingsSectionTitle("Предпросмотр")
        ScrollButtonPreviewCard(
            hazeState = previewHazeState,
            currentEffect = currentEffect
        )

        Spacer(Modifier.height(8.dp))

        SettingsSectionTitle("Кнопки быстрой прокрутки")
        SettingsGroup {
            ScrollButtonEffect.entries.forEachIndexed { index, effect ->
                key(effect.name) {
                    if (index > 0) {
                        SettingsDivider()
                    }
                    ScrollEffectItem(
                        effect = effect,
                        isSelected = (currentEffect == effect),
                        onSelect = onSelectEffect
                    )
                }
            }
        }
    }
}

@Composable
private fun ScrollEffectItem(
    effect: ScrollButtonEffect,
    isSelected: Boolean,
    onSelect: (ScrollButtonEffect) -> Unit,
    modifier: Modifier = Modifier
) {
    val onClick = remember(effect, onSelect) { { onSelect(effect) } }
    val radioColors = RadioButtonDefaults.colors(
        selectedColor = SettingsAccentColor,
        unselectedColor = Color(0xFF938F99)
    )
    val trailingContent: @Composable () -> Unit = remember(isSelected, radioColors) {
        {
            RadioButton(
                selected = isSelected,
                onClick = null,
                colors = radioColors
            )
        }
    }
    SettingsListItem(
        icon = R.drawable.ic_blur_24,
        text = effect.title,
        subtitle = effect.subtitle,
        trailing = trailingContent,
        onClick = onClick,
        modifier = modifier
    )
}

/**
 * Интерактивная демонстрационная карточка:
 * моделирует цветной фон галереи с контентом под кнопками, чтобы пользователь сразу видел
 * оптическую разницу между Заливкой, Блюром и Стеклом.
 */
@Composable
private fun ScrollButtonPreviewCard(
    hazeState: HazeState,
    currentEffect: ScrollButtonEffect,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = if (modifier == Modifier) PREVIEW_CARD_BASE_MODIFIER else modifier.then(PREVIEW_CARD_BASE_MODIFIER)
    ) {
        // Цветной имитационный фон галереи, помеченный как hazeSource
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PREVIEW_GRADIENT)
                .hazeSource(hazeState)
        ) {
            // Декоративные цветные круги для проверки преломления и размытия
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

        // Подпись образца
        Text(
            text = previewLabelText,
            color = Color.White.copy(alpha = 0.9f),
            style = previewLabelStyle,
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

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun AppearanceSettingsSectionPreview() {
    SettingsPreview {
        AppearanceSettingsSection()
    }
}
