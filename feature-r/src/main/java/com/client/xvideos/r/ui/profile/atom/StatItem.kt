package com.client.xvideos.r.ui.profile.atom

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.ui.theme.XvideosTheme

@Composable
fun StatItem(
    count: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(count, color = Color.White, fontFamily = Theme.R.fontFamilyPopinsMedium)
        Text(label, color = Color(0xFF9E9DA9), fontFamily = Theme.R.fontFamilyPopinsRegular)
    }
}

@Preview
@Composable
private fun StatItemPreview() {
    XvideosTheme {
        StatItem(
            count = "12.3K",
            label = "Подписчиков"
        )
    }
}
