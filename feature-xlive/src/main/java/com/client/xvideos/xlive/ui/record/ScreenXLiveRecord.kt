package com.client.xvideos.xlive.ui.record

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.common.videoplayer.local.atom.OrientationAndSystemBarsEffect
import com.client.xvideos.common.videoplayer.local.molecule.LocalVideoPlayerContent
import com.client.xvideos.xlive.model.XLiveBio
import com.client.xvideos.xlive.model.XLiveRecord
import com.client.xvideos.xlive.model.XLiveRecordRoom
import com.client.xvideos.xlive.model.genderLabel
import com.client.xvideos.xlive.model.isPlaybackFinished
import com.client.xvideos.xlive.model.splitRoomTitle
import com.client.xvideos.xlive.ui.XLiveColors
import com.client.xvideos.xlive.ui.record.molecule.XLiveRecordHeader
import com.client.xvideos.xlive.ui.room.ScreenXLiveRoom
import com.client.xvideos.xlive.ui.room.molecule.XLiveRoomAboutCard
import com.client.xvideos.xlive.ui.room.molecule.XLiveRoomTopic
import com.client.xvideos.xlive.ui.tag.ScreenXLiveTag
import java.io.File

/**
 * Запись эфира: плеер сверху, под ним шапка, тема с тегами и «О модели» —
 * как в комнате, но на момент записи. Плеер — плеер скачанного ролика X:
 * панель, превью кадра, на весь экран с поворотом; у записи есть звук,
 * включает его кнопка.
 */
class ScreenXLiveRecord(private val id: Long) : Screen {

    override val key: ScreenKey = "ScreenXLiveRecord_$id"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val vm = rememberScreenModel { XLiveRecordSM(id) }
        val record by vm.record.collectAsStateWithLifecycle()
        val storyboard by vm.storyboard.collectAsStateWithLifecycle()
        val isSaved by vm.isSaved.collectAsStateWithLifecycle()
        val muted by vm.muted.collectAsStateWithLifecycle()
        val isFullScreen by vm.isFullScreen.collectAsStateWithLifecycle()
        val aboutExpanded by vm.aboutExpanded.collectAsStateWithLifecycle()

        OrientationAndSystemBarsEffect(isFullScreen)

        val actions = remember(vm, navigator, context) {
            XLiveRecordActions(
                onBack = { navigator.pop() },
                onOpenRoom = { vm.record.value?.let { navigator.push(ScreenXLiveRoom(it.username)) } },
                onOpenTag = { tag -> navigator.push(ScreenXLiveTag(tag)) },
                onToggleSaved = vm::toggleSaved,
                onSaveToGallery = { vm.saveToGallery(context) },
                onToggleMute = vm::toggleMute,
                onToggleFullScreen = vm::toggleFullScreen,
                onExitFullScreen = vm::exitFullScreen,
                onToggleAbout = vm::toggleAbout,
                onSavePosition = { seconds, totalSec -> vm.savePosition(seconds, totalSec) },
            )
        }

        ScreenXLiveRecordContent(
            record = record,
            videoUrl = vm.videoUrl,
            storyboard = storyboard,
            isSaved = isSaved,
            muted = muted,
            isFullScreen = isFullScreen,
            aboutExpanded = aboutExpanded,
            actions = actions,
        )
    }
}

private const val VIDEO_ASPECT_RATIO = 16f / 9f

@Composable
fun ScreenXLiveRecordContent(
    record: XLiveRecord?,
    videoUrl: String,
    storyboard: File?,
    isSaved: Boolean,
    muted: Boolean,
    isFullScreen: Boolean,
    aboutExpanded: Boolean,
    actions: XLiveRecordActions,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XLiveColors.Background)
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
    ) {
        if (record == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
            return@Column
        }
        // Позиция старта берётся плеером один раз — при создании. Если прошлый
        // просмотр был завершён (позиция в конце), перекидываем в начало.
        val resume = remember(record.id) {
            if (record.isPlaybackFinished) null else record.positionSec.takeIf { it > 0f }
        }

        LaunchedEffect(record.id, record.isPlaybackFinished) {
            if (record.isPlaybackFinished) {
                actions.onSavePosition(0f, (record.durationMs / 1000f).toInt())
            }
        }

        LocalVideoPlayerContent(
            fileUrl = videoUrl,
            resumePosition = resume,
            onSaveProgress = actions.onSavePosition,
            onPopBack = actions.onBack,
            modifier = if (isFullScreen) {
                Modifier.fillMaxSize()
            } else {
                Modifier.fillMaxWidth().aspectRatio(VIDEO_ASPECT_RATIO)
            },
            storyboard = storyboard,
            isFullScreen = isFullScreen,
            title = record.username,
            onToggleFullScreen = actions.onToggleFullScreen,
            onExitFullScreen = actions.onExitFullScreen,
            muted = muted,
            onToggleMute = actions.onToggleMute,
            showResumeNotice = false,
        )
        if (isFullScreen) return@Column

        val topic = remember(record.room.title) { splitRoomTitle(record.room.title) }
        // Прокрутка на всю ширину, содержимое — по центру с лимитом ширины на
        // раскрытом складном, как в комнате.
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
                XLiveRecordHeader(
                    record = record,
                    isSaved = isSaved,
                    onToggleSaved = actions.onToggleSaved,
                    onOpenRoom = actions.onOpenRoom,
                    onSaveToGallery = actions.onSaveToGallery,
                )
                if (topic.text.isNotBlank() || topic.tags.isNotEmpty()) {
                    XLiveRoomTopic(topic = topic, onOpenTag = actions.onOpenTag)
                }
                record.bio?.let { bio ->
                    XLiveRoomAboutCard(
                        bio = bio,
                        gender = genderLabel(record.room.gender),
                        aboutExpanded = aboutExpanded,
                        onToggleAbout = actions.onToggleAbout,
                    )
                }
            }
        }
    }
}

@Preview(device = "id:pixel_9_pro")
@Composable
private fun ScreenXLiveRecordPreview() {
    ScreenXLiveRecordContent(
        record = XLiveRecord(
            id = 1_793_043_600_000L,
            username = "room_a",
            durationMs = 4_325_000L,
            sizeBytes = 2_254_857_830L,
            room = XLiveRecordRoom(title = "goal: 100 tk #new", viewers = 562, gender = "female", ratingPercent = 97),
            bio = XLiveBio(realName = "Anna", age = 24, followers = 20_332, aboutMe = "Hello!"),
        ),
        videoUrl = "",
        storyboard = null,
        isSaved = false,
        muted = true,
        isFullScreen = false,
        aboutExpanded = false,
        actions = XLiveRecordActions(),
    )
}
