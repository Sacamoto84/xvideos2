package com.client.xvideos.screenSettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun Preview() {
    var value by remember { mutableStateOf(false) }
    ConfigTextAndCheckBox("text", value, onValueChange = { value = it })
}

@Preview
@Composable
fun PreviewTrue() {
    var value by remember { mutableStateOf(true) }
    ConfigTextAndCheckBox("text", value, onValueChange = { value = it })
}


@Composable
fun ConfigTextAndCheckBox(text: String, value: Boolean, onValueChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 8.dp).padding(vertical = 2.dp).height(48.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = styleTextConfig)
        Checkbox(value, onValueChange, colors = CheckboxDefaults.colors(disabledColor = Color.White, uncheckedColor = Color.Gray))
    }
}
