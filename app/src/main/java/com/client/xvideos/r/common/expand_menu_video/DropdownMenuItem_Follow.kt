package com.client.xvideos.r.common.expand_menu_video

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PermIdentity
import androidx.compose.material.icons.filled.Person
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
fun DropdownMenuItem_Follow(item: GifsInfo? = null, redApi:()-> RedApi, savedRed: ()->SavedRed, onDismiss: () -> Unit){
    val isFollowed = savedRed.invoke().creators.list.any { it.username == item?.userName }
    DropdownMenuItem_FollowContent(
        isFollowed = isFollowed,
        onClick = {
            if (item == null) return@DropdownMenuItem_FollowContent
            // см. комментарий в DropdownMenuItem_Like: управляемый scope из
            // SavedRed вместо GlobalScope, переживающий закрытие меню.
            savedRed.invoke().scope.launch {
                delay(200)
                if (!isFollowed) {
                    // Раньше здесь было `creators.add(getOrNull()!!)` внутри
                    // `catch { printStackTrace() }`: при любой сетевой ошибке
                    // getOrNull() давал null, `!!` кидал NPE, catch его глотал,
                    // а printStackTrace писал в stderr мимо Timber — подписка
                    // молча не срабатывала и не оставляла следа в логе.
                    redApi.invoke().readCreator(item.userName)
                        .onSuccess { savedRed.invoke().creators.add(it) }
                        .onFailure { e ->
                            Timber.e(e, "Follow: не удалось получить профиль ${item.userName}")
                            SnackBar.error("Не удалось подписаться: ${e.message ?: "нет сети"}")
                        }
                }
                else {
                    savedRed.invoke().creators.remove(item.userName)
                }
            }
            onDismiss.invoke()
        }
    )
}

@Composable
private fun DropdownMenuItem_FollowContent(
    isFollowed: Boolean,
    onClick: () -> Unit
) {
    ExpandMenuActionItem(
        icon = if (isFollowed) Icons.Default.Person else Icons.Default.PermIdentity,
        text = if (isFollowed) "Unfollow" else "Follow",
        onClick = onClick
    )
}

@Preview(showBackground = true)
@Composable
private fun DropdownMenuItem_FollowPreview() {
    XvideosTheme {
        DropdownMenuItem_FollowContent(
            isFollowed = false,
            onClick = {}
        )
    }
}
