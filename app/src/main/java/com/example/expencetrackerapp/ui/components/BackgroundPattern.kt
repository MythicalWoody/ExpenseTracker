package com.example.expencetrackerapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * WhatsApp-style background pattern with random geometric shapes Adapts to light and dark themes
 * automatically
 */
@Composable
fun BackgroundPattern(modifier: Modifier = Modifier, isDarkTheme: Boolean = isSystemInDarkTheme()) {
    // Pattern color based on theme - increased opacity for visibility
    val patternColor =
        if (isDarkTheme) {
            Color.White.copy(alpha = 0.08f)
        } else {
            Color.Black.copy(alpha = 0.12f)
        }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Use Canvas actual size instead of screen configuration
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Generate shapes based on actual canvas size
        val shapes = generateShapes(
            screenWidthPx = canvasWidth,
            screenHeightPx = canvasHeight,
            seed = 100 // Fixed seed for consistent pattern
        )

        shapes.forEach { shape ->
            when (shape) {
                is ShapeData.Triangle -> drawTriangle(shape, patternColor)
                is ShapeData.Circle -> drawCircle(shape, patternColor)
                is ShapeData.ChatBubble -> drawChatBubble(shape, patternColor)
                is ShapeData.Star -> drawStar(shape, patternColor)
            }
        }
    }
}

/** Generate random shapes for the pattern */
private fun generateShapes(
        screenWidthPx: Float,
        screenHeightPx: Float,
        seed: Int
): List<ShapeData> {
    val random = Random(seed)
    val shapes = mutableListOf<ShapeData>()
    val shapeCount = 150 // Significantly increased density

    repeat(shapeCount) {
        val x = random.nextFloat() * screenWidthPx
        val y = random.nextFloat() * screenHeightPx
        val size = random.nextFloat() * 50f + 25f // Size between 25-75
        val rotation = random.nextFloat() * 360f

        // Randomly choose shape type
        when (random.nextInt(4)) {
            0 -> shapes.add(ShapeData.Triangle(x, y, size, rotation))
            1 -> shapes.add(ShapeData.Circle(x, y, size))
            2 -> shapes.add(ShapeData.ChatBubble(x, y, size, rotation))
            3 -> shapes.add(ShapeData.Star(x, y, size, rotation))
        }
    }

    return shapes
}

/** Sealed class representing different shape types */
private sealed class ShapeData {
    data class Triangle(val x: Float, val y: Float, val size: Float, val rotation: Float) :
            ShapeData()
    data class Circle(val x: Float, val y: Float, val size: Float) : ShapeData()
    data class ChatBubble(val x: Float, val y: Float, val size: Float, val rotation: Float) :
            ShapeData()
    data class Star(val x: Float, val y: Float, val size: Float, val rotation: Float) : ShapeData()
}

/** Draw a triangle shape */
private fun DrawScope.drawTriangle(triangle: ShapeData.Triangle, color: Color) {
    rotate(triangle.rotation, pivot = Offset(triangle.x, triangle.y)) {
        val path =
                Path().apply {
                    moveTo(triangle.x, triangle.y - triangle.size / 2)
                    lineTo(triangle.x - triangle.size / 2, triangle.y + triangle.size / 2)
                    lineTo(triangle.x + triangle.size / 2, triangle.y + triangle.size / 2)
                    close()
                }
        drawPath(path = path, color = color, style = Stroke(width = 1.5f))
    }
}

/** Draw a circle shape */
private fun DrawScope.drawCircle(circle: ShapeData.Circle, color: Color) {
    drawCircle(
            color = color,
            radius = circle.size / 2,
            center = Offset(circle.x, circle.y),
            style = Stroke(width = 1.5f)
    )
}

/** Draw a chat bubble shape (rounded rectangle with tail) */
private fun DrawScope.drawChatBubble(bubble: ShapeData.ChatBubble, color: Color) {
    rotate(bubble.rotation, pivot = Offset(bubble.x, bubble.y)) {
        val path =
                Path().apply {
                    // Main bubble body (rounded rectangle)
                    val left = bubble.x - bubble.size / 2
                    val top = bubble.y - bubble.size / 2
                    val right = bubble.x + bubble.size / 2
                    val bottom = bubble.y + bubble.size / 2
                    val cornerRadius = bubble.size / 6

                    // Top left
                    moveTo(left + cornerRadius, top)
                    // Top right
                    lineTo(right - cornerRadius, top)
                    quadraticBezierTo(right, top, right, top + cornerRadius)
                    // Bottom right
                    lineTo(right, bottom - cornerRadius)
                    quadraticBezierTo(right, bottom, right - cornerRadius, bottom)
                    // Bottom left (with tail)
                    lineTo(left + cornerRadius, bottom)
                    // Small tail
                    lineTo(left - bubble.size / 6, bottom + bubble.size / 8)
                    lineTo(left + cornerRadius, bottom)
                    quadraticBezierTo(left, bottom, left, bottom - cornerRadius)
                    // Left side
                    lineTo(left, top + cornerRadius)
                    quadraticBezierTo(left, top, left + cornerRadius, top)
                    close()
                }
        drawPath(path = path, color = color, style = Stroke(width = 1.5f))
    }
}

/** Draw a star shape */
private fun DrawScope.drawStar(star: ShapeData.Star, color: Color) {
    rotate(star.rotation, pivot = Offset(star.x, star.y)) {
        val path = Path()
        val outerRadius = star.size / 2
        val innerRadius = outerRadius * 0.4f
        val points = 5
        val angleStep = Math.PI / points

        for (i in 0 until points * 2) {
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val angle = i * angleStep - Math.PI / 2
            val x = star.x + (radius * cos(angle)).toFloat()
            val y = star.y + (radius * sin(angle)).toFloat()

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()

        drawPath(path = path, color = color, style = Stroke(width = 1.5f))
    }
}

@Preview(name = "Light Theme", showBackground = true)
@Composable
private fun BackgroundPatternLightPreview() {
    BackgroundPattern(isDarkTheme = false)
}

@Preview(name = "Dark Theme", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BackgroundPatternDarkPreview() {
    BackgroundPattern(isDarkTheme = true)
}
