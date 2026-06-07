package com.client.xvideos.screenRoot

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.client.xvideos.common.snackbar.UiMessage
import com.client.xvideos.common.snackbar.UiSnackbarVisuals
import com.client.xvideos.ui.theme.XvideosTheme
import kotlinx.coroutines.delay

@Composable
fun RootSnackbarHost(snackBarHostState: SnackbarHostState) {
    Box(modifier = Modifier.zIndex(Float.MAX_VALUE)) {
        SnackbarHost(snackBarHostState) { data ->
            val uiMsg = (data.visuals as? UiSnackbarVisuals)?.ui ?: UiMessage.Info(data.visuals.message)

            val (bg, fg, icon) = when (uiMsg) {
                is UiMessage.Success -> Triple(Color(0xFF0F9960), Color.White, Icons.Default.Check)
                is UiMessage.Error -> Triple(Color(0xFFD13913), Color.White, Icons.Default.ErrorOutline)
                is UiMessage.Info -> Triple(Color(0xFF137CBD), Color.White, Icons.Default.Info)
                is UiMessage.Warning -> Triple(Color(0xFFFF8E0C), Color.White, Icons.Default.Info)
            }

            LaunchedEffect(data) {
                val duration = when (uiMsg) {
                    is UiMessage.Error -> 5000L
                    else -> 2000L
                }
                delay(duration)
                data.dismiss()
            }

            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 104.dp),
                color = bg,
                contentColor = fg,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(data.visuals.message, fontFamily = Theme.R.fontFamilyDMsanss)
                    data.visuals.actionLabel?.let { label ->
                        TextButton(onClick = { data.performAction() }) {
                            Text(label)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = false)
@Composable
fun RootSnackbarHostPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        snackbarHostState.showSnackbar(
            UiSnackbarVisuals(
                ui = UiMessage.Success("Operation completed successfully!"),
                actionLabel = "OK"
            )
        )
    }
    XvideosTheme {
        RootSnackbarHost(snackBarHostState = snackbarHostState)
    }
}
