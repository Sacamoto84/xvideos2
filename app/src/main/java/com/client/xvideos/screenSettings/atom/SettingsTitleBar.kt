package com.client.xvideos.screenSettings.atom

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.screenSettings.components.SettingsRowTextPrimary

/** Заголовок страницы настроек со стрелкой «назад» слева, как в настройках Android. */
@Composable
internal fun SettingsTitleBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleStyle = remember(Theme.L.Type.screenTitle) {
        Theme.L.Type.screenTitle.copy(
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = SettingsRowTextPrimary
        )
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = SettingsRowTextPrimary
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            color = SettingsRowTextPrimary,
            style = titleStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Composable
private fun SettingsTitleBarPreview() {
    SettingsTitleBar(title = "Сеть и DNS", onBack = {})
}
