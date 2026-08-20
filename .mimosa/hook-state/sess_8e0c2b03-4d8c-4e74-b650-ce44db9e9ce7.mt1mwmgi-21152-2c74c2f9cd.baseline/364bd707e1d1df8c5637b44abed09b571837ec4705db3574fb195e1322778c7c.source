package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.l.model.albumFilterDisplay


@Preview(showSystemUi = false, showBackground = false)
@Composable
fun PreviewAlbumFilterDisplay() {
    val select by remember { mutableStateOf("alpha_a") }
    AlbumFilterDisplay(select, onRequestApply = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumFilterDisplay(startString: String, onRequestApply: (String) -> Unit) {

    val list = albumFilterDisplay
    val palette = StyleGenresTags.Palette

    val uniquePrimaryList = albumFilterDisplay.map { it.primary }.distinct()

    var expanded by remember { mutableStateOf(false) }
    var expanded2 by remember { mutableStateOf(false) }

    var selected by remember {
        mutableStateOf(list.firstOrNull { it.request == startString } ?: list.first())
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- Первое меню (Primary) ---
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier
                .weight(1f) // равная ширина
                .height(48.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize() // заполняет всю выделенную ширину
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, palette.border, RoundedCornerShape(6.dp))
                    .background(palette.field)
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BasicText(
                    selected.primary,
                    modifier = Modifier.padding(start = 4.dp),
                    maxLines = 1,
                    style = Theme.L.Type.rowValue.copy(color = palette.textPrimary)
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = palette.textSecondary
                )
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = palette.surfaceHigh
            ) {
                uniquePrimaryList.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                item,
                                color = palette.textPrimary,
                                style = Theme.L.Type.rowValue.copy(color = palette.textPrimary)
                            )
                        },
                        onClick = {
                            selected = list.first{it.primary == item}
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.width(4.dp))

        // --- Второе меню (Secondary) ---
        ExposedDropdownMenuBox(
            expanded = expanded2,
            onExpandedChange = { expanded2 = it },
            modifier = Modifier
                .weight(1f) // равная ширина
                .height(48.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, palette.border, RoundedCornerShape(6.dp))
                    .background(palette.field)
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BasicText(
                    selected.secondary,
                    modifier = Modifier.padding(start = 4.dp),
                    maxLines = 1,
                    style = Theme.L.Type.rowValue.copy(color = palette.textPrimary)
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = palette.textSecondary
                )
            }

            ExposedDropdownMenu(
                expanded = expanded2,
                onDismissRequest = { expanded2 = false },
                containerColor = palette.surfaceHigh
            ) {
                val items = list.filter { it.primary == selected.primary }
                items.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                item.secondary,
                                color = palette.textPrimary,
                                style = Theme.L.Type.rowValue.copy(color = palette.textPrimary),
                                maxLines = 1
                            )
                        },
                        onClick = {
                            selected = list.first{it.secondary == item.secondary}

                            expanded2 = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.width(4.dp))

        // --- Кнопка ---
        Box(
            modifier = Modifier
                .weight(1f) // равная ширина
                .height(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, palette.accent, RoundedCornerShape(6.dp))
                .background(palette.accentDark)
                .clickable(onClick = { onRequestApply(selected.request) }),
            contentAlignment = Alignment.Center
        ) {
            Text("Apply", color = Color.White, style = Theme.L.Type.button.copy(color = Color.White))
        }
    }

}
