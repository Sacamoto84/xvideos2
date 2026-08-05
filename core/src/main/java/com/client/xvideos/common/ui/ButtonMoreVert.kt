package com.client.xvideos.common.ui

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Кнопка "три точки" (MoreVert) с тенью.
 *
 *
 * @param size Размер иконки.
 * @param onClick Обработчик нажатия.
 */
@Composable
fun ButtonMoveVert(size : Dp = 26.dp, onClick : () -> Unit= {}){

    IconButton(onClick = onClick) {
        
        Icon(
            Icons.Default.MoreVert, contentDescription = "",
            tint = Color.Black, modifier = Modifier.size(size).offset(0.5.dp, 0.5.dp)
        )

        Icon(
            Icons.Default.MoreVert, contentDescription = "",
            tint = Color.White, modifier = Modifier.size(size)
        )
    }

}

@Preview(showBackground = true)
@Composable
fun Preview_ButtonMoveVert() { ButtonMoveVert() }
