package com.client.xvideos.xlive.ui.room

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import com.client.xvideos.common.videoplayer.local.atom.OrientationAndSystemBarsEffect
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.common.util.KeepScreenOn
import com.client.xvideos.common.util.findActivity
import com.client.xvideos.xlive.model.XLiveBio
import com.client.xvideos.xlive.model.XLiveRoom
import com.client.xvideos.xlive.model.genderLabel
import com.client.xvideos.xlive.model.onAirLabel
import com.client.xvideos.xlive.model.splitRoomTitle
import com.client.xvideos.xlive.record.XLiveRecordingUi
import com.client.xvideos.xlive.ui.XLiveColors
import com.client.xvideos.xlive.ui.login.ScreenXLiveLogin
import com.client.xvideos.xlive.ui.room.atom.XLiveMessage
import com.client.xvideos.xlive.ui.room.molecule.XLiveRoomAboutCard
import com.client.xvideos.xlive.ui.room.molecule.XLiveRoomButtons
import com.client.xvideos.xlive.ui.room.molecule.XLiveRoomHeader
import com.client.xvideos.xlive.ui.room.molecule.XLiveRoomPlayer
import com.client.xvideos.xlive.ui.room.molecule.XLiveRoomTopic
import com.client.xvideos.xlive.ui.tag.ScreenXLiveTag

/**
 * Комната XLive: живой поток сверху, под ним шапка модели, тема с тегами и
 * карточка «О модели». Чата пока нет.
 */
class ScreenXLiveRoom(private val room: String) : Screen {

    override val key: ScreenKey = "ScreenXLiveRoom_$room"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = rememberScreenModel { XLiveRoomSM(room) }
        val state by vm.state.collectAsStateWithLifecycle()
        val bio by vm.bio.collectAsStateWithLifecycle()
        val aboutExpanded by vm.aboutExpanded.collectAsStateWithLifecycle()
        val muted by vm.muted.collectAsStateWithLifecycle()
        val isSaved by vm.isSaved.collectAsStateWithLifecycle()
        val follow by vm.follow.collectAsStateWithLifecycle()
        val recording by vm.recording.collectAsStateWithLifecycle()
        val isFullScreen by vm.isFullScreen.collectAsStateWithLifecycle()

        OrientationAndSystemBarsEffect(isFullScreen)
        BackHandler(enabled = isFullScreen, onBack = vm::exitFullScreen)

        // Запись — пока открыт экран комнаты: уход с него и уход приложения в
        // фон её заканчивают. Пересоздание Activity (поворот, складывание) —
        // тоже уход композиции, но запись при нём продолжается.
        val activity = LocalContext.current.findActivity()
        DisposableEffect(vm) {
            onDispose { vm.onScreenGone(activity?.isChangingConfigurations == true) }
        }
        LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
            vm.onAppBackground(activity?.isChangingConfigurations == true)
        }
        KeepScreenOn(active = recording.isRecording)

        val actions = remember(vm, navigator) {
            XLiveRoomActions(
                onRetry = vm::reload,
                onToggleMute = vm::toggleMute,
                onPlayerStarted = vm::onPlayerStarted,
                onPlayerError = vm::onPlayerError,
                onBuffering = vm::onBuffering,
                onToggleAbout = vm::toggleAbout,
                onToggleSaved = vm::toggleSaved,
                onToggleRecording = vm::toggleRecording,
                onToggleFollow = vm::toggleFollow,
                onSignIn = { navigator.push(ScreenXLiveLogin()) },
                onOpenTag = { tag -> navigator.push(ScreenXLiveTag(tag)) },
                onToggleFullScreen = vm::toggleFullScreen,
                onExitFullScreen = vm::exitFullScreen,
            )
        }

        ScreenXLiveRoomContent(
            state = state,
            bio = bio,
            aboutExpanded = aboutExpanded,
            muted = muted,
            isSaved = isSaved,
            follow = follow,
            recording = recording,
            actions = actions,
            isFullScreen = isFullScreen,
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
    muted: Boolean,
    isSaved: Boolean,
    follow: XLiveFollowState,
    recording: XLiveRecordingUi,
    actions: XLiveRoomActions,
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = false,
) {
    // Над переключением состояний: позиция переживает и «Повторить», и сложение складного.
    val scrollState = rememberScrollState()

    // Сверху — отступ на вырез, как у меню и шапок X: статус-бар виден только
    // при вырезе сверху, и видео не должно уходить под него.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XLiveColors.Background)
            .windowInsetsPadding(
            if (isFullScreen) WindowInsets(0, 0, 0, 0)
            else WindowInsets.displayCutout.only(WindowInsetsSides.Top)
        )
    ) {
        when (state) {
            XLiveRoomState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
            is XLiveRoomState.Offline -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                XLiveMessage(text = "Комната не в открытом эфире: ${state.status}", onRetry = actions.onRetry)
            }
            is XLiveRoomState.Failed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                XLiveMessage(text = "Не удалось получить поток: ${state.reason}", onRetry = actions.onRetry)
            }
            is XLiveRoomState.Live -> {
                val info = state.room
                val topic = remember(info.title) { splitRoomTitle(info.title) }
                val onAir = remember(info.startedAt) {
                    onAirLabel(info.startedAt, System.currentTimeMillis() / MILLIS_PER_SECOND)
                }

                XLiveRoomPlayer(
                    hlsUrl = info.hlsUrl,
                    muted = muted,
                    onToggleMute = actions.onToggleMute,
                    onStarted = actions.onPlayerStarted,
                    onError = actions.onPlayerError,
                    onBuffering = actions.onBuffering,
                    modifier = if (isFullScreen) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(VIDEO_ASPECT_RATIO)
                    },
                    isFullScreen = isFullScreen,
                    title = info.username,
                    onToggleFullScreen = actions.onToggleFullScreen,
                    onExitFullScreen = actions.onExitFullScreen,
                    recordingStartedAt = recording.startedAtElapsedMs.takeIf { recording.isRecording },
                )
                if (isFullScreen) return@Column
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
                            buttons = {
                                XLiveRoomButtons(
                                    follow = follow,
                                    onToggleFollow = actions.onToggleFollow,
                                    onSignIn = actions.onSignIn,
                                    isSaved = isSaved,
                                    onToggleSaved = actions.onToggleSaved,
                                    recording = recording,
                                    onToggleRecording = actions.onToggleRecording,
                                )
                            },
                        )
                        if (topic.text.isNotBlank() || topic.tags.isNotEmpty()) {
                            XLiveRoomTopic(topic = topic, onOpenTag = actions.onOpenTag)
                        }
                        if (bio != null) {
                            XLiveRoomAboutCard(
                                bio = bio,
                                gender = genderLabel(info.gender),
                                aboutExpanded = aboutExpanded,
                                onToggleAbout = actions.onToggleAbout,
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
        muted = true,
        isSaved = false,
        follow = XLiveFollowState(),
        recording = XLiveRecordingUi(),
        actions = XLiveRoomActions(),
    )
}
