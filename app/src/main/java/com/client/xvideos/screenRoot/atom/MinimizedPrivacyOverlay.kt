package com.client.xvideos.screenRoot.atom

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.R
import com.client.xvideos.common.settings.Settings

@Composable
internal fun MinimizedPrivacyOverlay(modifier: Modifier = Modifier) {
    val isCamouflage = if (Settings.isInitialized) {
        val state by Settings.camouflage_calculator_enabled.field.collectAsStateWithLifecycle()
        state
    } else {
        false
    }
    val scrimAlpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.45f else 1.0f
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .background(Color.Black.copy(alpha = scrimAlpha)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(
                if (isCamouflage) R.drawable.ic_launcher_calculator else R.drawable.icon_red
            ),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Preview
@Composable
private fun MinimizedPrivacyOverlayPreview() {
    MinimizedPrivacyOverlay()
}
