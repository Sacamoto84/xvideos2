package com.client.xvideos.haptic.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomWaveformSection(
    onCustomVibrate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Кастомный waveform (мимо Compose, прямой Vibrator)",
            color = Color(0xFFB0B0B0),
            fontSize = 13.sp
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onCustomVibrate,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A3B00))
        ) {
            Text("Waveform: 255 → пауза → 127", color = Color.White)
        }
    }
}

@Preview
@Composable
private fun CustomWaveformSectionPreview() {
    CustomWaveformSection(onCustomVibrate = {})
}
