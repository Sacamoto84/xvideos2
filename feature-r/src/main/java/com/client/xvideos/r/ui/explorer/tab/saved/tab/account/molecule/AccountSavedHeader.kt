package com.client.xvideos.r.ui.explorer.tab.saved.tab.account.molecule

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.ui.atom.SavedTabHeader
import com.client.xvideos.common.util.getTopInsetDp

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

@Preview
@Composable
private fun AccountSavedHeaderPreview() {
    AccountSavedHeader(title = "Коллекции R", onBack = {}, onRefresh = {})
}
