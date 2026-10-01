package com.client.xvideos.r.ui.ui.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.ui.profile.TypeGifs

@Composable
fun GifTypes_Control(
    typeGifsList: List<TypeGifs>,
    selectedType: TypeGifs,
    onTypeSelected: (TypeGifs) -> Unit,
    modifier: Modifier = Modifier,
) {
    val item0 = typeGifsList.getOrNull(0)
    val item1 = typeGifsList.getOrNull(1)
    val onSelect0 = remember(item0, onTypeSelected) {
        { item0?.let(onTypeSelected) ?: Unit }
    }
    val onSelect1 = remember(item1, onTypeSelected) {
        { item1?.let(onTypeSelected) ?: Unit }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item0 != null) {
            TextAndLine(
                modifier = Modifier.weight(1f),
                str = item0.value,
                select = item0 == selectedType,
                onClick = onSelect0
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(48.dp)
                .background(Theme.R.colorBorderGray)
        )

        if (item1 != null) {
            TextAndLine(
                modifier = Modifier.weight(1f),
                str = item1.value,
                select = item1 == selectedType,
                onClick = onSelect1
            )
        }
    }
}

@Preview
@Composable
private fun GifTypes_ControlPreview() {
    Box(modifier = Modifier.background(Theme.background)) {
        GifTypes_Control(
            typeGifsList = listOf(TypeGifs.GIFS, TypeGifs.IMAGES),
            selectedType = TypeGifs.GIFS,
            onTypeSelected = {}
        )
    }
}
