package com.example.expencetrackerapp.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import org.intellij.lang.annotations.Language

    // Enhanced glass shader with subtle edge effects and realistic refraction
    @Language("AGSL")
    val GlassRefractionShader =
            """
    uniform shader composable;
    uniform float2 size;
    uniform float warpStrength;
    uniform float edgeThickness;
    
    half4 main(float2 coord) {
        // Normalize to UV space
        float2 uv = coord / size;
        
        // Distance from center
        float2 d = uv - 0.5;
        float r2 = dot(d, d);
        
        // Barrel distortion (convex lens effect)
        float f = 1.0 + r2 * (warpStrength * 2.0);
        float2 refractedUV = 0.5 + (d * f);
        
        // Edge detection for highlight
        float edgeDist = min(
            min(uv.x, 1.0 - uv.x),
            min(uv.y, 1.0 - uv.y)
        );
        float edgeFactor = smoothstep(0.0, edgeThickness, edgeDist);
        
        // Bounds check
        if (refractedUV.x < 0.0 || refractedUV.x > 1.0 || 
            refractedUV.y < 0.0 || refractedUV.y > 1.0) {
            return half4(0.0, 0.0, 0.0, 0.0); // Transparent if outside bounds
        }
        
        // Sample refracted background
        half4 refracted = composable.eval(refractedUV * size);
        
        // Add subtle edge highlight (fresnel-like)
        // More subtle power curve for smoother falloff
        float fresnel = pow(1.0 - edgeFactor, 4.0) * 0.15;
        refracted.rgb += fresnel;
        
        // BRIGHTNESS BOOST - This is key for the "Glass" look.
        // Glass collects light, so the content behind it should look slightly brighter, not darker.
        refracted.rgb *= 1.15;
        
        // Optional: Mix in a tiny bit of white to simulate surface reflection/milkiness
        refracted.rgb = mix(refracted.rgb, half3(1.0), 0.05);

        return refracted;
    }
"""

    @Composable
    fun GlassRefractiveBox(
            modifier: Modifier = Modifier,
            shape: Shape = RoundedCornerShape(16.dp),
            warpStrength: Float = 0.03f, // Much lower for subtle distortion
            edgeThickness: Float = 0.1f, 
            content: @Composable BoxScope.() -> Unit
    ) {
    // Only apply shader on Android 13+ (Tiramisu)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GlassRefractiveBoxHighApi(modifier, shape, warpStrength, edgeThickness, content)
    } else {
        // Fallback for older devices: Simple semi-transparent box
        Box(
                modifier =
                        modifier.clip(shape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), shape),
                content = content
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun GlassRefractiveBoxHighApi(
        modifier: Modifier,
        shape: Shape,
        warpStrength: Float,
        edgeThickness: Float,
        content: @Composable BoxScope.() -> Unit
) {
    val shader = remember { RuntimeShader(GlassRefractionShader) }
    var size by remember { mutableStateOf(Size.Zero) }

    Box(
            modifier =
                    modifier
                            .onSizeChanged { size = it.toSize() }
                            .graphicsLayer {
                                if (size.width > 0 && size.height > 0) {
                                    shader.setFloatUniform("size", size.width, size.height)
                                    shader.setFloatUniform("warpStrength", warpStrength)
                                    shader.setFloatUniform("edgeThickness", edgeThickness)
                                    renderEffect =
                                            RenderEffect.createRuntimeShaderEffect(
                                                            shader,
                                                            "composable"
                                                    )
                                                    .asComposeRenderEffect()
                                }
                                clip = true
                                this.shape = shape
                                // shadowElevation = 8.dp.toPx() // Optional shadow
                            }
                            .background(
                                    brush =
                                            Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    // Much more transparent!
                                                                    // Top is very faint white
                                                                    Color.White.copy(alpha = 0.08f),
                                                                    // Bottom is almost clear
                                                                    Color.White.copy(alpha = 0.01f)
                                                            )
                                            ),
                                    shape = shape
                            )
                            .border(
                                    width = 1.dp,
                                    brush =
                                            Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    // Thin crisp rim light at the top
                                                                    Color.White.copy(alpha = 0.3f),
                                                                    // Fades out at the bottom
                                                                    Color.White.copy(alpha = 0.05f)
                                                            )
                                            ),
                                    shape = shape
                            )
    ) { content() }
}
