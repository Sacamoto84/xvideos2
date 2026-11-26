package com.client.xvideos.r.ui.explorer.tab.saved.tab.account

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.client.xvideos.r.auth.RAccountSession
import com.client.xvideos.r.auth.RAccountStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

@Stable
internal sealed interface AccountSavedContent<out T> {
    data object Loading : AccountSavedContent<Nothing>
    data object SignedOut : AccountSavedContent<Nothing>
    data class Ready<T>(val value: T) : AccountSavedContent<T>
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> ScreenModel.accountSavedContent(create: (CoroutineScope) -> T) = RAccountSession.status
    .onStart { RAccountSession.restore() }
    .transformLatest<RAccountStatus, AccountSavedContent<T>> { account ->
        when (account) {
            RAccountStatus.Loading -> emit(AccountSavedContent.Loading)
            RAccountStatus.SignedOut, RAccountStatus.Expired -> emit(AccountSavedContent.SignedOut)
            is RAccountStatus.SignedIn -> {
                // Смена аккаунта отменяет загрузку и освобождает кэш его страниц.
                emit(AccountSavedContent.Ready(create(CoroutineScope(currentCoroutineContext()))))
                awaitCancellation()
            }
        }
    }
    .stateIn(screenModelScope, SharingStarted.Lazily, AccountSavedContent.Loading)
