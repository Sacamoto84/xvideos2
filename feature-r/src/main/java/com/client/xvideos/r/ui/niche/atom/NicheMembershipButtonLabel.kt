package com.client.xvideos.r.ui.niche.atom

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal const val NICHE_JOIN_LABEL = "Вступить в группу"
internal const val NICHE_LEAVE_LABEL = "Выйти из группы"
private val nicheLabelAutoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 16.sp)

@Composable
internal fun NicheMembershipButtonLabel(
    isMember: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    BasicText(
        text = if (isMember) NICHE_LEAVE_LABEL else NICHE_JOIN_LABEL,
        modifier = modifier.padding(horizontal = 6.dp),
        style = LocalTextStyle.current.copy(color = color),
        autoSize = nicheLabelAutoSize,
        maxLines = 1,
    )
}

@Preview
@Composable
private fun NicheMembershipButtonLabelPreview() {
    NicheMembershipButtonLabel(isMember = false, color = Color.Black)
}
