package com.client.xvideos.common.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme.L.Type
import com.client.xvideos.common.theme.Theme.L.fontFamilyApp
import com.client.xvideos.core.R as Res

/**
 * Зонтичный объект темы.
 *
 * Общее (цвета фона) — прямо в [Theme].
 * Уникальное (выделения, шрифты, размеры шрифтов) — во вложенных [R] / [L] / [X].
 */
object Theme {

    /** Основной фон экранов. */
    val background = Color(0xFF262626)

    /** Фон App root (только корневой Scaffold). */
    val backgroundAppRoot = Color(0xFF262626)

    /** Фоны баров/табов (используют X/R/L). */
    val tabLevel0 = Color(0xFF212121)
    val tabLevel1 = Color(0xFF282828)
    val tabLevel2 = Color(0xFF333333)
    val tabLevel3 = Color(0xFF444444)
    val tabLevel4 = Color(0xFF555555)
    val tabLevel5 = Color(0xFF666666)
    val tabLevel6 = Color(0xFF777777)

    /** Стандартизированная иерархия поверхностей (Elevation) */
    object Surface {
        val background = Theme.background
        val level0 = tabLevel0
        val level1 = tabLevel1
        val level2 = tabLevel2
        val level3 = tabLevel3
        val level4 = tabLevel4
        val border = Color(0xFF3F3F3F)
    }

    /** Семантические токены текста с повышенным контрастом (WCAG AA >= 4.5:1) */
    object Text {
        val primary = Color(0xFFEDEDED)
        val secondary = Color(0xFFA8A8A8)
        val muted = Color(0xFF757575)
    }

    /** Семантические токены системных уведомлений и статусов */
    object Feedback {
        val success = Color(0xFF43C558)
        val successContainer = Color(0xFF1E3A24)

        val error = Color(0xFFEA616F)
        val errorContainer = Color(0xFF3D1F23)

        val warning = Color(0xFFFFB74D)
        val warningContainer = Color(0xFF3D2C15)

        val info = Color(0xFF61B2EB)
        val infoContainer = Color(0xFF1B2F3D)
    }

    object ExpandMenu {
        val tintColor = Color(0xFF1F1F1F)  // Почти черный
        val backgroundColor = Color(0xFFFFFAF5)  // Теплый белый с кремовым оттенком
        val style = Type.menuItem.copy(color = tintColor)
    }

    /**
     * Стили для плавающих кнопок скролла (FAB в списках L и R).
     * Вариант: Настоящее матовое стекло (Haze Blur + Glass Border).
     */
    object ScrollFab {
        val size = 56.dp
        val spacing = 12.dp
        val shape = RoundedCornerShape(16.dp)

        // Глубокое темное стекло (Dark Smoked Glass)
        val backgroundColor = Color(0xD9181818) // ~85% глубокий темный тон
        val tintColor = Color(0x33000000)      // 20% темный оттенок (smoked glass)
        const val whitePoint = 0.02f            // Минимальная точка белого (убирает белесый налет)
        const val specularIntensity = 0.30f     // Тонкие деликатные блики без засветов
        const val ambientResponse = 0.15f       // Приглушенный рассеянный свет

        // Стеклянная фаска (тонкий блик на верхней грани)
        val glassBorder = Brush.verticalGradient(
            colors = listOf(
                Color(0x40FFFFFF), // 25% мягкий блик сверху
                Color(0x0DFFFFFF)  // 5% снизу
            )
        )
        val containerColor = Color.Transparent
        val contentColorR = Color.White
        val contentColorL = Color.White
        val contentColor = Color.White
        val borderWidth = 1.dp
    }

    /**
     * Цвета и стили для диалогов (Dark Lavender Glass style).
     */
    object DialogLavande {

        val content = Color(0xFF2B2833) // Глубокий темно-лавандовый фон окна

        val buttonBackground = Color(0xFF7E6BB8) // Фиолетово-лавандовый фон кнопки действия

        val buttonTextColor = Color.White // Цвет текста кнопки действия

        val titleColor = Color.White // Контрастный белый заголовок

        const val buttonBorderRadius = 50 // Радиус кнопки в процентах (50% = полукруг/pill)

        val button = TextStyle(
            fontSize = 16.sp,
            lineHeight = 20.sp,
            color = buttonTextColor,
            fontFamily = fontFamilyApp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp
        )

        val bodyColor = Color(0xFFC7C3CE)                   // Мягкий лавандово-серый текст тела
        val dismissTextColor = Color(0xFFA3AFF5)            // Светло-лавандовый текст кнопки отмены
        val buttonBackgroundDestructive = Color(0xFFEA616F) // Красный для деструктива
        val cornerRadius = 28.dp                            // Скругление диалога

        val iconSize = 96.dp                                // Размер иконки
    }

    /** Уникальные токены R (выделения, бордеры, шрифты). */
    object R {
        /** Legacy фон компонентов R (кнопки/боксы/дивайдеры). */
        val colorCommonBackground = Color(0xFF303030)

        val colorBottomBarDivider = Color(0xFF323153)

        val colorYellow = Color(0xFFEBFA63)

        val colorBlue = Color(0xFF61B2EB)

        val colorRed = Color(0xFFEA616F)
        val colorTextGray = Color(0xFF8B8B8B)

        val colorBorderSelect = Color(0xFF444444) // = Theme.tabLevel3
        val colorBorderGray = Color(0xFF3F3F3F)   // Окантовка

        // Стандартизированный акцентный канал R
        val accent = colorYellow
        val accentMuted = Color(0x26EBFA63) // ~15% alpha
        val accentBorder = colorYellow

        val fontFamilyPopinsRegular = FontFamily(Font(Res.font.poppins_regular))
        val fontFamilyPopinsMedium = FontFamily(Font(Res.font.poppins_medium))
        val fontFamilyPopinsSemiBold = FontFamily(Font(Res.font.poppins_semibold))
        val fontFamilyPopinsBold = FontFamily(Font(Res.font.poppins_bold))
        val fontFamilyPopinsExtraBold = FontFamily(Font(Res.font.poppins_extrabold))

        val fontFamilyDMsanss = FontFamily(Font(Res.font.dm_sans))
    }

    /** Уникальные токены L (палитра, выделения, текст-стили, шрифты). */
    object L {

        val g0 = Color(0xFF4CAF50)
        val r0 = Color(0xFFF44336)
        val b0 = Color(0xFF2196F3)

        val grey0 = Color(0xFFdedede)
        val grey1 = Color(0xFFbababa)
        val grey2 = Color(0xFF9c9c9c)
        val grey3 = Color(0xFF3b3b3b)
        val grey4 = Color(0xFF333333)
        val grey5 = Color(0xFF292929)
        val grey6 = Color(0xFF262626)
        val grey7 = Color(0xFF1c1c1c)

        val lavender = Color(0xFFa3aff5)

        val primaryColor = Color(0xFFff96a3)

        val red = Color(0xFFC9554C)

        val secondaryColor = Color(0xFF3b3b3b)
        val textColor = grey1

        // Стандартизированный акцентный канал L
        val accent = primaryColor
        val accentMuted = Color(0x26FF96A3) // ~15% alpha
        val accentBorder = primaryColor

        val fontFamilyPopinsRegular = FontFamily(Font(Res.font.poppins_regular))
        val fontFamilyPopinsMedium = FontFamily(Font(Res.font.poppins_medium))
        val fontFamilyPopinsSemiBold = FontFamily(Font(Res.font.poppins_semibold))
        val fontFamilyPopinsBold = FontFamily(Font(Res.font.poppins_bold))
        val fontFamilyPopinsExtraBold = FontFamily(Font(Res.font.poppins_extrabold))

        val fontFamilyApp = FontFamily(Font(Res.font.dm_sans))

        val fontFamilyDMsanss = fontFamilyApp

        val fontFamilyKarla = fontFamilyApp

        object Type {
            val screenTitle = TextStyle(
                fontSize = 24.sp,
                lineHeight = 30.sp,
                color = textColor,
                fontFamily = fontFamilyApp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp
            )

            val heroTitle = TextStyle(
                fontSize = 28.sp,
                lineHeight = 34.sp,
                color = textColor,
                fontFamily = fontFamilyApp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )

            val rowTitle = TextStyle(
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = textColor,
                fontFamily = fontFamilyApp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp
            )

            val rowValue = TextStyle(
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = textColor,
                fontFamily = fontFamilyApp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp
            )

            val rowSubtitle = TextStyle(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = grey2,
                fontFamily = fontFamilyApp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp
            )

            val body = TextStyle(
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = textColor,
                fontFamily = fontFamilyApp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp
            )

            val bodyLarge = TextStyle(
                fontSize = 18.sp,
                lineHeight = 24.sp,
                color = textColor,
                fontFamily = fontFamilyApp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp
            )

            val button = TextStyle(
                fontSize = 16.sp,
                lineHeight = 20.sp,
                color = textColor,
                fontFamily = fontFamilyApp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp
            )

            val caption = TextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = grey2,
                fontFamily = fontFamilyApp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp
            )

            val mediaIndex = TextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = textColor,
                fontFamily = fontFamilyApp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp
            )

            val menuItem = TextStyle(
                fontSize = 18.sp,
                lineHeight = 24.sp,
                color = textColor,
                fontFamily = fontFamilyApp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp
            )

            val dialogTitle = TextStyle(
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontFamily = fontFamilyApp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp
            )

            val dialogBody = TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontFamily = fontFamilyApp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp
            )
        }

    }

}
