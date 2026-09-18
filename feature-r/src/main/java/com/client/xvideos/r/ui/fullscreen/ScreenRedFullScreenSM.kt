package com.client.xvideos.r.ui.fullscreen


import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.common.search.R_SearchExplorer
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.common.video.PlayerControls
import com.client.xvideos.common.videoplayer.model.PlayerSpeed
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject


@Stable
class ScreenRedFullScreenSM @Inject constructor(
    val downloadRed: DownloadRed,
    val block: BlockRed,
    val redApi: RedApi,
    val savedRed: SavedRed,
    val search: R_SearchExplorer,
) : ScreenModel {

    var play by mutableStateOf(true)
    var mute by mutableStateOf(true)
    var autoRotate by mutableStateOf(false)
    var speed by mutableStateOf(PlayerSpeed.DEFAULT)

    var enableAB by mutableStateOf(false)
    var timeA by mutableFloatStateOf(3f)
    var timeB by mutableFloatStateOf(6f)

    var currentPlayerControls by mutableStateOf<PlayerControls?>(null)

    var currentPlayerTime by mutableFloatStateOf(0f)
    var currentPlayerDuration by mutableIntStateOf(0)

    fun setTimeA() {
        timeA = sanitizePointTime(currentPlayerTime, currentPlayerDuration)
        if (enableAB && timeB <= timeA) {
            enableAB = false
        }
    }

    fun setTimeB() {
        timeB = sanitizePointTime(currentPlayerTime, currentPlayerDuration)
        if (enableAB && timeB <= timeA) {
            enableAB = false
        }
    }

    fun toggleAB() {
        if (!enableAB && timeB <= timeA) {
            SnackBar.warning("Точка B должна быть больше точки A")
        } else {
            enableAB = !enableAB
        }
    }

    fun togglePlay() {
        play = !play
        if (play) {
            currentPlayerControls?.play()
        } else {
            currentPlayerControls?.pause()
        }
    }

    fun rewind(seconds: Float = 1f) {
        currentPlayerControls?.rewind(seconds)
    }

    fun forward(seconds: Float = 1f) {
        currentPlayerControls?.forward(seconds)
    }

    fun toggleMute() {
        mute = !mute
    }

    fun resetSpeed() {
        speed = PlayerSpeed.DEFAULT
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedFullScreen {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenRedFullScreenSM::class)
    abstract fun bindScreenRedFullScreenModel(screenModel: ScreenRedFullScreenSM): ScreenModel
}

internal fun sanitizePointTime(time: Float, durationSec: Int): Float {
    if (!time.isFinite() || time < 0f) return 0f
    return if (durationSec > 0) time.coerceAtMost(durationSec.toFloat()) else time
}
