package com.client.xvideos.x.screens.channel.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.core.R
import com.client.xvideos.x.model.ChannelCollaborator

@Composable
fun CollaboratorChip(
    collaborator: ChannelCollaborator,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isModel = collaborator.isModel
    val accentColor = if (isModel) Color(0xFFDE2600) else Color(0xFF1E88E5)
    val bgColor = if (isModel) Color(0xFF261418) else Color(0xFF141E26)
    val borderColor = if (isModel) Color(0x4DDE2600) else Color(0x4D1E88E5)
    val iconChar = if (isModel) "\uE9B8" else "\uE956"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = iconChar,
                style = TextStyle(
                    color = accentColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily(Font(R.font.iconfont))
                )
            )
            Text(
                text = collaborator.name,
                style = TextStyle(
                    color = Color(0xFFF0F0F0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }
    }
}

@Preview
@Composable
private fun CollaboratorChipPreview() {
    CollaboratorChip(
        collaborator = ChannelCollaborator(
            name = "Jane Doe",
            href = "/models/jane-doe",
            isModel = true,
        ),
        onClick = {}
    )
}
