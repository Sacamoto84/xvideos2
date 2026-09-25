package com.client.xvideos.l.ui.screens.screenAlbum

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.net.AlbumPicsDetails
import com.client.xvideos.l.net.LAlbumPageLoadIssue
import com.client.xvideos.l.repository.LRepositoryProtectionUiState
import com.client.xvideos.ui.theme.XvideosTheme
import kotlinx.coroutines.delay
import kotlin.math.ceil

private val PANEL_CORNER_SHAPE = RoundedCornerShape(8.dp)
private val CHALLENGE_WARNING_COLOR = Color(0xFFFFC857)
private val PANEL_BORDER_WIDTH = 1.dp
private val REFRESH_ICON_SPACER = 6.dp
private val HEADER_ICON_SPACER = 8.dp
private val PANEL_TOP_PADDING = 8.dp
private val PANEL_BOTTOM_PADDING = 4.dp
private val PANEL_INNER_PADDING = 10.dp
private val SUBTITLE_TOP_PADDING = 6.dp
private val BUTTON_TOP_PADDING = 8.dp
private val WARNING_ICON = Icons.Default.Warning
private val REFRESH_ICON = Icons.Default.Refresh
private val ROW_VERTICAL_ALIGNMENT = Alignment.CenterVertically
private val HEADER_ICON_SPACER_MODIFIER = Modifier.width(HEADER_ICON_SPACER)
private val REFRESH_ICON_SPACER_MODIFIER = Modifier.width(REFRESH_ICON_SPACER)
private val SUBTITLE_PADDING_MODIFIER = Modifier.padding(top = SUBTITLE_TOP_PADDING)
private val BUTTON_PADDING_MODIFIER = Modifier.padding(top = BUTTON_TOP_PADDING)
private val COLOR_BLACK = Color.Black

private const val TEXT_RETRYING = "Повторяю..."
private const val TEXT_RETRY = "Повторить страницы"
private const val TEXT_SERVER_CHALLENGE_RETRY_PREFIX = "Сервер временно отдаёт защитную страницу, повтор через "
private const val TEXT_SERVER_CHALLENGE_RETRY_SUFFIX = " сек."
private const val TEXT_SERVER_CHALLENGE = "Сервер временно отдаёт защитную страницу."
private const val TEXT_SOME_PAGES_FAILED = "Часть страниц альбома не загрузилась."
private const val TEXT_CACHED_PAGES_HINT_PREFIX = "Если старая страница была в кэше, она уже показана. Недогруженные страницы: "
private const val TEXT_FAILED_PAGES_EMPTY = "нет"

@Composable
internal fun LAlbumNetworkIssuePanel(
    albumPicsDetails: AlbumPicsDetails?,
    onRetryFailedPages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (albumPicsDetails == null) return
    val protectionState by albumPicsDetails.protectionUiState.collectAsStateWithLifecycle()
    LAlbumNetworkIssuePanel(
        failedPages = albumPicsDetails.failedPages.toList(),
        protectionState = protectionState,
        isRetryingFailedPages = albumPicsDetails.isRetryingFailedPages,
        onRetryFailedPages = onRetryFailedPages,
        modifier = modifier
    )
}

@Composable
private fun LAlbumNetworkIssuePanel(
    failedPages: List<LAlbumPageLoadIssue>,
    protectionState: LRepositoryProtectionUiState,
    isRetryingFailedPages: Boolean,
    onRetryFailedPages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shouldShow = failedPages.isNotEmpty() || protectionState.active
    if (!shouldShow) return

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(protectionState.active, protectionState.retryAtMs) {
        while (protectionState.active && protectionState.remainingMs(nowMs) > 0L) {
            nowMs = System.currentTimeMillis()
            delay(1_000L)
        }
        nowMs = System.currentTimeMillis()
    }

    val retryAfterSeconds = ceil(protectionState.remainingMs(nowMs) / 1000.0)
        .toInt()
        .coerceAtLeast(0)
    val htmlChallenge = remember(protectionState.active, failedPages) {
        protectionState.active || failedPages.any { it.htmlChallenge }
    }
    val failedPagesText = remember(failedPages) {
        failedPages.joinToString(", ") { it.page.toString() }.ifBlank { TEXT_FAILED_PAGES_EMPTY }
    }
    val warningColor = if (htmlChallenge) CHALLENGE_WARNING_COLOR else Theme.L.grey2

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = PANEL_TOP_PADDING, bottom = PANEL_BOTTOM_PADDING)
            .border(
                width = PANEL_BORDER_WIDTH,
                color = warningColor,
                shape = PANEL_CORNER_SHAPE
            )
            .background(Theme.L.grey5, PANEL_CORNER_SHAPE)
            .padding(PANEL_INNER_PADDING)
    ) {
        Row(verticalAlignment = ROW_VERTICAL_ALIGNMENT) {
            Icon(
                imageVector = WARNING_ICON,
                contentDescription = null,
                tint = warningColor
            )
            Spacer(HEADER_ICON_SPACER_MODIFIER)
            Text(
                text = if (htmlChallenge) {
                    if (retryAfterSeconds > 0) {
                        "$TEXT_SERVER_CHALLENGE_RETRY_PREFIX$retryAfterSeconds$TEXT_SERVER_CHALLENGE_RETRY_SUFFIX"
                    } else {
                        TEXT_SERVER_CHALLENGE
                    }
                } else {
                    TEXT_SOME_PAGES_FAILED
                },
                color = Theme.L.textColor,
                style = Theme.L.Type.rowTitle,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "$TEXT_CACHED_PAGES_HINT_PREFIX$failedPagesText",
            color = Theme.L.grey2,
            style = Theme.L.Type.rowSubtitle,
            modifier = SUBTITLE_PADDING_MODIFIER
        )

        Button(
            onClick = onRetryFailedPages,
            enabled = failedPages.isNotEmpty() && !isRetryingFailedPages,
            colors = ButtonDefaults.buttonColors(containerColor = Theme.L.primaryColor),
            shape = PANEL_CORNER_SHAPE,
            modifier = BUTTON_PADDING_MODIFIER
        ) {
            Icon(REFRESH_ICON, contentDescription = null, tint = COLOR_BLACK)
            Spacer(REFRESH_ICON_SPACER_MODIFIER)
            Text(
                if (isRetryingFailedPages) TEXT_RETRYING else TEXT_RETRY,
                color = COLOR_BLACK,
                style = Theme.L.Type.button
            )
        }
    }
}

// ----------------------------------------------------------------------------
// PREVIEW
// ----------------------------------------------------------------------------

@Preview(showBackground = true, backgroundColor = 0xFF262626, widthDp = 360)
@Composable
private fun LAlbumNetworkIssuePanelPreview() {
    XvideosTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Failed Pages only:", color = Color.White)
            LAlbumNetworkIssuePanel(
                failedPages = listOf(
                    LAlbumPageLoadIssue(1, "Error", false),
                    LAlbumPageLoadIssue(2, "Error", false)
                ),
                protectionState = LRepositoryProtectionUiState(active = false),
                isRetryingFailedPages = false,
                onRetryFailedPages = {}
            )

            Spacer(Modifier.height(16.dp))
            Text("HTML Challenge active:", color = Color.White)
            LAlbumNetworkIssuePanel(
                failedPages = emptyList(),
                protectionState = LRepositoryProtectionUiState(active = true, retryAtMs = System.currentTimeMillis() + 30000),
                isRetryingFailedPages = false,
                onRetryFailedPages = {}
            )

            Spacer(Modifier.height(16.dp))
            Text("Retrying state:", color = Color.White)
            LAlbumNetworkIssuePanel(
                failedPages = listOf(LAlbumPageLoadIssue(1, "Error", false)),
                protectionState = LRepositoryProtectionUiState(active = false),
                isRetryingFailedPages = true,
                onRetryFailedPages = {}
            )
        }
    }
}
