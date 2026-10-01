package com.client.xvideos.l.ui.element.expandMenu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import cafe.adriel.voyager.navigator.LocalNavigator
import com.client.xvideos.common.p2p.ui.P2pSendChooserDialog
import com.client.xvideos.common.p2p.ui.ScreenP2pSend

/**
 * Хост диалога P2P-шаринга для Luscious.
 * Должен компоноваться ровно один раз на контейнер (список/экран).
 */
@Composable
fun ExpandMenuP2pHost(viewModel: ExpandMenuViewModel) {
    val navigator = LocalNavigator.current
    viewModel.p2pChooserItem?.let { item ->
        P2pSendChooserDialog(
            onSystem = { viewModel.share(item) },
            onP2p = { viewModel.startP2p(item) },
            onDismiss = { viewModel.dismissChooser() },
        )
    }
    viewModel.p2pSource?.let { source ->
        LaunchedEffect(source) {
            navigator?.push(ScreenP2pSend(source))
            viewModel.dismissP2p()
        }
    }
}
