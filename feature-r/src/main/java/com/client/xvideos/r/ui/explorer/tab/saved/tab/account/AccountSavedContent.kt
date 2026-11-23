package com.client.xvideos.r.ui.explorer.tab.saved.tab.account

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.client.xvideos.common.ui.atom.SavedTabHeader
import com.client.xvideos.common.util.getTopInsetDp
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

@Composable
internal fun AccountSavedHeader(
    title: String,
    onBack: () -> Unit,
    onRefresh: (() -> Unit)? = null,
    refreshing: Boolean = false,
    actions: @Composable () -> Unit = {},
) {
    SavedTabHeader(
        title = title,
        topCutout = getTopInsetDp(),
        navigation = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
        },
        trailing = {
            actions()
            if (onRefresh != null) {
                IconButton(onClick = onRefresh, enabled = !refreshing) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Обновить")
                }
            }
        },
    )
}

@Composable
internal fun AccountSavedMessage(message: String) {
    Text(message, modifier = Modifier.padding(horizontal = 20.dp), color = Color.White, textAlign = TextAlign.Center)
}
