package com.client.xvideos.calculator.atom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun KeypadRow(
    spacing: Dp = 12.dp,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = spacing),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

@Preview
@Composable
private fun KeypadRowPreview() {
    KeypadRow {
    }
}
