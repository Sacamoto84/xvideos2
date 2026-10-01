package com.client.xvideos.x.screens.subscriptions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.model.XSubscriptionItem
import com.client.xvideos.x.screens.channel.ScreenX_Channel
import com.client.xvideos.x.screens.subscriptions.molecule.DialogXSubscriptionDelete
import com.client.xvideos.x.screens.subscriptions.molecule.X_SubscriptionsList

/**
 * Контент экрана подписок раздела X (вкладки «Каналы» и «Актрисы»).
 *
 * Отображает вертикальный список подписанных авторов:
 * - Превью (аватарка/баннер) с закругленными углами.
 * - Имя автора и метаинформация (число видео, подписчики, просмотры).
 * - Нажатие на элемент открывает экран канала/актрисы ([ScreenX_Channel]).
 * - Долгое нажатие или кнопка удаления вызывают диалог подтверждения отписки ([DialogXSubscriptionDelete]).
 *
 * @param saved Фасад локальных данных X ([SavedX]).
 * @param isModel `true` для вкладки актрис/моделей, `false` для вкладки каналов.
 */
@Composable
fun X_SubscriptionsContent(
    saved: SavedX,
    isModel: Boolean,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.currentOrThrow

    val itemsList = if (isModel) {
        saved.subscriptions.modelsList
    } else {
        saved.subscriptions.channelsList
    }

    var itemToDelete by remember { mutableStateOf<XSubscriptionItem?>(null) }
    val listState = rememberLazyListState()
    val topCutout = getTopInsetDp()

    BackHandler(enabled = itemToDelete != null) {
        itemToDelete = null
    }

    val onOpenCreator = remember(navigator, isModel) {
        { item: XSubscriptionItem ->
            navigator.push(ScreenX_Channel(slug = item.cleanSlug, isModel = item.isModel))
        }
    }

    val onConfirmDelete = remember(saved, isModel) {
        { item: XSubscriptionItem ->
            if (item.isModel) {
                saved.subscriptions.removeModel(item.cleanSlug)
            } else {
                saved.subscriptions.removeChannel(item.cleanSlug)
            }
            itemToDelete = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040404))
    ) {
        X_SubscriptionsList(
            itemsList = itemsList,
            isModel = isModel,
            topCutout = topCutout,
            listState = listState,
            onOpenCreator = onOpenCreator,
            onDeleteRequest = { itemToDelete = it }
        )

        // Диалог подтверждения удаления/отписки
        DialogXSubscriptionDelete(
            item = itemToDelete,
            onDismiss = { itemToDelete = null },
            onConfirm = onConfirmDelete,
        )
    }
}
