package com.client.xvideos.r.ui.ui.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.r.ui.profile.ScreenRedProfileSM
import com.client.xvideos.r.ui.profile.TypeGifs

private val DIVIDER_WIDTH = 1.dp
private val CONTROL_HEIGHT = 48.dp
private val LABEL_FONT_SIZE = 18.sp
private val INDICATOR_WIDTH = 48.dp
private val INDICATOR_HEIGHT = 4.dp
private val INDICATOR_OFFSET_Y = 16.dp
private val INDICATOR_OFFSET_X = 0.dp

@Composable
fun GifTypes_Control(vm: ScreenRedProfileSM) {
    val handleTypeSelected: (TypeGifs) -> Unit = remember(vm) {
        { type ->
            vm.typeGifs = type
            vm.clear()
        }
    }
    GifTypes_Control(
        typeGifsList = vm.typeGifsList,
        selectedType = vm.typeGifs,
        onTypeSelected = handleTypeSelected
    )
}

@Composable
fun GifTypes_Control(
    typeGifsList: List<TypeGifs>,
    selectedType: TypeGifs,
    onTypeSelected: (TypeGifs) -> Unit
) {
    val item0 = typeGifsList.getOrNull(0)
    val item1 = typeGifsList.getOrNull(1)
    val onSelect0 = remember(item0, onTypeSelected) {
        { item0?.let(onTypeSelected) ?: Unit }
    }
    val onSelect1 = remember(item1, onTypeSelected) {
        { item1?.let(onTypeSelected) ?: Unit }
    }

    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

        if (item0 != null) {
            TextAndLine(
                modifier = Modifier.weight(1f),
                str = item0.value,
                select = item0 == selectedType,
                onClick = onSelect0
            )
        }

        Box(Modifier.width(DIVIDER_WIDTH).height(CONTROL_HEIGHT).background(Theme.R.colorBorderGray))

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

@Composable
private fun TextAndLine(
    str: String,
    select: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor = if (select) Color.White else Theme.R.colorTextGray
    val indicatorColor = if (select) Theme.R.colorRed else Color.Transparent

    Box(
        modifier = modifier
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = str,
            fontSize = LABEL_FONT_SIZE,
            color = textColor,
            fontFamily = Theme.R.fontFamilyPopinsRegular
        )

        Box(
            Modifier
                //.align(Alignment.BottomCenter)
                .offset(INDICATOR_OFFSET_X, INDICATOR_OFFSET_Y)
                .width(INDICATOR_WIDTH)
                .height(INDICATOR_HEIGHT)
                .background(indicatorColor)
        )

    }


}

@Preview
@Composable
fun GifTypes_ControlPreview() {
    Box(modifier = Modifier.background(Theme.background)) {
        GifTypes_Control(
            typeGifsList = listOf(TypeGifs.GIFS, TypeGifs.IMAGES),
            selectedType = TypeGifs.GIFS,
            onTypeSelected = {}
        )
    }
}

@Preview
@Composable
private fun TextAndLinePreviewSelected() {
    Box(modifier = Modifier.background(Theme.background)) {
        TextAndLine(
            str = "Gifs",
            select = true,
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun TextAndLinePreviewUnselected() {
    Box(modifier = Modifier.background(Theme.background)) {
        TextAndLine(
            str = "Images",
            select = false,
            onClick = {}
        )
    }
}
