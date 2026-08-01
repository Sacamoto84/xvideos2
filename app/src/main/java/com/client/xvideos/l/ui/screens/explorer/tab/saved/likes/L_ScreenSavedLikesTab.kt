package com.client.xvideos.l.ui.screens.explorer.tab.saved.likes

import com.client.xvideos.common.theme.Theme

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuType
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.L_LazyRowPictureDetails
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.LazyRowPictureDetailsHost
import com.client.xvideos.r.ui.explorer.tab.gifs.ColumnSelect_AddColumn
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

fun L_ScreenSavedLikesTab_AddColumn(){
    ColumnSelect_AddColumn(Settings.l_likesTab_column_current_count, Settings.l_likesTab_G_0_4)
}
/**
 * [Extended FAB image](1https://ah-img.luscious.net/the-one/596517/1000026019_01KFCDPQ2VPGM41HXS4G543Z2P.1680x0.jpg?md5=9bYJbclKQfs6MvGig7YDpw&expires=1769523334)

 * ![256x75 Extended FAB image](https://media.istockphoto.com/id/1346600407/ru/векторная/здоровая-семейная-ходьба.jpg?s=612x612&w=0&k=20&c=1SdYbCJCqN1h7rHfr1XoqGjYWP43d3lEixOQb3GqvU4=)
 * !2[256x Extended FAB image](https://media.istockphoto.com/id/1346600407/ru/векторная/здоровая-семейная-ходьба.jpg?s=612x612&w=0&k=20&c=1SdYbCJCqN1h7rHfr1XoqGjYWP43d3lEixOQb3GqvU4=)
 * !2[x75 Extended FAB image](hhttps://media.istockphoto.com/id/1346600407/ru/векторная/здоровая-семейная-ходьба.jpg?s=612x612&w=0&k=20&c=1SdYbCJCqN1h7rHfr1XoqGjYWP43d3lEixOQb3GqvU4=)
 *
 * Space is not necessary but is used for readability.
 */
object L_ScreenSavedLikesTab : Screen {

    private fun readResolve(): Any = L_ScreenSavedLikesTab

    override val key: ScreenKey = uniqueScreenKey

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    override fun Content() {

        val vm: ScreenSavedLLikesSM = getScreenModel()

        val column = Settings.l_likesTab_column_current_count.field.collectAsStateWithLifecycle().value

        LaunchedEffect(column) {
            if (column != 0) {
                vm.host.columns = column
            }
        }

        var selectedIndex by remember { mutableIntStateOf(0) }

        val options = listOf("All", "Image", "Gif")

        LaunchedEffect(vm.original.size, selectedIndex) {
            vm.filterSelect(selectedIndex)
        }

        Scaffold(modifier = Modifier.fillMaxSize().background(Theme.background)) {

            L_LazyRowPictureDetails(
                vm.host,
                expandMenu = ExpandMenuType.LIKES,
                tag = "lLikes",
                itemBefore = {

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.displayCutoutPadding().padding(horizontal = 4.dp)) {

                        options.forEachIndexed { index, label ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = options.size
                                ),
                                onClick = {
                                    selectedIndex = index
                                    vm.filterSelect(selectedIndex)
                                },
                                selected = index == selectedIndex,
                                label = { Text(label) },
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor =  Color(0xFF938F99)// Theme.L.b0
                                )
                            )
                        }

                    }


                }
            )
        }

    }

}


enum class AllImagGif {
    ALL, IMAGE, GIF
}

class ScreenSavedLLikesSM @Inject constructor(
    val savedL: SavedL
) : ScreenModel {

    val host = LazyRowPictureDetailsHost("l_likes")

    /**
     * Выбор типа отображаемого контента
     */
    private var selectorFilter = AllImagGif.ALL

    val original = savedL.likes.listUrl

    init {
        filterSelect(selectorFilter)
    }

    fun filterSelect(item: AllImagGif) {
        when (item) {
            AllImagGif.ALL   -> { selectAll() }
            AllImagGif.IMAGE -> { selectImage() }
            AllImagGif.GIF   -> { selectGif() }
        }
    }

    fun filterSelect(index: Int) {
        when (index) {
            0 -> {
                selectorFilter = AllImagGif.ALL
                selectAll()
            }
            1 -> {
                selectorFilter = AllImagGif.IMAGE
                selectImage()
            }
            2 -> {
                selectorFilter = AllImagGif.GIF
                selectGif()
            }
        }
    }

    fun delete(item: PicsDetails) {
        // url_to_original у Luscious опционален (именно поэтому ключи в
        // LazyLayout строятся с фолбэком). Было `!!` — краш на удалении
        // элемента без него; удалять там нечего, просто выходим.
        val url = item.url_to_original ?: return
        savedL.likes.remove(url)
    }

    fun selectGif() {
        host.replaceFilteredPictures(original.filter { it.is_animated })
    }

    fun selectImage() {
        host.replaceFilteredPictures(original.filter { !it.is_animated })
    }

    fun selectAll() {
        host.replaceFilteredPictures(original)
    }

}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLSavedLikes {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenSavedLLikesSM::class)
    abstract fun bindScreenLSavedLikesScreenModel(hiltListScreenModel: ScreenSavedLLikesSM): ScreenModel
}
