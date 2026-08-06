package com.client.xvideos.r.ui.expand_menu_video

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Unsubscribe
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.ui.theme.XvideosTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun DropdownMenuItem_Subscribtion(item: GifsInfo? = null, redApi:()-> RedApi , savedRed: ()->SavedRed, onDismiss: () -> Unit){

    val isSubscribed = savedRed.invoke().subscriptions.listCreators.any { it.username == item?.userName }

    DropdownMenuItem_SubscriptionContent(

        isSubscribted = isSubscribed,

        onClick = {
            if (item == null) return@DropdownMenuItem_SubscriptionContent

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
                            Timber.e(e, "Subscribe: не удалось получить профиль ${item.userName}")
                            SnackBar.error("Не удалось оформить подписку: ${e.message ?: "нет сети"}")
                        }
                }
                else {
                    savedRed.invoke().subscriptions.remove(item.userName)
                }
            }
            onDismiss.invoke()
        }
    )

}

@Composable
private fun DropdownMenuItem_SubscriptionContent(
    isSubscribted: Boolean,
    onClick: () -> Unit
) {
    ExpandMenuActionItem(
        icon = if (isSubscribted) Icons.Default.Unsubscribe else Icons.Default.Subscriptions,
        text = if (isSubscribted) "Unsubscribe" else "Subscribe",
        onClick = onClick
    )
}

@Preview(showBackground = true)
@Composable
private fun DropdownMenuItem_SubscriptionPreview() {
    XvideosTheme {
        DropdownMenuItem_SubscriptionContent(
            isSubscribted = true,
            onClick = {}
        )
    }
}
