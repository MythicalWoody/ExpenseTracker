package com.example.expencetrackerapp.ui.components

import android.graphics.RuntimeShader
import android.os.Build
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.expencetrackerapp.util.LocalDeviceRotation
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import org.intellij.lang.annotations.Language
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

// Optimized Film Grain Shader
@Language("AGSL")
val NoiseShader = """
    uniform float2 size;
    uniform float alpha;
    
    // Gold Noise function for better distribution than standard random
    float hash(float2 xy) {
        return fract(tan(distance(xy * 1.61803398874989484820459, xy) * xy.x) * xy.y);
    }

    half4 main(float2 coord) {
        float noise = hash(coord);
        // Output white noise with controlled alpha
        return half4(1.0, 1.0, 1.0, noise * alpha);
    }
"""

@Composable
fun GlassRefractiveBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    lightSource: LightSourceDirection = LightSourceDirection.TOP_LEFT,
    glareStrength: Float = 0.20f, // Controls surface reflection opacity (0.0 - 1.0)
    rimStrength: Float = 1.0f,    // Controls edge light opacity (0.0 - 1.0)
    rimWidth: Dp = 2.dp,          // Controls edge light thickness
    content: @Composable BoxScope.() -> Unit
) {
    val hazeState = LocalHazeState.current
    val (pitch, roll) = LocalDeviceRotation.current

    val noiseShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(NoiseShader)
        } else {
            null
        }
    }

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
                                tint = HazeTint(Color.White.copy(alpha = 0.03f))
                            )
                        )
                        .drawWithContent {
                            // Draw content first
                            drawContent()
                            
                            // Re-enable noise for texture
                            if (noiseShader != null) {
                                noiseShader.setFloatUniform("size", size.width, size.height)
                                noiseShader.setFloatUniform("alpha", 0.02f)
                                drawRect(ShaderBrush(noiseShader), blendMode = BlendMode.Overlay)
                            }

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

                            drawOutline(
                                outline = outline,
                                style = Stroke(width = rimWidth.toPx()), // Configurable width
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = rimStrength.coerceIn(0f, 1f)), // Configurable peak
                                        Color.White.copy(alpha = (rimStrength * 0.3f).coerceIn(0f, 1f)), // Stronger mid-tone
                                        Color.Black.copy(alpha = 0.15f)  // Dark shadow on the opposite side
                                    ),
                                    start = Offset(rimStartX, rimStartY),
                                    end = Offset(rimEndX, rimEndY)
                                )
                            )
                            
                            // 3. INNER BEVEL HIGHLIGHT (Fake 3D Thickness)
                             drawOutline(
                                outline = outline,
                                style = Stroke(width = 1.dp.toPx()),
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.05f),
                                        Color.Transparent, // Removed white from shadow side
                                        Color.Transparent
                                    ),
                                    start = Offset(rimEndX, rimEndY), // Inverted direction
                                    end = Offset(rimStartX, rimStartY)
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
