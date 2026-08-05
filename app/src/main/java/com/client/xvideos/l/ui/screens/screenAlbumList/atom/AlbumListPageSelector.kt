package com.client.xvideos.l.ui.screens.screenAlbumList.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.client.xvideos.x.screens.common.bottomKeyboard.KeyboardNumber
import com.client.xvideos.x.screens.common.bottomKeyboard.KeyboardNumberTheme

@Preview
@Composable
private fun Preview() {
    AlbumListPageSelector(1, 199)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumListPageSelector(
    page: Int,
    pageMax: Int,
    onChange: (Int) -> Unit = {}
) {

    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .background(Theme.L.red)
                .clickable(onClick = { onChange((page - 1).coerceAtLeast(1)) }),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.KeyboardArrowLeft, tint = Color.White, contentDescription = null) }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(2f)
                .drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    val color = Theme.L.grey3.toArgb()

                    // верхняя линия
                    drawLine(
                        color = Theme.L.grey3,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = strokeWidth
                    )
                    // нижняя линия
                    drawLine(
                        color = Theme.L.grey3,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = strokeWidth
                    )
                }
                .clickable(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    expanded = true
                }),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Page ${page + 1} of $pageMax",
                color = Theme.L.textColor,
                style = Theme.L.Type.rowTitle.copy(textAlign = TextAlign.Center)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .background(Theme.L.red)
                .clickable(
                    onClick = { onChange((page + 1).coerceAtMost(pageMax)) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.KeyboardArrowRight,
                tint = Color.White,
                contentDescription = null
            )
        }
    }

    //-- Диалог --
    if (expanded) {

        Dialog(onDismissRequest = { expanded = false }) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, Color(0xFF3E3E3E), RoundedCornerShape(16.dp))
                    .background(Color(0xFF373737))
                    .padding(16.dp), contentAlignment = Alignment.Center
            )
            {
                KeyboardNumber(

                    theme = KeyboardNumberTheme(
                        colorBackground = Color(0xFF2D2D2D),
                        colorBorderBackground = Color(0xFF282828),
                        colorText = Color(0xFFFFFFFF),
                        buttonColor = Color(0xFF282828),
                        colorButtonBorder = Color(0xFF232323),
                    ),

                    value = -1, max = pageMax,
                    onClick = {
                        onChange(it - 1)
                        expanded = false
                    }
                )
            }
        }
    }

}




