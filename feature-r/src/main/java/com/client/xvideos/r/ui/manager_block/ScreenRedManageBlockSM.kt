package com.client.xvideos.r.ui.manager_block

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.model.GifsInfo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
class ScreenRedManageBlockSM @Inject constructor(
    private val blockRed: BlockRed
) : ScreenModel {
    val blockList: StateFlow<List<GifsInfo>> = blockRed.blockList

    fun unblock(item: GifsInfo) {
        blockRed.unblockItem(item)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedManageBlock {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenRedManageBlockSM::class)
    abstract fun bindScreenRedManageBlockScreenModel(hiltListScreenModel: ScreenRedManageBlockSM): ScreenModel
}


