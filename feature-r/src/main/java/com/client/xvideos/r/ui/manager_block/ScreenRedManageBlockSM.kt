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

/**
 * [ScreenModel] экрана управления черным списком заблокированных роликов RedGifs.
 *
 * @param blockRed Синглтон управления блокировками.
 */
@Stable
class ScreenRedManageBlockSM @Inject constructor(
    private val blockRed: BlockRed
) : ScreenModel {
    /** Список всех заблокированных элементов. */
    val blockList: StateFlow<List<GifsInfo>> = blockRed.blockList

    /** Разблокирует указанный элемент [item]. */
    fun unblock(item: GifsInfo) {
        blockRed.unblockItem(item)
    }
}

/** Hilt-модуль регистрации [ScreenRedManageBlockSM]. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedManageBlock {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenRedManageBlockSM::class)
    abstract fun bindScreenRedManageBlockScreenModel(hiltListScreenModel: ScreenRedManageBlockSM): ScreenModel
}
