package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.molecule.LCollectionDetailTopBar
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.molecule.L_CollectionNameContent

class ScreenCollectionName(
    val collectionName: String,
    private val popOnBack: Boolean = false
) : Screen {

    override val key: ScreenKey = "LCollection:$collectionName:$popOnBack"

    @Composable
    override fun Content() {

        val vm = getScreenModel<ScreenLCollectionNameSM, ScreenLCollectionNameSM.Factory> { factory -> factory.create(collectionName) }
        val navigator = LocalNavigator.currentOrThrow

        val onExitCollection: () -> Unit = remember(vm.savedL, popOnBack, navigator) {
            {
                vm.savedL.collection.exitCollection()
                if (popOnBack) {
                    navigator.pop().let {}
                }
            }
        }

        L_CollectionNameContent(
            collectionName = collectionName,
            savedL = vm.savedL,
            host = vm.host,
            onExitCollection = onExitCollection
        )

    }
}

@Preview
@Composable
private fun ScreenCollectionNamePreview() {
    LCollectionDetailTopBar(
        collectionName = "Preview Collection",
        searchQuery = "",
        searchVisible = false,
        onSearchChange = {},
        onToggleSearch = {},
        onExitCollection = {}
    )
}
