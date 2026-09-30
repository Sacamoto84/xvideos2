package com.client.xvideos.x.screens.channel.atom

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.core.R
import com.client.xvideos.ui.theme.XvideosTheme
import com.client.xvideos.x.model.ChannelCollaborator
import com.client.xvideos.x.model.ChannelHeaderModel
import com.client.xvideos.x.model.ChannelRankGroup
import com.client.xvideos.x.model.ChannelRankItem
import com.client.xvideos.x.model.ChannelRankingCategory
import com.client.xvideos.x.model.ProfileType

private val FLAG_FONT = FontFamily(Font(com.client.xvideos.feature.x.R.font.flag))

/**
 * Шапка профиля канала или актрисы/модели X с баннером, аватаром, статистикой и информацией.
 *
 * @param header Метаданные канала [ChannelHeaderModel].
 * @param onBack Колбэк нажатия кнопки «Назад».
 * @param isSubscribed Флаг, подписан ли пользователь на этот профиль.
 * @param onToggleSubscription Колбэк переключения подписки (подписаться / отписаться).
 * @param onCollaboratorClick Колбэк нажатия на автора/студию/модель из списка «Сотрудничество».
 * @param onRankingClick Колбэк нажатия на элемент рейтинга актрисы/канала.
 * @param modifier Модификатор макета.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChannelHeader(
    header: ChannelHeaderModel,
    onBack: () -> Unit,
    isSubscribed: Boolean = false,
    onToggleSubscription: () -> Unit = {},
    onCollaboratorClick: (ChannelCollaborator) -> Unit = {},
    onRankingClick: (targetUrl: String, title: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var isAboutExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF040404))
    ) {
        ChannelHeaderBanner(header = header, onBack = onBack)

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

@Composable
private fun ChannelHeaderBanner(
    header: ChannelHeaderModel,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        if (header.hasBanner) {
            UrlImage(
                url = header.bannerUrl,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (header.isModel) {
                                listOf(Color(0xFF5A101C), Color(0xFF1F080C))
                            } else {
                                listOf(Color(0xFF1E3C72), Color(0xFF2A5298))
                            }
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF040404))
                    )
                )
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(8.dp)
                .size(36.dp)
                .align(Alignment.TopStart)
                .clip(CircleShape)
                .background(Color(0x80000000))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ChannelHeaderProfileRow(
    header: ChannelHeaderModel,
    isSubscribed: Boolean,
    onToggleSubscription: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .offset(y = (-24).dp)
                .size(68.dp)
                .clip(CircleShape)
                .border(2.dp, Color.White, CircleShape)
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center
        ) {
            if (header.hasAvatar) {
                UrlImage(
                    url = header.avatarUrl,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (header.isModel) {
                Text(
                    text = "\uE9B8",
                    style = TextStyle(
                        color = Color(0xFFDE2600),
                        fontSize = 30.sp,
                        fontFamily = FontFamily(Font(R.font.iconfont))
                    )
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = Color(0xFF1E88E5),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .offset(y = (-12).dp)
                .weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (header.hasFlag) {
                    Text(
                        text = header.flagEmoji,
                        fontFamily = FLAG_FONT,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                Text(
                    text = header.displayName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            if (header.isModel && header.subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = header.subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFFBBBBBB),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (header.isModel) Color(0xFFDE2600) else Color(0xFF1E88E5))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (header.isModel) "\uE9B8" else "\uE956",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily(Font(R.font.iconfont))
                        )
                    )
                    Text(
                        text = if (header.isModel) "Модель" else "Канал",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                SubscribeHeaderButton(
                    isSubscribed = isSubscribed,
                    isModel = header.isModel,
                    onClick = onToggleSubscription
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChannelHeaderStatsRow(header: ChannelHeaderModel) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-10).dp)
    ) {
        if (header.hasSubscribers) {
            StatPill(text = "${header.subscribers} подписчиков")
        }
        if (header.hasTotalViews) {
            StatPill(text = "${header.totalViews} просмотров")
        }
        if (header.videoCount > 0) {
            StatPill(text = "${header.videoCount} видео")
        }
    }
}

@Composable
private fun ChannelHeaderInfoCard(
    header: ChannelHeaderModel,
    isAboutExpanded: Boolean,
    onToggleAboutExpanded: () -> Unit,
    onCollaboratorClick: (ChannelCollaborator) -> Unit,
    onRankingClick: (targetUrl: String, title: String) -> Unit,
) {
    Column(
        modifier = Modifier
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

@Composable
private fun CollaboratorsFlow(
    collaborators: List<ChannelCollaborator>,
    onCollaboratorClick: (ChannelCollaborator) -> Unit,
    modifier: Modifier = Modifier,
    collapsedLimit: Int = 6,
) {
    var isExpanded by rememberSaveable(collaborators) { mutableStateOf(false) }
    val canToggle = collaborators.size > collapsedLimit
    val visibleList = if (isExpanded || !canToggle) collaborators else collaborators.take(collapsedLimit)
    val hiddenCount = collaborators.size - collapsedLimit

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        visibleList.forEach { collaborator ->
            CollaboratorChip(
                collaborator = collaborator,
                onClick = { onCollaboratorClick(collaborator) }
            )
        }

        if (canToggle) {
            val toggleText = if (isExpanded) "Свернуть" else "+$hiddenCount"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF222226))
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = toggleText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2196F3)
                )
            }
        }
    }
}

@Composable
private fun CollaboratorChip(
    collaborator: ChannelCollaborator,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isModel = collaborator.isModel
    val accentColor = if (isModel) Color(0xFFDE2600) else Color(0xFF1E88E5)
    val bgColor = if (isModel) Color(0xFF261418) else Color(0xFF141E26)
    val borderColor = if (isModel) Color(0x4DDE2600) else Color(0x4D1E88E5)
    val iconChar = if (isModel) "\uE9B8" else "\uE956"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = iconChar,
                style = TextStyle(
                    color = accentColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily(Font(R.font.iconfont))
                )
            )
            Text(
                text = collaborator.name,
                style = TextStyle(
                    color = Color(0xFFF0F0F0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun InfoFieldItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Text(
            text = "$label:",
            fontSize = 12.sp,
            color = Color(0xFF888888),
            fontWeight = FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = Color(0xFFE0E0E0),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF222226))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFCCCCCC)
        )
    }
}

@Composable
private fun SubscribeHeaderButton(
    isSubscribed: Boolean,
    isModel: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brandColor = if (isModel) Color(0xFFDE2600) else Color(0xFF1E88E5)
    val bgColor = if (isSubscribed) brandColor.copy(alpha = 0.22f) else Color(0xFF222226)
    val borderColor = if (isSubscribed) brandColor else Color(0xFF444448)
    val textColor = if (isSubscribed) brandColor else Color.White

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (isSubscribed) Icons.Default.Check else Icons.Default.Add,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = if (isSubscribed) "В подписках" else "Подписаться",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

/**
 * Блок отображения рейтингов модели или канала («Рейтинги порноактрис», «Глобальные рейтинги»).
 */
@Composable
private fun ChannelRankingsSection(
    rankings: List<ChannelRankingCategory>,
    onRankingClick: (targetUrl: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1719))
            .border(1.dp, Color(0x33DE2600), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rankings.forEachIndexed { index, category ->
            if (index > 0) {
                HorizontalDivider(
                    color = Color(0x22FFFFFF),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${category.label}:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                category.ranks.forEach { group ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${group.label}:",
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = Color(0xFFCCCCCC)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    group.ranks.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = item.geo,
                                fontSize = 12.sp,
                                color = Color(0xFFE0E0E0),
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "# ${item.rank}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDE2600),
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .clickable {
                                        onRankingClick(
                                            item.link,
                                            item.label.ifBlank { "${category.label} - ${item.geo} #${item.rank}" }
                                        )
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF040404)
@Composable
private fun ChannelHeaderInfoCardPreview() {
    XvideosTheme(darkTheme = true) {
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
                countryCode = "br",
                collaborators = listOf(
                    ChannelCollaborator(name = "Studio Alpha", href = "/channels/studio-alpha", isModel = false),
                    ChannelCollaborator(name = "Jane Doe", href = "/models/jane-doe", isModel = true),
                ),
                rankings = listOf(
                    ChannelRankingCategory(
                        label = "Рейтинги моделей",
                        ranks = listOf(
                            ChannelRankGroup(
                                label = "По всему миру",
                                ranks = listOf(
                                    ChannelRankItem(rank = 42, geo = "Мировой", link = "/rankings/world", label = "Мировой рейтинг")
                                )
                            )
                        )
                    )
                )
            ),
            isAboutExpanded = false,
            onToggleAboutExpanded = {},
            onCollaboratorClick = {},
            onRankingClick = { _, _ -> }
        )
    }
}


