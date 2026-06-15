package com.client.xvideos.r.common.expand_menu_video

import com.client.xvideos.common.theme.Theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PermIdentity
import androidx.compose.material.icons.filled.Person
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
                    try {
                        val a = redApi.invoke().readCreator(item.userName).getOrNull()
                        savedRed.invoke().creators.add(a!!)
                    } catch (e: Exception) { e.printStackTrace() }
                }
                else {
                    savedRed.invoke().creators.remove(item.userName)
                }
            }
            onDismiss.invoke()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownMenuItem_FollowContent(
    isFollowed: Boolean,
    onClick: () -> Unit
) {
    val textFollowed = if (isFollowed) "Unfollow" else "Follow"
    val textFollowedIcon = if (isFollowed) Icons.Default.Person else Icons.Default.PermIdentity
    DropdownMenuItem(
        leadingIcon = {Icon(textFollowedIcon, contentDescription = "", tint = Theme.ExpandMenu.tintColor)},
        text = { Text(textFollowed, style = Theme.ExpandMenu.style) },
        onClick = onClick,
        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
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
