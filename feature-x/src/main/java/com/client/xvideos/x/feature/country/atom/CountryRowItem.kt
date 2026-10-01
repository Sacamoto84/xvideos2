package com.client.xvideos.x.feature.country.atom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.feature.x.R
import com.client.xvideos.ui.theme.PornHubOrange
import com.client.xvideos.x.feature.country.model.Country

@Composable
fun CountryRowItem(
    item: Country,
    isSelected: Boolean,
    onClick: (Country) -> Unit,
    modifier: Modifier = Modifier,
) {
    val emojiFont = remember { FontFamily(Font(R.font.flag)) }
    val handleClick = remember(item, onClick) { { onClick(item) } }
    val textStyle = TextStyle(
        fontFamily = emojiFont,
        fontSize = 28.sp,
        color = if (isSelected) PornHubOrange else Color.LightGray
    )
    val label = remember(item.flagEmoji, item.name) { "${item.flagEmoji}  ${item.name} " }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp, horizontal = 8.dp)
            .clickable(onClick = handleClick)
    ) {
        BasicText(
            text = label,
            style = textStyle
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF151515)
@Composable
private fun CountryRowItemPreview() {
    CountryRowItem(
        item = Country(name = "Германия", url = "/change-country/de", flagClass = "flag-de"),
        isSelected = true,
        onClick = {}
    )
}
