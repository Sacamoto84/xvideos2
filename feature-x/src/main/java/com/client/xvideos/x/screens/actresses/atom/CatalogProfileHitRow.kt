package com.client.xvideos.x.screens.actresses.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.x.model.CatalogProfileHit

/** Найденная по имени модель или канал в выпадающем списке каталога. */
@Composable
fun CatalogProfileHitRow(
    hit: CatalogProfileHit,
    onClick: (CatalogProfileHit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick(hit) }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        UrlImage(
            url = hit.avatarUrl,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF26262B))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = hit.name,
                fontSize = 13.sp,
                color = Color(0xFFE0E0E0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (hit.subscribers.isNotBlank()) {
                Text(
                    text = "${hit.subscribers} подписчиков",
                    fontSize = 11.sp,
                    color = Color(0xFF8E8E96),
                    maxLines = 1
                )
            }
        }
    }
}

@Preview
@Composable
private fun CatalogProfileHitRowPreview() {
    CatalogProfileHitRow(
        hit = CatalogProfileHit(name = "Jane Doe", slug = "jane-doe", avatarUrl = "", subscribers = "12,3 к", isModel = true),
        onClick = {}
    )
}
