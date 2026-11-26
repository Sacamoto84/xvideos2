package com.client.xvideos.r.ui.explorer.tab.saved.tab.savedNiche.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.r.model.Niche
import com.client.xvideos.r.network.api.AccountMembership
import com.client.xvideos.r.network.api.AccountNicheList
import com.client.xvideos.r.ui.explorer.tab.niches.molecule.NichePreview2Content
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.atom.AccountSavedMessage
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.molecule.AccountMembershipMenu

@Composable
internal fun AccountNichesList(
    niches: List<Niche>,
    list: AccountNicheList,
    onOpenNiche: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (niches.isEmpty()) {
        AccountSavedMessage(
            if (list == AccountNicheList.FOLLOWING) "Вы пока не вступили ни в одну группу R"
            else "У вас пока нет созданных групп R"
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(Theme.Content.itemSpacing),
    ) {
        items(niches, key = { it.id }) { niche ->
            NichePreview2Content(
                niche = niche,
                isFollowed = list == AccountNicheList.FOLLOWING,
                onClick = { onOpenNiche(niche.id) },
                modifier = Modifier.adaptiveContentWidth(),
                trailing = {
                    if (list == AccountNicheList.FOLLOWING) {
                        AccountMembershipMenu(AccountMembership.NICHE, niche.id, knownMember = true)
                    }
                },
            )
        }
    }
}

@Preview
@Composable
private fun AccountNichesListPreview() {
    AccountNichesList(
        niches = listOf(Niche("sample", "Sample Niche", 10, 100, "", null)),
        list = AccountNicheList.FOLLOWING,
        onOpenNiche = {},
    )
}
