package com.client.xvideos.x.screens.videoplayer.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.model.TagsModel

/**
 * Отобразить список каналов, порноактрис, тегов
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComposeTags(tags: TagsModel, onClick: (String) -> Unit) {

    FlowRow(verticalArrangement = Arrangement.Center) {

        // Каналы
        tags.mainUploader.forEach {
            ScreenItemTagsModelPornostars(it.name, Color(0xFF1E88E5), it.count)
        }

        // Порноактрисы
        tags.pornstars.forEach {
            ScreenItemTagsModelPornostars(it.name, Color(0xFFDE2600), it.count)
        }

        // Теги
        tags.tags.sorted().forEach {
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp, vertical = 2.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC26262B))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                    .clickable { onClick.invoke(it) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = it,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

