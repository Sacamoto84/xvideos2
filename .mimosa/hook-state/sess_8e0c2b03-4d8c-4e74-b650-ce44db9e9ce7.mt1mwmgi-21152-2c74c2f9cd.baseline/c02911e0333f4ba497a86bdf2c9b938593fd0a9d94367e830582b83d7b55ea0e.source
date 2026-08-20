package com.client.xvideos.r.ui.top_this_week.state

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ErrorState(modifier: Modifier = Modifier, message: String, onRetry: () -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF923117)).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Ошибка загрузки", color = Color.White, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color.DarkGray)
        Text(text = message, color =Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color.DarkGray)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRetry) {
            Text("Повторить")
        }
    }
}

@Preview
@Composable
fun ErrorStatePreview() {
    ErrorState(message = "Something went wrong", onRetry = {})
}
