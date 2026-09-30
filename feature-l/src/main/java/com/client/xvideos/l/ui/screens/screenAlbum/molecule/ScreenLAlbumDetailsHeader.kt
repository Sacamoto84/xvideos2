package com.client.xvideos.l.ui.screens.screenAlbum.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.Audience
import com.client.xvideos.l.model.Genre
import com.client.xvideos.l.net.AlbumPicsDetails
import com.client.xvideos.l.ui.screens.screenAlbum.LAlbumNetworkIssuePanel
import com.client.xvideos.l.ui.screens.screenAlbum.ScreenLAlbumSM
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoAudiences
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoButtonSaveAlbum
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoButtonServerFavorite
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoButtonShareAlbum
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoFilterButton
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoGreeting
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoTags
import com.client.xvideos.l.ui.screens.screenAlbum.formatEpochSeconds

@Composable
fun ScreenLAlbumDetailsHeader(
    parsed: AlbumDetails,
    idAlbum: Long,
    saved: Boolean,
    vm: ScreenLAlbumSM?,
    hasAnimatedItems: Boolean,
    albumPicsDetails: AlbumPicsDetails?,
    onGenreClick: (Genre) -> Unit,
    onAudienceClick: (Audience) -> Unit,
    onTagClick: (String) -> Unit,
    onRequestDelete: (AlbumDetails) -> Unit,
    onRetryFailedPages: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UrlImage(
                parsed.cover?.url.orEmpty(),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .size(72.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(parsed.title, color = Theme.L.textColor, style = Theme.L.Type.rowTitle)
                Text("${parsed.number_of_animated_pictures} gifs / ${parsed.number_of_pictures} pictures", color = Theme.L.textColor)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        val strId = remember(idAlbum) {
            buildAnnotatedString {
                withStyle(style = Theme.L.Type.rowTitle.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp).toSpanStyle()) { append("Id: ") }
                withStyle(style = Theme.L.Type.rowTitle.copy(fontSize = 14.sp).toSpanStyle()) { append(idAlbum.toString()) }
            }
        }
        Text(strId, color = Theme.L.textColor)

        Spacer(modifier = Modifier.height(4.dp))

        val textCreated = remember(parsed.created) { formatEpochSeconds(parsed.created) }
        val textModified = remember(parsed.modified) { formatEpochSeconds(parsed.modified) }

        val strCreated = remember(textCreated) {
            textCreated?.let { created ->
                buildAnnotatedString {
                    withStyle(style = Theme.L.Type.rowTitle.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp).toSpanStyle()) { append("Created: ") }
                    withStyle(style = Theme.L.Type.rowTitle.copy(fontSize = 14.sp).toSpanStyle()) { append(created) }
                }
            }
        }
        if (strCreated != null) {
            Text(strCreated, color = Theme.L.textColor)
        }

        val strModified = remember(textModified) {
            textModified?.let { modified ->
                buildAnnotatedString {
                    withStyle(style = Theme.L.Type.rowTitle.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp).toSpanStyle()) { append("Modified: ") }
                    withStyle(style = Theme.L.Type.rowTitle.copy(fontSize = 14.sp).toSpanStyle()) { append(modified) }
                }
            }
        }
        if (strModified != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(strModified, color = Theme.L.textColor)
        }

        Spacer(modifier = Modifier.height(4.dp))
        AlbumInfoGreeting(parsed, onGenreClick)
        AlbumInfoAudiences(parsed, onAudienceClick)
        val activeTags = remember(parsed.tags) {
            parsed.tags.reversed().filter { it.count > 0 }
        }
        AlbumInfoTags({ activeTags }, onTagClick)
        AlbumInfoButtonSaveAlbum(saved, onClick = {
            if (!saved) {
                vm?.saveAlbum()
            } else {
                onRequestDelete(parsed)
            }
        })
        val isFavorite = vm?.isServerFavorite
            ?: (parsed.likeStatus.orEmpty().isNotBlank() && parsed.likeStatus != "none" && parsed.likeStatus != "dislike")
        AlbumInfoButtonServerFavorite(
            isFavorite = isFavorite,
            isLoading = vm?.isServerFavoriteLoading == true,
            onClick = { vm?.toggleServerFavorite(parsed) }
        )
        AlbumInfoButtonShareAlbum(onClick = { vm?.shareAlbumP2p(parsed) })
        AlbumInfoFilterButton(
            parsed = parsed,
            checked = vm?.showOnlyAnimated == true,
            hasAnimatedItems = hasAnimatedItems,
            onCheckedChange = { vm?.showOnlyAnimated = it }
        )
        LAlbumNetworkIssuePanel(
            albumPicsDetails = albumPicsDetails,
            onRetryFailedPages = onRetryFailedPages
        )
    }
}

@Preview
@Composable
private fun ScreenLAlbumDetailsHeaderPreview() {
    ScreenLAlbumDetailsHeader(
        parsed = AlbumDetails(
            id = "123",
            title = "Test Album",
            number_of_pictures = 10,
            number_of_animated_pictures = 2
        ),
        idAlbum = 123L,
        saved = false,
        vm = null,
        hasAnimatedItems = true,
        albumPicsDetails = null,
        onGenreClick = {},
        onAudienceClick = {},
        onTagClick = {},
        onRequestDelete = {},
        onRetryFailedPages = {}
    )
}
