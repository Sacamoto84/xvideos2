package com.client.xvideos.x.screens.search.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        color = Color(0xFFFF9900),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF202020))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Preview
@Composable
private fun SectionHeaderPreview() {
    SectionHeader(title = "Категории и фразы")
}
