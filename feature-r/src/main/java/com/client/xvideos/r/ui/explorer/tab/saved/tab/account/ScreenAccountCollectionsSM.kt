package com.client.xvideos.r.ui.explorer.tab.saved.tab.account

import androidx.compose.runtime.Stable
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.r.auth.RAccountStatus
import com.client.xvideos.r.network.api.AccountCollection
import com.client.xvideos.r.network.api.RedApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

private const val COLLECTION_PAGE_SIZE = 50

@Stable
class ScreenAccountCollectionsSM @Inject constructor(redApi: RedApi) : ScreenModel {
    @OptIn(ExperimentalCoroutinesApi::class)
    internal val state = accountSavedContent { accountScope ->
        AccountSavedActions.revision(AccountSavedKey(AccountSavedList.COLLECTIONS))
            .flatMapLatest { accountCollectionsPager(redApi).flow }
            .cachedIn(accountScope)
    }
}

internal fun accountCollectionsPager(redApi: RedApi, account: RAccountStatus.SignedIn? = null) = Pager(
    PagingConfig(pageSize = COLLECTION_PAGE_SIZE, initialLoadSize = COLLECTION_PAGE_SIZE, enablePlaceholders = false),
) { AccountCollectionsPagingSource(redApi, account) }

private class AccountCollectionsPagingSource(
    private val redApi: RedApi,
    private val account: RAccountStatus.SignedIn?,
) : PagingSource<Int, AccountCollection>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AccountCollection> {
        val page = params.key ?: 1
        return redApi.getAccountCollections(page, COLLECTION_PAGE_SIZE, account).fold(
            onSuccess = { response ->
                LoadResult.Page(
                    data = response.collections.filter { it.folderId.isNotBlank() }.distinctBy { it.folderId },
                    prevKey = null,
                    nextKey = if (page < response.pages && response.collections.isNotEmpty()) page + 1 else null,
                )
            },
            onFailure = { LoadResult.Error(it) },
        )
    }

    override fun getRefreshKey(state: PagingState<Int, AccountCollection>): Int? = null
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedAccountCollections {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenAccountCollectionsSM::class)
    abstract fun bindScreenModel(model: ScreenAccountCollectionsSM): ScreenModel
}
