package com.client.xvideos.x.screens.profile

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey

/**
 * Экран профиля X — заготовка: [Content] ничего не рисует.
 *
 * Открывается по кнопке «Профиль» из [com.client.xvideos.x.screens.favorites.ScreenFavorites],
 * то есть пользователь попадает на пустой экран. Здесь были два неиспользуемых
 * значения (navigator и ScreenModel), они удалены — на поведение это не влияет,
 * экран как был пустым, так и остался.
 */
class ScreenProfile : Screen {

    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
    }
}
