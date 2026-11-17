package com.client.xvideos.n.ui.screens.explorer.tab.saved.likes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.settings.ColumnSelect_AddColumn
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.ui.molecule.NCollectionDialogs
import com.client.xvideos.n.ui.molecule.NPictureGrid
import com.client.xvideos.n.ui.molecule.NPictureItemExpandMenu
import com.client.xvideos.n.ui.screens.gallery.ScreenNGallery
import com.client.xvideos.n.ui.screens.picture.N_FullScreenPicture

fun N_ScreenSavedLikesTab_AddColumn() {
    ColumnSelect_AddColumn(Settings.n_likesTab_column_current_count, Settings.n_likesTab_G_0_4)
}

/**
 * Вкладка сохранённых понравившихся страниц/картинок манги.
 */
object N_ScreenSavedLikesTab : Screen {

    private fun readResolve(): Any = N_ScreenSavedLikesTab

    override val key: ScreenKey = "N_ScreenSavedLikesTab"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenSavedNLikesSM>()

        val columns by vm.columns.collectAsStateWithLifecycle()
        val likes = vm.likes

        val onPictureClick: (NPicture) -> Unit = remember(navigator, likes) {
            { picture ->
                val index = likes.indexOfFirst { it.id == picture.id }.coerceAtLeast(0)
                navigator.push(N_FullScreenPicture(pictures = likes.toList(), initialIndex = index))
            }
        }

        val onOpenGallery: (NPicture) -> Unit = remember(navigator) {
            { picture -> navigator.push(ScreenNGallery(picture.galleryId)) }
        }

        val onAddToCollection: (NPicture) -> Unit = remember(vm) {
            { picture -> vm.openAddToCollection(picture) }
        }

        val onPictureDelete: (NPicture) -> Unit = remember(vm) {
            { picture -> vm.removeLike(picture) }
        }

        val pictureMenuContent: @Composable (NPicture) -> Unit = { picture ->
            NPictureItemExpandMenu(
                picture = picture,
                isLiked = true,
                onOpenGallery = { onOpenGallery(picture) },
                onAddToCollection = { onAddToCollection(picture) },
                onDelete = { onPictureDelete(picture) }
            )
        }

        N_ScreenSavedLikesTabContent(
            likes = likes,
            columns = columns,
            onPictureClick = onPictureClick,
            pictureMenuContent = pictureMenuContent
        )

        NCollectionDialogs(
            isSelectOpen = vm.isSelectCollectionOpen,
            isCreateOpen = vm.isCreateCollectionOpen,
            collections = vm.collections,
            onDismiss = {
                vm.isSelectCollectionOpen = false
                vm.isCreateCollectionOpen = false
            },
            onOpenCreate = {
                vm.isSelectCollectionOpen = false
                vm.isCreateCollectionOpen = true
            },
            onCreateCollection = { name -> vm.createCollectionAndAdd(name) },
            onSelectCollection = { name -> vm.addToCollection(name) }
        )
    }
}

@Composable
fun N_ScreenSavedLikesTabContent(
    likes: List<NPicture>,
    columns: Int,
    onPictureClick: (NPicture) -> Unit,
    pictureMenuContent: @Composable (NPicture) -> Unit,
    modifier: Modifier = Modifier
) {
    val topInset = getTopInsetDp()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.background)
    ) {
        if (topInset > 0.dp) {
            Spacer(Modifier.height(topInset))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            if (likes.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                ) {
                    Text(
                        text = "В лайках пока пусто",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Нажмите на значок сердечка при чтении манги, чтобы сохранить страницу сюда.",
                        color = Color(0xFFAAAAAA),
                        fontSize = 14.sp
                    )
                }
            } else {
                NPictureGrid(
                    items = likes,
                    columns = columns,
                    onItemClick = onPictureClick,
                    menuContent = pictureMenuContent,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Preview
@Composable
private fun N_ScreenSavedLikesTabContentPreview() {
    N_ScreenSavedLikesTabContent(
        likes = emptyList(),
        columns = 2,
        onPictureClick = {},
        pictureMenuContent = {}
    )
}
