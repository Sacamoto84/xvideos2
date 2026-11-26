package com.client.xvideos.l.ui.screens.explorer.tab.saved.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.l.ui.screens.explorer.tab.saved.LServerLoginScreen

@Composable
internal fun LServerLoginRequired(message: String) {
    val navigator = LocalNavigator.current
    Column(
        modifier = Modifier.fillMaxSize()
            .background(Theme.background)
            .padding(top = getTopInsetDp())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = { navigator?.push(LServerLoginScreen) }) {
            Text("Войти в L")
        }
    }
}

@Preview
@Composable
private fun LServerLoginRequiredPreview() {
    LServerLoginRequired(message = "Войдите в L, чтобы открыть этот список")
}
