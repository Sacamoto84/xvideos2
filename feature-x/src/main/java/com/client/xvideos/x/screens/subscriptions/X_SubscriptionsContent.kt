package com.client.xvideos.x.screens.subscriptions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.model.XSubscriptionItem
import com.client.xvideos.x.screens.channel.ScreenX_Channel
import com.client.xvideos.x.screens.subscriptions.molecule.DialogXSubscriptionDelete
import com.client.xvideos.x.screens.subscriptions.molecule.X_SubscriptionsList
import kotlinx.coroutines.launch

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
 * @param listState Прокрутка списка из ScreenModel дашбордов.
 */
@Composable
fun X_SubscriptionsContent(
    saved: SavedX,
    isModel: Boolean,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.currentOrThrow

    // Порядок — свой у каналов и у моделей, хранится в настройках.
    val orderSetting = if (isModel) Settings.x_subscriptions_models_order else Settings.x_subscriptions_channels_order
    val orderOrdinal by orderSetting.field.collectAsStateWithLifecycle()
    val order = xSubscriptionsOrderOf(orderOrdinal)

    // Список хранилища — SnapshotStateList: сортировка через derivedStateOf с
    // чтением внутри, иначе remember не заметил бы новой или удалённой подписки.
    val itemsList by remember(saved, isModel, order) {
        derivedStateOf {
            val source = if (isModel) saved.subscriptions.modelsList else saved.subscriptions.channelsList
            xSortedSubscriptions(source, order)
        }
    }

    var itemToDelete by remember { mutableStateOf<XSubscriptionItem?>(null) }
    val scope = rememberCoroutineScope()

    // Новый порядок — с начала списка.
    val onOrderChange: (XSubscriptionsOrder) -> Unit = remember(orderSetting, listState, scope) {
        { value ->
            orderSetting.setValue(value.ordinal)
            scope.launch { listState.scrollToItem(0) }
        }
    }
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

    // Сохранённый адрес аватара устаревает — свежий берётся со страницы профиля.
    val onImageError = remember(saved) {
        { item: XSubscriptionItem -> saved.subscriptions.refreshImages(item) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.L.grey6)
    ) {
        X_SubscriptionsList(
            itemsList = itemsList,
            isModel = isModel,
            topCutout = topCutout,
            listState = listState,
            onOpenCreator = onOpenCreator,
            onDeleteRequest = { itemToDelete = it },
            onImageError = onImageError,
            order = order,
            onOrderChange = onOrderChange
        )

        // Диалог подтверждения удаления/отписки
        DialogXSubscriptionDelete(
            item = itemToDelete,
            onDismiss = { itemToDelete = null },
            onConfirm = onConfirmDelete,
        )
    }
}
