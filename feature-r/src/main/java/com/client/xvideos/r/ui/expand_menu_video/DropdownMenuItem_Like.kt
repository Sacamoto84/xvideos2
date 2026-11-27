package com.client.xvideos.r.ui.expand_menu_video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.GifsInfo
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
