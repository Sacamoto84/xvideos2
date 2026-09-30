package com.client.xvideos.r.ui.explorer.tab.saved.tab.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme

@Composable
fun CreatorChip(
    creator: String,
    url: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .padding(4.dp)
            .clip(CircleShape)
            .border(1.dp, Color.Gray, CircleShape)
            .background(
                if (isSelected) Color.Gray else Color.Transparent,
                CircleShape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                indication = null,
                interactionSource = null,
            )
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            if (url != null) {
                UrlImage(url = url)
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(Modifier.width(8.dp))
        Text(
            text = creator,
            fontSize = 16.sp,
            color = Color.White,
            fontFamily = Theme.R.fontFamilyPopinsRegular
        )
        Spacer(Modifier.width(4.dp))
    }
}

@Preview
@Composable
private fun CreatorChipPreview() {
    CreatorChip(
        creator = "Sample Creator",
        url = null,
        isSelected = true,
        onClick = {}
    )
}
