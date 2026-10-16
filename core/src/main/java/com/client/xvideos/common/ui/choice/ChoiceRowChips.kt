package com.client.xvideos.common.ui.choice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val ChipShape = RoundedCornerShape(8.dp)

/**
 * Отдельные кнопки со скруглением 8dp: [ChoiceRowStyle.OUTLINE] (A),
 * [ChoiceRowStyle.SOLID] (B), [ChoiceRowStyle.DOT] (E), [ChoiceRowStyle.INVERSE] (H).
 */
@Composable
internal fun ChoiceRowChips(
    options: ImmutableList<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    style: ChoiceRowStyle,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEachIndexed { index, label ->
            key(label) {
                val selected = index == selectedIndex
                val base = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(ChipShape)
                val look = when (style) {
                    ChoiceRowStyle.OUTLINE -> base
                        .background(if (selected) accent.copy(alpha = 0.15f) else Color.Transparent)
                        .border(1.dp, if (selected) accent else Theme.L.grey3, ChipShape)
                    ChoiceRowStyle.SOLID -> base.background(if (selected) accent else Theme.L.grey3)
                    ChoiceRowStyle.DOT -> base.background(if (selected) Theme.L.grey4 else Color.Transparent)
                    else -> if (selected) {
                        base.background(Theme.L.grey0)
                    } else {
                        base.border(1.dp, Theme.L.grey3, ChipShape)
                    }
                }
                Row(
                    modifier = look.selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = { onSelectIndex(index) }
                    ),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (style == ChoiceRowStyle.DOT && selected) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(accent)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        label,
                        color = chipTextColor(style, selected, accent),
                        fontSize = 14.sp,
                        fontWeight = chipFontWeight(style, selected),
                        fontFamily = Theme.L.fontFamilyApp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Цвет подписи варианта: у A и E — свой, у B и H выбранная тёмная на светлом. */
private fun chipTextColor(style: ChoiceRowStyle, selected: Boolean, accent: Color): Color = when {
    style == ChoiceRowStyle.OUTLINE -> if (selected) accent else Theme.L.textColor
    style == ChoiceRowStyle.DOT -> if (selected) Color.White else Theme.L.grey2
    selected -> Theme.L.grey6
    else -> Theme.L.textColor
}

/** Выбранная на сплошной плашке (B, H) — полужирная, иначе обычная. */
private fun chipFontWeight(style: ChoiceRowStyle, selected: Boolean): FontWeight =
    if (selected && (style == ChoiceRowStyle.SOLID || style == ChoiceRowStyle.INVERSE)) {
        FontWeight.Medium
    } else {
        FontWeight.Normal
    }

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun ChoiceRowChipsPreview() {
    ChoiceRowChips(
        options = persistentListOf("Все", "Картинки", "Gif"),
        selectedIndex = 0,
        onSelectIndex = {},
        style = ChoiceRowStyle.OUTLINE,
        accent = Theme.L.primaryColor
    )
}
