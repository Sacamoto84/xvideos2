package com.client.xvideos.n.ui.screens.reader.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.n.repository.CloudflareBlockedException
import com.client.xvideos.n.ui.molecule.NRequestChallengeDialog
import java.io.IOException

@Composable
internal fun ReaderLoadingScreen(error: Throwable?, onBack: () -> Unit, onRetry: () -> Unit) {
    var dismissed by remember(error) { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        if (error == null) {
            CircularProgressIndicator()
        } else {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error.localizedMessage ?: "Ошибка загрузки альбома", color = Color.White)
                TextButton(onClick = {
                    if (error is CloudflareBlockedException) dismissed = false else onRetry()
                }) {
                    Text(if (error is CloudflareBlockedException) "Пройти проверку" else "Повторить")
                }
                TextButton(onClick = onBack) { Text("Назад") }
            }
        }
    }
    if (error is CloudflareBlockedException && !dismissed) {
        NRequestChallengeDialog(challenge = error, onDismiss = { dismissed = true }, onVerified = onRetry)
    }
}

@Preview(name = "Ошибка загрузки")
@Composable
private fun ReaderLoadingScreenPreview() {
    ReaderLoadingScreen(error = IOException("Сеть недоступна"), onBack = {}, onRetry = {})
}
