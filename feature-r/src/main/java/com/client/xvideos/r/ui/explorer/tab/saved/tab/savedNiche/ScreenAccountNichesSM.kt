package com.client.xvideos.r.ui.explorer.tab.saved.tab.savedNiche

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.r.auth.RAccountSession
import com.client.xvideos.r.auth.RAccountStatus
import com.client.xvideos.r.model.Niche
import com.client.xvideos.r.network.api.AccountNicheList
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.network.toRUserMessage
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedActions
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedKey
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedList
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import javax.inject.Inject

internal sealed interface AccountNichesContent {
    data object Loading : AccountNichesContent
    data object SignedOut : AccountNichesContent
    data class Loaded(val niches: List<Niche>) : AccountNichesContent
    data class Failed(val message: String) : AccountNichesContent
}

internal data class AccountNichesState(val list: AccountNicheList, val content: AccountNichesContent)

@Stable
class ScreenAccountNichesSM @Inject constructor(redApi: RedApi) : ScreenModel {
    private data class Request(val list: AccountNicheList = AccountNicheList.FOLLOWING, val refresh: Int = 0)

    private val request = MutableStateFlow(Request())

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val state = combine(
        RAccountSession.status, request, AccountSavedActions.revision(AccountSavedKey(AccountSavedList.NICHES)),
    ) { account, request, _ -> account to request }
        .transformLatest { (account, request) ->
            val list = request.list
            when (account) {
                RAccountStatus.Loading -> emit(AccountNichesState(list, AccountNichesContent.Loading))
                is RAccountStatus.SignedIn -> {
                    emit(AccountNichesState(list, AccountNichesContent.Loading))
                    val content = redApi.getAccountNiches(list).fold(
                        onSuccess = { AccountNichesContent.Loaded(it) },
                        onFailure = { AccountNichesContent.Failed(it.toRUserMessage()) },
                    )
                    if (RAccountSession.status.value == account) emit(AccountNichesState(list, content))
                }
                RAccountStatus.SignedOut, RAccountStatus.Expired ->
                    emit(AccountNichesState(list, AccountNichesContent.SignedOut))
            }
        }
        .onStart { RAccountSession.restore() }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(replayExpirationMillis = 0),
            AccountNichesState(AccountNicheList.FOLLOWING, AccountNichesContent.Loading),
        )

    internal fun select(list: AccountNicheList) {
        request.update { if (it.list == list) it else Request(list) }
    }

    fun refresh() {
        request.update { it.copy(refresh = it.refresh + 1) }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedAccountNiches {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenAccountNichesSM::class)
    abstract fun bindScreenModel(model: ScreenAccountNichesSM): ScreenModel
}
