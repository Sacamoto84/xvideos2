package com.client.xvideos.r.ui.manager_block

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.common.block.useCase.blockGetAllBlockedGifsInfo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ScreenRedManageBlockSM @Inject constructor() : ScreenModel {
    val _blockList = MutableStateFlow<List<GifsInfo>>(emptyList())
    val blockList: StateFlow<List<GifsInfo>> = _blockList

    init {
        _blockList.value = blockGetAllBlockedGifsInfo()
    }

}


@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedMnageBlock {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenRedManageBlockSM::class)
    abstract fun bindScreenRedMnageBlockScreenModel(hiltListScreenModel: ScreenRedManageBlockSM): ScreenModel
}


