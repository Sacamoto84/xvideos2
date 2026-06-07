package com.client.xvideos.r.common.expand_menu_video

import com.client.xvideos.common.theme.Theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Unsubscribe
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.ui.theme.XvideosTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
                    try {
                        val a = redApi.invoke().readCreator(item.userName).getOrNull()
                        savedRed.invoke().subscriptions.add(a!!)
                    } catch (e: Exception) { e.printStackTrace() }
                }
                else {
                    savedRed.invoke().subscriptions.remove(item.userName)
                }
            }
            onDismiss.invoke()
        }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownMenuItem_SubscriptionContent(
    isSubscribted: Boolean,
    onClick: () -> Unit
) {
    val textFollowed = if (isSubscribted) "Unsubscribe" else "Subscribe"
    val textFollowedIcon = if (isSubscribted) Icons.Default.Unsubscribe else Icons.Default.Subscriptions
    DropdownMenuItem(
        leadingIcon = {Icon(textFollowedIcon, contentDescription = "", tint = Theme.L.ExpandMenu.tintColor)},
        text = { Text(textFollowed, style = Theme.L.ExpandMenu.style) },
        onClick = onClick,
        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
    )
}

@Preview(showBackground = true)
@Composable
private fun DropdownMenuItem_FollowPreview() {
    XvideosTheme {
        DropdownMenuItem_SubscriptionContent(
            isSubscribted = true,
            onClick = {}
        )
    }
}
