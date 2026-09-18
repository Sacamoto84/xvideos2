package com.client.xvideos.common.videoplayer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.videoplayer.model.PlayerSpeed

/**
 * Всплывающее меню выбора скорости воспроизведения видео.
 *
 * @param currentSpeed Текущая выбранная скорость.
 * @param onSpeedSelected Обработчик выбора новой скорости.
 * @param modifier Модификатор контейнера.
 * @param trigger Компонент-кнопка для открытия меню. По умолчанию используется [DefaultSpeedBadge].
 */
@Composable
fun PlaybackSpeedMenu(
    currentSpeed: PlayerSpeed,
    onSpeedSelected: (PlayerSpeed) -> Unit,
    modifier: Modifier = Modifier,
    trigger: @Composable (onClick: () -> Unit) -> Unit = { onClick ->
        DefaultSpeedBadge(speed = currentSpeed, onClick = onClick)
    },
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        trigger { expanded = true }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color(0xE61E1E1E), shape = RoundedCornerShape(8.dp))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
        ) {
            PlayerSpeed.entries.forEach { speed ->
                val isSelected = speed == currentSpeed
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = if (isSelected) "Выбрано" else null,
                                tint = if (isSelected) Color.White else Color.Transparent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = speed.displayName,
                                color = if (isSelected) Color.White else Color.LightGray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    },
                    onClick = {
                        onSpeedSelected(speed)
                        expanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Текстовый бейдж текущей скорости воспроизведения по умолчанию.
 */
@Composable
fun DefaultSpeedBadge(
    speed: PlayerSpeed,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = speed.displayName,
        color = Color.White,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}
