package com.client.xvideos.xlive.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.xlive.ui.atom.XLiveMessage
import com.client.xvideos.xlive.ui.atom.XLiveRoomBadge
import com.client.xvideos.xlive.ui.molecule.XLiveStreamPlayer

/**
 * Корневой экран раздела XLive.
 *
 * Пока эксперимент: живой поток одной комнаты [room]. Список комнат и выбор —
 * следующими шагами.
 */
class ScreenXLive(private val room: String = EXPERIMENT_ROOM) : Screen {

    override val key: ScreenKey = "ScreenXLive_$room"

    @Composable
    override fun Content() {
        val vm = rememberScreenModel { XLiveSM(room) }
        val state by vm.state.collectAsStateWithLifecycle()

        ScreenXLiveContent(
            state = state,
            requestedRoom = room,
            onRetry = vm::reload,
            onPlayerError = vm::onPlayerError,
            onBuffering = vm::onBuffering,
        )
    }
}

/** Комната, на которой проверяется получение потока. */
private const val EXPERIMENT_ROOM = "glossybabe_"

@Composable
fun ScreenXLiveContent(
    state: XLiveState,
    requestedRoom: String,
    onRetry: () -> Unit,
    onPlayerError: (String) -> Unit,
    onBuffering: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.backgroundAppRoot),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            XLiveState.Loading -> CircularProgressIndicator(color = Color.White)
            is XLiveState.Live -> {
                XLiveStreamPlayer(
                    hlsUrl = state.hlsUrl,
                    onError = onPlayerError,
                    onBuffering = onBuffering,
                    modifier = Modifier.fillMaxSize(),
                )
                XLiveRoomBadge(
                    text = state.requestedStatus
                        ?.let { "$requestedRoom: $it · показываю ${state.room}" }
                        ?: state.room,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(8.dp),
                )
            }
            is XLiveState.Offline -> XLiveMessage(
                text = "Комната не в открытом эфире: ${state.status}",
                onRetry = onRetry,
            )
            is XLiveState.Failed -> XLiveMessage(
                text = "Не удалось получить поток: ${state.reason}",
                onRetry = onRetry,
            )
        }
    }
}

@Preview(device = "id:pixel_9_pro")
@Composable
private fun ScreenXLivePreview() {
    ScreenXLiveContent(
        state = XLiveState.Offline("offline"),
        requestedRoom = "room_a",
        onRetry = {},
        onPlayerError = {},
        onBuffering = {},
    )
}
