package com.client.xvideos.r.common.expand_menu_video

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.ui.theme.XvideosTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DropdownMenuItem_Like(item: GifsInfo? = null, onRunLike: () -> Unit, savedRed: ()-> SavedRed, onDismiss: () -> Unit){
    val isLiked = savedRed.invoke().likes.list.any { it.id == item?.id }
    DropdownMenuItem_LikeContent(
        isLiked = isLiked,
        onClick = {
            if (item == null) return@DropdownMenuItem_LikeContent
            // scope из SavedRed (@ApplicationScope) живёт, пока живо приложение,
            // поэтому переживает закрытие меню — отложенная на 200 мс мутация
            // (нужна, чтобы список не дёргался во время анимации скрытия) точно
            // выполнится. При этом scope управляемый, в отличие от GlobalScope.
            savedRed.invoke().scope.launch {
                delay(200)
                if (!isLiked) savedRed.invoke().likes.add(item) else savedRed.invoke().likes.remove(item)
                onRunLike.invoke()
                onDismiss.invoke()
            }
        }
    )
}

@Composable
private fun DropdownMenuItem_LikeContent(
    isLiked: Boolean,
    onClick: () -> Unit
) {
    ExpandMenuActionItem(
        icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        text = if (isLiked) "Unlike" else "Like",
        onClick = onClick
    )
}

@Preview(showBackground = true)
@Composable
private fun DropdownMenuItem_LikePreview() {

    XvideosTheme {
        Column {
            DropdownMenuItem_LikeContent(
                isLiked = true,
                onClick = {}
            )

            DropdownMenuItem_LikeContent(
                isLiked = false,
                onClick = {}
            )
        }

    }

}
