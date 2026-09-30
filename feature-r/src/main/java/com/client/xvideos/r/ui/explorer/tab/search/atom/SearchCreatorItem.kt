package com.client.xvideos.r.ui.explorer.tab.search.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.util.toPrettyCount
import com.client.xvideos.r.model.search.SearchItemCreatorsResponse

@Composable
fun SearchCreatorItem(
    item: SearchItemCreatorsResponse,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val handle = item.text.removePrefix("@").ifBlank { item.name }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val image = item.image
        if (!image.isNullOrBlank()) {
            UrlImage(
                image,
                modifier = Modifier
                    .clip(CircleShape)
                    .size(56.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(56.dp)
                    .background(Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PersonOutline,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name.ifBlank { handle },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                if (item.verified) {
                    Text(
                        text = " ✓",
                        color = Color(0xFFE5A00D),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            Text(
                text = "@$handle",
                color = Color.Gray,
                fontSize = 13.sp,
                maxLines = 1
            )
            if (item.followers > 0) {
                Text(
                    text = "Подписчиков: ${item.followers.toPrettyCount()}",
                    color = Color(0xFF9E9E9E),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Preview
@Composable
private fun SearchCreatorItemPreview() {
    SearchCreatorItem(
        item = SearchItemCreatorsResponse(
            name = "Ana",
            image = null,
            followers = 1234
        ),
        onClick = {}
    )
}
