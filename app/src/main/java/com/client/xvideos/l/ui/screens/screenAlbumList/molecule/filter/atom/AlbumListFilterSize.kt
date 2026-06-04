package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.client.xvideos.l.theme.ThemeL
import com.client.xvideos.l.model.enum.PictureCountRank

private val style = ThemeL.Type.rowTitle.copy(fontWeight = FontWeight.Bold)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumListFilterSize(value: PictureCountRank, onChanged: (PictureCountRank) -> Unit) {

    var expanded by remember { mutableStateOf(false) }
    val palette = StyleGenresTags.Palette

    val itemS = listOf("Any", "0..25", "25..50", "50..100", "200..800", "800..3200", "3200..12800")

    val a = when (value) {
        PictureCountRank.All -> "Any"
        PictureCountRank.C0_25 -> "0..25"
        PictureCountRank.C25_50 -> "25..50"
        PictureCountRank.C50_100 -> "50..100"
        PictureCountRank.C100_200 -> "100..200"
        PictureCountRank.C200_800 -> "200..800"
        PictureCountRank.C800_3200 -> "800..3200"
        PictureCountRank.C3200_12800 -> "3200..12800"
    }

    // --- Первое меню (Primary) ---

    Row(modifier = Modifier.padding(start = 4.dp). fillMaxWidth(),verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {

        Text("Album Size", style = style.copy(color = palette.textPrimary))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.height(43.dp)
        )
        {
            Row(
                modifier = Modifier
                    .width(160.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, palette.border, RoundedCornerShape(6.dp))
                    .background(palette.field)
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                BasicText(
                    a,
                    modifier = Modifier.padding(start = 8.dp),
                    maxLines = 1,
                    style = ThemeL.Type.rowTitle.copy(color = palette.textPrimary, fontWeight = FontWeight.Bold)
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
                itemS.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                item,
                                color = palette.textPrimary,
                                style = ThemeL.Type.rowValue.copy(color = palette.textPrimary)
                            )
                        },
                        onClick = {

                            val selected = when (item) {
                                "Any" -> PictureCountRank.All
                                "0..25" -> PictureCountRank.C0_25
                                "25..50" -> PictureCountRank.C25_50
                                "50..100" -> PictureCountRank.C50_100
                                "100..200" -> PictureCountRank.C100_200
                                "200..800" -> PictureCountRank.C200_800
                                "800..3200" -> PictureCountRank.C800_3200
                                "3200..12800" -> PictureCountRank.C3200_12800
                                else -> PictureCountRank.All
                            }

                            onChanged(selected)
                            expanded = false
                        }
                    )
                }
            }
        }

    }

}
