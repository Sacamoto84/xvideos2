package com.client.xvideos.x.screens.channel.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.client.xvideos.x.model.ChannelCollaborator
import com.client.xvideos.x.model.ChannelHeaderModel
import com.client.xvideos.x.model.ProfileType

/**
 * Шапка профиля канала или актрисы/модели X с баннером, аватаром, статистикой и информацией.
 *
 * @param header Метаданные канала [ChannelHeaderModel].
 * @param isSubscribed Флаг, подписан ли пользователь на этот профиль.
 * @param onToggleSubscription Колбэк переключения подписки (подписаться / отписаться).
 * @param onCollaboratorClick Колбэк нажатия на автора/студию/модель из списка «Сотрудничество».
 * @param onRankingClick Колбэк нажатия на элемент рейтинга актрисы/канала.
 * @param modifier Модификатор макета.
 * @param topInset Вырез камеры: баннер заходит под него.
 */
@Composable
fun ChannelHeader(
    header: ChannelHeaderModel,
    isSubscribed: Boolean = false,
    onToggleSubscription: () -> Unit = {},
    onCollaboratorClick: (ChannelCollaborator) -> Unit = {},
    onRankingClick: (targetUrl: String, title: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
) {
    var isAboutExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF040404))
    ) {
        ChannelHeaderBanner(header = header, topInset = topInset)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            ChannelHeaderProfileRow(
                header = header,
                isSubscribed = isSubscribed,
                onToggleSubscription = onToggleSubscription
            )

            ChannelHeaderStatsRow(header = header)

            val hasInfo = header.hasAboutMe || header.hasCollaborators || header.hasRankings ||
                (header.isModel && (header.hasAge || header.hasCountry || header.hasGender || header.hasWorkedWith))
            if (hasInfo) {
                Spacer(modifier = Modifier.height(4.dp))
                ChannelHeaderInfoCard(
                    header = header,
                    isAboutExpanded = isAboutExpanded,
                    onToggleAboutExpanded = { isAboutExpanded = !isAboutExpanded },
                    onCollaboratorClick = onCollaboratorClick,
                    onRankingClick = onRankingClick,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview
@Composable
private fun ChannelHeaderPreview() {
    ChannelHeader(
        header = ChannelHeaderModel(
            name = "Jane Doe",
            subscribers = "100K",
            videoCount = 42,
            profileType = ProfileType.MODEL,
        ),
    )
}
