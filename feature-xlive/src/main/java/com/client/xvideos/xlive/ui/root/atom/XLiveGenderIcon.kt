package com.client.xvideos.xlive.ui.root.atom

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Female
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Male
import androidx.compose.material.icons.outlined.Transgender
import androidx.compose.material.icons.outlined.Wc
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.client.xvideos.xlive.model.XLiveGender

/** Значок пола комнат: группа у «все», ♀, ♂, пара, ⚧ — как значки категорий X. */
@Composable
fun XLiveGenderIcon(
    gender: XLiveGender,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
) {
    val icon = when (gender) {
        XLiveGender.ALL -> Icons.Outlined.Groups
        XLiveGender.FEMALE -> Icons.Outlined.Female
        XLiveGender.MALE -> Icons.Outlined.Male
        XLiveGender.COUPLE -> Icons.Outlined.Wc
        XLiveGender.TRANS -> Icons.Outlined.Transgender
    }
    Icon(
        imageVector = icon,
        contentDescription = gender.title,
        tint = Color.White,
        modifier = modifier.size(size),
    )
}

@Preview
@Composable
private fun XLiveGenderIconPreview() {
    XLiveGenderIcon(gender = XLiveGender.FEMALE)
}
