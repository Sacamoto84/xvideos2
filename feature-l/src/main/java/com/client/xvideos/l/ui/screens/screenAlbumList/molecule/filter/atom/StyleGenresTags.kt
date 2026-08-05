package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object StyleGenresTags {

    object Palette {
        val screen = Color(0xFF171717)
        val surface = Color(0xFF242424)
        val surfaceHigh = Color(0xFF303030)
        val field = Color(0xFF2D2D2D)
        val fieldStrong = Color(0xFF3A3A3A)
        val border = Color(0xFF606060)
        val textPrimary = Color(0xFFF4F4F4)
        val textSecondary = Color(0xFFD0D0D0)
        val accent = Color(0xFFFF7468)
        val accentDark = Color(0xFFC9554C)
        val selected = Color(0xFF2D4D2F)
        val selectedBorder = Color(0xFF85CE6D)
        val selectedText = Color(0xFFF0FFE9)
        val excluded = Color(0xFF63373A)
        val excludedBorder = Color(0xFFFF8277)
        val excludedText = Color(0xFFFFE1DD)
        val panelBlack = Color(0xFF1D1D1D)
    }

    //
    val modifierSelectTextItem = Modifier
        .padding(start = 4.dp, end = 4.dp, top = 4.dp)
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(Palette.selected)
        .border(1.dp, Palette.selectedBorder, RoundedCornerShape(8.dp))
        .padding(horizontal = 10.dp, vertical = 5.dp)
        .fillMaxWidth()

    val modifierExcludedTextItem = Modifier
        .padding(start = 4.dp, end = 4.dp, top = 4.dp)
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(Palette.excluded)
        .border(1.dp, Palette.excludedBorder, RoundedCornerShape(8.dp))
        .padding(horizontal = 10.dp, vertical = 5.dp)
        .fillMaxWidth()

    val colorSelectTextItem = Palette.selectedText
    val colorExcludedTextItem = Palette.excludedText
}
