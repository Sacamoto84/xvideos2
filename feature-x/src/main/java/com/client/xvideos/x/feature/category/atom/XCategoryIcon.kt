package com.client.xvideos.x.feature.category.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Female
import androidx.compose.material.icons.outlined.Transgender
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.client.xvideos.x.model.XCategory

private val RAINBOW_STRIPES = listOf(
    Color(0xFFE40303),
    Color(0xFFFF8C00),
    Color(0xFFFFED00),
    Color(0xFF008026),
    Color(0xFF004DFF),
    Color(0xFF750787),
)

/**
 * Значок категории X, как в меню категорий сайта: ♀ у гетеро, радужный флаг у
 * геев, ⚧ у трансов.
 */
@Composable
fun XCategoryIcon(
    category: XCategory,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
) {
    when (category) {
        XCategory.STRAIGHT -> Icon(
            imageVector = Icons.Outlined.Female,
            contentDescription = category.title,
            tint = Color.White,
            modifier = modifier.size(size),
        )
        XCategory.TRANS -> Icon(
            imageVector = Icons.Outlined.Transgender,
            contentDescription = category.title,
            tint = Color.White,
            modifier = modifier.size(size),
        )
        XCategory.GAY -> Column(
            modifier = modifier
                .size(width = size, height = size * RAINBOW_ASPECT)
                .clip(RoundedCornerShape(2.dp))
        ) {
            RAINBOW_STRIPES.forEach { stripe ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(stripe)
                )
            }
        }
    }
}

private const val RAINBOW_ASPECT = 0.7f

@Preview
@Composable
private fun XCategoryIconPreview() {
    XCategoryIcon(category = XCategory.GAY)
}
