package com.client.xvideos.common.ui.choice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Ряд как вкладки — текст без фона: [ChoiceRowStyle.TABS] (C) — акцентная полоса
 * на всю ширину варианта и линия под рядом; [ChoiceRowStyle.UNDERLINE] (F) —
 * короткая акцентная черта под выбранным текстом.
 */
@Composable
internal fun ChoiceRowTabs(
    options: ImmutableList<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    style: ChoiceRowStyle,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val fullBar = style == ChoiceRowStyle.TABS
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
        ) {
            options.forEachIndexed { index, label ->
                key(label) {
                    val selected = index == selectedIndex
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .selectable(selected = selected, role = Role.Tab, onClick = { onSelectIndex(index) }),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.height(if (fullBar) 42.dp else 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (selected) Color.White else Theme.L.grey2,
                                fontSize = 14.sp,
                                fontWeight = if (selected && !fullBar) FontWeight.Medium else FontWeight.Normal,
                                fontFamily = Theme.L.fontFamilyApp,
                                maxLines = 1
                            )
                        }
                        val bar = if (selected) accent else Color.Transparent
                        if (fullBar) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(bar)
                            )
                        } else {
                            Box(
                                Modifier
                                    .width(20.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(bar)
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
        if (fullBar) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Theme.L.grey3)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun ChoiceRowTabsPreview() {
    ChoiceRowTabs(
        options = persistentListOf("Все", "Картинки", "Gif"),
        selectedIndex = 0,
        onSelectIndex = {},
        style = ChoiceRowStyle.UNDERLINE,
        accent = Theme.L.primaryColor
    )
}
