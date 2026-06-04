package com.client.xvideos.common.icons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Composable
fun IconSave18(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Filled.Save,
        contentDescription = "Save",
        tint = Color.White,
        modifier = modifier.size(18.dp)
    )
}

@Preview
@Composable
fun IconSave18Preview() {
    IconSave18()
}
