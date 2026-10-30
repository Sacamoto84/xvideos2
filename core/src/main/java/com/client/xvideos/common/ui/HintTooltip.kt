package com.client.xvideos.common.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

/**
 * Подсказка к кнопке-значку: [text] всплывает над [content] по долгому
 * нажатию — как подсказки значков в самом Android. Обычное нажатие доходит до
 * кнопки как раньше.
 *
 * Пустой [text] — подсказки нет, [content] рисуется как есть.
 *
 * [modifier] ложится на внешний Box, а не на сам TooltipBox: TooltipBox
 * отдаёт его вложенному узлу, и `weight`/`align` родителя до ряда не доходят.
 * Ширину, заданную снаружи, [content] заполняет сам (`fillMaxWidth`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HintTooltip(
    text: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (text.isEmpty()) {
        Box(modifier = modifier) { content() }
        return
    }
    Box(modifier = modifier, propagateMinConstraints = true) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(text = text) } },
            state = rememberTooltipState(),
            content = content,
        )
    }
}

@Preview
@Composable
private fun HintTooltipPreview() {
    HintTooltip(text = "Подсказка") {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = Color.White)
    }
}
