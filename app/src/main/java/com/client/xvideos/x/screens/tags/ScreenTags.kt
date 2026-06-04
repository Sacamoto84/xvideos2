package com.client.xvideos.x.screens.tags

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.x.screens.tags.atom.TagsPaginatedListScreen

class ScreenTags(private val tag: String) : Screen {

    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val vm = getScreenModel<ScreenTagsViewModel, ScreenTagsViewModel.Factory> { factory -> factory.create(tag) }

        Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
            Column {
                Text(tag)
                Row {
                    Text(vm.screen.title0 + " ")
                    Text(vm.screen.title1, color = Color(0xFF787878))
                }
            }
        }) { _ ->
            TagsPaginatedListScreen(0)
        }
    }

}