package com.client.xvideos.r.ui.expand_menu_video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.network.api.RedApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

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
                    delay(200L)
                    if (!isFollowed) {
                        // Раньше здесь было `creators.add(getOrNull()!!)` внутри
                        // `catch { printStackTrace() }` молча проглатывал NPE
                        // при любой сетевой ошибке: подписка не срабатывала.
                        redApi.invoke().readCreator(item.userName)
                            .onSuccess { savedRed.invoke().creators.add(it) }
                            .onFailure { e ->
                                Timber.e("Follow: не удалось получить профиль ${item.userName}: ${e.javaClass.simpleName}")
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
