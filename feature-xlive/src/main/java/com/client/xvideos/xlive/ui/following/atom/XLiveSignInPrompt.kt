package com.client.xvideos.xlive.ui.following.atom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.xlive.ui.XLiveColors

/**
 * «Подписки» без входа: что здесь будет и кнопка «Войти» — она открывает
 * страницу входа сайта.
 */
@Composable
fun XLiveSignInPrompt(
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Подписки аккаунта на сайте видны после входа",
            color = XLiveColors.Label,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onSignIn,
            colors = ButtonDefaults.buttonColors(containerColor = XLiveColors.Accent, contentColor = Color.White),
        ) {
            Text(text = "Войти")
        }
    }
}

@Preview
@Composable
private fun XLiveSignInPromptPreview() {
    XLiveSignInPrompt(onSignIn = {})
}
