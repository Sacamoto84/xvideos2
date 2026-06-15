package com.client.xvideos.l.ui.element.expandMenu.element

import com.client.xvideos.common.theme.Theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.common.theme.Theme.ExpandMenu.style
import com.client.xvideos.common.theme.Theme.ExpandMenu.tintColor

@Preview(apiLevel = 29, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun Preview(){
    DropdownMenuItem_Share(null, onClick={}, onDismiss = {})
}

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