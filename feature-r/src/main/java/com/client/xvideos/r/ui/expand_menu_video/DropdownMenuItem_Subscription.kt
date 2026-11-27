package com.client.xvideos.r.ui.expand_menu_video

import androidx.compose.runtime.Composable
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.network.api.RedApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun DropdownMenuItem_Subscription(item: GifsInfo? = null, redApi: () -> RedApi, savedRed: () -> SavedRed, onDismiss: () -> Unit) {
    val isSubscribed = item?.userName?.takeIf { it.isNotBlank() }?.let { name ->
        savedRed.invoke().subscriptions.listCreators.any { it.username == name }
    } ?: false

    DropdownMenuItem_SubscriptionContent(
        isSubscribed = isSubscribed,
        onClick = {
            if (item == null || item.userName.isBlank()) {
                onDismiss.invoke()
                return@DropdownMenuItem_SubscriptionContent
            }

            // см. комментарий в DropdownMenuItem_Like: управляемый scope из
            // SavedRed вместо GlobalScope, переживающий закрытие меню.
            savedRed.invoke().scope.launch {
                delay(200)
                if (!isSubscribed) {
                    // См. DropdownMenuItem_Follow: прежний `add(getOrNull()!!)`
                    // внутри `catch { printStackTrace() }` молча проглатывал NPE
                    // при любой сетевой ошибке — подписка не срабатывала без
                    // единого следа в логе.
                    redApi.invoke().readCreator(item.userName)
                        .onSuccess { savedRed.invoke().subscriptions.add(it) }
                        .onFailure { e ->
                            Timber.e("Subscribe: не удалось получить профиль ${item.userName}: ${e.javaClass.simpleName}")
                            SnackBar.error("Не удалось оформить подписку: ${e.message ?: "нет сети"}")
                        }
                } else {
                    savedRed.invoke().subscriptions.remove(item.userName)
                }
            }
            onDismiss.invoke()
        }
    )
}
