package com.client.xvideos.x.screens.videoplayer.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.client.xvideos.x.model.TagsModel

/**
 * Отобразить список каналов, порноактрис, тегов
 *
 * Пока реакция на тег есть
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComposeTags(tags: TagsModel, onClick : (String)->Unit) {

    FlowRow(verticalArrangement = Arrangement.Center) {

        //Каналы
        tags.mainUploader.forEach {
            ScreenItemTagsModelPornostars(it.name, Color.Black, it.count)
        }

        //Порноактрисы
        tags.pornstars.forEach {
            ScreenItemTagsModelPornostars(it.name, Color(0xFFDE2600), it.count)
        }

        //Теги
        tags.tags.sorted().forEach {

            Box(modifier = Modifier.height(28.dp).clickable {
                //По нажатию на тег выполнить
                onClick.invoke(it)
            }, contentAlignment = Alignment.Center) {
                Text(
                    it,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 1.dp, vertical = 1.dp)
                        .background(Color(0xFFD9D9D9))
                        .padding(horizontal = 2.dp, vertical = 0.dp),
                    fontFamily = FontFamily.SansSerif,
                )
            }
        }

    }

}
