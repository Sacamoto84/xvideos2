package com.client.xvideos.n.ui.screens.tag

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.ui.atom.NGalleryTopBar
import com.client.xvideos.n.ui.molecule.NGalleryPager
import com.client.xvideos.n.ui.screens.gallery.ScreenNGallery

/**
 * Экран списка работ по выбранному тегу, категории или автору.
 */
class ScreenNTag(val tag: NTag) : Screen {

    override val key: ScreenKey = "NTag:${tag.type}:${tag.id}:${tag.name}"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenNTagSM, ScreenNTagSM.Factory> { factory ->
            factory.create(tag)
        }

        val onBack: () -> Unit = remember(navigator) { { navigator.pop() } }
        BackHandler(onBack = onBack)

        val columns by vm.columns.collectAsStateWithLifecycle()
        val title = if (tag.type.isNotEmpty() && tag.type != "tag") "${tag.type}: ${tag.name}" else tag.name

        Column(Modifier.fillMaxSize().background(Theme.background)) {
            NGalleryTopBar(title = title, onBack = onBack, topInset = getTopInsetDp())
            NGalleryPager(
                pages = vm.pages,
                columns = columns,
                onGalleryClick = { id -> navigator.push(ScreenNGallery(id)) },
                emptyMessage = "Работ с этим тегом пока нет"
            )
        }
    }
}
