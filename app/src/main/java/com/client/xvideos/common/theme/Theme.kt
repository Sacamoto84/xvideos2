package com.client.xvideos.common.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.client.xvideos.R as Res
import com.client.xvideos.l.theme.ThemeBackgroundLevel

/**
 * Зонтичный объект темы.
 *
 * Общее (цвета фона) — прямо в [Theme].
 * Уникальное (выделения, шрифты, размеры шрифтов) — во вложенных [R] / [L] / [X].
 */
object Theme {

    // --- общее: только фоны ---

    /** Основной фон экранов. */
    val background = Color(0xFF212121)

    /** Фон App root (только корневой Scaffold). */
    val backgroundAppRoot = Color(0xFF262626)

    /** Фоны баров/табов (используют X/R/L). */
    val tabLevel0 = Color(0xFF212121)
    val tabLevel1 = Color(0xFF282828)
    val tabLevel2 = Color(0xFF333333)
    val tabLevel3 = Color(0xFF444444)

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

        val fontFamilyPopinsRegular = FontFamily(Font(Res.font.poppins_regular))
        val fontFamilyPopinsMedium = FontFamily(Font(Res.font.poppins_medium))
        val fontFamilyPopinsSemiBold = FontFamily(Font(Res.font.poppins_semibold))
        val fontFamilyPopinsBold = FontFamily(Font(Res.font.poppins_bold))
        val fontFamilyPopinsExtraBold = FontFamily(Font(Res.font.poppins_extrabold))

        val fontFamilyDMsanss = FontFamily(Font(Res.font.dm_sans))
    }

    /** Уникальные токены L (палитра, выделения, текст-стили, шрифты). */
    object L {

        val grayLevel = object : ThemeBackgroundLevel {
            override val gray0 = Color(0xFF141414)
            override val gray1 = Color(0xFF1F1F1F)
            override val gray2 = Color(0xFF282828)
            override val gray3 = Color(0xFF353535)
            override val gray4 = Color(0xFF3B3B3B)
            override val gray5 = Color(0xFF3F3F3F)
            override val gray6 = Color(0xFF414141)
            override val gray7 = Color(0xFF474747)
            override val textColor = Color(0xFFC5C8C6)
            override val blue = Color(0xFF45687A)
        }

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

            val sectionTitle = TextStyle(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = grey2,
                fontFamily = fontFamilyApp,
                fontWeight = FontWeight.Medium,
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

        //--- Screen Config ---
        val styleTextConfigL = Type.rowTitle

        //--- Expand Menu ---
        object ExpandMenu {
            val tintColor = Color(0xFF1F1F1F)  // Почти черный
            val backgroundColor = Color(0xFFFFFAF5)  // Теплый белый с кремовым оттенком
            val style = Type.menuItem.copy(color = tintColor)
        }
    }

    /** Уникальные токены X. */
    object X {
        /** Фон контейнера expand-menu на дашборде X. */
        val expandMenuBackground = Color(0xFFF2EDF7)
    }
}
