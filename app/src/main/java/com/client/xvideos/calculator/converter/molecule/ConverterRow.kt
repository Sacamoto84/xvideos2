package com.client.xvideos.calculator.converter.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.calculator.CalculatorColors

@Composable
internal fun ConverterRow(
    unit: String,
    value: String,
    isOrange: Boolean,
    availableUnits: List<String>,
    onUnitSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { showMenu = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = unit,
                    color = CalculatorColors.XIAOMI_TEXT_SECONDARY,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = " ⇅",
                    color = CalculatorColors.XIAOMI_TEXT_SECONDARY,
                    fontSize = 14.sp
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(CalculatorColors.XIAOMI_BG)
            ) {
                availableUnits.forEach { u ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = u,
                                color = if (u == unit) {
                                    CalculatorColors.XIAOMI_ORANGE
                                } else {
                                    CalculatorColors.XIAOMI_TEXT_PRIMARY
                                }
                            )
                        },
                        onClick = {
                            onUnitSelected(u)
                            showMenu = false
                        }
                    )
                }
            }
        }

        Text(
            text = value,
            color = if (isOrange) CalculatorColors.XIAOMI_ORANGE else CalculatorColors.XIAOMI_TEXT_PRIMARY,
            fontSize = 34.sp,
            fontWeight = if (isOrange) FontWeight.Normal else FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}

@Preview
@Composable
private fun ConverterRowPreview() {
    ConverterRow(
        unit = "м",
        value = "12.5",
        isOrange = true,
        availableUnits = listOf("м", "см", "км"),
        onUnitSelected = {},
    )
}
