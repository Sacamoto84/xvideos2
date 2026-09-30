package com.client.xvideos.r.ui.manager_block.atom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.composables.core.HorizontalSeparator

@Composable
fun BottomrBar(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        HorizontalSeparator(Theme.R.colorBottomBarDivider, thickness = 2.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {}
    }
}

@Preview
@Composable
private fun BottomrBarPreview() {
    BottomrBar()
}
