package com.client.xvideos.common.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme.L.Type
import com.client.xvideos.common.theme.Theme.L.fontFamilyApp
import com.client.xvideos.R as Res

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


    object ExpandMenu {
        val tintColor = Color(0xFF1F1F1F)  // Почти черный
        val backgroundColor = Color(0xFFFFFAF5)  // Теплый белый с кремовым оттенком
        val style = Type.menuItem.copy(color = tintColor)
    }

    /**
     * Цвета и стили для диалогов.
     */
    object DialogLavande {

        val content = Color(0xFFEBE6EE) //Цвет фона диалогового окна

        val buttonBackground = Color(0xFF6552A5) //Цвет фона кнопки

        val buttonTextColor = Color.White //Цвет текста кнопки


        const val buttonBorderRadius = 50 // Радиус кнопки в процентах (50% = полукруг/pill)

        val button = TextStyle(
            fontSize = 16.sp,
            lineHeight = 20.sp,
            color = buttonTextColor,
            fontFamily = fontFamilyApp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp
        )

        val bodyColor = Color(0xFF474747)                   // Цвет текста тела
        val dismissTextColor = Color(0xFF6552A5)            // Цвет текста кнопки отмены
        val buttonBackgroundDestructive = dismissTextColor //Color(0xFFF44336) // Красный для деструктива
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
