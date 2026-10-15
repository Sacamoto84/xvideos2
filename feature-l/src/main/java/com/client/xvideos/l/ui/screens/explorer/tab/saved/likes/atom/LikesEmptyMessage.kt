package com.client.xvideos.l.ui.screens.explorer.tab.saved.likes.atom

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme

/** Надпись пустой вкладки лайков: без неё пустой экран выглядит как незагрузившийся. */
@Composable
fun LikesEmptyMessage(modifier: Modifier = Modifier) {
    Text(
        text = "Нет элементов",
        color = Theme.L.grey2,
        fontSize = 16.sp,
        modifier = modifier
    )
}

@Preview
@Composable
private fun LikesEmptyMessagePreview() {
    LikesEmptyMessage()
}
