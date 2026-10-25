package com.client.xvideos.xlive.ui.following.molecule

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.xlive.ui.XLiveColors

/**
 * Строка над «Подписками»: сколько подписок в эфире и «Выйти» — с
 * подтверждением. Открыт ли диалог — состояние самой строки.
 *
 * @param online подписок в эфире.
 * @param total подписок всего.
 */
@Composable
fun XLiveAccountBar(
    online: Int,
    total: Int,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirming by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .adaptiveContentWidth()
            .padding(start = 12.dp, end = 4.dp)
    ) {
        Text(
            text = "Подписки на сайте: в эфире $online из $total",
            color = XLiveColors.Label,
            fontSize = 13.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { confirming = true }) {
            Text(text = "Выйти", color = XLiveColors.Link)
        }
    }

    if (confirming) {
        XLiveSignOutDialog(
            onDismiss = { confirming = false },
            onConfirm = {
                confirming = false
                onSignOut()
            },
        )
    }
}

@Preview
@Composable
private fun XLiveAccountBarPreview() {
    XLiveAccountBar(online = 3, total = 12, onSignOut = {})
}
