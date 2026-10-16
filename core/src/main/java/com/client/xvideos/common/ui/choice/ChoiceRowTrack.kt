package com.client.xvideos.common.ui.choice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Общая тёмная дорожка с выбранным вариантом-плашкой: [ChoiceRowStyle.TRACK] (D) —
 * плашка светлее, белый текст; [ChoiceRowStyle.TRACK_ACCENT] (G) — серая плашка,
 * акцентный текст.
 */
@Composable
internal fun ChoiceRowTrack(
    options: ImmutableList<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    style: ChoiceRowStyle,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val accentText = style == ChoiceRowStyle.TRACK_ACCENT
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Theme.L.grey7)
            .padding(3.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEachIndexed { index, label ->
            key(label) {
                val selected = index == selectedIndex
                val fill = when {
                    !selected -> Color.Transparent
                    accentText -> Theme.L.grey3
                    else -> Theme.tabLevel3
                }
                val textColor = when {
                    !selected -> Theme.L.grey2
                    accentText -> accent
                    else -> Color.White
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(fill)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelectIndex(index) }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = textColor,
                        fontSize = 14.sp,
                        fontFamily = Theme.L.fontFamilyApp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun ChoiceRowTrackPreview() {
    ChoiceRowTrack(
        options = persistentListOf("Все", "Картинки", "Gif"),
        selectedIndex = 0,
        onSelectIndex = {},
        style = ChoiceRowStyle.TRACK,
        accent = Theme.L.primaryColor
    )
}
