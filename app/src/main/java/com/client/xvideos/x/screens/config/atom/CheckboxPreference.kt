package com.client.xvideos.screens.config.atom

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.edit

@Preview
@Composable
fun CheckboxPreferencePreview() {
    CheckboxPreference(title = "2 столбика", key = "dark_mode", defaultValue = false)
}






@Composable
fun CheckboxPreference(
    title: String,
    state: Boolean,
    onChange : (Boolean) -> Unit ,
    modifier: Modifier = Modifier,
) {

    Row(
        modifier = modifier.padding(horizontal = 8.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = title,
            modifier = Modifier.padding(start = 8.dp)
        )

        Checkbox(
            checked = state,
            onCheckedChange = { isChecked ->
              onChange(isChecked)
            }
        )

    }
}




@Composable
fun CheckboxPreference(
    title: String,
    key: String,
    defaultValue: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var checked by remember {
        mutableStateOf(context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
            .getBoolean(key, defaultValue))
    }

    Row(
        modifier = modifier.padding(horizontal = 8.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = title,
            modifier = Modifier.padding(start = 8.dp)
        )

        Checkbox(
            checked = checked,
            onCheckedChange = { isChecked ->
                checked = isChecked
                context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                    .edit {
                        putBoolean(key, isChecked)
                    }
            }
        )

    }
}