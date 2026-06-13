package com.client.xvideos.l.ui.element.expandMenu

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.common.theme.Theme.L.ExpandMenu.backgroundColor
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.ui.element.expandMenu.element.DropdownMenuItem_AddCollection
import com.client.xvideos.l.ui.element.expandMenu.element.DropdownMenuItem_Delete
import com.client.xvideos.l.ui.element.expandMenu.element.DropdownMenuItem_RemoveFromCollection
import com.client.xvideos.l.ui.element.expandMenu.element.DropdownMenuItem_SaveToGallery
import com.client.xvideos.l.ui.element.expandMenu.element.DropdownMenuItem_SetCover
import com.client.xvideos.l.ui.element.expandMenu.element.DropdownMenuItem_Share
import com.client.xvideos.ui.theme.XvideosTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedLikesItemExpandMenu(
    item: PicsDetails? = null,
    onClick: () -> Unit = {},
    onDelete: (PicsDetails) -> Unit = {},
    onAddCollection: (PicsDetails) -> Unit = {},
    onRemoveFromCollection: (PicsDetails) -> Unit = {},
    onShare: (PicsDetails) -> Unit = {},
    onSaveToGallery: (PicsDetails) -> Unit = {},
    isCollection: Boolean = false,
    savedL: SavedL? = null,
    haptic : ()->Unit = {}
) {

    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) { haptic.invoke() }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (it) onClick.invoke(); expanded = it },
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

            DropdownMenuItem_Share(item, onClick = { onShare(it) }) { expanded = false }

            DropdownMenuItem_SaveToGallery(item, onClick = { onSaveToGallery(it) }) { expanded = false }

            // Show Delete only when NOT in collection view
            if (!isCollection) {
                DropdownMenuItem_Delete(item, onClick = {onDelete(it)}
                ){ expanded = false }
            }

            DropdownMenuItem_AddCollection(item, savedL) { expanded = false }

            // Show RemoveFromCollection always (when in collection view or when item is in any collection)
            if (isCollection) {
                DropdownMenuItem_RemoveFromCollection(item, onRemoveFromCollection, savedL) { expanded = false }
                DropdownMenuItem_SetCover(item, savedL) { expanded = false }
            }

        }

    }
}



@Preview(showBackground = true, backgroundColor = 0xFF303030)
@Composable
private fun SavedLikesItemExpandMenuPreview() {
    XvideosTheme(darkTheme = true) {
        SavedLikesItemExpandMenu(
            item = PicsDetails(
                height = 1080,
                width = 1920,
                is_animated = false,
                url_to_original = null,
                url_to_video = null,
                album = "preview-album",
                thumbnails = emptyList(),
            )
        )
    }
}
//@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
//@Composable
//fun DropdownMenuItem_Like(item: GifsInfo? = null, onRunLike: () -> Unit, savedRed: SavedRed, onDismiss: () -> Unit){
//    val isLiked = savedRed.likes.list.any { it.id == item?.id }
//    val textLiked = if (isLiked) "Unlike" else "Like"
//    val textLikedIcon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder
//    DropdownMenuItem(
//        leadingIcon = {Icon(textLikedIcon, contentDescription = "", tint = tintColor)},
//        text = { Text(textLiked, style = style) },
//        onClick = {
//            if (item == null) return@DropdownMenuItem
//            GlobalScope.launch {
//                delay(200)
//                if (!isLiked) savedRed.likes.add(item) else savedRed.likes.remove(item)
//                onRunLike.invoke()
//                onDismiss.invoke()
//            }
//        }, contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
//    )
//}

//@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
//@Composable
//fun DropdownMenuItem_Follow(item: GifsInfo? = null, redApi: RedApi, savedRed: SavedRed, onDismiss: () -> Unit){
//    val isFollowed = savedRed.creators.list.any { it.username == item?.userName }
//    val textFollowed = if (isFollowed) "Unfollow" else "Follow"
//    val textFollowedIcon = if (isFollowed) Icons.Default.Person else Icons.Default.PermIdentity
//    DropdownMenuItem(
//        leadingIcon = {Icon(textFollowedIcon, contentDescription = "", tint = tintColor)},
//        text = { Text(textFollowed, style = style) },
//        onClick = {
//            if (item == null) return@DropdownMenuItem
//            GlobalScope.launch {
//                delay(200)
//                if (!isFollowed) {
//                    try {
//                        val a = redApi.readCreator(item.userName).getOrNull()
//                        savedRed.creators.add(a!!)
//                    } catch (e: Exception) { e.printStackTrace() }
//                }
//                else {
//                    savedRed.creators.remove(item.userName)
//                }
//            }
//            onDismiss.invoke()
//        }, contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
//    )
//}
