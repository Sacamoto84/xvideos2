package com.client.xvideos.x.screens.saved.atom

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.client.xvideos.x.screens.common.atom.SavedTabHeader

@Composable
fun SavedHeader(
    modifier: Modifier = Modifier,
    topCutout: Dp = 0.dp,
) {
    SavedTabHeader(title = "Сохранённое", topCutout = topCutout, modifier = modifier)
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun SavedHeaderPreview() {
    SavedHeader(topCutout = 0.dp)
}
