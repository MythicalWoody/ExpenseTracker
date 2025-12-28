package com.example.expencetrackerapp.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.expencetrackerapp.util.LocalDeviceRotation
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

val LocalHazeState = compositionLocalOf<HazeState?> { null }

/**
 * Defines the direction of the virtual light source for glass refraction effects.
 */
enum class LightSourceDirection(val angleRad: Float) {
    TOP_LEFT(-3f * PI.toFloat() / 4f),     // -135 degrees
    TOP(-PI.toFloat() / 2f),               // -90 degrees
    TOP_RIGHT(-PI.toFloat() / 4f),         // -45 degrees
    RIGHT(0f),                             // 0 degrees
    BOTTOM_RIGHT(PI.toFloat() / 4f),       // 45 degrees
    BOTTOM(PI.toFloat() / 2f),             // 90 degrees
    BOTTOM_LEFT(3f * PI.toFloat() / 4f),   // 135 degrees
    LEFT(PI.toFloat())                     // 180 degrees
}

@Composable
fun GlassRefractiveBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    lightSource: LightSourceDirection = LightSourceDirection.TOP_LEFT,
    glareStrength: Float = 0.20f, // Controls surface reflection opacity (0.0 - 1.0)
    rimStrength: Float = 1.0f,    // Controls edge light opacity (0.0 - 1.0)
    rimWidth: Dp = 2.dp,          // Controls edge light thickness
    glassThickness: Dp = 6.dp,    // Controls apparent thickness (volume)
    content: @Composable BoxScope.() -> Unit
) {
    val hazeState = LocalHazeState.current
    val (pitch, roll) = LocalDeviceRotation.current

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (hazeState != null) {
                    Modifier
                        .hazeEffect(
                            state = hazeState,
                            style = HazeStyle(
                                blurRadius = 40.dp,
                                tint = HazeTint(Color.White.copy(alpha = 0.03f)),
                                noiseFactor = 0.02f // Use Haze's built-in noise
                            )
                        )
                        .drawWithContent {
                            // Draw content first
                            drawContent()
                            
                            // ---------------------------------------------------------------------
                            // CONFIGURATION: LIGHT SOURCE ANGLE
                            // ---------------------------------------------------------------------
                            val lightSourceBaseAngle = lightSource.angleRad
                            
                            // 1. DYNAMIC SURFACE SHEEN (Specular Reflection)
                            // Increased sensitivity (3.0f) to make the glare movement hyper-evident
                            // This exaggerates the effect so it doesn't look like a static physical reflection
                            val glareAngle = lightSourceBaseAngle + (roll * 3.0f) - (pitch * 3.0f)
                            
                            val width = size.width
                            val height = size.height
                            val diagonal = sqrt(width * width + height * height)
                            
                            val glareStartX = (width / 2) + cos(glareAngle) * diagonal * 0.8f
                            val glareStartY = (height / 2) + sin(glareAngle) * diagonal * 0.8f
                            val glareEndX = (width / 2) - cos(glareAngle) * diagonal * 0.8f
                            val glareEndY = (height / 2) - sin(glareAngle) * diagonal * 0.8f

                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = glareStrength.coerceIn(0f, 1f)), // Configurable glare
                                        Color.White.copy(alpha = (glareStrength * 0.3f).coerceIn(0f, 1f)),  // Softer falloff
                                        Color.Transparent
                                    ),
                                    start = Offset(glareStartX, glareStartY),
                                    end = Offset(glareEndX, glareEndY)
                                )
                            )
                            
                            // 2. DYNAMIC EDGE LIGHTING (Fresnel Effect / Rim Light)
                            val outline = shape.createOutline(size, layoutDirection, this)
                            
                            // Increased rim sensitivity (1.5f) so the edge light travels further on tilt
                            val rimAngle = lightSourceBaseAngle + (roll * 1.5f) - (pitch * 1.5f)
                            
                            val rimStartX = (width / 2) + cos(rimAngle) * diagonal
                            val rimStartY = (height / 2) + sin(rimAngle) * diagonal
                            val rimEndX = (width / 2) - cos(rimAngle) * diagonal
                            val rimEndY = (height / 2) - sin(rimAngle) * diagonal

                            // 3. GLASS THICKNESS / VOLUME SIMULATION
                            // We use soft, highly blurred strokes to create a seamless transition.
                            // The key is high blur radius relative to thickness to avoid "banding" or sharp edges.

                            val thicknessPx = glassThickness.toPx()
                            // Increase blur radius to be larger than the thickness itself for ultra-smooth blend
                            val blurRadius = thicknessPx * 1.2f 

                            // A) VOLUME FILL (Soft inner gradient)
                            // A wide, soft wash that defines the glass body.
                            // We reduce alpha and increase blur to make it "foggy" rather than "lined".
                            drawBlurredBorder(
                                outline = outline,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.08f), // Lower alpha for subtlety
                                        Color.White.copy(alpha = 0.02f),
                                        Color.Black.copy(alpha = 0.05f)  // Very subtle shadow
                                    ),
                                    start = Offset(rimStartX, rimStartY),
                                    end = Offset(rimEndX, rimEndY)
                                ),
                                strokeWidth = thicknessPx * 3.0f, // Wider stroke to cover more area smoothly
                                blurRadius = blurRadius
                            )

                            // B) EXIT LIGHT / CAUSTIC (Soft back reflection)
                            // This mimics the light catching the inner bevel on the far side.
                            drawBlurredBorder(
                                outline = outline,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent, 
                                        Color.White.copy(alpha = 0.05f),
                                        Color.White.copy(alpha = 0.3f) // Soft but visible exit light
                                    ),
                                    start = Offset(rimStartX, rimStartY),
                                    end = Offset(rimEndX, rimEndY)
                                ),
                                strokeWidth = thicknessPx * 3.0f,
                                blurRadius = blurRadius
                            )

                            // C) INNER GLOW (Ambient Refraction)
                            // An additional very soft pass to bind the layers together.
                            drawBlurredBorder(
                                outline = outline,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.05f),
                                        Color.Transparent
                                    ),
                                    start = Offset(rimStartX, rimStartY),
                                    end = Offset(rimEndX, rimEndY)
                                ),
                                strokeWidth = thicknessPx * 4.0f,
                                blurRadius = thicknessPx * 2.0f // Very high blur for ambient feel
                            )

                            // 4. SHARP OUTER RIM
                            // Kept crisp to define the physical boundary.
                            drawOutline(
                                outline = outline,
                                style = Stroke(width = rimWidth.toPx()),
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = rimStrength.coerceIn(0f, 1f)),
                                        Color.White.copy(alpha = (rimStrength * 0.3f).coerceIn(0f, 1f)),
                                        Color.Black.copy(alpha = 0.1f)
                                    ),
                                    start = Offset(rimStartX, rimStartY),
                                    end = Offset(rimEndX, rimEndY)
                                )
                            )
                        }
                } else {
                    Modifier
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), shape)
                }
            ),
        content = content
    )
}

/**
 * Helper to draw a blurred border for soft depth effects.
 * This softens the inner edge of the stroke to merge seamlessly with the content.
 */
fun DrawScope.drawBlurredBorder(
    outline: Outline,
    brush: Brush,
    strokeWidth: Float,
    blurRadius: Float
) {
    drawIntoCanvas { canvas ->
        val paint = Paint()
        paint.style = PaintingStyle.Stroke
        paint.strokeWidth = strokeWidth
        
        // Apply brush to paint
        brush.applyTo(size, paint, 1.0f)
        
        // Apply blur to native paint
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        
        when (outline) {
            is Outline.Generic -> canvas.drawPath(outline.path, paint)
            is Outline.Rounded -> {
                val path = Path().apply { addRoundRect(outline.roundRect) }
                canvas.drawPath(path, paint)
            }
            is Outline.Rectangle -> {
                val path = Path().apply { addRect(outline.rect) }
                canvas.drawPath(path, paint)
            }
        }
    }
}
