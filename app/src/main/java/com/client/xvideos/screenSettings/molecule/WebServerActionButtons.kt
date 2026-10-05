package com.client.xvideos.screenSettings.molecule

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.components.SettingsRowTextPrimary

private const val TEXT_COPY = "Скопировать"
private const val TEXT_SHARE = "Поделиться"

@Composable
internal fun WebServerActionButtons(
    serverUrl: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val onShare: () -> Unit = remember(serverUrl, context) {
        {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, serverUrl)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Поделиться ссылкой")
            context.startActivity(shareIntent)
        }
    }

    val copyButtonColors = ButtonDefaults.buttonColors(
        containerColor = SettingsAccentColor,
        contentColor = Color(0xFF2E2961)
    )

    val rowBaseModifier = Modifier.fillMaxWidth()
    val rowModifier = if (modifier == Modifier) rowBaseModifier else modifier.then(rowBaseModifier)

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onCopy,
            modifier = Modifier.weight(1f),
            colors = copyButtonColors,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = TEXT_COPY, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(TEXT_COPY, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        OutlinedButton(
            onClick = onShare,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = TEXT_SHARE, modifier = Modifier.size(16.dp), tint = SettingsRowTextPrimary)
            Spacer(Modifier.width(6.dp))
            Text(TEXT_SHARE, fontSize = 13.sp, color = SettingsRowTextPrimary)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun WebServerActionButtonsPreview() = SettingsPreview {
    WebServerActionButtons(serverUrl = "http://192.168.0.10:8080", onCopy = {})
}
