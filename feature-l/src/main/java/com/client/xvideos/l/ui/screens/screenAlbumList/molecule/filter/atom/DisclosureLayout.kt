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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.composeunstyled.Disclosure
import com.composeunstyled.DisclosureHeading
import com.composeunstyled.DisclosurePanel
import com.composeunstyled.rememberDisclosureState

private val ROW_TITLE_STYLE = Theme.L.Type.rowTitle.copy(fontWeight = FontWeight.Bold)
private val TWEEN_ROTATION = tween<Float>()
private val DISCLOSURE_ENTER = expandVertically(
    spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntSize.VisibilityThreshold)
)
private val DISCLOSURE_EXIT = shrinkVertically()
private val DISCLOSURE_ROW_MODIFIER = Modifier.fillMaxWidth().height(48.dp)
private val DISCLOSURE_ICON_SIZE_MODIFIER = Modifier.size(32.dp)
private val ICON_ARROW_DROP_DOWN = Icons.Default.ArrowDropDown
private val ROW_VERTICAL_ALIGNMENT = Alignment.CenterVertically

@Composable
fun DisclosureLayout(contentDisclosureHeading: String, contentDisclosurePanel: @Composable () -> Unit) {

    val state = rememberDisclosureState()
    val palette = StyleGenresTags.Palette
    val headingStyle = remember(palette.textPrimary) { ROW_TITLE_STYLE.copy(color = palette.textPrimary) }

    Disclosure(state = state) {
        DisclosureHeading(backgroundColor = Color.Transparent) {
            val degrees by animateFloatAsState(if (state.expanded) 0f else -90f, TWEEN_ROTATION)

            Row(
                modifier = DISCLOSURE_ROW_MODIFIER,
                verticalAlignment = ROW_VERTICAL_ALIGNMENT
            ) {
                Icon(
                    imageVector = ICON_ARROW_DROP_DOWN,
                    contentDescription = null,
                    modifier = Modifier
                        .graphicsLayer { rotationZ = degrees }
                        .then(DISCLOSURE_ICON_SIZE_MODIFIER),
                    tint = palette.textSecondary
                )
                Text(contentDisclosureHeading, style = headingStyle)
            }

        }
        DisclosurePanel(
            enter = DISCLOSURE_ENTER,
            exit = DISCLOSURE_EXIT
        ) {
            contentDisclosurePanel.invoke()
        }
    }

}
