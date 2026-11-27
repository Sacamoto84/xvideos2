package com.client.xvideos.n.ui.screens.explorer.tab.search.molecule

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.ui.excludedFromAutofill

@Composable
internal fun NFilterNumberField(
    value: Int?,
    label: String,
    onChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value?.toString().orEmpty(),
        onValueChange = { text ->
            if (text.isEmpty()) onChange(null)
            else text.toIntOrNull()?.takeIf { it >= 0 && text.all(Char::isDigit) }?.let(onChange)
        },
        modifier = modifier.excludedFromAutofill(),
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Preview
@Composable
private fun NFilterNumberFieldPreview() {
    NFilterNumberField(value = 12, label = "От", onChange = {})
}
