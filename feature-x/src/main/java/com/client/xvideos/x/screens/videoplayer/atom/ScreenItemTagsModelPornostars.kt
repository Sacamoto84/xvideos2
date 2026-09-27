package com.client.xvideos.x.screens.videoplayer.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.core.R

/**
 * ## Отображение текста канала и порноактрисы и показ количества подписок на них
 */
@Composable
fun ScreenItemTagsModelPornostars(
    icon : String = "",
    text: String,
    color: Color,
    count: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val hasCount = count.isNotBlank()
    val baseModifier = modifier
        .padding(horizontal = 3.dp, vertical = 2.dp)
        .height(28.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(color)
    val rowModifier = if (onClick != null) baseModifier.clickable(onClick = onClick) else baseModifier

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {

        if (icon.isNotBlank()) {
            Text(
                text = icon,
                modifier = Modifier.padding(start = 8.dp, end = if (hasCount) 4.dp else 8.dp),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily(Font(R.font.iconfont))
                )
            )
        }

        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp, end = if (hasCount) 4.dp else 8.dp),
            style = TextStyle(
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        )

        if (hasCount) {
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x33000000))
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF141418)
@Composable
private fun ScreenItemTagsModelPornostarsWithCountPreview() {
    ScreenItemTagsModelPornostars(
        text = "Sweetie Fox",
        color = Color(0xFFE91E63),
        count = "120K",
        onClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF141418)
@Composable
private fun ScreenItemTagsModelPornostarsWithoutCountPreview() {
    ScreenItemTagsModelPornostars(
        text = "Verified Channel",
        color = Color(0xFF3F51B5),
        count = "",
        onClick = {}
    )
}
