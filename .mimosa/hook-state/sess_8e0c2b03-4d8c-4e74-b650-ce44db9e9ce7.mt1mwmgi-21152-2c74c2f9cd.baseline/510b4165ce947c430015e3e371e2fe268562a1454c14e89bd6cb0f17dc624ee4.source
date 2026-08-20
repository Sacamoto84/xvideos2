package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.composeunstyled.Disclosure
import com.composeunstyled.DisclosureHeading
import com.composeunstyled.DisclosurePanel
import com.composeunstyled.rememberDisclosureState

private val style = Theme.L.Type.rowTitle.copy(fontWeight = FontWeight.Bold)

@Composable
fun DisclosureLayout(contentDisclosureHeading: String, contentDisclosurePanel: @Composable () -> Unit ) {

    val state = rememberDisclosureState()
    val palette = StyleGenresTags.Palette

    Disclosure(state = state) {
        DisclosureHeading(backgroundColor = Color.Transparent) {
            val degrees by animateFloatAsState(if (state.expanded) -0f else -90f, tween())

            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Icon( imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.rotate(degrees).size(32.dp), tint = palette.textSecondary )
                Text(contentDisclosureHeading, style = style.copy(color = palette.textPrimary))
            }

        }
        DisclosurePanel(
            enter = expandVertically( spring( stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntSize.VisibilityThreshold ) ),
            exit = shrinkVertically()
        ) {
            contentDisclosurePanel.invoke()
        }
    }

}
