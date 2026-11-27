package com.client.xvideos.calculator.converter.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.calculator.CalculatorColors
import com.client.xvideos.calculator.converter.ConverterCategory
import com.client.xvideos.calculator.converter.ConverterCategoryIcon

@Composable
internal fun ConverterGridView(
    onSelect: (ConverterCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        modifier = modifier
            .fillMaxSize()
            .background(CalculatorColors.XIAOMI_BG)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        items(ConverterCategory.entries) { category ->
            Column(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(category) }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ConverterCategoryIcon(
                    category = category,
                    size = 32.dp,
                    tint = Color(0xFF707070)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = category.title,
                    color = CalculatorColors.XIAOMI_TEXT_PRIMARY,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Preview
@Composable
private fun ConverterGridViewPreview() {
    ConverterGridView(onSelect = {})
}
