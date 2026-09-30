package com.client.xvideos.r.ui.search.atom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme

@Composable
fun HistoryMenuItem(
    text: String,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Text(
                text = text,
                color = Color.Black,
                fontSize = 18.sp,
                modifier = Modifier
                    .padding(vertical = 0.dp)
                    .padding(start = 16.dp)
                    .offset(0.75.dp, 0.75.dp),
                fontFamily = Theme.R.fontFamilyDMsanss
            )

            Text(
                text = text,
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier
                    .padding(vertical = 0.dp)
                    .padding(start = 16.dp),
                fontFamily = Theme.R.fontFamilyDMsanss
            )
        }

        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Удалить из истории",
                tint = Color.LightGray
            )
        }
    }
}

@Preview
@Composable
private fun HistoryMenuItemPreview() {
    HistoryMenuItem(
        text = "Sample Query",
        onClick = {},
        onDeleteClick = {}
    )
}
