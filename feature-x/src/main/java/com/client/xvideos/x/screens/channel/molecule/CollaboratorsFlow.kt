package com.client.xvideos.x.screens.channel.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.model.ChannelCollaborator
import com.client.xvideos.x.screens.channel.atom.CollaboratorChip

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollaboratorsFlow(
    collaborators: List<ChannelCollaborator>,
    onCollaboratorClick: (ChannelCollaborator) -> Unit,
    modifier: Modifier = Modifier,
    collapsedLimit: Int = 6,
) {
    var isExpanded by rememberSaveable(collaborators) { mutableStateOf(false) }
    val canToggle = collaborators.size > collapsedLimit
    val visibleList = if (isExpanded || !canToggle) collaborators else collaborators.take(collapsedLimit)
    val hiddenCount = collaborators.size - collapsedLimit

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        visibleList.forEach { collaborator ->
            CollaboratorChip(
                collaborator = collaborator,
                onClick = { onCollaboratorClick(collaborator) }
            )
        }

        if (canToggle) {
            val toggleText = if (isExpanded) "Свернуть" else "+$hiddenCount"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF222226))
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = toggleText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2196F3)
                )
            }
        }
    }
}

@Preview
@Composable
private fun CollaboratorsFlowPreview() {
    CollaboratorsFlow(
        collaborators = listOf(
            ChannelCollaborator(name = "Studio Alpha", href = "/channels/alpha", isModel = false),
            ChannelCollaborator(name = "Jane Doe", href = "/models/jane", isModel = true),
        ),
        onCollaboratorClick = {}
    )
}
