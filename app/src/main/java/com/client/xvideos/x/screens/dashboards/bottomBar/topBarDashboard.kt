package com.client.xvideos.x.screens.dashboards.bottomBar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.x.screens.favorites.ScreenFavorites
import com.client.xvideos.ui.theme.grayColor
import com.client.xvideos.x.feature.country.ComposeCountry

@Composable
fun TopBarDashboard(){

    val navigator = LocalNavigator.currentOrThrow

    Row(
        modifier = Modifier.fillMaxWidth().background(grayColor(0x0E)),
        horizontalArrangement = Arrangement.Absolute.SpaceBetween
    ) {
        ComposeCountry(modifier = Modifier)

        Box(modifier = Modifier.fillMaxWidth().weight(1f))

        IconButton(onClick = {
            navigator.push(ScreenFavorites())
        }) {
            Icon(
                imageVector = Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(32.dp)
            )
        }

    }
}

@Preview
@Composable
fun TopBarDashboardPreview() {
    TopBarDashboard()
}