package com.client.xvideos.x.screens.channel.molecule

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.model.ChannelCollaborator
import com.client.xvideos.x.model.ChannelHeaderModel
import com.client.xvideos.x.model.ProfileType
import com.client.xvideos.x.screens.channel.atom.InfoFieldItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChannelHeaderInfoCard(
    header: ChannelHeaderModel,
    isAboutExpanded: Boolean,
    onToggleAboutExpanded: () -> Unit,
    onCollaboratorClick: (ChannelCollaborator) -> Unit,
    onRankingClick: (targetUrl: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF161618))
            .padding(10.dp)
            .animateContentSize()
    ) {
        Text(
            text = if (header.isModel) "О модели" else "О канале",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFB0B0B0)
        )

        if (header.isModel) {
            if (header.hasGender || header.hasAge || header.hasCountry) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (header.hasGender) {
                        InfoFieldItem(label = "Пол", value = header.gender)
                    }
                    if (header.hasAge) {
                        InfoFieldItem(label = "Возраст", value = header.age)
                    }
                    if (header.hasCountry) {
                        InfoFieldItem(label = "Страна", value = header.country)
                    }
                }
            }
        }

        if (header.hasCollaborators) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Сотрудничество:",
                fontSize = 12.sp,
                color = Color(0xFF888888),
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(6.dp))
            CollaboratorsFlow(
                collaborators = header.collaborators,
                onCollaboratorClick = onCollaboratorClick,
            )
        } else if (header.hasWorkedWith) {
            Spacer(modifier = Modifier.height(6.dp))
            InfoFieldItem(label = "Сотрудничество", value = header.workedWith)
        }

        if (header.hasRankings) {
            Spacer(modifier = Modifier.height(8.dp))
            ChannelRankingsSection(
                rankings = header.rankings,
                onRankingClick = onRankingClick,
            )
        }

        if (header.hasAboutMe) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = header.aboutMe,
                fontSize = 13.sp,
                color = Color(0xFFE0E0E0),
                maxLines = if (isAboutExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            if (header.aboutMe.length > 80 || header.aboutMe.contains("\n")) {
                Text(
                    text = if (isAboutExpanded) "Свернуть" else "Показать полностью...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2196F3),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable(onClick = onToggleAboutExpanded)
                )
            }
        }
    }
}

@Preview
@Composable
private fun ChannelHeaderInfoCardPreview() {
    ChannelHeaderInfoCard(
        header = ChannelHeaderModel(
            slug = "sample_model",
            name = "Sample Model",
            subscribers = "150.5K",
            totalViews = "25.4M",
            aboutMe = "Welcome to my official profile! Thank you for all your support.",
            videoCount = 128,
            profileType = ProfileType.MODEL,
            gender = "Женщина",
            age = "24 года",
            country = "Бразилия",
        ),
        isAboutExpanded = false,
        onToggleAboutExpanded = {},
        onCollaboratorClick = {},
        onRankingClick = { _, _ -> }
    )
}
