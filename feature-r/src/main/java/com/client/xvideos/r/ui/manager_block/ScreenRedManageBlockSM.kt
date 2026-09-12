package com.client.xvideos.r.ui.manager_block

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

class ScreenRedManageBlockSM @Inject constructor(
    blockRed: BlockRed
) : ScreenModel {
    val blockList: StateFlow<List<GifsInfo>> = blockRed.blockList
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedManageBlock {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenRedManageBlockSM::class)
    abstract fun bindScreenRedManageBlockScreenModel(hiltListScreenModel: ScreenRedManageBlockSM): ScreenModel
}


