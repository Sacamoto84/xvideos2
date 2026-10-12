package com.client.xvideos.common.applock.atom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme

/**
 * Что видит владелец вместо поля кода, когда замок взведён, а проверить код
 * нечем — см. `AppLockRepository.isSecretLost`.
 */
@Composable
fun AppLockUnavailableNotice(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Защищённое хранилище, где лежит код доступа, не открылось. " +
                "Закройте приложение и запустите его снова.",
            color = MaterialTheme.colorScheme.onBackground,
            style = Theme.L.Type.body
        )
        Text(
            text = "Если после перезапуска ничего не изменилось, код утрачен. Войти можно " +
                "только после очистки данных приложения в настройках системы — " +
                "сохранённое в приложении при этом удалится.",
            color = MaterialTheme.colorScheme.error,
            style = Theme.L.Type.body
        )
    }
}

@Preview
@Composable
private fun AppLockUnavailableNoticePreview() {
    AppLockUnavailableNotice()
}
