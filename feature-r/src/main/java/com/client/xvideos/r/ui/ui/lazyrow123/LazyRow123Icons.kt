package com.client.xvideos.r.ui.ui.lazyrow123

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.icons.IconCollection18
import com.client.xvideos.common.icons.IconFavorite18
import com.client.xvideos.common.icons.IconPerson18
import com.client.xvideos.common.icons.IconSave18
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.GifsInfo

@Composable
fun LazyRow123Icons(
    modifier: Modifier = Modifier,
    savedRed: () -> SavedRed,
    item: GifsInfo,
    isDownloaded: Boolean
) {

    Row(
        modifier = Modifier.fillMaxWidth().then(modifier),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.End
    ) {

        val saved = savedRed()

        if (saved.collections.collectionList.any { it.items.any { it2 -> it2.id == item.id } }) {
            IconCollection18(Modifier.padding(bottom = 6.dp, end = 6.dp))
        }

        //
        if (saved.creators.list.any { it.username == item.userName }) {
            IconPerson18(Modifier.padding(bottom = 6.dp, end = 6.dp))
        }

        //✅ Лайк
        if (saved.likes.list.any { it.id == item.id }) {
            IconFavorite18(Modifier.padding(bottom = 6.dp, end = 6.dp))
        }

        //✅ Иконка того что видео скачано
        if (isDownloaded) {
            IconSave18(Modifier.padding(bottom = 6.dp, end = 6.dp))
        }

    }

}

@Preview(showBackground = true)
@Composable
private fun PreviewLazyRow123Icons() {
    Column(
        modifier = Modifier
            .background(Color.DarkGray)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Visual representation of icons for Preview purposes, 
        // as SavedRed cannot be easily instantiated without its dependencies.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.End
        ) {
            IconCollection18(Modifier.padding(bottom = 6.dp, end = 6.dp))
            IconPerson18(Modifier.padding(bottom = 6.dp, end = 6.dp))
            IconFavorite18(Modifier.padding(bottom = 6.dp, end = 6.dp))
            IconSave18(Modifier.padding(bottom = 6.dp, end = 6.dp))
        }
    }
}
