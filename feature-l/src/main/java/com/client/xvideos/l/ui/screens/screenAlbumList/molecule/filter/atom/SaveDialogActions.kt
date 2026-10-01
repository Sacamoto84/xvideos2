package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.ui.screens.screenAlbumList.filter.style.StyleGenresTags

@Composable
fun SaveDialogActions(
    canSave: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = StyleGenresTags.Palette
    val buttonShape = RoundedCornerShape(8.dp)
    val saveButtonStyle = remember { Theme.L.Type.button.copy(fontWeight = FontWeight.Bold) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(buttonShape)
                .clickable(onClick = onCancel)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Cancel",
                color = palette.textSecondary,
                style = Theme.L.Type.button
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(buttonShape)
                .border(1.dp, if (canSave) palette.accent else palette.border, buttonShape)
                .background(if (canSave) palette.accentDark else palette.field)
                .clickable(enabled = canSave, onClick = onSave)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Save",
                color = if (canSave) Color.White else palette.textSecondary,
                style = saveButtonStyle
            )
        }
    }
}

@Preview
@Composable
private fun SaveDialogActionsPreview() {
    SaveDialogActions(
        canSave = true,
        onCancel = {},
        onSave = {}
    )
}
