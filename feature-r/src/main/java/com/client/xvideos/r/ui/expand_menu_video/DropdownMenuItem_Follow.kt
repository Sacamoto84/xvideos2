package com.client.xvideos.r.ui.expand_menu_video

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PermIdentity
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

private const val TEXT_FOLLOW = "Follow"
private const val TEXT_UNFOLLOW = "Unfollow"
private const val FOLLOW_ACTION_DELAY_MS = 200L
private val ICON_PERSON = Icons.Default.Person
private val ICON_PERM_IDENTITY = Icons.Default.PermIdentity

@Composable
fun DropdownMenuItem_Follow(item: GifsInfo? = null, redApi: () -> RedApi, savedRed: () -> SavedRed, onDismiss: () -> Unit) {
    val isFollowed = item?.userName?.takeIf { it.isNotBlank() }?.let { name ->
        savedRed.invoke().creators.list.any { it.username == name }
    } ?: false
    val handleClick = remember(item, isFollowed, redApi, savedRed, onDismiss) {
        {
            if (item == null || item.userName.isBlank()) {
                onDismiss.invoke()
            } else {
                // см. комментарий в DropdownMenuItem_Like: управляемый scope из
                // SavedRed вместо GlobalScope, переживающий закрытие меню.
                savedRed.invoke().scope.launch {
                    delay(FOLLOW_ACTION_DELAY_MS)
                    if (!isFollowed) {
                        // Раньше здесь было `creators.add(getOrNull()!!)` внутри
                        // `catch { printStackTrace() }` молча проглатывал NPE
                        // при любой сетевой ошибке: подписка не срабатывала.
                        redApi.invoke().readCreator(item.userName)
                            .onSuccess { savedRed.invoke().creators.add(it) }
                            .onFailure { e ->
                                Timber.e(e, "Follow: не удалось получить профиль ${item.userName}")
                                SnackBar.error("Не удалось подписаться: ${e.message ?: "нет сети"}")
                            }
                    } else {
                        savedRed.invoke().creators.remove(item.userName)
                    }
                }
                onDismiss.invoke()
            }
        }
    }
    DropdownMenuItem_FollowContent(
        isFollowed = isFollowed,
        onClick = handleClick
    )
}

@Composable
fun DropdownMenuItem_FollowContent(
    isFollowed: Boolean,
    onClick: () -> Unit
) {
    ExpandMenuActionItem(
        icon = if (isFollowed) ICON_PERSON else ICON_PERM_IDENTITY,
        text = if (isFollowed) TEXT_UNFOLLOW else TEXT_FOLLOW,
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
