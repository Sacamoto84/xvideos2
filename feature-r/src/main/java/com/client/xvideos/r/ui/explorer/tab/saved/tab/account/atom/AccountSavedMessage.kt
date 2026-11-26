package com.client.xvideos.r.ui.explorer.tab.saved.tab.account.atom

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
internal fun AccountSavedMessage(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        modifier = modifier.padding(horizontal = 20.dp),
        color = Color.White,
        textAlign = TextAlign.Center,
    )
}

@Preview
@Composable
private fun AccountSavedMessagePreview() {
    AccountSavedMessage("Список пуст")
}
