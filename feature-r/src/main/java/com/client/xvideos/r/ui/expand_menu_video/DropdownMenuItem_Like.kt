package com.client.xvideos.r.ui.expand_menu_video

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.ui.theme.XvideosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DropdownMenuItem_Like(item: GifsInfo? = null, onRunLike: () -> Unit, savedRed: () -> SavedRed, onDismiss: () -> Unit) {
    val isLiked = savedRed.invoke().likes.list.any { it.id == item?.id }
    val handleClick = remember(item, isLiked, savedRed, onRunLike, onDismiss) {
        {
            if (item != null) {
                // scope из SavedRed (@ApplicationScope) живёт, пока живо приложение,
                // поэтому переживает закрытие меню — отложенная на 200 мс мутация
                // (нужна, чтобы список не дёргался во время анимации скрытия) точно
                // выполнится. При этом scope управляемый, в отличие от GlobalScope.
                savedRed.invoke().scope.launch {
                    delay(200L)
                    if (!isLiked) savedRed.invoke().likes.add(item) else savedRed.invoke().likes.remove(item)
                    withContext(Dispatchers.Main) {
                        onRunLike.invoke()
                    }
                }
                onDismiss.invoke()
            }
        }
    }
    DropdownMenuItem_LikeContent(
        isLiked = isLiked,
        onClick = handleClick
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
