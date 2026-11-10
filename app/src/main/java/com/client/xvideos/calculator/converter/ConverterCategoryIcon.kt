package com.client.xvideos.calculator.converter

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Векторные иконки категорий конвертера в минималистичном стиле Xiaomi MIUI / HyperOS.
 * Все иконки рисуются на Canvas без растровых артефактов и зависимостей от версий Android.
 */
@Composable
fun ConverterCategoryIcon(
    category: ConverterCategory,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    tint: Color = Color(0xFF707070)
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 1.8.dp.toPx()
        when (category) {
            ConverterCategory.CURRENCY -> drawCurrencyIcon(tint, strokeWidth)
            ConverterCategory.LENGTH -> drawLengthIcon(tint, strokeWidth)
            ConverterCategory.MASS -> drawMassIcon(tint, strokeWidth)
            ConverterCategory.AREA -> drawAreaIcon(tint, strokeWidth)
            ConverterCategory.TIME -> drawTimeIcon(tint, strokeWidth)
            ConverterCategory.FINANCE -> drawFinanceIcon(tint, strokeWidth)
            ConverterCategory.DATA -> drawDataIcon(tint, strokeWidth)
            ConverterCategory.DATE -> drawDateIcon(tint, strokeWidth)
            ConverterCategory.DISCOUNT -> drawDiscountIcon(tint, strokeWidth)
            ConverterCategory.VOLUME -> drawVolumeIcon(tint, strokeWidth)
            ConverterCategory.BASE -> drawBaseIcon(tint, strokeWidth)
            ConverterCategory.SPEED -> drawSpeedIcon(tint, strokeWidth)
            ConverterCategory.TEMPERATURE -> drawTemperatureIcon(tint, strokeWidth)
            ConverterCategory.BMI -> drawBmiIcon(tint, strokeWidth)
        }
    }
}

private fun DrawScope.drawCurrencyIcon(tint: Color, strokeWidth: Float) {
    val r = size.minDimension * 0.38f
    val center = Offset(size.width / 2f, size.height / 2f)
    drawCircle(color = tint, radius = r, center = center, style = Stroke(width = strokeWidth))
    drawCircle(color = tint, radius = r * 0.45f, center = center, style = Stroke(width = strokeWidth))
    drawLine(tint, Offset(center.x, center.y - r * 0.7f), Offset(center.x, center.y + r * 0.7f), strokeWidth)
    drawLine(tint, Offset(center.x - r * 0.7f, center.y), Offset(center.x + r * 0.7f, center.y), strokeWidth)
}

private fun DrawScope.drawLengthIcon(tint: Color, strokeWidth: Float) {
    val w = size.width * 0.85f
    val h = size.height * 0.45f
    val left = (size.width - w) / 2f
    val top = (size.height - h) / 2f
    drawRoundRect(
        color = tint,
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = Stroke(width = strokeWidth)
    )
    val tickStep = w / 5f
    for (i in 1..4) {
        val x = left + i * tickStep
        val tickH = if (i % 2 == 0) h * 0.5f else h * 0.32f
        drawLine(tint, Offset(x, top), Offset(x, top + tickH), strokeWidth)
    }
}

private fun DrawScope.drawMassIcon(tint: Color, strokeWidth: Float) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    // Ручка гири
    drawCircle(
        color = tint,
        radius = size.width * 0.16f,
        center = Offset(cx, cy - size.height * 0.22f),
        style = Stroke(width = strokeWidth)
    )
    // Корпус гири (трапеция)
    val path = Path().apply {
        moveTo(cx - size.width * 0.2f, cy - size.height * 0.08f)
        lineTo(cx + size.width * 0.2f, cy - size.height * 0.08f)
        lineTo(cx + size.width * 0.35f, cy + size.height * 0.36f)
        lineTo(cx - size.width * 0.35f, cy + size.height * 0.36f)
        close()
    }
    drawPath(path, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
}

private fun DrawScope.drawAreaIcon(tint: Color, strokeWidth: Float) {
    val s = size.minDimension * 0.7f
    val left = (size.width - s) / 2f
    val top = (size.height - s) / 2f
    drawRoundRect(
        color = tint,
        topLeft = Offset(left, top),
        size = Size(s, s),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = Stroke(width = strokeWidth)
    )
    // Штриховка внутри квадрата
    drawLine(tint, Offset(left, top + s * 0.5f), Offset(left + s * 0.5f, top), strokeWidth)
    drawLine(tint, Offset(left, top + s), Offset(left + s, top), strokeWidth)
    drawLine(tint, Offset(left + s * 0.5f, top + s), Offset(left + s, top + s * 0.5f), strokeWidth)
}

private fun DrawScope.drawTimeIcon(tint: Color, strokeWidth: Float) {
    val r = size.minDimension * 0.4f
    val cx = size.width / 2f
    val cy = size.height / 2f
    drawCircle(color = tint, radius = r, center = Offset(cx, cy), style = Stroke(width = strokeWidth))
    drawCircle(color = tint, radius = 2.dp.toPx(), center = Offset(cx, cy), style = Fill)
    // Стрелки (10:10)
    drawLine(tint, Offset(cx, cy), Offset(cx - r * 0.45f, cy - r * 0.45f), strokeWidth, cap = StrokeCap.Round)
    drawLine(tint, Offset(cx, cy), Offset(cx + r * 0.6f, cy - r * 0.3f), strokeWidth, cap = StrokeCap.Round)
}

private fun DrawScope.drawFinanceIcon(tint: Color, strokeWidth: Float) {
    val cx = size.width / 2f
    // Монета над ладонью
    drawCircle(
        color = tint,
        radius = size.width * 0.16f,
        center = Offset(cx, size.height * 0.32f),
        style = Stroke(width = strokeWidth)
    )
    // Ладонь
    val path = Path().apply {
        moveTo(size.width * 0.15f, size.height * 0.68f)
        lineTo(size.width * 0.45f, size.height * 0.68f)
        lineTo(size.width * 0.72f, size.height * 0.55f)
        lineTo(size.width * 0.85f, size.height * 0.65f)
        lineTo(size.width * 0.55f, size.height * 0.82f)
        lineTo(size.width * 0.15f, size.height * 0.82f)
    }
    drawPath(path, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
}

private fun DrawScope.drawDataIcon(tint: Color, strokeWidth: Float) {
    val w = size.width * 0.78f
    val h = size.height * 0.54f
    val left = (size.width - w) / 2f
    val top = (size.height - h) / 2f
    drawRoundRect(
        color = tint,
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = CornerRadius(6.dp.toPx()),
        style = Stroke(width = strokeWidth)
    )
    val midY = top + h * 0.62f
    drawLine(tint, Offset(left, midY), Offset(left + w, midY), strokeWidth)
    drawCircle(color = tint, radius = 2.dp.toPx(), center = Offset(left + w * 0.8f, midY + (top + h - midY) / 2f), style = Fill)
}

private fun DrawScope.drawDateIcon(tint: Color, strokeWidth: Float) {
    val w = size.width * 0.72f
    val h = size.height * 0.7f
    val left = (size.width - w) / 2f
    val top = (size.height - h) / 2f + 2.dp.toPx()
    drawRoundRect(
        color = tint,
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = Stroke(width = strokeWidth)
    )
    val headerY = top + h * 0.3f
    drawLine(tint, Offset(left, headerY), Offset(left + w, headerY), strokeWidth)
    // Петельки календаря
    drawLine(tint, Offset(left + w * 0.28f, top - 3.dp.toPx()), Offset(left + w * 0.28f, top + 3.dp.toPx()), strokeWidth, cap = StrokeCap.Round)
    drawLine(tint, Offset(left + w * 0.72f, top - 3.dp.toPx()), Offset(left + w * 0.72f, top + 3.dp.toPx()), strokeWidth, cap = StrokeCap.Round)
}

private fun DrawScope.drawDiscountIcon(tint: Color, strokeWidth: Float) {
    val path = Path().apply {
        moveTo(size.width * 0.35f, size.height * 0.15f)
        lineTo(size.width * 0.75f, size.height * 0.15f)
        lineTo(size.width * 0.88f, size.height * 0.52f)
        lineTo(size.width * 0.52f, size.height * 0.88f)
        lineTo(size.width * 0.15f, size.height * 0.52f)
        close()
    }
    drawPath(path, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
    drawCircle(color = tint, radius = 2.5.dp.toPx(), center = Offset(size.width * 0.55f, size.height * 0.35f), style = Stroke(width = strokeWidth))
}

private fun DrawScope.drawVolumeIcon(tint: Color, strokeWidth: Float) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val dx = size.width * 0.34f
    val dy = size.height * 0.19f
    val h = size.height * 0.36f

    val topFace = Path().apply {
        moveTo(cx, cy - h / 2f - dy)
        lineTo(cx + dx, cy - h / 2f)
        lineTo(cx, cy - h / 2f + dy)
        lineTo(cx - dx, cy - h / 2f)
        close()
    }
    drawPath(topFace, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

    // Вертикальные грани
    drawLine(tint, Offset(cx - dx, cy - h / 2f), Offset(cx - dx, cy + h / 2f), strokeWidth, cap = StrokeCap.Round)
    drawLine(tint, Offset(cx, cy - h / 2f + dy), Offset(cx, cy + h / 2f + dy), strokeWidth, cap = StrokeCap.Round)
    drawLine(tint, Offset(cx + dx, cy - h / 2f), Offset(cx + dx, cy + h / 2f), strokeWidth, cap = StrokeCap.Round)

    // Нижние ребра
    drawLine(tint, Offset(cx - dx, cy + h / 2f), Offset(cx, cy + h / 2f + dy), strokeWidth, cap = StrokeCap.Round)
    drawLine(tint, Offset(cx, cy + h / 2f + dy), Offset(cx + dx, cy + h / 2f), strokeWidth, cap = StrokeCap.Round)
}

private fun DrawScope.drawBaseIcon(tint: Color, strokeWidth: Float) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    // Скобка левая
    val leftParen = Path().apply {
        moveTo(cx - size.width * 0.32f, cy - size.height * 0.28f)
        quadraticBezierTo(cx - size.width * 0.44f, cy, cx - size.width * 0.32f, cy + size.height * 0.28f)
    }
    drawPath(leftParen, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

    // Скобка правая
    val rightParen = Path().apply {
        moveTo(cx + size.width * 0.32f, cy - size.height * 0.28f)
        quadraticBezierTo(cx + size.width * 0.44f, cy, cx + size.width * 0.32f, cy + size.height * 0.28f)
    }
    drawPath(rightParen, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

    // Цифра 0
    drawRoundRect(
        color = tint,
        topLeft = Offset(cx - size.width * 0.2f, cy - size.height * 0.22f),
        size = Size(size.width * 0.16f, size.height * 0.44f),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = Stroke(width = strokeWidth)
    )
    // Цифра 1
    drawLine(tint, Offset(cx + size.width * 0.12f, cy - size.height * 0.22f), Offset(cx + size.width * 0.12f, cy + size.height * 0.22f), strokeWidth, cap = StrokeCap.Round)
}

private fun DrawScope.drawSpeedIcon(tint: Color, strokeWidth: Float) {
    val cx = size.width / 2f
    val cy = size.height * 0.58f
    val r = size.width * 0.38f
    drawArc(
        color = tint,
        startAngle = 160f,
        sweepAngle = 220f,
        useCenter = false,
        topLeft = Offset(cx - r, cy - r),
        size = Size(r * 2f, r * 2f),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
    // Стрелка спидометра
    drawLine(tint, Offset(cx, cy), Offset(cx + r * 0.6f, cy - r * 0.55f), strokeWidth * 1.2f, cap = StrokeCap.Round)
    drawCircle(color = tint, radius = 3.dp.toPx(), center = Offset(cx, cy), style = Fill)
}

private fun DrawScope.drawTemperatureIcon(tint: Color, strokeWidth: Float) {
    val cx = size.width / 2f
    val cy = size.height * 0.68f
    val bulbR = size.width * 0.16f
    // Колба снизу
    drawCircle(color = tint, radius = bulbR, center = Offset(cx, cy), style = Fill)
    // Трубка
    drawRoundRect(
        color = tint,
        topLeft = Offset(cx - bulbR * 0.5f, size.height * 0.16f),
        size = Size(bulbR, cy - size.height * 0.16f),
        cornerRadius = CornerRadius(bulbR * 0.5f),
        style = Stroke(width = strokeWidth)
    )
    // Столбик ртути
    drawLine(tint, Offset(cx, cy), Offset(cx, size.height * 0.32f), strokeWidth * 1.2f, cap = StrokeCap.Round)
}

private fun DrawScope.drawBmiIcon(tint: Color, strokeWidth: Float) {
    val w = size.width * 0.76f
    val h = size.height * 0.72f
    val left = (size.width - w) / 2f
    val top = (size.height - h) / 2f
    drawRoundRect(
        color = tint,
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = CornerRadius(6.dp.toPx()),
        style = Stroke(width = strokeWidth)
    )
    // Окошко шкалы
    val windowW = w * 0.44f
    val windowH = h * 0.28f
    drawRoundRect(
        color = tint,
        topLeft = Offset(left + (w - windowW) / 2f, top + h * 0.14f),
        size = Size(windowW, windowH),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = Stroke(width = strokeWidth)
    )
}
