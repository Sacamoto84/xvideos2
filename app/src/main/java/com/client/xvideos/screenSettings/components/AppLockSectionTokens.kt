package com.client.xvideos.screenSettings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import kotlin.math.ceil

internal val appLockErrorColor = Color(0xFFB3261E)

internal val TIMEOUT_ITEM_FULL_MODIFIER = Modifier
    .fillMaxWidth()
    .clip(RoundedCornerShape(12.dp))
    .padding(horizontal = 8.dp, vertical = 10.dp)

internal val ERROR_TEXT_STYLE = Theme.L.Type.dialogBody.copy(color = appLockErrorColor)

/** Счётчик попыток общий с экраном замка, поэтому срок здесь тот же, что покажет он. */
internal fun lockedOutMessage(remainingMs: Long): String =
    "Слишком много попыток. Повторите через ${ceil(remainingMs / 1000.0).toInt()} с"
