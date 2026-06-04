package com.client.xvideos.l.ui.element.expandMenu

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.theme.ThemeL.ExpandMenu
import com.client.xvideos.l.theme.ThemeL.ExpandMenu.backgroundColor
import com.client.xvideos.l.theme.ThemeL.ExpandMenu.style
import com.client.xvideos.l.theme.ThemeL.ExpandMenu.tintColor
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.ui.element.expandMenu.atom.DropdownMenuItem_Download
import com.client.xvideos.l.ui.element.expandMenu.atom.DropdownMenuItem_Share

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumItemExpandMenu(
    item: PicsDetails? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onDownload : (PicsDetails) -> Unit = {},
    onShare: (PicsDetails) -> Unit = {},
    isCollection: Boolean = false,
    savedL: SavedL? = null,
    onRemoveFromCollection: (PicsDetails) -> Unit = {},
    haptic : ()->Unit = {},
    idAlbum: String = ""
) {

    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) { haptic.invoke() }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (it) onClick.invoke(); expanded = it },
        modifier = Modifier.then(modifier)
    )
    {
        IconButton(
            modifier = Modifier.size(48.dp).menuAnchor(ExposedDropdownMenuAnchorType.SecondaryEditable),
            onClick = {}) {
            Icon( Icons.Default.MoreVert, contentDescription = "", tint = Color.Black, modifier = Modifier.size(24.dp).offset(0.5.dp, 0.5.dp))
            Icon( Icons.Default.MoreVert, contentDescription = "", tint = Color.White, modifier = Modifier.size(24.dp))
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(IntrinsicSize.Min),
            containerColor = backgroundColor
        ) {

            DropdownMenuItem_Download(item, onClick = {onDownload(it)}
            ){ expanded = false }

            DropdownMenuItem_Share(item, onClick = {onShare(it)}
            ){ expanded = false }

            DropdownMenuItem_AddCollection(item, savedL, idAlbum) { expanded = false }

            // Show RemoveFromCollection only when in collection view
            if (isCollection) {
                DropdownMenuItem_RemoveFromCollection(item, onRemoveFromCollection, savedL) { expanded = false }
            }

        }

    }
}








