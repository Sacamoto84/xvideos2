package com.client.xvideos.xlive.ui.room.molecule

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.xlive.model.XLiveBio
import com.client.xvideos.xlive.model.ageLabel
import com.client.xvideos.xlive.model.interestLabel
import com.client.xvideos.xlive.ui.XLiveColors
import com.client.xvideos.xlive.ui.room.atom.XLiveInfoField

/** Строк «о себе» в свёрнутом виде. */
private const val ABOUT_COLLAPSED_LINES = 3

/**
 * Карточка «О модели»: поля анкеты и «о себе» со сворачиванием — как
 * карточка «О модели» у канала X.
 *
 * @param gender пол по-русски; пустой — поля нет.
 * @param aboutExpanded «о себе» развёрнуто целиком.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun XLiveRoomAboutCard(
    bio: XLiveBio,
    gender: String,
    aboutExpanded: Boolean,
    onToggleAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fields = listOfNotNull(
        gender.takeIf { it.isNotBlank() }?.let { "Пол" to it },
        bio.age?.let { "Возраст" to ageLabel(it) },
        bio.location.takeIf { it.isNotBlank() }?.let { "Откуда" to it },
        bio.languages.takeIf { it.isNotBlank() }?.let { "Языки" to it },
        bio.bodyType.takeIf { it.isNotBlank() }?.let { "Телосложение" to it },
        bio.interestedIn.takeIf { it.isNotEmpty() }
            ?.let { list -> "Интересуют" to list.joinToString(", ") { interestLabel(it) } },
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(XLiveColors.Card)
            .padding(10.dp)
            .animateContentSize()
    ) {
        Text(
            text = "О модели",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = XLiveColors.CardTitle
        )
        if (fields.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                fields.forEach { (label, value) -> XLiveInfoField(label = label, value = value) }
            }
        }
        if (bio.aboutMe.isNotBlank()) {
            // Кнопка — только когда текст правда не влезает в свёрнутый вид.
            var aboutOverflows by remember(bio.aboutMe) { mutableStateOf(false) }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = bio.aboutMe,
                fontSize = 13.sp,
                color = XLiveColors.Value,
                maxLines = if (aboutExpanded) Int.MAX_VALUE else ABOUT_COLLAPSED_LINES,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                onTextLayout = { result ->
                    aboutOverflows = if (aboutExpanded) {
                        result.lineCount > ABOUT_COLLAPSED_LINES
                    } else {
                        result.hasVisualOverflow
                    }
                }
            )
            if (aboutOverflows) {
                Text(
                    text = if (aboutExpanded) "Свернуть" else "Показать полностью...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = XLiveColors.Link,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable(onClick = onToggleAbout)
                )
            }
        }
    }
}

@Preview
@Composable
private fun XLiveRoomAboutCardPreview() {
    XLiveRoomAboutCard(
        bio = XLiveBio(
            realName = "Anna",
            age = 24,
            location = "My Bedroom",
            languages = "English",
            bodyType = "Petite",
            interestedIn = listOf("Women", "Men"),
            aboutMe = "Hello! Thank you for watching.\nTip menu is in the chat.",
        ),
        gender = "Женщина",
        aboutExpanded = false,
        onToggleAbout = {},
    )
}
