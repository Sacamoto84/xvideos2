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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.r.ui.profile.ScreenRedProfileSM
import com.client.xvideos.r.ui.profile.TypeGifs
import com.composeunstyled.Text

@Composable
fun GifTypes_Control(vm: ScreenRedProfileSM) {
    GifTypes_Control(
        typeGifsList = vm.typeGifsList,
        selectedType = vm.typeGifs,
        onTypeSelected = {
            vm.typeGifs = it
            vm.clear()
        }
    )
}

@Composable
fun GifTypes_Control(
    typeGifsList: List<TypeGifs>,
    selectedType: TypeGifs,
    onTypeSelected: (TypeGifs) -> Unit
) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

        TextAndLine(Modifier.weight(1f), typeGifsList[0].value, typeGifsList[0] == selectedType) {
            onTypeSelected(typeGifsList[0])
        }

        Box(Modifier.width(1.dp).height(48.dp).background(Theme.R.colorBorderGray))

        TextAndLine(Modifier.weight(1f), typeGifsList[1].value, typeGifsList[1] == selectedType) {
            onTypeSelected(typeGifsList[1])
        }

    }
}

@Composable
private fun TextAndLine(
    modifier: Modifier = Modifier,
    str: String,
    select: Boolean,
    onClick: () -> Unit,
) {

    Box(
        modifier = Modifier
            .then(modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {

        Text(
            str,
            fontSize = 18.sp,
            color = if (select) Color.White else Theme.R.colorTextGray,
            fontFamily = Theme.R.fontFamilyPopinsRegular
        )

        Box(
            Modifier
                //.align(Alignment.BottomCenter)
                .offset(0.dp, 16.dp)
                .width(48.dp)
                .height(4.dp)
                .background(if (select) Theme.R.colorRed else Color.Transparent)
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
