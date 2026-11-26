package com.client.xvideos.r.ui.explorer.tab.saved.tab.account.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import com.client.xvideos.r.ui.auth.ScreenRLogin
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.atom.AccountSavedMessage

@Composable
internal fun AccountSignInPrompt() {
    val navigator = LocalNavigator.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AccountSavedMessage("Войдите в R, чтобы открыть этот список.")
        Button(onClick = { navigator?.push(ScreenRLogin) }) { Text("Войти в R") }
    }
}

@Preview
@Composable
private fun AccountSignInPromptPreview() {
    AccountSignInPrompt()
}
