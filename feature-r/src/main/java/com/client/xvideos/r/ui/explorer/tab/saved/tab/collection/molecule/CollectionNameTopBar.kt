package com.client.xvideos.r.ui.explorer.tab.saved.tab.collection.molecule

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp

@Composable
fun CollectionNameTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 4.dp,
                top = getTopInsetDp(),
                end = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = Theme.R.colorYellow
            )
        }
        Text(
            text = title,
            modifier = Modifier.padding(start = 4.dp),
            color = Theme.R.colorYellow,
            fontSize = 18.sp,
            fontFamily = Theme.R.fontFamilyPopinsRegular
        )
    }
}

@Preview
@Composable
private fun CollectionNameTopBarPreview() {
    CollectionNameTopBar(
        title = "Favorites",
        onBackClick = {}
    )
}
