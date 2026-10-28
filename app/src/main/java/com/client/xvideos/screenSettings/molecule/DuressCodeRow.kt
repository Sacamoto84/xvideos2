package com.client.xvideos.screenSettings.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.R
import com.client.xvideos.screenSettings.atom.SettingsOutlinedButton
import com.client.xvideos.screenSettings.components.SettingsDestructiveColor
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsPreview

/**
 * Строка настройки кода уничтожения в разделе «Приватность».
 *
 * @param isSet задан ли код уничтожения в текущий момент.
 * @param onSetClick действие для первоначального задания кода.
 * @param onChangeClick действие для изменения существующего кода.
 * @param onRemoveClick действие для удаления кода уничтожения.
 */
@Composable
fun DuressCodeRow(
    isSet: Boolean,
    onSetClick: () -> Unit,
    onChangeClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subtitle = remember(isSet) { if (isSet) "Задан" else "Не задан" }

    val trailingContent: @Composable () -> Unit = remember(isSet, onSetClick, onChangeClick, onRemoveClick) {
        {
            if (!isSet) {
                Button(onClick = onSetClick) {
                    Text("Задать")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onChangeClick) {
                        Text("Изменить")
                    }
                    SettingsOutlinedButton(
                        text = "Убрать",
                        onClick = onRemoveClick,
                        color = SettingsDestructiveColor,
                    )
                }
            }
        }
    }

    SettingsListItem(
        modifier = modifier,
        icon = R.drawable.key_24,
        text = "Код уничтожения",
        subtitle = subtitle,
        trailing = trailingContent,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun DuressCodeRowPreview() = SettingsPreview {
    DuressCodeRow(
        isSet = true,
        onSetClick = {},
        onChangeClick = {},
        onRemoveClick = {},
    )
}
