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

@Composable
internal fun LAlbumNetworkIssuePanel(
    albumPicsDetails: AlbumPicsDetails?,
    onRetryFailedPages: () -> Unit
) {
    if (albumPicsDetails == null) return
    val protectionState by albumPicsDetails.protectionUiState.collectAsStateWithLifecycle()
    LAlbumNetworkIssuePanel(
        failedPages = albumPicsDetails.failedPages.toList(),
        protectionState = protectionState,
        isRetryingFailedPages = albumPicsDetails.isRetryingFailedPages,
        onRetryFailedPages = onRetryFailedPages
    )
}

@Composable
private fun LAlbumNetworkIssuePanel(
    failedPages: List<LAlbumPageLoadIssue>,
    protectionState: LRepositoryProtectionUiState,
    isRetryingFailedPages: Boolean,
    onRetryFailedPages: () -> Unit
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
    val htmlChallenge = protectionState.active || failedPages.any { it.htmlChallenge }
    val failedPagesText = failedPages.joinToString(", ") { it.page.toString() }
        .ifBlank { "нет" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
            .border(
                width = 1.dp,
                color = if (htmlChallenge) Color(0xFFFFC857) else Theme.L.grey2,
                shape = RoundedCornerShape(8.dp)
            )
            .background(Theme.L.grey5, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = if (htmlChallenge) Color(0xFFFFC857) else Theme.L.grey2
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (htmlChallenge) {
                    if (retryAfterSeconds > 0) {
                        "Сервер временно отдаёт защитную страницу, повтор через $retryAfterSeconds сек."
                    } else {
                        "Сервер временно отдаёт защитную страницу."
                    }
                } else {
                    "Часть страниц альбома не загрузилась."
                },
                color = Theme.L.textColor,
                style = Theme.L.Type.rowTitle,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "Если старая страница была в кэше, она уже показана. Недогруженные страницы: $failedPagesText",
            color = Theme.L.grey2,
            style = Theme.L.Type.rowSubtitle,
            modifier = Modifier.padding(top = 6.dp)
        )

        Button(
            onClick = onRetryFailedPages,
            enabled = failedPages.isNotEmpty() && !isRetryingFailedPages,
            colors = ButtonDefaults.buttonColors(containerColor = Theme.L.primaryColor),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
            Spacer(Modifier.width(6.dp))
            Text(
                if (isRetryingFailedPages) "Повторяю..." else "Повторить страницы",
                color = Color.Black,
                style = Theme.L.Type.button
            )
        }
    }
}


// ----------------------------------------------------------------------------
// PREVIEW
//
// ScreenLAlbum.Content() завязан на ScreenModel (getScreenModel) и stateful
// L_LazyRowPictureDetails(host=...), поэтому реальный экран в @Preview не
// построить. Ниже — stateless-копия раскладки (Scaffold + нижний прогресс-бар
// + плашка-шапка альбома) с фейковыми данными. Только для визуальной проверки.
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


