package com.client.xvideos.r.ui.niche.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.r.model.NichesResponse
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.model.TopCreatorsResponse
import com.client.xvideos.r.ui.niche.NicheBottomBar
import com.client.xvideos.r.ui.niche.atom.NicheCreatorItem
import com.client.xvideos.r.ui.niche.atom.NichePreviewItem
import com.client.xvideos.r.ui.niche.atom.NicheProfileContent
import com.client.xvideos.ui.theme.XvideosTheme

/**
 * Секция заголовка экрана ниши: профиль ниши, похожие ниши, топовые креаторы и нижняя панель сортировки.
 */
@Composable
fun NicheHeaderContent(
    niche: NichesInfo,
    relatedNiches: () -> NichesResponse,
    topCreators: () -> TopCreatorsResponse,
    onNicheClick: (String) -> Unit,
    onCreatorClick: (String) -> Unit,
    isFollowed: Boolean,
    onFollowClick: () -> Unit,
    currentSort: Order,
    onSortChange: (Order) -> Unit,
    columns: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
            .fillMaxWidth()
            .background(Theme.background)
    ) {
        NicheProfileContent(
            niche = { niche },
            isFollowed = isFollowed,
            onFollowClick = onFollowClick
        )

        val related = relatedNiches().niches
        if (related.isNotEmpty()) {
            Text(
                "Related Niches",
                color = Color.White,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                fontFamily = Theme.R.fontFamilyDMsanss
            )
            LazyRow(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(
                    items = related,
                    key = { item -> item.id },
                    contentType = { "niche_preview" }
                ) { item ->
                    NichePreviewItem(item = item, onClick = onNicheClick)
                }
            }
        }

        val creators = topCreators().creators
        if (creators.isNotEmpty()) {
            Text(
                "✨ Top Creators in ${niche.name}",
                color = Color.White,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                fontFamily = Theme.R.fontFamilyDMsanss
            )
            LazyRow(
                modifier = Modifier.padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(
                    items = creators,
                    key = { creator -> creator.username },
                    contentType = { "top_creator" }
                ) { creator ->
                    NicheCreatorItem(creator = creator, onClick = onCreatorClick)
                }
            }
        }

        Spacer(Modifier.height(2.dp))

        NicheBottomBar(
            niche = niche,
            currentSort = currentSort,
            onSortChange = onSortChange,
            columns = columns
        )
    }
}

@Preview
@Composable
private fun NicheHeaderContentPreview() {
    XvideosTheme {
        NicheHeaderContent(
            niche = NichesInfo(id = "female-backs", name = "Female Backs"),
            relatedNiches = { NichesResponse() },
            topCreators = { TopCreatorsResponse() },
            onNicheClick = {},
            onCreatorClick = {},
            isFollowed = true,
            onFollowClick = {},
            currentSort = Order.NICHES_NAME_A_Z,
            onSortChange = {},
            columns = 2,
        )
    }
}
