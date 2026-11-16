package com.client.xvideos.n.ui.atom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.excludedFromAutofill

/**
 * Форма ручного ввода API-ключа или cookie sessionid для авторизации в nhentai.
 */
@Composable
fun NLoginManualInput(
    tokenText: String,
    usernameText: String,
    onTokenChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color(0xFFED2553),
        unfocusedBorderColor = Theme.tabLevel3,
        focusedLabelColor = Color(0xFFED2553),
        unfocusedLabelColor = Color(0xFFAAAAAA)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Ручной вход по API-ключу или куке",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Вы можете сгенерировать API Key в настройках профиля (nhentai.net/user/settings#apikeys) " +
                "или скопировать куку sessionid из браузера. Это даёт те же повышенные лимиты без прохождения капчи.",
            color = Color(0xFFAAAAAA),
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = tokenText,
            onValueChange = onTokenChange,
            label = { Text("API Key или sessionid") },
            placeholder = { Text("Вставьте ключ или куку sessionid...") },
            modifier = Modifier.fillMaxWidth().excludedFromAutofill(),
            colors = textFieldColors,
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = usernameText,
            onValueChange = onUsernameChange,
            label = { Text("Имя пользователя (необязательно)") },
            placeholder = { Text("zorromen") },
            modifier = Modifier.fillMaxWidth().excludedFromAutofill(),
            colors = textFieldColors,
            singleLine = true
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onSubmit,
            enabled = tokenText.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFED2553),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF444444)
            )
        ) {
            Text("Сохранить и войти", fontWeight = FontWeight.Bold)
        }
    }
}

@Preview
@Composable
private fun NLoginManualInputPreview() {
    NLoginManualInput(
        tokenText = "",
        usernameText = "",
        onTokenChange = {},
        onUsernameChange = {},
        onSubmit = {}
    )
}
