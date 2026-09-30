package com.client.xvideos.r.ui.fullscreen.atom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.toTwoDecimalPlacesWithColon

@Composable
fun TimeMarkerButton(
    label: String,
    time: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textStyle = remember {
        TextStyle(
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = Theme.R.fontFamilyPopinsRegular,
            textAlign = TextAlign.Center
        )
    }
    Column(
        modifier = modifier
            .size(46.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicText(
            time.toTwoDecimalPlacesWithColon(),
            style = textStyle,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            label,
            color = Color.White,
            fontSize = 20.sp,
            fontFamily = Theme.R.fontFamilyPopinsRegular,
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
private fun TimeMarkerButtonPreview() {
    TimeMarkerButton(
        label = "A",
        time = 12.34f,
        onClick = {}
    )
}
