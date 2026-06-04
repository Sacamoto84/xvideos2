package com.client.xvideos.r.common.expand_menu_video


import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.r.common.ThemeRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.ui.theme.XvideosTheme


private val tintColor = Color(0xFF48454E)
private val style = TextStyle(color = tintColor, fontFamily = ThemeRed.fontFamilyPopinsRegular, fontSize = 20.sp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandMenuVideoTags(
    item: GifsInfo? = null,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit = {},
) {

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.then(modifier)
    )
    {
        IconButton(
            modifier = Modifier
                .size(48.dp)
                .menuAnchor(ExposedDropdownMenuAnchorType.SecondaryEditable),
            onClick = {}) {
            Icon(
                Icons.Outlined.Tag,
                contentDescription = "",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(IntrinsicSize.Min),
            containerColor = ThemeRed.colorTabLevel3
        ) {

            FlowRow {
                item?.tags?.forEach {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .padding(vertical = 4.dp)
                            .border(1.dp, ThemeRed.colorYellow, RoundedCornerShape(4.dp))
                            .clickable(onClick = {
                                onClick(it)
                                expanded = false
                            }),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            it,
                            color = Color.White,
                            fontFamily = ThemeRed.fontFamilyDMsanss,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF303030)
@Composable
fun ExpandMenuVideoTagsPreview() {
    XvideosTheme {
        ExpandMenuVideoTags(
            item = GifsInfo(
                tags = listOf("Action", "Adventure", "Comedy", "Drama", "Fantasy", "Sci-Fi"),
                id = "id",
            )
        )
    }
}
