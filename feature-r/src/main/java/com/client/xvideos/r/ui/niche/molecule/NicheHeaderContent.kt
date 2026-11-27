package com.client.xvideos.r.ui.niche.molecule

import com.client.xvideos.r.network.api.AccountMembership
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.molecule.AccountMembershipMenu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.r.model.NichesResponse
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.model.TopCreatorsResponse
import com.client.xvideos.r.ui.niche.atom.NicheCreatorItem
import com.client.xvideos.r.ui.niche.atom.NichePreviewItem
import com.client.xvideos.r.ui.niche.atom.NicheProfileContent

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
    modifier: Modifier = Modifier,
    scrollMode: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
            .fillMaxWidth()
            .background(Theme.background)
            .adaptiveContentWidth()
    ) {
        NicheProfileContent(
            niche = { niche },
            isFollowed = isFollowed,
            onFollowClick = onFollowClick,
            trailing = { AccountMembershipMenu(AccountMembership.NICHE, niche.id) },
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
                horizontalArrangement = Arrangement.spacedBy(Theme.Content.itemSpacing),
                modifier = Modifier
                    .padding(top = 2.dp, start = 2.dp, end = 2.dp)
                    .fillMaxWidth(),
            ) {
                // Индекс в ключе: поле приходит с сервера, и повтор или пустое значение уронили бы список.
                itemsIndexed(
                    items = related,
                    key = { index, item -> "${item.id}#$index" },
                    contentType = { _, _ -> "niche_preview" }
                ) { _, item ->
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
                horizontalArrangement = Arrangement.spacedBy(Theme.Content.itemSpacing),
                modifier = Modifier.padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                itemsIndexed(
                    items = creators,
                    key = { index, creator -> "${creator.username}#$index" },
                    contentType = { _, _ -> "top_creator" }
                ) { _, creator ->
                    NicheCreatorItem(creator = creator, onClick = onCreatorClick)
                }
            }
        }

        NicheBottomBar(
            currentSort = currentSort,
            onSortChange = onSortChange,
            scrollMode = scrollMode,
        )

        Spacer(Modifier.height(2.dp))
    }
}

@Preview
@Composable
private fun NicheHeaderContentPreview() {
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
    )
}
