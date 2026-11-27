package com.client.xvideos.common.icons

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.core.R

@Composable
fun IconCollection18(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.collection_multi_input_svgrepo_com),
        contentDescription = null,
        tint = Color.White,
        modifier = modifier.size(18.dp)
    )
}

@Preview
@Composable
fun IconCollection18Preview() {
    IconCollection18()
}
