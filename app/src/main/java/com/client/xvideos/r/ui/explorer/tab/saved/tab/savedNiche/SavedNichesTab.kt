package com.client.xvideos.r.ui.explorer.tab.saved.tab.savedNiche

import com.client.xvideos.common.theme.Theme

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.r.ui.niche.R_ScreenNiche
import com.client.xvideos.r.ui.profile.atom.VerticalScrollbar
import com.client.xvideos.r.ui.profile.rememberVisibleRangePercentIgnoringFirstNForLazyColumn
import com.composeunstyled.Text
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

object SavedNichesTab : Screen {

    private fun readResolve(): Any = SavedNichesTab

    override val key: ScreenKey = uniqueScreenKey

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenSavedNichesSM = getScreenModel()
        val state = rememberLazyListState()

        val scrollPercent by rememberVisibleRangePercentIgnoringFirstNForLazyColumn(
            gridState = state, itemsToIgnore = 0
        )

        var itemPendingDelete by remember { mutableStateOf<NichesInfo?>(null) }

        DialogNicheDelete(
            item = itemPendingDelete,
            onDismiss = { itemPendingDelete = null },
            onConfirm = { pending ->
                vm.savedRed.niches.remove(pending)
                itemPendingDelete = null
            }
        )

        Scaffold(topBar = {
            Text(
                ">Группы",
                modifier = Modifier.padding(start = 8.dp),
                color = Theme.R.colorYellow,
                fontSize = 18.sp,
                fontFamily = Theme.R.fontFamilyPopinsRegular
            )
        },
            containerColor = Theme.background
        ) { padding ->

            Box(
                modifier = Modifier
                    .padding(top = padding.calculateTopPadding())
                    .fillMaxSize()
            ) {

                LazyColumn(
                    state = state,
                    modifier = Modifier.fillMaxSize()
                )
                {
                    items(vm.savedRed.niches.list, key = { it.id }) { item ->

                        Row(
                            modifier = Modifier
                                .padding(vertical = 2.dp, horizontal = 6.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Theme.tabLevel3)
                                .clickable(onClick = {
                                    navigator.push( R_ScreenNiche(item.id) )
                                }),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            UrlImage(item.thumbnail, modifier = Modifier.size(96.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                item.name,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontFamily = Theme.R.fontFamilyDMsanss,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Box(
                                modifier = Modifier
                                    .width(96.dp)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.White, RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                                    .clickable { itemPendingDelete = item },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Выйти",
                                    fontFamily = Theme.R.fontFamilyDMsanss,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .width(2.dp)
                ) {
                    VerticalScrollbar(scrollPercent)
                }
            }
        }
    }
}



class ScreenSavedNichesSM @Inject constructor( val savedRed: SavedRed ) : ScreenModel

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedSavedNiches {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenSavedNichesSM::class)
    abstract fun bindScreenRedSavedNichesScreenModel(screenModel: ScreenSavedNichesSM): ScreenModel
}

