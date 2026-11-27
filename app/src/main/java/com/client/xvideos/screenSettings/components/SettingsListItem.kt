package com.client.xvideos.screenSettings.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.R
import com.client.xvideos.common.theme.Theme

@Composable
fun SettingsListItem(
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = 0,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val inGroup = LocalSettingsInGroup.current
    val baseModifier = if (inGroup) SETTINGS_ITEM_IN_GROUP_BASE_MODIFIER else SETTINGS_ITEM_STANDALONE_BASE_MODIFIER
    val rowModifier = if (onClick != null) {
        modifier.then(baseModifier).clickable(onClick = onClick)
    } else {
        modifier.then(baseModifier)
    }

    val titleStyle = remember(Theme.L.Type.rowTitle) {
        Theme.L.Type.rowTitle.copy(
            color = SettingsRowTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 22.sp
        )
    }
    val subtitleStyle = remember(Theme.L.Type.rowSubtitle) {
        Theme.L.Type.rowSubtitle.copy(
            color = SettingsRowTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 18.sp
        )
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != 0) {
            SettingsIcon(icon)
            Spacer(Modifier.width(16.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                color = SettingsRowTextPrimary,
                style = titleStyle
            )
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = SettingsRowTextSecondary,
                    style = subtitleStyle
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Preview
@Composable
private fun SettingsListItemPreview() {
    SettingsListItem(
        icon = R.drawable.icon_red,
        text = "Название",
        subtitle = "Подзаголовок"
    )
}
