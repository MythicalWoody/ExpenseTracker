package com.example.expencetrackerapp.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws a glassmorphic arc with color-tinted glass effect
 * Optimized for circular/pie chart views without rectangular border artifacts
 */
fun DrawScope.drawGlassmorphicArc(
    color: Color,
    startAngle: Float,
    sweepAngle: Float,
    topLeft: Offset,
    size: Size,
    strokeWidth: Float,
    isSelected: Boolean = false,
    glassIntensity: Float = 0.7f // Controls glass transparency (0.0 - 1.0)
) {
    val currentStrokeWidth = if (isSelected) strokeWidth + 8.dp.toPx() else strokeWidth
    val rimWidth = 2.dp.toPx()
    
    // 1. BASE COLORED GLASS LAYER
    // Semi-transparent base with the category color
    drawArc(
        color = color.copy(alpha = glassIntensity * if (isSelected) 0.9f else 0.75f),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(
            width = currentStrokeWidth,
            cap = StrokeCap.Butt
        )
    )
    
    // Calculate parameters for edges
    // The main arc is drawn centered on the 'size' rect.
    // The path radius is size.width / 2
    val mainRadius = size.width / 2
    val centerX = topLeft.x + mainRadius
    val centerY = topLeft.y + mainRadius
    
    // Outer edge calculation
    // Outer edge radius is mainRadius + currentStrokeWidth / 2
    // We want to draw a rim *inside* this outer edge.
    // Rim path radius = (mainRadius + currentStrokeWidth / 2) - rimWidth / 2
    val outerRimRadius = mainRadius + (currentStrokeWidth / 2) - (rimWidth / 2)
    val outerRimSize = Size(outerRimRadius * 2, outerRimRadius * 2)
    val outerRimTopLeft = Offset(centerX - outerRimRadius, centerY - outerRimRadius)
    
    // Inner edge calculation
    // Inner edge radius is mainRadius - currentStrokeWidth / 2
    // We want to draw a rim *outside* (towards center of stroke) this inner edge.
    // Rim path radius = (mainRadius - currentStrokeWidth / 2) + rimWidth / 2
    val innerRimRadius = mainRadius - (currentStrokeWidth / 2) + (rimWidth / 2)
    val innerRimSize = Size(innerRimRadius * 2, innerRimRadius * 2)
    val innerRimTopLeft = Offset(centerX - innerRimRadius, centerY - innerRimRadius)

    // 2. OUTER GLOW (Refraction on outer edge)
    drawArc(
        color = Color.White.copy(alpha = if (isSelected) 0.6f else 0.4f),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = outerRimTopLeft,
        size = outerRimSize,
        style = Stroke(
            width = rimWidth,
            cap = StrokeCap.Butt
        )
    )
    
    // 3. INNER GLOW (Refraction on inner edge)
    drawArc(
        color = Color.White.copy(alpha = if (isSelected) 0.4f else 0.2f),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = innerRimTopLeft,
        size = innerRimSize,
        style = Stroke(
            width = rimWidth,
            cap = StrokeCap.Butt
        )
    )
    
    // 4. DYNAMIC SHINE (Subtle gradient overlay, but not as a stroke line)
    // We use a large, soft gradient over the whole arc to give it "volume" without lines
    val arcCenterAngle = startAngle + sweepAngle / 2
    val arcCenterRad = arcCenterAngle * PI.toFloat() / 180f
    
    // Shine direction
    val shineStartX = centerX + cos(arcCenterRad - PI.toFloat() / 4) * mainRadius
    val shineStartY = centerY + sin(arcCenterRad - PI.toFloat() / 4) * mainRadius
    val shineEndX = centerX + cos(arcCenterRad + PI.toFloat() / 4) * mainRadius
    val shineEndY = centerY + sin(arcCenterRad + PI.toFloat() / 4) * mainRadius

    // Very subtle gradient overlay on the main body
    drawArc(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isSelected) 0.2f else 0.1f),
                Color.Transparent
            ),
            start = Offset(shineStartX, shineStartY),
            end = Offset(shineEndX, shineEndY)
        ),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(
            width = currentStrokeWidth, // Matches main body
            cap = StrokeCap.Butt
        )
    )
}
