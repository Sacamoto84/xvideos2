package com.client.xvideos.l.ui.element.expandMenu.atom

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.theme.ThemeL.ExpandMenu.style
import com.client.xvideos.l.theme.ThemeL.ExpandMenu.tintColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuItem_Share(item: PicsDetails? = null, onClick: (PicsDetails) -> Unit = {}, onDismiss: () -> Unit){
    DropdownMenuItem(
        leadingIcon = {Icon(Icons.Default.Share, contentDescription = "", tint = tintColor)},
        text = { Text("Поделиться", style = style) },
        onClick = {
            if (item == null) return@DropdownMenuItem
            //DownloadRed.downloadItem(item)
            onClick.invoke(item)
            onDismiss.invoke()
        }, contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
    )
}