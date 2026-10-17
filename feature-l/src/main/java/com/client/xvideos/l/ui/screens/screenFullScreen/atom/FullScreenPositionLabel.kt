package com.client.xvideos.l.ui.screens.screenFullScreen.atom

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.edge.avoidCameraAndCorner
import com.client.xvideos.l.ui.screens.screenFullScreen.model.lPicturePositionLabel

/**
 * Номер картинки «n / всего» в левом верхнем углу полноэкранного просмотра.
 * Мелко и в одну строку выше иконок панели (их верх — 16dp); от скругления угла
 * и выреза камеры подпись уходит вправо.
 */
@Composable
fun FullScreenPositionLabel(position: Int, total: Int, modifier: Modifier = Modifier) {
    Text(
        text = lPicturePositionLabel(position, total),
        color = Color.Gray,
        modifier = modifier
            .padding(start = 8.dp)
            .avoidCameraAndCorner(atStart = true),
        fontSize = 10.sp,
        lineHeight = 12.sp,
        fontFamily = Theme.L.fontFamilyKarla
    )
}

@Preview
@Composable
private fun FullScreenPositionLabelPreview() {
    FullScreenPositionLabel(position = 13, total = 100)
}
