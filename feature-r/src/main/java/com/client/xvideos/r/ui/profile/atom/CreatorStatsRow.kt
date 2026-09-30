package com.client.xvideos.r.ui.profile.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.ui.theme.XvideosTheme

/**
 * Строка со статистикой креатора (подписчики, просмотры, посты).
 */
@Composable
fun CreatorStatsRow(
    followersPretty: String,
    viewsPretty: String,
    publishedGifsPretty: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        StatItem(
            count = followersPretty,
            label = "Подписчиков",
            modifier = Modifier.fillMaxWidth().weight(1f)
        )

        StatDivider()

        StatItem(
            count = viewsPretty,
            label = "Просмотров",
            modifier = Modifier.fillMaxWidth().weight(1f)
        )

        StatDivider()

        StatItem(
            count = publishedGifsPretty,
            label = "Постов",
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}

@Composable
private fun StatItem(
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

@Composable
private fun StatDivider(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(1.dp)
            .height(24.dp)
            .background(Color(0xFF3D3C53))
    )
}

@Preview
@Composable
private fun CreatorStatsRowPreview() {
    XvideosTheme {
        CreatorStatsRow(
            followersPretty = "12.3K",
            viewsPretty = "450K",
            publishedGifsPretty = "89"
        )
    }
}
