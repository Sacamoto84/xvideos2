package com.client.xvideos.common.ui.choice

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.Theme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Ряд выбора одного варианта из [options] — общий компонент дизайн-системы.
 *
 * Вид задаёт [style] (см. [ChoiceRowStyle]), акцентный цвет — [accent]: его
 * выбирает экран. Все стили, кроме [ChoiceRowStyle.MATERIAL], растягивают ряд
 * на ширину, которую даёт [modifier], и делят её поровну между вариантами.
 */
@Composable
fun ChoiceRow(
    options: ImmutableList<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    style: ChoiceRowStyle,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    when (style) {
        ChoiceRowStyle.MATERIAL -> ChoiceRowMaterial(options, selectedIndex, onSelectIndex, modifier)
        ChoiceRowStyle.OUTLINE,
        ChoiceRowStyle.SOLID,
        ChoiceRowStyle.DOT,
        ChoiceRowStyle.INVERSE -> ChoiceRowChips(options, selectedIndex, onSelectIndex, style, accent, modifier)
        ChoiceRowStyle.TABS,
        ChoiceRowStyle.UNDERLINE -> ChoiceRowTabs(options, selectedIndex, onSelectIndex, style, accent, modifier)
        ChoiceRowStyle.TRACK,
        ChoiceRowStyle.TRACK_ACCENT -> ChoiceRowTrack(options, selectedIndex, onSelectIndex, style, accent, modifier)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun ChoiceRowPreview() {
    ChoiceRow(
        options = persistentListOf("Все", "Картинки", "Gif"),
        selectedIndex = 0,
        onSelectIndex = {},
        style = ChoiceRowStyle.UNDERLINE,
        accent = Theme.L.primaryColor
    )
}
