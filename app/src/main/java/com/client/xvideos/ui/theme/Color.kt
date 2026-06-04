package com.client.xvideos.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)


val PornHubOrange = Color(0xFFEF9E00)

//val myGray = grayColor(0xC1)
fun grayColor(hex: Int): Color {
    require(hex in 0x00..0xFF) { "hex должен быть в диапазоне от 0x00 до 0xFF" }
    val colorValue = (0xFF shl 24) or (hex shl 16) or (hex shl 8) or hex
    return Color(colorValue)
}

val colorHorisontalSeparator = Color(0xFF9E9E9E)

val PornHubGray14_0x0E = Color(0xFF0E0E0E) //14 14 14
val PornHubGray16_0x10 = Color(0xFF101010)  //16 16 16
val PornHubGray21_0x15 = Color(0xFF151515)  //21 21 21
val PornHubGray31_0x1F = Color(0xFF1F1F1F)  //31 31 31
val PornHubGray33_0x21 = Color(0xFF212121)  //33 33 33
val PornHubGray37_0x25 = Color(0xFF252525)  //37 37 37
val PornHubGray63_0x3F = Color(0xFF3F3F3F)  //63 63 63
val PornHubGray150_0x96 = Color(0xFF969696) //150 150 150
val PornHubGray198_0xC6 = Color(0xFFC6C6C6) //198 198 198


val PornHubRed = Color(0xFFE01E5A)
val PornHubGreen = Color(0xFF1ED760)
val PornHubBlue = Color(0xFF0095F6)
val PornHubPurple = Color(0xFF9C27B0)
val PornHubYellow = Color(0xFFFFD600)
val PornHubPink = Color(0xFFF06292)
val PornHubBrown = Color(0xFFA1887F)
val PornHubGrey = Color(0xFF9E9E9E)
val PornHubBlack = Color(0xFF000000)
val PornHubWhite = Color(0xFFFFFFFF)
