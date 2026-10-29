package com.client.xvideos.screenSettings.molecule
import com.client.xvideos.screenSettings.model.SettingsDetailParams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.client.xvideos.screenSettings.SettingsDataHolders
import com.client.xvideos.screenSettings.SettingsPage
import com.client.xvideos.screenSettings.atom.SettingsNavigationItem
import com.client.xvideos.screenSettings.atom.SettingsTitleBar
import com.client.xvideos.screenSettings.components.EmptyStorageStats
import com.client.xvideos.screenSettings.components.SettingsDivider2
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsScreenBackground
import com.client.xvideos.screenSettings.components.SettingsSectionTitle

private val BODY_COLUMN_BASE_MODIFIER = Modifier
    .background(SettingsScreenBackground)
    .fillMaxWidth()
    .padding(bottom = 24.dp)

/**
 * Тело экрана настроек. [onBack] — стрелка в заголовке: со страницы раздела
 * ведёт на главную настроек, с главной — назад, в меню.
 */
@Composable
internal fun AppSettingsScreenBody(
    params: SettingsDetailParams,
    modifier: Modifier = Modifier,
    topCutout: Dp = 0.dp,
    onOpenPage: (SettingsPage) -> Unit = {},
    onBack: () -> Unit = {}
) {
    // Строка заголовка 56dp, как прежний текст с отступами 12dp: стрелка не
    // сдвигает страницу вниз.
    val titleModifier = remember(topCutout) {
        Modifier
            .fillMaxWidth()
            .padding(
                top = topCutout + 4.dp,
                bottom = 4.dp,
                start = 4.dp,
                end = 16.dp
            )
    }

    val bodyModifier = modifier.then(BODY_COLUMN_BASE_MODIFIER)
    Column(
        modifier = bodyModifier
    ) {
        SettingsTitleBar(
            title = params.currentPage.title,
            onBack = onBack,
            modifier = titleModifier
        )

        if (params.currentPage == SettingsPage.Main) {
            SettingsSectionTitle("Основное")

            SettingsGroup {
                SettingsPage.primaryPages.forEachIndexed { index, page ->
                    key(page) {
                        if (index > 0) { SettingsDivider2() }
                        SettingsNavigationItem(
                            page = page,
                            onOpenPage = onOpenPage
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SettingsSectionTitle("Разделы")
            SettingsGroup {
                SettingsPage.contentPages.forEachIndexed { index, page ->
                    key(page) {
                        if (index > 0) { SettingsDivider2() }
                        SettingsNavigationItem(
                            page = page,
                            onOpenPage = onOpenPage
                        )
                    }
                }
            }
        } else {
            SettingsDetailPage(
                params = params
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun AppSettingsScreenBodyPreview() {
    val context = LocalContext.current
    AppSettingsScreenBody(
        params = SettingsDetailParams(
            currentPage = SettingsPage.Main,
            imageCacheSizeBytes = 128_000_000L,
            storageStats = EmptyStorageStats,
            sizeRedTotal = 512_000_000L,
            sizeRedDownload = 64_000_000L,
            onClearImageCache = {},
            onClearDownload = {},
            data = SettingsDataHolders(),
            context = context,
            onBackupDataChanged = {},
            onOpenLLogin = {},
            onOpenXLogin = {}
        )
    )
}
