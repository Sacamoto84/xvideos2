package com.client.xvideos.x.screens.tags

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import com.client.xvideos.x.screens.tags.atom.TagsPaginatedListScreen

class ScreenTags(private val tag: String) : Screen {

    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {

        val vm = getScreenModel<ScreenTagsViewModel, ScreenTagsViewModel.Factory> { factory -> factory.create(tag) }

        Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
            Column {
                Text(tag)
                Row {
                    Text(vm.screen.title0 + " ")
                    Text(vm.screen.title1, color = Color(0xFF787878))
                }
            }
        }) { padding ->
            // Раньше padding игнорировался (`{ _ -> }`) — список рисовался под
            // topBar'ом, и его первые строки оказывались перекрыты заголовком.
            Box(modifier = Modifier.padding(padding)) {
                TagsPaginatedListScreen(0)
            }
        }
    }

}
