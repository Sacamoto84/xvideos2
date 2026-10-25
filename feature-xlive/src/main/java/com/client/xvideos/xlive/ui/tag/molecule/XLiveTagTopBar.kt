package com.client.xvideos.xlive.ui.tag.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.xlive.ui.XLiveColors

/**
 * Шапка экрана тега — вид шапок экранов X: круглая кнопка «Назад», тег
 * крупно, под ним пол.
 *
 * @param subtitle пол комнат; пустой — строки нет.
 */
@Composable
fun XLiveTagTopBar(
    tag: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Фон — на всю ширину, ряд — с лимитом ширины по центру, после фона.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0B0B0E))
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
            .adaptiveContentWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x33FFFFFF))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = "#$tag",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(text = subtitle, fontSize = 12.sp, color = XLiveColors.Subtitle, maxLines = 1)
            }
        }
    }
}

@Preview
@Composable
private fun XLiveTagTopBarPreview() {
    XLiveTagTopBar(tag = "petite", subtitle = "Женщины", onBack = {})
}
