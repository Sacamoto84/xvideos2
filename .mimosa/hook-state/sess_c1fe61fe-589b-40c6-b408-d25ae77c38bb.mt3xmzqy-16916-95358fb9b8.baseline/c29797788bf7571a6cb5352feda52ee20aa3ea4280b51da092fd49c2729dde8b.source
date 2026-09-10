package com.client.xvideos.r.ui.fullscreen


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.connectivityObserver.ConnectivityObserver
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.common.search.R_SearchExplorer
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.common.video.PlayerControls
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject


class ScreenRedFullScreenSM @Inject constructor(
    val connectivityObserver: ConnectivityObserver,
    val downloadRed: DownloadRed,
    val block: BlockRed,
    val redApi: RedApi,
    val savedRed: SavedRed,
    val search: R_SearchExplorer,
) : ScreenModel {

    var play by mutableStateOf(true)
    var mute by mutableStateOf(true)
    var autoRotate by mutableStateOf(false)

    var enableAB by mutableStateOf(false)
    var timeA by mutableFloatStateOf(3f)
    var timeB by mutableFloatStateOf(6f)

    var currentPlayerControls by mutableStateOf<PlayerControls?>(null)

    var currentPlayerTime by mutableFloatStateOf(0f)
    var currentPlayerDuration by mutableIntStateOf(0)

    var bufferIng by mutableStateOf(false)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedFullScreen {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenRedFullScreenSM::class)
    abstract fun bindScreenRedFullScreenModel(screenModel: ScreenRedFullScreenSM): ScreenModel
}
