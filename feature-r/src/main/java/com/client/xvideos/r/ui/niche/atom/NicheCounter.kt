package com.client.xvideos.r.ui.niche.atom

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.util.toPrettyCount
import com.client.xvideos.core.R

@Composable
internal fun NicheCounter(
    count: Long,
    @DrawableRes icon: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    val countText = remember(count) { count.toPrettyCount() }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = countText,
            modifier = Modifier.padding(horizontal = 4.dp).wrapContentWidth(Alignment.CenterHorizontally),
            style = TextStyle(color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp),
        )
    }
}

@Preview
@Composable
private fun NicheCounterPreview() {
    NicheCounter(count = 914, icon = R.drawable.members, label = "Подписчики")
}
