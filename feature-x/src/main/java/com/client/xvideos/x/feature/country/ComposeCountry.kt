package com.client.xvideos.x.feature.country

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Deprecated(
    message = "Use molecule.ComposeCountry",
    replaceWith = ReplaceWith("com.client.xvideos.x.feature.country.molecule.ComposeCountry(modifier)")
)
@Composable
fun ComposeCountry(modifier: Modifier = Modifier) {
    com.client.xvideos.x.feature.country.molecule.ComposeCountry(modifier)
}
