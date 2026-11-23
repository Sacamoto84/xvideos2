package com.client.xvideos.r.ui.explorer.tab.saved.tab.account

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.r.auth.RAccountSession
import com.client.xvideos.r.auth.RAccountStatus
import com.client.xvideos.r.network.api.AccountMembership
import com.client.xvideos.r.network.toRUserMessage
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal enum class AccountSavedList { LIKES, AUTHORS, NICHES, COLLECTIONS, COLLECTION_ITEMS }

internal val AccountMembership.savedList: AccountSavedList
    get() = when (this) {
        AccountMembership.LIKE -> AccountSavedList.LIKES
        AccountMembership.AUTHOR -> AccountSavedList.AUTHORS
        AccountMembership.NICHE -> AccountSavedList.NICHES
    }

internal data class AccountSavedKey(val list: AccountSavedList, val id: String = "")
internal data class AccountSavedOperation(val account: RAccountStatus.SignedIn, val key: AccountSavedKey)

internal object AccountSavedActions {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val revisions = MutableStateFlow<Map<AccountSavedKey, Long>>(emptyMap())
    private val mutablePending = MutableStateFlow<Set<AccountSavedOperation>>(emptySet())
    val pending = mutablePending.asStateFlow()

    private data class Members(val account: RAccountStatus.SignedIn, val revision: Long, val ids: Set<String>)
    private val memberLocks = AccountMembership.entries.associateWith { Mutex() }
    private val members = mutableMapOf<AccountMembership, Members>()

    fun revision(key: AccountSavedKey) = revisions.map { it[key] ?: 0L }.distinctUntilChanged()

    suspend fun member(
        account: RAccountStatus.SignedIn,
        kind: AccountMembership,
        id: String,
    ): Result<Boolean> = memberLocks.getValue(kind).withLock {
        val revision = revisions.value[AccountSavedKey(kind.savedList)] ?: 0L
        val cached = members[kind]
        if (cached != null && cached.account === account && cached.revision == revision) {
            return@withLock Result.success(id in cached.ids)
        }
        kind.ids(account)
            .onSuccess { ids -> members[kind] = Members(account, revision, ids) }
            .map { id in it }
    }

    fun <T> run(
        context: Context,
        operation: AccountSavedOperation,
        vararg changedLists: AccountSavedKey,
        successMessage: String? = null,
        request: suspend () -> Result<T>,
        onResult: (Result<T>) -> Unit = {},
    ) {
        if (operation in pending.value) {
            onResult(Result.failure(IllegalStateException("Дождитесь завершения предыдущего действия")))
            return
        }
        val appContext = context.applicationContext
        mutablePending.update { it + operation }
        scope.launch {
            val result = try {
                request()
            } finally {
                mutablePending.update { it - operation }
            }
            if (result.isSuccess) {
                if (RAccountSession.status.value !== operation.account) return@launch
                revisions.update { previous ->
                    previous.toMutableMap().apply {
                        for (key in changedLists) put(key, (get(key) ?: 0L) + 1)
                    }
                }
            }
            val message = result.fold({ successMessage }, { it.accountActionMessage() })
            if (message != null) Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
            onResult(result)
        }
    }
}

internal fun Throwable.accountActionMessage(): String =
    if (this is ResponseException && response.status == HttpStatusCode.Unauthorized) {
        "Войдите в RedGIFs повторно в настройках R"
    } else {
        toRUserMessage()
    }

@Composable
internal fun signedInRedgifsAccount(): RAccountStatus.SignedIn? {
    if (LocalInspectionMode.current) return null
    val account by RAccountSession.status.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { RAccountSession.restore() }
    return account as? RAccountStatus.SignedIn
}
