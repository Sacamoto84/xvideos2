package com.client.xvideos.r.ui.manager_block.atom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme

@Composable
fun BlockedEmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Список заблокированных пуст",
            color = Theme.Text.secondary,
            fontSize = 14.sp,
            fontFamily = Theme.R.fontFamilyDMsanss
        )
    }
}

@Preview
@Composable
private fun BlockedEmptyStatePreview() {
    BlockedEmptyState()
}
