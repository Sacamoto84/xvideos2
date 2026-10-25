package com.client.xvideos.xlive.ui.room

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.xlive.model.XLiveBio
import com.client.xvideos.xlive.model.XLiveRoom
import com.client.xvideos.xlive.model.genderLabel
import com.client.xvideos.xlive.model.onAirLabel
import com.client.xvideos.xlive.model.splitRoomTitle
import com.client.xvideos.xlive.ui.XLiveColors
import com.client.xvideos.xlive.ui.room.atom.XLiveMessage
import com.client.xvideos.xlive.ui.room.molecule.XLiveRoomAboutCard
import com.client.xvideos.xlive.ui.room.molecule.XLiveRoomHeader
import com.client.xvideos.xlive.ui.room.molecule.XLiveRoomPlayer
import com.client.xvideos.xlive.ui.room.molecule.XLiveRoomTopic

/**
 * Комната XLive: живой поток сверху, под ним шапка модели, тема с тегами и
 * карточка «О модели». Чата пока нет.
 */
class ScreenXLiveRoom(private val room: String) : Screen {

    override val key: ScreenKey = "ScreenXLiveRoom_$room"

    @Composable
    override fun Content() {
        val vm = rememberScreenModel { XLiveRoomSM(room) }
        val state by vm.state.collectAsStateWithLifecycle()
        val bio by vm.bio.collectAsStateWithLifecycle()
        val aboutExpanded by vm.aboutExpanded.collectAsStateWithLifecycle()

        ScreenXLiveRoomContent(
            state = state,
            bio = bio,
            aboutExpanded = aboutExpanded,
            onRetry = vm::reload,
            onPlayerStarted = vm::onPlayerStarted,
            onPlayerError = vm::onPlayerError,
            onBuffering = vm::onBuffering,
            onToggleAbout = vm::toggleAbout,
        )
    }
}

private const val VIDEO_ASPECT_RATIO = 16f / 9f
private const val MILLIS_PER_SECOND = 1000L

@Composable
fun ScreenXLiveRoomContent(
    state: XLiveRoomState,
    bio: XLiveBio?,
    aboutExpanded: Boolean,
    onRetry: () -> Unit,
    onPlayerStarted: (String) -> Unit,
    onPlayerError: (String) -> Unit,
    onBuffering: (Boolean) -> Unit,
    onToggleAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Над переключением состояний: позиция переживает и «Повторить», и сложение складного.
    val scrollState = rememberScrollState()

    // Сверху — отступ на вырез, как у меню и шапок X: статус-бар виден только
    // при вырезе сверху, и видео не должно уходить под него.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XLiveColors.Background)
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
    ) {
        when (state) {
            XLiveRoomState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
            is XLiveRoomState.Offline -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                XLiveMessage(text = "Комната не в открытом эфире: ${state.status}", onRetry = onRetry)
            }
            is XLiveRoomState.Failed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                XLiveMessage(text = "Не удалось получить поток: ${state.reason}", onRetry = onRetry)
            }
            is XLiveRoomState.Live -> {
                val info = state.room
                val topic = remember(info.title) { splitRoomTitle(info.title) }
                val onAir = remember(info.startedAt) {
                    onAirLabel(info.startedAt, System.currentTimeMillis() / MILLIS_PER_SECOND)
                }

                XLiveRoomPlayer(
                    hlsUrl = info.hlsUrl,
                    onStarted = onPlayerStarted,
                    onError = onPlayerError,
                    onBuffering = onBuffering,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(VIDEO_ASPECT_RATIO),
                )
                // Прокрутка на всю ширину, содержимое — по центру с лимитом ширины
                // на раскрытом складном: свайп работает и по краям.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    Column(
                        modifier = Modifier
                            .adaptiveContentWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        XLiveRoomHeader(
                            name = info.username,
                            realName = bio?.realName.orEmpty(),
                            viewers = info.viewers,
                            followers = bio?.followers,
                            onAir = onAir,
                            ratingPercent = info.ratingPercent,
                        )
                        if (topic.text.isNotBlank() || topic.tags.isNotEmpty()) {
                            XLiveRoomTopic(topic = topic)
                        }
                        if (bio != null) {
                            XLiveRoomAboutCard(
                                bio = bio,
                                gender = genderLabel(info.gender),
                                aboutExpanded = aboutExpanded,
                                onToggleAbout = onToggleAbout,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(device = "id:pixel_9_pro")
@Composable
private fun ScreenXLiveRoomPreview() {
    ScreenXLiveRoomContent(
        state = XLiveRoomState.Live(
            XLiveRoom(
                status = "public",
                hlsUrl = "",
                username = "room_a",
                title = "goal: 100 tk #petite #ginger #new",
                viewers = 562,
                gender = "female",
                ratingPercent = 97,
            )
        ),
        bio = XLiveBio(realName = "Anna", age = 24, languages = "English", followers = 20332, aboutMe = "Hello!"),
        aboutExpanded = false,
        onRetry = {},
        onPlayerStarted = {},
        onPlayerError = {},
        onBuffering = {},
        onToggleAbout = {},
    )
}
