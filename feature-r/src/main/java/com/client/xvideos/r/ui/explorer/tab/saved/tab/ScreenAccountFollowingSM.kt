package com.client.xvideos.r.ui.explorer.tab.saved.tab

import androidx.compose.runtime.Stable
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.r.auth.RAccountSession
import com.client.xvideos.r.auth.RAccountStatus
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedActions
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedKey
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedList
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import javax.inject.Inject

private const val FOLLOWING_PAGE_SIZE = 100

internal sealed interface AccountFollowingState {
    data object Loading : AccountFollowingState
    data object SignedOut : AccountFollowingState
    data class Ready(val creators: Flow<PagingData<UserInfo>>) : AccountFollowingState
}

@Stable
class ScreenAccountFollowingSM @Inject constructor(redApi: RedApi) : ScreenModel {
    @OptIn(ExperimentalCoroutinesApi::class)
    internal val state = RAccountSession.status
        .onStart { RAccountSession.restore() }
        .transformLatest<RAccountStatus, AccountFollowingState> { account ->
            when (account) {
                RAccountStatus.Loading -> emit(AccountFollowingState.Loading)
                RAccountStatus.SignedOut, RAccountStatus.Expired -> emit(AccountFollowingState.SignedOut)
                is RAccountStatus.SignedIn -> {
                    val config = PagingConfig(
                        pageSize = FOLLOWING_PAGE_SIZE,
                        initialLoadSize = FOLLOWING_PAGE_SIZE,
                        enablePlaceholders = false,
                    )
                    // Кэш страниц живёт вместе с сессией этого аккаунта.
                    val accountScope = CoroutineScope(currentCoroutineContext())
                    val creators = AccountSavedActions.revision(AccountSavedKey(AccountSavedList.AUTHORS))
                        .flatMapLatest { Pager(config) { AccountFollowingPagingSource(redApi) }.flow }
                        .cachedIn(accountScope)
                    emit(AccountFollowingState.Ready(creators))
                    awaitCancellation()
                }
            }
        }
        .stateIn(screenModelScope, SharingStarted.Lazily, AccountFollowingState.Loading)
}

private class AccountFollowingPagingSource(private val redApi: RedApi) : PagingSource<Int, UserInfo>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UserInfo> {
        val page = params.key ?: 1
        return redApi.getAccountFollowing(page, FOLLOWING_PAGE_SIZE).fold(
            onSuccess = { response ->
                LoadResult.Page(
                    data = response.items.filter { it.isValid }.distinctBy { it.normalizedUsername },
                    prevKey = null,
                    nextKey = (page + 1).takeIf { page < response.pages && response.items.isNotEmpty() },
                )
            },
            onFailure = { LoadResult.Error(it) },
        )
    }

    override fun getRefreshKey(state: PagingState<Int, UserInfo>): Int? = null
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedAccountFollowing {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenAccountFollowingSM::class)
    abstract fun bindScreenModel(model: ScreenAccountFollowingSM): ScreenModel
}
