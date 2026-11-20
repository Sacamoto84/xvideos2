package com.client.xvideos.n.ui.screens.reader.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
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

/**
 * Верхняя панель управления ридера манги с кнопкой «три точки» (меню действий).
 */
@Composable
fun NReaderTopBar(
    title: String,
    currentPage: Int,
    totalPages: Int,
    isVertical: Boolean,
    onBack: () -> Unit,
    onToggleOrientation: () -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    menuContent: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xCC000000))
    ) {
        if (topInset > 0.dp) {
            Spacer(Modifier.height(topInset))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
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
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "$currentPage / $totalPages",
                color = Color(0xFFED2553),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            IconButton(onClick = onToggleOrientation) {
                Icon(
                    imageVector = if (isVertical) Icons.Default.SwapVert else Icons.Default.SwapHoriz,
                    contentDescription = if (isVertical) "Горизонтально" else "Вертикально",
                    tint = Color.White
                )
            }

            if (menuContent != null) {
                menuContent()
            }
        }
    }
}

@Preview
@Composable
private fun NReaderTopBarPreview() {
    NReaderTopBar(
        title = "Preview Manga Title",
        currentPage = 3,
        totalPages = 24,
        isVertical = false,
        onBack = {},
        onToggleOrientation = {},
        menuContent = null
    )
}
