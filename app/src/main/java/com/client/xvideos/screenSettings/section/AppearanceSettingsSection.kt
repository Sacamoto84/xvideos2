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

private val RADIO_UNSELECTED_COLOR = Color(0xFF938F99)
private val PREVIEW_CARD_CORNER = 24.dp
private val PREVIEW_CARD_SHAPE = RoundedCornerShape(PREVIEW_CARD_CORNER)
private val PREVIEW_GRADIENT = Brush.linearGradient(
    colors = listOf(
        Color(0xFF2C194D),
        Color(0xFF880E4F),
        Color(0xFF0D47A1),
        Color(0xFF004D40)
    )
)
private const val CIRCLE_PINK_ALPHA = 0.85f
private const val CIRCLE_ORANGE_ALPHA = 0.85f
private const val CIRCLE_CYAN_ALPHA = 0.70f
private val CIRCLE_COLOR_PINK = Color(0xFFE91E63).copy(alpha = CIRCLE_PINK_ALPHA)
private val CIRCLE_COLOR_ORANGE = Color(0xFFFF9800).copy(alpha = CIRCLE_ORANGE_ALPHA)
private val CIRCLE_COLOR_CYAN = Color(0xFF00E5FF).copy(alpha = CIRCLE_CYAN_ALPHA)
private const val TITLE_PREVIEW = "Предпросмотр"
private const val TITLE_SCROLL_BUTTONS = "Кнопки быстрой прокрутки"
private const val PREVIEW_LABEL_PREFIX = "Режим: "

private val SECTION_SPACER_HEIGHT = 8.dp
private val PREVIEW_CARD_HORIZONTAL_PADDING = 16.dp
private val PREVIEW_CARD_HEIGHT = 180.dp
private val CIRCLE_PINK_SIZE = 110.dp
private val CIRCLE_PINK_PADDING_START = 16.dp
private val CIRCLE_PINK_PADDING_TOP = 16.dp
private val CIRCLE_ORANGE_SIZE = 90.dp
private val CIRCLE_CYAN_SIZE = 120.dp
private val CIRCLE_CYAN_PADDING_END = 24.dp
private val PREVIEW_LABEL_PADDING = 16.dp
private val PREVIEW_LABEL_FONT_SIZE = 13.sp
private const val PREVIEW_LABEL_ALPHA = 0.9f
private val SCROLL_BUTTONS_END_PADDING = 20.dp

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

    Column(modifier = modifier.fillMaxWidth()) {
        SettingsSectionTitle(TITLE_PREVIEW)
        ScrollButtonPreviewCard(
            hazeState = previewHazeState,
            currentEffect = currentEffect
        )

        Spacer(Modifier.height(SECTION_SPACER_HEIGHT))

        SettingsSectionTitle(TITLE_SCROLL_BUTTONS)
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
    val trailingContent: @Composable () -> Unit = remember(isSelected) {
        {
            RadioButton(
                selected = isSelected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = SettingsAccentColor,
                    unselectedColor = RADIO_UNSELECTED_COLOR
                )
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
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PREVIEW_CARD_HORIZONTAL_PADDING)
            .height(PREVIEW_CARD_HEIGHT)
            .clip(PREVIEW_CARD_SHAPE)
            .background(SettingsCardColor)
    ) {
        // Цветной имитационный фон галереи, помеченный как hazeSource
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(PREVIEW_GRADIENT)
        ) {
            // Декоративные цветные круги для проверки преломления и размытия
            Box(
                modifier = Modifier
                    .size(CIRCLE_PINK_SIZE)
                    .align(Alignment.TopStart)
                    .padding(start = CIRCLE_PINK_PADDING_START, top = CIRCLE_PINK_PADDING_TOP)
                    .clip(CircleShape)
                    .background(CIRCLE_COLOR_PINK)
            )
            Box(
                modifier = Modifier
                    .size(CIRCLE_ORANGE_SIZE)
                    .align(Alignment.BottomCenter)
                    .clip(CircleShape)
                    .background(CIRCLE_COLOR_ORANGE)
            )
            Box(
                modifier = Modifier
                    .size(CIRCLE_CYAN_SIZE)
                    .align(Alignment.CenterEnd)
                    .padding(end = CIRCLE_CYAN_PADDING_END)
                    .clip(CircleShape)
                    .background(CIRCLE_COLOR_CYAN)
            )
        }

        val previewLabelStyle = remember(Theme.L.Type.caption) {
            Theme.L.Type.caption.copy(
                fontWeight = FontWeight.Medium,
                fontSize = PREVIEW_LABEL_FONT_SIZE
            )
        }

        // Подпись образца
        Text(
            text = "$PREVIEW_LABEL_PREFIX${currentEffect.title}",
            color = Color.White.copy(alpha = PREVIEW_LABEL_ALPHA),
            style = previewLabelStyle,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(PREVIEW_LABEL_PADDING)
        )

        val onScrollPreview = remember { {} }

        // Плавающие кнопки скролла в правом краю карточки
        FloatingScrollButtons(
            showScrollToTop = true,
            showScrollToBottom = true,
            hazeState = hazeState,
            onScrollToTop = onScrollPreview,
            onScrollToBottom = onScrollPreview,
            effect = currentEffect,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = SCROLL_BUTTONS_END_PADDING)
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
