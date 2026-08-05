package com.client.xvideos.common.p2p.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.eventBus.Event
import com.client.xvideos.common.eventBus.EventBus
import com.client.xvideos.common.theme.Theme
import kotlinx.coroutines.delay

/**
 * Плашка поверх контента с ходом приёма файлов по P2P (Nearby):
 * прогресс, успешное завершение или ошибка. Слушает [EventBus].
 */
@Composable
fun P2pBackgroundOverlay() {
    val event by EventBus.events.collectAsState(null)
    var visible by rememberSaveable { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf("") }
    var progress by remember { mutableStateOf(0f) }
    var isError by rememberSaveable { mutableStateOf(false) }
    var isSuccess by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(event) {
        when (val e = event) {
            is Event.P2pTransferUpdate.Progress -> {
                visible = true
                isError = false
                isSuccess = false
                text = "Приём от ${e.endpointName}"
                progress = if (e.total > 0) e.transferred.toFloat() / e.total else 0f
            }
            is Event.P2pTransferUpdate.Success -> {
                visible = true
                isError = false
                isSuccess = true
                text = "Получено от ${e.endpointName} ✓"
                progress = 1f
                delay(3000)
                visible = false
            }
            is Event.P2pTransferUpdate.Error -> {
                visible = true
                isError = true
                isSuccess = false
                text = "Ошибка приёма: ${e.message}"
                delay(5000)
                visible = false
            }
            else -> {}
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                // Бары спрятаны (hide(systemBars)), их инсет всегда 0 — отступаем от выреза камеры.
                .displayCutoutPadding()
                .padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (isError) Theme.L.r0 else if (isSuccess) Theme.L.g0 else Color(0xFF2C2C2C),
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Wifi,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
                if (!isError && !isSuccess) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}
