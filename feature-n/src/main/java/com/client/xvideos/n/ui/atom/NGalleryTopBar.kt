package com.client.xvideos.n.ui.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme

/**
 * Верхняя панель экрана манги с кнопкой «Назад», заголовком и случайным выбором.
 */
@Composable
fun NGalleryTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRandomClick: (() -> Unit)? = null,
    topInset: Dp = 0.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.tabLevel0)
    ) {
        if (topInset > 0.dp) {
            Spacer(Modifier.height(topInset))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White
                )
            }
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            if (onRandomClick != null) {
                IconButton(onClick = onRandomClick) {
                    Icon(
                        imageVector = Icons.Outlined.Shuffle,
                        contentDescription = "Случайная манга",
                        tint = Color(0xFFED2553)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun NGalleryTopBarPreview() {
    NGalleryTopBar(
        title = "Preview Manga Title",
        onBack = {},
        topInset = 24.dp
    )
}
