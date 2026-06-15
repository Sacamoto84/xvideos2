package com.client.xvideos.r.common.expand_menu_video

import com.client.xvideos.common.theme.Theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.ui.theme.XvideosTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuItem_AddCollection(item: GifsInfo? = null, savedRed: ()->SavedRed, onDismiss: () -> Unit){
    DropdownMenuItem_AddCollectionContent(
        onClick = {
            if (item == null) return@DropdownMenuItem_AddCollectionContent
            savedRed.invoke().collections.collectionItemGifInfo = item
            savedRed.invoke().collections.visibleDialog = true
            onDismiss.invoke()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownMenuItem_AddCollectionContent(
    onClick: () -> Unit
) {
    DropdownMenuItem(
        leadingIcon = { Icon( Icons.Default.AddCircleOutline, contentDescription = "", tint = Theme.ExpandMenu.tintColor ) },
        text = { Text("Add to Collection", style = Theme.ExpandMenu.style) },
        onClick = onClick,
        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
    )
}

@Preview(showBackground = true)
@Composable
private fun DropdownMenuItem_AddCollectionPreview() {
    XvideosTheme {
        DropdownMenuItem_AddCollectionContent(
            onClick = {}
        )
    }
}